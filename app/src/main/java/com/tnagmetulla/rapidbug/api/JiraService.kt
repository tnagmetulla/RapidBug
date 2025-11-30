package com.tnagmetulla.rapidbug.api

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import com.tnagmetulla.rapidbug.model.AtlassianDocument
import com.tnagmetulla.rapidbug.model.ContentNode
import com.tnagmetulla.rapidbug.model.CreateIssueRequest
import com.tnagmetulla.rapidbug.model.CreateIssueResponse
import com.tnagmetulla.rapidbug.model.IssueFields
import com.tnagmetulla.rapidbug.model.IssueType
import com.tnagmetulla.rapidbug.model.Project
import com.tnagmetulla.rapidbug.model.ProjectKey
import com.tnagmetulla.rapidbug.model.TextNode
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Base64.getEncoder

class JiraService(private val context: Context) {

    companion object {
        private const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100MB
        private val SUPPORTED_EXTENSIONS = listOf("png", "jpg", "jpeg", "gif", "txt", "mp4")
    }

    private var jiraApi: JiraApi? = null
    private var email: String = ""
    private var apiToken: String = ""
    private var jiraUrl: String = ""

    fun initialize(email: String, apiToken: String, jiraUrl: String) {
        this.email = email
        this.apiToken = apiToken
        this.jiraUrl = jiraUrl.removeSuffix("/")

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("$jiraUrl/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()

        jiraApi = retrofit.create(JiraApi::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAuthHeader(): String {
        val credentials = "$email:$apiToken"
        val encoded = getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getProjects(): Result<List<Project>> {
        return try {
            if (jiraApi == null) {
                Result.failure(Exception("Jira сервис не инициализирован"))
            } else {
                val response = jiraApi!!.getProjects(getAuthHeader())

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.values)
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Ошибка аутентификации. Проверьте email и API token."
                        403 -> "Доступ запрещен. У вас нет доступа к проектам."
                        else -> "Ошибка загрузки проектов: ${response.code()} ${response.message()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createIssue(
        projectKey: String,
        summary: String,
        description: String? = null,
        issueType: String = "Bug"
    ): Result<CreateIssueResponse> {
        return try {
            if (jiraApi == null) {
                Result.failure<CreateIssueResponse>(Exception("Jira сервис не инициализирован"))
            } else {
                // Получаем ID проекта по его ключу
                val projectsResult = getProjects()
                val projectId = projectsResult.getOrNull()?.find { it.key == projectKey }?.id

                if (projectId == null) {
                    return Result.failure(Exception("Проект '$projectKey' не найден. Попробуйте выбрать другой проект."))
                }

                val descriptionDoc = if (description != null) {
                    AtlassianDocument(
                        content = listOf(
                            ContentNode(
                                type = "paragraph",
                                content = listOf(
                                    TextNode(text = description)
                                )
                            )
                        )
                    )
                } else {
                    null
                }

                val request = CreateIssueRequest(
                    fields = IssueFields(
                        project = ProjectKey(projectId),
                        summary = summary,
                        description = descriptionDoc,
                        issuetype = IssueType(issueType)
                    )
                )

                val response = jiraApi!!.createIssue(getAuthHeader(), request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Некорректные данные для создания задачи. Проверьте название и выбранный проект."
                        401 -> "Ошибка аутентификации. Проверьте email и API token."
                        403 -> "Доступ запрещен. У вас нет прав на создание задач в этом проекте."
                        else -> "Ошибка создания задачи: ${response.code()} ${response.message()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addAttachment(
        issueKey: String,
        fileName: String,
        fileContent: ByteArray
    ): Result<Unit> {
        return try {
            if (jiraApi == null) {
                Result.failure<Unit>(Exception("Jira сервис не инициализирован"))
            } else {
                val requestBody = fileContent.toRequestBody()
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                val response = jiraApi!!.addAttachment(getAuthHeader(), issueKey, part, "no-check")

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMessage = when (response.code()) {
                        404 -> "Задача '$issueKey' не найдена. Проверьте ID задачи и доступ к проекту."
                        401 -> "Ошибка аутентификации. Проверьте email и API token."
                        403 -> "Доступ запрещен. У вас нет прав на добавление вложений к этой задаче."
                        413 -> "Файл слишком большой. Максимальный размер обычно 20MB."
                        else -> "Ошибка добавления вложения: ${response.code()} ${response.message()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isInitialized(): Boolean {
        return email.isNotEmpty() && apiToken.isNotEmpty() && jiraUrl.isNotEmpty()
    }

    fun validateFile(fileName: String, fileSize: Long): FileValidationResult {
        // Проверяем размер файла
        if (fileSize > MAX_FILE_SIZE) {
            val sizeMb = fileSize / (1024 * 1024)
            val maxMb = MAX_FILE_SIZE / (1024 * 1024)
            return FileValidationResult.FileTooLarge(
                "Файл слишком большой: ${sizeMb}MB. Максимально допустимый размер: ${maxMb}MB"
            )
        }

        // Проверяем расширение файла
        val extension = fileName.substringAfterLast(".").lowercase()
        if (extension.isEmpty() || !SUPPORTED_EXTENSIONS.contains(extension)) {
            val supported = SUPPORTED_EXTENSIONS.joinToString(", ")
            return FileValidationResult.UnsupportedFormat(
                "Неподдерживаемый формат файла. Поддерживаемые: $supported"
            )
        }

        return FileValidationResult.Valid
    }
}

sealed class FileValidationResult {
    object Valid : FileValidationResult()
    data class FileTooLarge(val message: String) : FileValidationResult()
    data class UnsupportedFormat(val message: String) : FileValidationResult()

    fun getErrorMessage(): String? = when (this) {
        is FileTooLarge -> message
        is UnsupportedFormat -> message
        is Valid -> null
    }
}