package com.indusy55.mimottsapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indusy55.mimottsapp.R
import com.indusy55.mimottsapp.audio.AudioPlayer
import com.indusy55.mimottsapp.audio.StreamingAudioPlayer
import com.indusy55.mimottsapp.data.models.AssetType
import com.indusy55.mimottsapp.ui.MimoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MimoViewModel, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayer(context) }
    val streamingPlayer = remember { StreamingAudioPlayer() }
    
    var showDirectorHelper by remember { mutableStateOf(false) }
    var directorRole by remember { mutableStateOf("") }
    var directorScene by remember { mutableStateOf("") }
    var directorGuide by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
            streamingPlayer.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        
        if (viewModel.error != null) {
            ErrorMessage(viewModel.error!!)
        }

        // --- SECTION 1: Engine Core ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabelWithIcon(Icons.Outlined.SettingsSuggest, stringResource(R.string.section_model_settings))
            
            val modelOptions = listOf(
                stringResource(R.string.model_basic),
                stringResource(R.string.model_design),
                stringResource(R.string.model_clone)
            )
            val modelIds = listOf("mimo-v2.5-tts", "mimo-v2.5-tts-voicedesign", "mimo-v2.5-tts-voiceclone")
            
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modelIds.forEachIndexed { index, id ->
                    SegmentedButton(
                        selected = viewModel.selectedModel == id,
                        onClick = { viewModel.updateModel(id) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modelIds.size),
                        label = { Text(modelOptions[index], style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Text(
                text = when(viewModel.selectedModel) {
                    "mimo-v2.5-tts" -> stringResource(R.string.library_preset)
                    "mimo-v2.5-tts-voicedesign" -> stringResource(R.string.library_design)
                    "mimo-v2.5-tts-voiceclone" -> stringResource(R.string.library_clone)
                    else -> stringResource(R.string.library_generic)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val currentVoiceOptions = when(viewModel.selectedModel) {
                "mimo-v2.5-tts" -> listOf("冰糖", "茉莉", "苏打", "白桦", "Mia", "Chloe", "Milo", "Dean")
                "mimo-v2.5-tts-voicedesign" -> viewModel.savedAssets.filter { it.type == AssetType.DESIGN }.map { it.name }
                "mimo-v2.5-tts-voiceclone" -> viewModel.savedAssets.filter { it.type == AssetType.CLONE }.map { it.name }
                else -> emptyList()
            }

            if (currentVoiceOptions.isEmpty() && viewModel.selectedModel != "mimo-v2.5-tts") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.no_assets_guide),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentVoiceOptions.forEach { voice ->
                        val isSelected = viewModel.selectedVoice == voice
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateVoice(voice) },
                            label = { Text(voice) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // --- SECTION 2: Control Logic ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabelWithIcon(Icons.Outlined.Tune, stringResource(R.string.section_style_control))
            
            OutlinedTextField(
                value = viewModel.styleTags,
                onValueChange = { viewModel.updateStyleTags(it) },
                label = { Text(stringResource(R.string.style_tags_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.style_tags_hint)) },
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) }
            )

            val tagGroups = listOf(
                stringResource(R.string.tag_emotion) to listOf("(开心)", "(悲伤)", "(愤怒)", "(恐惧)", "(惊讶)", "(委屈)", "(平静)", "(冷漠)"),
                stringResource(R.string.tag_style) to listOf("(温柔)", "(高冷)", "(活泼)", "(慵懒)", "(俏皮)", "(深沉)", "(凌厉)", "(磁性)", "(甜美)", "(唱歌)"),
                stringResource(R.string.tag_dialect) to listOf("(东北话)", "(四川话)", "(河南话)", "(粤语)", "(台湾腔)", "(孙悟空)", "(林黛玉)")
            )
            
            tagGroups.forEach { (groupName, tags) ->
                TagRow(groupName, tags, viewModel)
            }

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.prompt_label), style = MaterialTheme.typography.titleSmall)
                        FilledTonalIconToggleButton(
                            checked = showDirectorHelper,
                            onCheckedChange = { showDirectorHelper = it }
                        ) {
                            Icon(if (showDirectorHelper) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.TheaterComedy, null)
                        }
                    }

                    if (showDirectorHelper) {
                        DirectorHelperView(
                            role = directorRole, onRoleChange = { directorRole = it },
                            scene = directorScene, onSceneChange = { directorScene = it },
                            guide = directorGuide, onGuideChange = { directorGuide = it },
                            onGenerate = {
                                val generated = buildString {
                                    if (directorRole.isNotBlank()) append("【角色】$directorRole\n")
                                    if (directorScene.isNotBlank()) append("【场景】$directorScene\n")
                                    if (directorGuide.isNotBlank()) append("【指导】$directorGuide")
                                }.trim()
                                viewModel.updateUserPrompt(generated)
                                showDirectorHelper = false
                            }
                        )
                    } else {
                        TextField(
                            value = viewModel.userPrompt,
                            onValueChange = { viewModel.updateUserPrompt(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(
                                    if (viewModel.selectedModel == "mimo-v2.5-tts-voicedesign") 
                                        stringResource(R.string.prompt_voicedesign_placeholder) 
                                    else stringResource(R.string.prompt_placeholder)
                                ) 
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        // --- SECTION 3: Content & Action ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabelWithIcon(Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.section_reading_text))
            
            OutlinedTextField(
                value = viewModel.assistantContent,
                onValueChange = { viewModel.assistantContent = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text(stringResource(R.string.content_placeholder)) },
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    if (viewModel.assistantContent.isNotEmpty()) {
                        IconButton(onClick = { viewModel.assistantContent = "" }) {
                            Icon(Icons.Outlined.Close, null)
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!viewModel.isLoading) {
                            if (viewModel.assistantContent.isBlank()) {
                                viewModel.error = context.getString(R.string.empty_content_error)
                            } else {
                                streamingPlayer.stop()
                                streamingPlayer.init()
                                viewModel.generateTTSStream(
                                    onAudioChunk = { streamingPlayer.write(it) },
                                    onDone = {}
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    expanded = !viewModel.isLoading,
                    icon = { 
                        if (viewModel.isLoading) 
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else 
                            Icon(Icons.Rounded.PlayArrow, null) 
                    },
                    text = { Text(stringResource(R.string.start_preview), fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )

                if (viewModel.isLoading) {
                    LargeFloatingActionButton(
                        onClick = {
                            viewModel.stopSynthesis()
                            streamingPlayer.stop()
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Rounded.Stop, null)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun LabelWithIcon(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TagRow(groupName: String, tags: List<String>, viewModel: MimoViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = groupName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val isSelected = viewModel.styleTags.contains(tag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            viewModel.updateStyleTags(viewModel.styleTags.replace(tag, "").replace("  ", " ").trim())
                        } else {
                            viewModel.updateStyleTags("${viewModel.styleTags} $tag".trim())
                        }
                    },
                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
fun DirectorHelperView(
    role: String, onRoleChange: (String) -> Unit,
    scene: String, onSceneChange: (String) -> Unit,
    guide: String, onGuideChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField(
            value = role, onValueChange = onRoleChange, label = { Text(stringResource(R.string.director_role)) }, 
            modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall,
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface)
        )
        TextField(
            value = scene, onValueChange = onSceneChange, label = { Text(stringResource(R.string.director_scene)) }, 
            modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall,
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface)
        )
        TextField(
            value = guide, onValueChange = onGuideChange, label = { Text(stringResource(R.string.director_guide)) }, 
            modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall,
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface)
        )
        Button(
            onClick = onGenerate, 
            modifier = Modifier.align(Alignment.End),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.confirm_generate), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ErrorMessage(error: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Error, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
