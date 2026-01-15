package com.denysshulhin.pulsetorch.feature.control

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTChipRow
import com.denysshulhin.pulsetorch.core.design.components.PTLabeledSliderPercent
import com.denysshulhin.pulsetorch.core.design.components.PTPrimaryButton
import com.denysshulhin.pulsetorch.core.design.components.PTSegmentedControl
import com.denysshulhin.pulsetorch.core.design.components.PTTopBarCentered
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTModeTabs
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.components.PTPanelCard
import com.denysshulhin.pulsetorch.core.design.components.PTSignalRing
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.core.design.theme.PTDimen

@Composable
fun HomeControlScreen(
    onOpenSettings: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenCapture: () -> Unit
) {
    var effectIndex by remember { mutableIntStateOf(2) } // 0 Smooth, 1 Pulse, 2 Strobe
    var sensitivity by remember { mutableFloatStateOf(0.75f) }
    var smoothness by remember { mutableFloatStateOf(0.40f) }

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
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = PTColor.TextSilver
                        )
                    }
                }
            )

            PTModeTabs(
                selectedIndex = 2,
                onSelect = {
                    when (it) {
                        0 -> onOpenFile()
                        1 -> onOpenCapture()
                        2 -> {}
                    }
                }
            )

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PTSignalRing()
            }

            Spacer(Modifier.height(4.dp))


            PTPrimaryButton(
                text = "Start",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = null,
                        tint = PTColor.Background
                    )
                },
                onClick = {}
            )

            Text(
                text = "STATUS:  IDLE",
                style = MaterialTheme.typography.labelLarge,
                color = PTColor.TextSilver.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            PTChipRow(
                items = listOf("Smooth", "Pulse", "Strobe"),
                selectedIndex = effectIndex,
                onSelected = { effectIndex = it }
            )

            PTPanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    PTLabeledSliderPercent("Sensitivity", sensitivity) { sensitivity = it }
                    PTLabeledSliderPercent("Smoothness", smoothness) { smoothness = it }
                }
            }
        }
    }
}
