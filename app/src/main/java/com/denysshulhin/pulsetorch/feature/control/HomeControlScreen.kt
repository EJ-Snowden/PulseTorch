package com.denysshulhin.pulsetorch.feature.control

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTChipRow
import com.denysshulhin.pulsetorch.core.design.components.PTLabeledSliderPercent
import com.denysshulhin.pulsetorch.core.design.components.PTPrimaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTTopBarCentered
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.components.PTPanelCard
import com.denysshulhin.pulsetorch.core.design.components.PTSignalRing
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen
import com.denysshulhin.pulsetorch.core.permissions.AudioPermission
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import com.denysshulhin.pulsetorch.domain.model.toChipIndex
import com.denysshulhin.pulsetorch.domain.model.toTabIndex

@Composable
fun HomeControlScreen(
    state: AppUiState,
    onOpenSettings: () -> Unit,
    onSelectMode: (Mode) -> Unit,
    onSelectEffectIndex: (Int) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onSmoothnessChange: (Float) -> Unit,
    onToggleRunning: () -> Unit
) {
    val ctx = LocalContext.current
    val s = state.settings

    val requestMic = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // if user granted and we are in MIC mode, try starting immediately
        if (granted && s.mode == Mode.MIC && !state.isRunning) {
            onToggleRunning()
        }
    }

    val onStartStopClick = remember(state.isRunning, s.mode) {
        {
            if (state.isRunning) {
                onToggleRunning()
            } else {
                if (s.mode == Mode.MIC && !AudioPermission.hasRecordAudio(ctx)) {
                    requestMic.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    onToggleRunning()
                }
            }
        }
    }

    PulseTorchScreen(background = PTColor.Background, glowTop = PTColor.AccentBlue, glowBottom = PTColor.CardBlue) {
        Column(
            modifier = Modifier
                .padding(horizontal = PTDimen.ScreenHPadding)
                .padding(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PTTopBarCentered(
                title = "PulseTorch",
                right = {
                    PTIconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = PTColor.TextSilver)
                    }
                }
            )

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

            Spacer(Modifier.height(6.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PTSignalRing(level01 = state.signalLevel01)
            }

            Spacer(Modifier.height(4.dp))

            PTPrimaryButton(
                text = if (state.isRunning) "Stop" else "Start",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = null,
                        tint = PTColor.Background
                    )
                },
                onClick = onStartStopClick
            )

            Text(
                text = "STATUS:  ${state.statusText}",
                style = MaterialTheme.typography.labelLarge,
                color = PTColor.TextSilver.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            PTChipRow(
                items = listOf("Smooth", "Pulse", "Strobe"),
                selectedIndex = s.effect.toChipIndex(),
                onSelected = { onSelectEffectIndex(it) }
            )

            PTPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    PTLabeledSliderPercent("Sensitivity", s.sensitivity, onSensitivityChange)
                    PTLabeledSliderPercent("Smoothness", s.smoothness, onSmoothnessChange)
                }
            }
        }
    }
}
