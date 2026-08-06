package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.LocalFolderSyncConfig
import com.example.data.model.WebDavConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsBottomSheet(
    webDavConfig: WebDavConfig,
    localSyncConfig: LocalFolderSyncConfig,
    onSaveWebDav: (WebDavConfig) -> Unit,
    onSaveLocalSync: (LocalFolderSyncConfig) -> Unit,
    onTestWebDav: (WebDavConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var serverUrl by remember { mutableStateOf(webDavConfig.serverUrl) }
    var username by remember { mutableStateOf(webDavConfig.username) }
    var authKey by remember { mutableStateOf(webDavConfig.authKey) }
    var remotePath by remember { mutableStateOf(webDavConfig.remotePath) }
    var isWebDavEnabled by remember { mutableStateOf(webDavConfig.isEnabled) }

    var localFolderPath by remember { mutableStateOf(localSyncConfig.customFolderPath) }
    var isLocalEnabled by remember { mutableStateOf(localSyncConfig.isAutoExportEnabled) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Cloud & Local Sync Destinations",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F)
                )
            }

            Text(
                text = "Automatically back up converted PDFs to local storage or WebDAV cloud servers (Nextcloud, NAS, ownCloud).",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F)
            )

            // LOCAL FOLDER SYNC SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Local Auto-Export Folder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Switch(
                            checked = isLocalEnabled,
                            onCheckedChange = { isLocalEnabled = it }
                        )
                    }

                    if (isLocalEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = localFolderPath,
                            onValueChange = { localFolderPath = it },
                            label = { Text("Local Destination Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // WEBDAV CLOUD SYNC SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WebDAV Cloud Storage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Switch(
                            checked = isWebDavEnabled,
                            onCheckedChange = { isWebDavEnabled = it }
                        )
                    }

                    if (isWebDavEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL (e.g., https://nas.local/dav)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = authKey,
                            onValueChange = { authKey = it },
                            label = { Text("Password or App Token") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = remotePath,
                            onValueChange = { remotePath = it },
                            label = { Text("Remote Folder Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                onTestWebDav(
                                    WebDavConfig(
                                        serverUrl = serverUrl,
                                        username = username,
                                        authKey = authKey,
                                        remotePath = remotePath,
                                        isEnabled = true
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_webdav_connection_button")
                        ) {
                            Text("Test WebDAV Server Connection")
                        }
                    }
                }
            }

            // SAVE ACTIONS
            Button(
                onClick = {
                    onSaveLocalSync(
                        LocalFolderSyncConfig(
                            customFolderPath = localFolderPath,
                            isAutoExportEnabled = isLocalEnabled
                        )
                    )
                    onSaveWebDav(
                        WebDavConfig(
                            serverUrl = serverUrl,
                            username = username,
                            authKey = authKey,
                            remotePath = remotePath,
                            isEnabled = isWebDavEnabled
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_sync_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Sync Destinations", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
