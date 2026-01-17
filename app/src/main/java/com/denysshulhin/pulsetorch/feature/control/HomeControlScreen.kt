package com.denysshulhin.pulsetorch.feature.control

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTLabeledSliderPercent
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PTPanelCard
import com.denysshulhin.pulsetorch.core.design.components.PTPrimaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTSignalRing
import com.denysshulhin.pulsetorch.core.design.components.PTTopBarCentered
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import com.denysshulhin.pulsetorch.domain.model.toTabIndex

@Composable
fun HomeControlScreen(
    state: AppUiState,
    onOpenSettings: () -> Unit,
    onSelectMode: (Mode) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onSmoothnessChange: (Float) -> Unit,
    onToggleRunning: () -> Unit
) {
    val ctx = LocalContext.current
    val s = state.settings
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun isGranted(p: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun missingPermissionsForStart(): Array<String> {
        val need = mutableListOf<String>()

        if (!isGranted(Manifest.permission.CAMERA)) need += Manifest.permission.CAMERA

        if (Build.VERSION.SDK_INT >= 33 && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            need += Manifest.permission.POST_NOTIFICATIONS
        }

        if (s.mode == Mode.MIC && !isGranted(Manifest.permission.RECORD_AUDIO)) {
            need += Manifest.permission.RECORD_AUDIO
        }

        return need.toTypedArray()
    }

    val requestAll = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted && !state.isRunning) onToggleRunning()
    }

    val onStartStopClick = remember(state.isRunning, s.mode) {
        {
            if (state.isRunning) {
                onToggleRunning()
                return@remember
            }

            val missing = missingPermissionsForStart()
            if (missing.isNotEmpty()) requestAll.launch(missing) else onToggleRunning()
        }
    }

    PulseTorchScreen(
        background = PTColor.Background,
        glowTop = PTColor.AccentBlue,
        glowBottom = PTColor.CardBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PTDimen.ScreenHPadding)
                .padding(top = 8.dp, bottom = 18.dp)
        ) {
            PTTopBarCentered(
                title = "PulseTorch",
                right = {
                    PTIconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = PTColor.TextSilver)
                    }
                }
            )

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

            val scroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                PTSignalRing(level01 = state.signalLevel01)
                            }

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
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PTPanelCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                    PTLabeledSliderPercent("Sensitivity", s.sensitivity, onSensitivityChange)
                                    PTLabeledSliderPercent("Smoothness", s.smoothness, onSmoothnessChange)
                                }
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PTSignalRing(level01 = state.signalLevel01)
                    }

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

                    PTPanelCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            PTLabeledSliderPercent("Sensitivity", s.sensitivity, onSensitivityChange)
                            PTLabeledSliderPercent("Smoothness", s.smoothness, onSmoothnessChange)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
