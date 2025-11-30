package com.tnagmetulla.rapidbug.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import com.tnagmetulla.rapidbug.R

@Composable
fun CreateBugDialog(
    projects: List<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (projectKey: String, title: String) -> Unit
) {
    val selectedProjectState = remember { mutableStateOf(projects.firstOrNull() ?: "") }
    val titleState = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_create_bug)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.label_select_project))
                Spacer(modifier = Modifier.height(8.dp))

                if (projects.isEmpty()) {
                    Text(stringResource(R.string.label_no_projects))
                } else {
                    projects.forEach { project ->
                        Button(
                            onClick = { selectedProjectState.value = project },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val icon = if (selectedProjectState.value == project) "✓" else "○"
                            Text("$icon $project")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text(stringResource(R.string.label_bug_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedProjectState.value, titleState.value)
                    onDismiss()
                },
                enabled = titleState.value.isNotEmpty() && selectedProjectState.value.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.button_create))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
fun AddAttachmentDialog(
    projects: List<String> = emptyList(),
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (issueKey: String) -> Unit
) {
    val selectedProjectState = remember { mutableStateOf(projects.firstOrNull() ?: "") }
    val issueNumberState = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_add_attachment)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (projects.isNotEmpty()) {
                    Text(stringResource(R.string.label_select_project))
                    Spacer(modifier = Modifier.height(8.dp))

                    projects.forEach { project ->
                        Button(
                            onClick = { selectedProjectState.value = project },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val icon = if (selectedProjectState.value == project) "✓" else "○"
                            Text("$icon $project")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(stringResource(R.string.label_enter_issue_number))
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = issueNumberState.value,
                    onValueChange = { newValue ->
                        // Разрешить только цифры
                        if (newValue.all { it.isDigit() }) {
                            issueNumberState.value = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.label_issue_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text(stringResource(R.string.placeholder_issue_number)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val issueKey = if (projects.isNotEmpty()) {
                        "${selectedProjectState.value}-${issueNumberState.value}"
                    } else {
                        issueNumberState.value
                    }
                    onConfirm(issueKey)
                    onDismiss()
                },
                enabled = issueNumberState.value.isNotEmpty() && selectedProjectState.value.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.button_add))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}