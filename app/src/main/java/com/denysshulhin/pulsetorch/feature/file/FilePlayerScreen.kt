package com.denysshulhin.pulsetorch.feature.file

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PTSurfaceCard
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import com.denysshulhin.pulsetorch.domain.model.toTabIndex

@Composable
fun FilePlayerScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onUseInHome: () -> Unit,
    onSelectMode: (Mode) -> Unit
) {
    val s = state.settings
    var progress by remember { mutableFloatStateOf(0.35f) }

    PulseTorchScreen(
        background = PTColor.BackgroundFile,
        glowTop = PTColor.CardBlue,
        glowBottom = PTColor.CardBlue
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PTDimen.ScreenHPadding)
                .padding(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // top bar (proper centering)
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

            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                PTSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "Midnight City.mp3",
                            style = MaterialTheme.typography.titleMedium,
                            color = PTColor.White
                        )
                        Text(
                            text = "04:03  •  320kbps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PTColor.TextSecondary.copy(alpha = 0.65f)
                        )
                    }
                }

                // waveform mock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeHeights = listOf(10, 14, 22, 12, 28, 40, 22, 34, 16, 46, 24, 12, 34)
                    val passiveHeights = listOf(40, 22, 34, 16, 28, 12, 18, 10, 14, 22, 12, 16, 10)

                    activeHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .size(width = 4.dp, height = h.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(PTColor.AccentBlue.copy(alpha = 0.95f))
                        )
                    }
                    passiveHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .size(width = 4.dp, height = h.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(PTColor.TextSilver.copy(alpha = 0.25f))
                        )
                    }
                }

                // seek
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("01:23", style = MaterialTheme.typography.labelLarge, color = PTColor.AccentBlue)
                        Text("04:03", style = MaterialTheme.typography.labelLarge, color = PTColor.TextSecondary.copy(alpha = 0.6f))
                    }
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        colors = SliderDefaults.colors(
                            thumbColor = PTColor.PrimarySoft,
                            activeTrackColor = PTColor.AccentBlue,
                            inactiveTrackColor = PTColor.CardBlue.copy(alpha = 0.45f)
                        )
                    )
                }

                // controls mock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PTIconButton(onClick = {}) { Icon(Icons.Outlined.SkipPrevious, null, tint = PTColor.TextSilver) }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PTColor.PrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = PTColor.BackgroundFile,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    PTIconButton(onClick = {}) { Icon(Icons.Outlined.SkipNext, null, tint = PTColor.TextSilver) }
                }
            }

            // footer action (now clickable)
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

            Spacer(Modifier.height(6.dp))
        }
    }
}
