package com.denysshulhin.pulsetorch.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PTPrimaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTSecondaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTStatusBadge
import com.denysshulhin.pulsetorch.core.design.components.PTStereoMeter
import com.denysshulhin.pulsetorch.core.design.components.PTSurfaceCard
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import com.denysshulhin.pulsetorch.domain.model.toTabIndex

@Composable
fun SystemCaptureScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSelectMode: (Mode) -> Unit,
    onToggleRunning: () -> Unit
) {
    val s = state.settings

    PulseTorchScreen(
        background = PTColor.BackgroundCapture,
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
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = PTColor.White)
                }

                Text(
                    text = "System Audio Capture",
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

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PTSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.height(24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = PTColor.TextSecondary
                            )
                        }
                        Text(
                            text = "Capture Limitations",
                            style = MaterialTheme.typography.titleMedium,
                            color = PTColor.White
                        )
                        Text(
                            text = "Captures internal audio directly. Note: Does not work with Apple Music or Spotify due to DRM. Works best with local files or YouTube.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PTColor.TextSecondary
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    PTStatusBadge(
                        if (state.isRunning) "Status: Capturing" else "Status: Idle"
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PTStereoMeter()
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PTPrimaryButton(
                    text = if (state.isRunning) "Stop Capture" else "Start Capture",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonChecked,
                            contentDescription = null,
                            tint = PTColor.BackgroundCapture
                        )
                    },
                    onClick = onToggleRunning
                )

                PTSecondaryButton(
                    text = "Switch to Mic",
                    leadingIcon = { Icon(Icons.Outlined.Mic, null, tint = PTColor.TextSecondary) },
                    onClick = { onSelectMode(Mode.MIC) }
                )
            }
        }
    }
}
