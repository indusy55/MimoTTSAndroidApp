package com.indusy55.mimottsapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.indusy55.mimottsapp.R
import com.indusy55.mimottsapp.ui.MimoViewModel

@Composable
fun SettingsScreen(viewModel: MimoViewModel, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- API 配置组 ---
        SettingsGroup(title = stringResource(R.string.settings_group_api)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text(stringResource(R.string.api_key_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(if (isKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                            }
                            IconButton(onClick = { 
                                clipboardManager.getText()?.let { viewModel.updateApiKey(it.text) }
                            }) {
                                Icon(Icons.Outlined.ContentPaste, null)
                            }
                        }
                    }
                )
                
                Text(
                    text = stringResource(R.string.api_key_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.xiaomimimo.com/console/api-keys"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Launch, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.get_api_key))
                }
            }
        }

        // --- 服务状态组 ---
        SettingsGroup(title = stringResource(R.string.settings_group_service)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.api_connectivity)) },
                supportingContent = { 
                    Text(when(viewModel.isApiOnline) {
                        true -> stringResource(R.string.service_online)
                        false -> stringResource(R.string.service_offline)
                        else -> stringResource(R.string.check_conn_tip)
                    }) 
                },
                leadingContent = { 
                    Icon(
                        when(viewModel.isApiOnline) {
                            true -> Icons.Outlined.CheckCircle
                            false -> Icons.Outlined.ErrorOutline
                            else -> Icons.Outlined.Wifi
                        },
                        null,
                        tint = when(viewModel.isApiOnline) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                trailingContent = {
                    TextButton(onClick = { viewModel.checkApiConnection() }) {
                        Text(stringResource(R.string.check_connectivity))
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.model_sync)) },
                supportingContent = { Text(stringResource(R.string.model_sync_desc)) },
                leadingContent = { Icon(Icons.Outlined.Sync, null) }
            )
        }

        // --- 关于组 ---
        SettingsGroup(title = stringResource(R.string.settings_group_about)) {
            ListItem(
                headlineContent = { Text("Mimo TTS App") },
                supportingContent = { Text(stringResource(R.string.app_desc)) },
                leadingContent = { Icon(Icons.Outlined.Info, null) },
                trailingContent = { Text("v1.0.0", style = MaterialTheme.typography.labelMedium) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.visit_console)) },
                leadingContent = { Icon(Icons.Outlined.Language, null) },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.xiaomimimo.com/console"))
                    context.startActivity(intent)
                }
            )
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}
