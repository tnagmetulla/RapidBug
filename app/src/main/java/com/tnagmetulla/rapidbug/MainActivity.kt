package com.tnagmetulla.rapidbug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tnagmetulla.rapidbug.api.JiraService
import com.tnagmetulla.rapidbug.data.SettingsManager
import com.tnagmetulla.rapidbug.ui.screens.SettingsScreen
import com.tnagmetulla.rapidbug.ui.theme.RapidBugTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var jiraService: JiraService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsManager = SettingsManager(this)
        jiraService = JiraService(this)

        setContent {
            RapidBugTheme {
                SettingsScreen(
                    settingsManager = settingsManager,
                    jiraService = jiraService,
                    onSettingsSaved = {
                        // Settings saved
                    }
                )
            }
        }
    }
}