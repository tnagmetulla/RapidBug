package com.tnagmetulla.rapidbug.model

import com.google.gson.annotations.SerializedName

// Модель для создания новой задачи в Jira
data class CreateIssueRequest(
    val fields: IssueFields
)

data class IssueFields(
    val project: ProjectKey,
    val summary: String,
    val description: AtlassianDocument? = null,
    val issuetype: IssueType,
    val labels: List<String>? = null
)

data class AtlassianDocument(
    val version: Int = 1,
    val type: String = "doc",
    val content: List<ContentNode> = emptyList()
)

data class ContentNode(
    val type: String,
    val content: List<TextNode>? = null
)

data class TextNode(
    val type: String = "text",
    val text: String
)

data class ProjectKey(
    val id: String
)

data class IssueType(
    val name: String
)

// Модель ответа при создании задачи
data class CreateIssueResponse(
    val id: String,
    val key: String,
    val self: String
)

// Модель для получения информации о проекте
data class Project(
    val key: String,
    val name: String,
    val id: String
)

// Модель для получения списка проектов
data class ProjectsResponse(
    val values: List<Project>
)

// Модель для добавления вложения (уже содержит файловый контент)
data class AttachmentRequest(
    val fileName: String,
    val fileContent: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentRequest

        if (fileName != other.fileName) return false
        if (!fileContent.contentEquals(other.fileContent)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileContent.contentHashCode()
        return result
    }
}

// Модель для ответа при добавлении вложения
data class AttachmentResponse(
    val id: String,
    val filename: String,
    val created: String,
    val size: Int
)