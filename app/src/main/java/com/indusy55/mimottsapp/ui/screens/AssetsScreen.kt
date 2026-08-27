package com.indusy55.mimottsapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.indusy55.mimottsapp.R
import com.indusy55.mimottsapp.audio.AudioPlayer
import com.indusy55.mimottsapp.audio.VoiceRecorder
import com.indusy55.mimottsapp.data.models.AssetType
import com.indusy55.mimottsapp.data.models.VoiceAsset
import com.indusy55.mimottsapp.ui.MimoViewModel
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: MimoViewModel, 
    modifier: Modifier = Modifier,
    onAssetSelected: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.design_tab), stringResource(R.string.clone_tab))
    var showDesignDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<VoiceAsset?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    val audioPlayer = remember { AudioPlayer(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            recorder.startRecording()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "audio/mpeg"
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            if (bytes != null) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                viewModel.addAsset(
                    name = "Imported Sample ${viewModel.savedAssets.count { it.type == AssetType.CLONE } + 1}",
                    type = AssetType.CLONE, 
                    data = base64,
                    mimeType = mimeType
                )
            }
        }
    }

    val currentAssets = viewModel.savedAssets.filter {
        if (selectedTab == 0) it.type == AssetType.DESIGN else it.type == AssetType.CLONE
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title) }
                    )
                }
            }

            if (currentAssets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = if (selectedTab == 0) stringResource(R.string.no_design_assets) else stringResource(R.string.no_clone_assets),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTab == 1) {
                            OutlinedButton(onClick = { filePickerLauncher.launch("audio/*") }) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.upload_file))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.saved_assets_header),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(currentAssets) { asset ->
                        ListItem(
                            headlineContent = { Text(asset.name) },
                            supportingContent = { 
                                Text(
                                    text = if (asset.type == AssetType.DESIGN) asset.data.take(50) else "音频采样素材",
                                    maxLines = 1
                                ) 
                            },
                            leadingContent = {
                                if (asset.type == AssetType.CLONE) {
                                    IconButton(onClick = { audioPlayer.playBase64Audio(asset.data) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { 
                                        viewModel.selectAsset(asset)
                                        onAssetSelected()
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "选中", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.deleteAsset(asset) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_confirm))
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                editingAsset = asset
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (selectedTab == 1 && currentAssets.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = { filePickerLauncher.launch("audio/*") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "上传")
                }
            }
            
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        showDesignDialog = true
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isRecording = true
                            recorder.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            ) {
                Icon(if (selectedTab == 0) Icons.Default.Add else Icons.Default.Mic, contentDescription = "添加")
            }
        }
    }

    if (isRecording) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.recording_title)) },
            text = { Text(stringResource(R.string.recording_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        val base64 = recorder.stopRecording()
                        if (base64 != null) {
                            viewModel.addAsset(
                                name = "录音采样 ${viewModel.savedAssets.count { it.type == AssetType.CLONE } + 1}", 
                                type = AssetType.CLONE, 
                                data = base64,
                                mimeType = "audio/wav"
                            )
                        }
                        isRecording = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_and_save))
                }
            }
        )
    }

    if (showDesignDialog) {
        var designName by remember { mutableStateOf("") }
        var designDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDesignDialog = false },
            title = { Text(stringResource(R.string.new_design_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = designName,
                        onValueChange = { designName = it },
                        label = { Text(stringResource(R.string.asset_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = designDesc,
                        onValueChange = { designDesc = it },
                        label = { Text(stringResource(R.string.voice_design_label)) },
                        placeholder = { Text(stringResource(R.string.voice_design_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (designDesc.isNotBlank()) {
                            val name = designName.ifBlank { "设计音色 ${viewModel.savedAssets.count { it.type == AssetType.DESIGN } + 1}" }
                            viewModel.addAsset(name, AssetType.DESIGN, designDesc)
                            showDesignDialog = false
                        }
                    },
                    enabled = designDesc.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDesignDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    editingAsset?.let { asset ->
        var editName by remember(asset) { mutableStateOf(asset.name) }
        var editData by remember(asset) { mutableStateOf(asset.data) }

        AlertDialog(
            onDismissRequest = { editingAsset = null },
            title = { Text(stringResource(R.string.edit_asset_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.asset_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (asset.type == AssetType.DESIGN) {
                        OutlinedTextField(
                            value = editData,
                            onValueChange = { editData = it },
                            label = { Text(stringResource(R.string.voice_design_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateAsset(asset.copy(name = editName, data = editData))
                        editingAsset = null
                    }
                ) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAsset = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}
