package com.tnagmetulla.rapidbug.data

import android.content.Context
import android.content.SharedPreferences
import com.tnagmetulla.rapidbug.model.JiraSettings

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("jira_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_JIRA_URL = "jira_url"
        private const val KEY_SELECTED_PROJECTS = "selected_projects"
        private const val KEY_IS_CONFIGURED = "is_configured"
        private const val KEY_MAX_FILE_SIZE_MB = "max_file_size_mb"
        private const val KEY_AVAILABLE_PROJECTS = "available_projects"
    }

    fun saveSettings(settings: JiraSettings) {
        sharedPreferences.edit().apply {
            putString(KEY_EMAIL, settings.email)
            putString(KEY_API_TOKEN, settings.apiToken)
            putString(KEY_JIRA_URL, settings.jiraUrl)
            putStringSet(KEY_SELECTED_PROJECTS, settings.selectedProjects.toSet())
            putBoolean(KEY_IS_CONFIGURED, settings.isConfigured)
            putInt(KEY_MAX_FILE_SIZE_MB, settings.maxFileSizeMB)
            apply()
        }
    }

    fun getSettings(): JiraSettings {
        return JiraSettings(
            email = sharedPreferences.getString(KEY_EMAIL, "") ?: "",
            apiToken = sharedPreferences.getString(KEY_API_TOKEN, "") ?: "",
            jiraUrl = sharedPreferences.getString(KEY_JIRA_URL, "") ?: "",
            selectedProjects = sharedPreferences.getStringSet(KEY_SELECTED_PROJECTS, emptySet())?.toList() ?: emptyList(),
            isConfigured = sharedPreferences.getBoolean(KEY_IS_CONFIGURED, false),
            maxFileSizeMB = sharedPreferences.getInt(KEY_MAX_FILE_SIZE_MB, 100)
        )
    }

    fun clearSettings() {
        sharedPreferences.edit().clear().apply()
    }

    fun isConfigured(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_CONFIGURED, false)
    }

    fun saveAvailableProjects(projects: List<String>) {
        sharedPreferences.edit().putStringSet(KEY_AVAILABLE_PROJECTS, projects.toSet()).apply()
    }

    fun getAvailableProjects(): List<String> {
        return sharedPreferences.getStringSet(KEY_AVAILABLE_PROJECTS, emptySet())?.toList() ?: emptyList()
    }
}