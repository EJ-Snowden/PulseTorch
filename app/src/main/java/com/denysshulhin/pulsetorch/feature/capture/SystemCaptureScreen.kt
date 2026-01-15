package com.denysshulhin.pulsetorch.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.denysshulhin.pulsetorch.core.design.components.PTSurfaceCard
import com.denysshulhin.pulsetorch.core.design.components.PTSecondaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTPrimaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTStatusBadge
import com.denysshulhin.pulsetorch.core.design.components.PTStereoMeter
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen

@Composable
fun SystemCaptureScreen(
    onBack: () -> Unit,
    onSwitchToMic: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenSystem: () -> Unit,
    onOpenMic: () -> Unit
) {
    PulseTorchScreen(background = PTColor.BackgroundCapture, glowTop = PTColor.CardBlue, glowBottom = PTColor.CardBlue) {
        Column(
            modifier = Modifier
                .padding(horizontal = PTDimen.ScreenHPadding)
                .padding(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // header
            Row(
                modifier = Modifier.height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PTIconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = PTColor.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "System Audio Capture",
                    style = MaterialTheme.typography.titleMedium,
                    color = PTColor.White
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(40.dp))
            }

            PTModeTabs(
                selectedIndex = 1,
                onSelect = {
                    when (it) {
                        0 -> onOpenFile()
                        1 -> onOpenSystem()
                        2 -> onOpenMic()
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // info card
                PTSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .padding(top = 2.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = PTColor.TextSecondary
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    PTStatusBadge("Status: Capturing")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PTStereoMeter()
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PTPrimaryButton(
                    text = "Start Capture",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonChecked,
                            contentDescription = null,
                            tint = PTColor.BackgroundCapture
                        )
                    },
                    onClick = {}
                )
                PTSecondaryButton(
                    text = "Switch to Mic",
                    leadingIcon = {
                        Icon(Icons.Outlined.Mic, null, tint = PTColor.TextSecondary)
                    },
                    onClick = onSwitchToMic
                )
            }
        }
    }
}
