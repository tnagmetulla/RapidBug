package com.tnagmetulla.rapidbug.model

data class JiraSettings(
    val email: String = "",
    val apiToken: String = "",
    val jiraUrl: String = "",
    val selectedProjects: List<String> = emptyList(),
    val isConfigured: Boolean = false,
    val maxFileSizeMB: Int = 100
)