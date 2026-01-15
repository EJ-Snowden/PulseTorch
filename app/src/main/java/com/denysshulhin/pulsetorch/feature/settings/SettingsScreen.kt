package com.denysshulhin.pulsetorch.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShutterSpeed
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PTTextButton
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onAutoBrightnessChange: (Boolean) -> Unit,
    onMaxStrobeHzChange: (Float) -> Unit,
    onMicGainChange: (Float) -> Unit,
    onSmoothingChange: (Float) -> Unit,
    onBassFocusChange: (Boolean) -> Unit,
    onStrobeWarningChange: (Boolean) -> Unit
) {
    val s = state.settings

    val maxHzLabel = "${s.maxStrobeHz.roundToInt()}Hz"
    val micLabel = when {
        s.micGain < 0.9f -> "Low"
        s.micGain < 1.4f -> "Med"
        else -> "High"
    }
    val smoothingLabel = "${(s.smoothing * 100f).roundToInt()}%"

    PulseTorchScreen(
        background = PTColor.BackgroundSettings,
        glowTop = PTColor.CardBlue,
        glowBottom = PTColor.CardBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
        ) {
            // Top bar (not scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
            ) {
                PTIconButton(
                    onClick = onBack,
                    content = { Icon(Icons.Outlined.ArrowBack, "Back", tint = PTColor.TextSilver) }
                )

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = PTColor.White.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    PTTextButton(text = "Done", onClick = onDone)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    SectionTitle("Torch Configuration")
                    Text(
                        text = "Control torch behavior and strobe limits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PTColor.TextMuted.copy(alpha = 0.9f),
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                    )
                }

                item {
                    SectionCard {
                        SettingsRow(
                            icon = { Icon(Icons.Outlined.BrightnessAuto, null, tint = PTColor.TextSilver.copy(alpha = 0.8f)) },
                            title = "Auto Brightness",
                            subtitle = "Auto choose torch levels or on/off",
                            trailing = {
                                Switch(
                                    checked = s.autoBrightness,
                                    onCheckedChange = onAutoBrightnessChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PTColor.White,
                                        checkedTrackColor = PTColor.PrimarySoft,
                                        uncheckedThumbColor = PTColor.White,
                                        uncheckedTrackColor = PTColor.Black.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        )

                        DividerSoft()

                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Outlined.ShutterSpeed, null, tint = PTColor.TextSilver.copy(alpha = 0.8f))
                                    Text("Max Strobe Rate", style = MaterialTheme.typography.bodyLarge, color = PTColor.TextSilver)
                                }
                                Text(maxHzLabel, style = MaterialTheme.typography.bodyMedium, color = PTColor.PrimarySoft)
                            }

                            Slider(
                                value = s.maxStrobeHz,
                                onValueChange = onMaxStrobeHzChange,
                                valueRange = 1f..20f,
                                steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = PTColor.PrimarySoft,
                                    activeTrackColor = PTColor.PrimarySoft,
                                    inactiveTrackColor = PTColor.Black.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }

                item {
                    SectionTitle("Audio Sync")
                }

                item {
                    SectionCard {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Mic, null, tint = PTColor.TextSilver.copy(alpha = 0.8f))
                                    Text("Mic Sensitivity", style = MaterialTheme.typography.bodyLarge, color = PTColor.TextSilver)
                                }
                                Text(micLabel, style = MaterialTheme.typography.bodyMedium, color = PTColor.PrimarySoft)
                            }

                            Slider(
                                value = s.micGain,
                                onValueChange = onMicGainChange,
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = PTColor.PrimarySoft,
                                    activeTrackColor = PTColor.PrimarySoft,
                                    inactiveTrackColor = PTColor.Black.copy(alpha = 0.35f)
                                )
                            )
                        }

                        DividerSoft()

                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Equalizer, null, tint = PTColor.TextSilver.copy(alpha = 0.8f))
                                    Text("Smoothing", style = MaterialTheme.typography.bodyLarge, color = PTColor.TextSilver)
                                }
                                Text(smoothingLabel, style = MaterialTheme.typography.bodyMedium, color = PTColor.PrimarySoft)
                            }

                            Slider(
                                value = s.smoothing,
                                onValueChange = onSmoothingChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PTColor.PrimarySoft,
                                    activeTrackColor = PTColor.PrimarySoft,
                                    inactiveTrackColor = PTColor.Black.copy(alpha = 0.35f)
                                )
                            )
                        }

                        DividerSoft()

                        SettingsRow(
                            icon = { Icon(Icons.Outlined.Equalizer, null, tint = PTColor.TextSilver.copy(alpha = 0.8f)) },
                            title = "Bass Focus",
                            subtitle = "React only to low frequencies",
                            trailing = {
                                Switch(
                                    checked = s.bassFocus,
                                    onCheckedChange = onBassFocusChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PTColor.White,
                                        checkedTrackColor = PTColor.PrimarySoft,
                                        uncheckedThumbColor = PTColor.White,
                                        uncheckedTrackColor = PTColor.Black.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        )
                    }
                }

                item {
                    SectionTitle("Safety")
                }

                item {
                    SectionCard {
                        SettingsRow(
                            icon = { Icon(Icons.Outlined.Warning, null, tint = PTColor.TextSilver.copy(alpha = 0.8f)) },
                            title = "Strobe Warning",
                            trailing = {
                                Switch(
                                    checked = s.strobeWarning,
                                    onCheckedChange = onStrobeWarningChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PTColor.White,
                                        checkedTrackColor = PTColor.PrimarySoft,
                                        uncheckedThumbColor = PTColor.White,
                                        uncheckedTrackColor = PTColor.Black.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        )

                        DividerSoft()

                        Text(
                            text = "CAUTION: Prolonged exposure to flashing lights may trigger seizures in photosensitive individuals. Use with caution in public spaces.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PTColor.TextMuted.copy(alpha = 0.95f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PTColor.Black.copy(alpha = 0.20f))
                                .padding(14.dp)
                        )
                    }
                }

                item {
                    SectionTitle("Privacy")
                }

                item {
                    SectionCard {
                        SettingsRow(
                            icon = { Icon(Icons.Outlined.Shield, null, tint = PTColor.TextSilver.copy(alpha = 0.8f)) },
                            title = "Device-Only Processing",
                            trailing = {
                                Row(
                                    modifier = Modifier
                                        .background(PTColor.PrimarySoft.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
                                        .border(1.dp, PTColor.PrimarySoft.copy(alpha = 0.20f), RoundedCornerShape(99.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Canvas(Modifier.size(6.dp)) { drawCircle(PTColor.PrimarySoft) }
                                    Text("ACTIVE", style = MaterialTheme.typography.labelMedium, color = PTColor.PrimarySoft)
                                }
                            }
                        )

                        DividerSoft()

                        Text(
                            text = "PulseTorch analyzes audio locally on your device. No raw microphone data is uploaded to the cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PTColor.TextMuted.copy(alpha = 0.95f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PTColor.Black.copy(alpha = 0.20f))
                                .padding(14.dp)
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(10.dp))
                    FooterBrand()
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = PTColor.TextMuted.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PTColor.CardBlue, RoundedCornerShape(16.dp))
            .border(1.dp, PTColor.BorderSofter, RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = PTColor.TextSilver, maxLines = 1)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelLarge, color = PTColor.TextMuted, maxLines = 1)
                }
            }
        }
        trailing()
    }
}

@Composable
private fun DividerSoft() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PTColor.BorderSofter)
    )
}

@Composable
private fun FooterBrand() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(PTColor.PrimarySoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.FlashlightOn, null, tint = PTColor.White, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "PULSETORCH",
            style = MaterialTheme.typography.labelMedium,
            color = PTColor.White.copy(alpha = 0.55f)
        )
    }
}
