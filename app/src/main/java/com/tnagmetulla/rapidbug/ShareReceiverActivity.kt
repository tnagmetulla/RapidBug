package com.tnagmetulla.rapidbug

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tnagmetulla.rapidbug.api.JiraService
import com.tnagmetulla.rapidbug.data.SettingsManager
import com.tnagmetulla.rapidbug.ui.components.AddAttachmentDialog
import com.tnagmetulla.rapidbug.ui.components.CreateBugDialog
import com.tnagmetulla.rapidbug.ui.theme.RapidBugTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class ShareReceiverActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var jiraService: JiraService
    private var sharedFileUri: Uri? = null
    private var sharedFileName: String? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsManager = SettingsManager(this)
        jiraService = JiraService(this)

        // Инициализируем JiraService сохраненными настройками
        val settings = settingsManager.getSettings()
        if (settings.isConfigured) {
            jiraService.initialize(settings.email, settings.apiToken, settings.jiraUrl)
        }

        // Получаем общие данные из Intent
        if (intent.action == Intent.ACTION_SEND) {
            sharedFileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            sharedFileName = getFileName(sharedFileUri)
        }

        setContent {
            RapidBugTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val showCreateBugDialog = remember { mutableStateOf(false) }
                val showAddAttachmentDialog = remember { mutableStateOf(false) }
                val isLoading = remember { mutableStateOf(false) }
                val uploadProgress = remember { mutableStateOf(0f) }
                val settingsNotConfigured = remember { mutableStateOf(!settingsManager.isConfigured()) }

                // Проверяем файл при инициализации
                val fileError = remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    sharedFileUri?.let { uri ->
                        val fileSize = getFileSize(uri)
                        val validationResult = jiraService.validateFile(sharedFileName ?: "file", fileSize)
                        fileError.value = validationResult.getErrorMessage()
                    }
                }

                // Animate upload progress while loading
                LaunchedEffect(isLoading.value) {
                    if (isLoading.value) {
                        uploadProgress.value = 0f
                        while (isLoading.value && uploadProgress.value < 0.9f) {
                            uploadProgress.value += Random.nextFloat() * 0.2f + 0.1f
                            delay(500)
                        }
                        if (isLoading.value) {
                            uploadProgress.value = 0.9f
                        }
                    } else {
                        uploadProgress.value = 0f
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.title_share)) })
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (settingsNotConfigured.value) {
                            Text(stringResource(R.string.label_app_not_configured))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.button_close))
                            }
                        } else if (sharedFileUri == null) {
                            Text(stringResource(R.string.label_file_not_found))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.button_close))
                            }
                        } else if (fileError.value != null) {
                            Text("❌ ${fileError.value}")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.button_close))
                            }
                        } else {
                            Text(stringResource(R.string.label_select_action, sharedFileName.orEmpty()))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.label_supported_formats),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showCreateBugDialog.value = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading.value
                            ) {
                                Text(stringResource(R.string.button_create_bug))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showAddAttachmentDialog.value = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading.value
                            ) {
                                Text(stringResource(R.string.button_add_attachment))
                            }
                        }

                        if (isLoading.value) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { uploadProgress.value },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${(uploadProgress.value * 100).toInt()}%",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (showCreateBugDialog.value) {
                    CreateBugDialog(
                        projects = settingsManager.getSettings().selectedProjects,
                        isLoading = isLoading.value,
                        onDismiss = { showCreateBugDialog.value = false },
                        onConfirm = { projectKey, title ->
                            scope.launch {
                                isLoading.value = true
                                try {
                                    val result = jiraService.createIssue(
                                        projectKey = projectKey,
                                        summary = title,
                                        description = "Скриншот: $sharedFileName"
                                    )

                                    result.onSuccess { response ->
                                        // Теперь добавляем вложение к новой задаче
                                        val issueKey = response.key
                                        sharedFileUri?.let { uri ->
                                            val fileContent = readFileFromUri(uri)
                                            jiraService.addAttachment(
                                                issueKey,
                                                sharedFileName ?: "screenshot.png",
                                                fileContent
                                            ).onSuccess {
                                                snackbarHostState.showSnackbar(
                                                    "Баг создан: $issueKey",
                                                    duration = SnackbarDuration.Short
                                                )
                                                showCreateBugDialog.value = false
                                                // Закрываем после успеха
                                                finish()
                                            }.onFailure { error ->
                                                snackbarHostState.showSnackbar(
                                                    "Баг создан, но вложение не добавлено: ${error.message}",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    }.onFailure { error ->
                                        snackbarHostState.showSnackbar(
                                            "Ошибка создания бага: ${error.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                } finally {
                                    isLoading.value = false
                                }
                            }
                        }
                    )
                }

                if (showAddAttachmentDialog.value) {
                    AddAttachmentDialog(
                        projects = settingsManager.getSettings().selectedProjects,
                        isLoading = isLoading.value,
                        onDismiss = { showAddAttachmentDialog.value = false },
                        onConfirm = { issueKey ->
                            scope.launch {
                                isLoading.value = true
                                try {
                                    sharedFileUri?.let { uri ->
                                        val fileContent = readFileFromUri(uri)
                                        jiraService.addAttachment(
                                            issueKey,
                                            sharedFileName ?: "screenshot.png",
                                            fileContent
                                        ).onSuccess {
                                            snackbarHostState.showSnackbar(
                                                "Вложение добавлено к $issueKey",
                                                duration = SnackbarDuration.Short
                                            )
                                            showAddAttachmentDialog.value = false
                                            // Закрываем после успеха
                                            finish()
                                        }.onFailure { error ->
                                            snackbarHostState.showSnackbar(
                                                "Ошибка добавления вложения: ${error.message}",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                } finally {
                                    isLoading.value = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun getFileName(uri: Uri?): String {
        if (uri == null) return "unknown"
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        it.getString(it.getColumnIndexOrThrow("_display_name"))
                    } else "unknown"
                } ?: "unknown"
            }

            ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).name } ?: "unknown"
            else -> uri.lastPathSegment ?: "unknown"
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        it.getLong(it.getColumnIndexOrThrow("_size"))
                    } else 0L
                } ?: 0L
            }

            ContentResolver.SCHEME_FILE -> {
                uri.path?.let { File(it).length() } ?: 0L
            }

            else -> 0L
        }
    }

    private fun readFileFromUri(uri: Uri): ByteArray {
        return contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: byteArrayOf()
    }
}