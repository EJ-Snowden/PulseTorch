package com.denysshulhin.pulsetorch.feature.file

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PTSurfaceCard
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import com.denysshulhin.pulsetorch.domain.model.toTabIndex

@Composable
fun FilePlayerScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onUseInHome: () -> Unit,
    onSelectMode: (Mode) -> Unit,
    onPickFile: (Uri, String?) -> Unit,
    onTogglePlay: () -> Unit,
    onStopAll: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val s = state.settings

    val isPlaying by PulseTorchRuntime.fileIsPlaying.collectAsState()
    val posMs by PulseTorchRuntime.filePosMs.collectAsState()
    val durMs by PulseTorchRuntime.fileDurMs.collectAsState()

    val scroll = rememberScrollState()

    fun guessDisplayName(uri: Uri): String? {
        return runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                }
        }.getOrNull()
    }

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val name = runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c ->
                        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (i >= 0 && c.moveToFirst()) c.getString(i) else null
                    }
            }.getOrNull()

            onPickFile(uri, name ?: "Selected audio")
        }
    }

    fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(m, sec)
    }

    val hasDuration = durMs > 0
    val progress01 = if (hasDuration) (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f) else 0f

    var userDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val sliderShown = if (userDragging) dragValue else progress01

    PulseTorchScreen(
        background = PTColor.BackgroundFile,
        glowTop = PTColor.CardBlue,
        glowBottom = PTColor.CardBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PTDimen.ScreenHPadding)
                .padding(top = 8.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                PTIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = PTColor.White.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "File",
                    style = MaterialTheme.typography.titleMedium,
                    color = PTColor.White,
                    modifier = Modifier.align(Alignment.Center)
                )

                Spacer(modifier = Modifier.align(Alignment.CenterEnd))
            }

            Spacer(Modifier.height(12.dp))

            PTModeTabs(
                selectedIndex = s.mode.toTabIndex(),
                onSelect = { idx ->
                    when (idx) {
                        0 -> onSelectMode(Mode.FILE)
                        1 -> onSelectMode(Mode.SYSTEM)
                        else -> onSelectMode(Mode.MIC)
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                PTSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(PTColor.BackgroundFile.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = PTColor.TextSilver.copy(alpha = 0.8f),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = s.fileName ?: "No file selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = PTColor.White
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PTColor.White.copy(alpha = 0.06f))
                                .clickable { pickFile.launch(arrayOf("audio/*")) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (s.fileUri.isNullOrBlank()) "SELECT FILE" else "CHANGE FILE",
                                style = MaterialTheme.typography.labelLarge,
                                color = PTColor.White
                            )
                        }
                    }
                }

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatMs(posMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = PTColor.AccentBlue
                        )
                        Text(
                            formatMs(durMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = PTColor.TextSecondary.copy(alpha = 0.6f)
                        )
                    }

                    Slider(
                        value = sliderShown,
                        onValueChange = { v ->
                            if (!hasDuration) return@Slider
                            userDragging = true
                            dragValue = v
                        },
                        onValueChangeFinished = {
                            if (!hasDuration) return@Slider
                            val target = (durMs.toFloat() * dragValue).toLong()
                            userDragging = false
                            onSeek(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = PTColor.PrimarySoft,
                            activeTrackColor = PTColor.AccentBlue,
                            inactiveTrackColor = PTColor.CardBlue.copy(alpha = 0.45f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PTColor.PrimarySoft)
                            .clickable {
                                // Стартуем/пауза уже из UI (без ограничений пикера)
                                onTogglePlay()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = PTColor.BackgroundFile,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PTColor.White.copy(alpha = 0.08f))
                            .clickable { onStopAll() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            tint = PTColor.TextSilver,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PTColor.White.copy(alpha = 0.06f))
                    .clickable(onClick = onUseInHome)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "USE IN HOME",
                    style = MaterialTheme.typography.labelLarge,
                    color = PTColor.White
                )
                Spacer(Modifier.size(10.dp))
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = PTColor.TextSecondary.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
