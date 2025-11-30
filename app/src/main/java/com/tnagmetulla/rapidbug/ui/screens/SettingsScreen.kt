package com.tnagmetulla.rapidbug.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.tnagmetulla.rapidbug.R
import com.tnagmetulla.rapidbug.api.JiraService
import com.tnagmetulla.rapidbug.data.SettingsManager
import com.tnagmetulla.rapidbug.model.JiraSettings
import com.tnagmetulla.rapidbug.ui.theme.RapidBugTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    jiraService: JiraService,
    onSettingsSaved: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val emailState = remember { mutableStateOf("") }
    val apiTokenState = remember { mutableStateOf("") }
    val jiraUrlState = remember { mutableStateOf("") }
    val isLoadingState = remember { mutableStateOf(false) }
    val selectedProjectsState = remember { mutableStateOf(setOf<String>()) }
    val availableProjectsState = remember { mutableStateOf(emptyList<String>()) }
    val maxFileSizeState = remember { mutableStateOf("100") }

    val expandedCredentials = remember { mutableStateOf(false) }
    val expandedProjects = remember { mutableStateOf(false) }
    val expandedFileSettings = remember { mutableStateOf(false) }
    val showClearDialog = remember { mutableStateOf(false) }
    val securityTooltip = stringResource(R.string.security_tooltip)

    LaunchedEffect(Unit) {
        val settings = settingsManager.getSettings()
        emailState.value = settings.email
        apiTokenState.value = settings.apiToken
        jiraUrlState.value = settings.jiraUrl
        selectedProjectsState.value = settings.selectedProjects.toSet()
        maxFileSizeState.value = settings.maxFileSizeMB.toString()
        availableProjectsState.value = settingsManager.getAvailableProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_settings)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Welcome header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Block 1: Jira Credentials
            ExpandableSection(
                title = stringResource(R.string.section_jira_credentials),
                expanded = expandedCredentials.value,
                onToggle = { expandedCredentials.value = !expandedCredentials.value }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = emailState.value,
                        onValueChange = { emailState.value = it },
                        label = { Text(stringResource(R.string.label_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isLoadingState.value
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = apiTokenState.value,
                            onValueChange = { apiTokenState.value = it },
                            label = { Text(stringResource(R.string.label_api_token)) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !isLoadingState.value
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        securityTooltip,
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Security info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = jiraUrlState.value,
                        onValueChange = { jiraUrlState.value = it },
                        label = { Text(stringResource(R.string.label_jira_url)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        enabled = !isLoadingState.value
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingState.value = true
                                val errorMsg = try {
                                    jiraService.initialize(
                                        emailState.value,
                                        apiTokenState.value,
                                        jiraUrlState.value
                                    )

                                    val projectsResult = jiraService.getProjects()
                                    var errMsg: String? = null
                                    projectsResult.onSuccess { projects ->
                                        val projectKeys = projects.map { it.key }
                                        availableProjectsState.value = projectKeys
                                        settingsManager.saveAvailableProjects(projectKeys)
                                        expandedProjects.value = true
                                    }.onFailure { error ->
                                        errMsg = error.message ?: "Unknown error"
                                    }
                                    errMsg
                                } catch (e: Exception) {
                                    e.message ?: "Unknown error"
                                } finally {
                                    isLoadingState.value = false
                                }

                                if (errorMsg != null) {
                                    snackbarHostState.showSnackbar(
                                        "Ошибка: $errorMsg",
                                        duration = SnackbarDuration.Long
                                    )
                                } else if (availableProjectsState.value.isNotEmpty()) {
                                    snackbarHostState.showSnackbar(
                                        "Проекты загружены успешно",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = emailState.value.isNotEmpty() && apiTokenState.value.isNotEmpty() && jiraUrlState.value.isNotEmpty() && !isLoadingState.value
                    ) {
                        if (isLoadingState.value) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text(stringResource(R.string.button_load_projects))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Block 2: Projects
            ExpandableSection(
                title = stringResource(R.string.section_projects),
                expanded = expandedProjects.value,
                onToggle = { expandedProjects.value = !expandedProjects.value }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (availableProjectsState.value.isEmpty()) {
                        Text(
                            stringResource(R.string.hint_load_projects_first),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Text(
                            stringResource(R.string.label_select_projects),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableProjectsState.value.forEach { projectKey ->
                                val isSelected = projectKey in selectedProjectsState.value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedProjectsState.value = if (isSelected) {
                                            selectedProjectsState.value - projectKey
                                        } else {
                                            selectedProjectsState.value + projectKey
                                        }
                                    },
                                    label = { Text(projectKey) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Block 3: File Settings
            ExpandableSection(
                title = stringResource(R.string.section_file_settings),
                expanded = expandedFileSettings.value,
                onToggle = { expandedFileSettings.value = !expandedFileSettings.value }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.label_max_file_size),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = maxFileSizeState.value,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                maxFileSizeState.value = newValue
                            }
                        },
                        label = { Text("MB") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isLoadingState.value
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        val fileSizeInt = maxFileSizeState.value.toIntOrNull() ?: 100
                        val settings = JiraSettings(
                            email = emailState.value,
                            apiToken = apiTokenState.value,
                            jiraUrl = jiraUrlState.value,
                            selectedProjects = selectedProjectsState.value.toList(),
                            isConfigured = emailState.value.isNotEmpty() && apiTokenState.value.isNotEmpty(),
                            maxFileSizeMB = fileSizeInt
                        )
                        settingsManager.saveSettings(settings)
                        snackbarHostState.showSnackbar(
                            "Настройки сохранены",
                            duration = SnackbarDuration.Short
                        )
                        onSettingsSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = emailState.value.isNotEmpty() && apiTokenState.value.isNotEmpty() && jiraUrlState.value.isNotEmpty()
            ) {
                Text(stringResource(R.string.button_save_settings))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clear button
            Button(
                onClick = { showClearDialog.value = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_clear_settings))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Clear confirmation dialog
    if (showClearDialog.value) {
        AlertDialog(
            onDismissRequest = { showClearDialog.value = false },
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsManager.clearSettings()
                        emailState.value = ""
                        apiTokenState.value = ""
                        jiraUrlState.value = ""
                        selectedProjectsState.value = emptySet()
                        availableProjectsState.value = emptyList()
                        maxFileSizeState.value = "100"
                        showClearDialog.value = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Все настройки удалены",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog.value = false }
                ) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    RapidBugTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "RapidBug",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Быстро делитесь скриншотами и видео с вашей Jira",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Учетные данные Jira")
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Collapse")
                    }
                }
            }
        }
    }
}