package com.denysshulhin.pulsetorch.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShutterSpeed
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.components.PTTextButton
import com.denysshulhin.pulsetorch.core.design.components.PTIconButton
import com.denysshulhin.pulsetorch.core.design.components.PulseTorchScreen
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    var autoBrightness by remember { mutableStateOf(false) }
    var strobeRate by remember { mutableFloatStateOf(0.40f) } // 0..1 -> 20Hz label mock
    var micSensitivity by remember { mutableFloatStateOf(0.75f) }
    var bassFocus by remember { mutableStateOf(true) }
    var strobeWarning by remember { mutableStateOf(true) }

    PulseTorchScreen(background = PTColor.BackgroundSettings, glowTop = PTColor.CardBlue, glowBottom = PTColor.CardBlue) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // header (sticky look by just having a darker bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PTColor.BackgroundSettings.copy(alpha = 0.90f))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PTIconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = PTColor.TextSilver)
                }
                Spacer(Modifier.weight(1f))
                Text("Settings", style = MaterialTheme.typography.titleMedium, color = PTColor.White.copy(alpha = 0.92f))
                Spacer(Modifier.weight(1f))
                PTTextButton(text = "Done", onClick = onDone)
            }

            Column(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SectionTitle("Torch Configuration")
                SectionCard {
                    // Auto Brightness
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.BrightnessAuto, null, tint = PTColor.TextSilver.copy(alpha = 0.8f)) },
                        title = "Auto Brightness",
                        trailing = {
                            Switch(
                                checked = autoBrightness,
                                onCheckedChange = { autoBrightness = it },
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
                    // Max Strobe Rate
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.ShutterSpeed, null, tint = PTColor.TextSilver.copy(alpha = 0.8f))
                                Text("Max Strobe Rate", style = MaterialTheme.typography.bodyLarge, color = PTColor.TextSilver)
                            }
                            Text("20Hz", style = MaterialTheme.typography.bodyMedium, color = PTColor.PrimarySoft)
                        }
                        Slider(
                            value = strobeRate,
                            onValueChange = { strobeRate = it },
                            colors = SliderDefaults.colors(
                                thumbColor = PTColor.PrimarySoft,
                                activeTrackColor = PTColor.PrimarySoft,
                                inactiveTrackColor = PTColor.Black.copy(alpha = 0.35f)
                            )
                        )
                    }
                }

                SectionTitle("Audio Sync")
                SectionCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.Mic, null, tint = PTColor.TextSilver.copy(alpha = 0.8f))
                                Text("Mic Sensitivity", style = MaterialTheme.typography.bodyLarge, color = PTColor.TextSilver)
                            }
                            Text("High", style = MaterialTheme.typography.bodyMedium, color = PTColor.PrimarySoft)
                        }
                        Slider(
                            value = micSensitivity,
                            onValueChange = { micSensitivity = it },
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
                                checked = bassFocus,
                                onCheckedChange = { bassFocus = it },
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

                SectionTitle("Safety")
                SectionCard {
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.Warning, null, tint = PTColor.TextSilver.copy(alpha = 0.8f)) },
                        title = "Strobe Warning",
                        trailing = {
                            Switch(
                                checked = strobeWarning,
                                onCheckedChange = { strobeWarning = it },
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

                SectionTitle("Privacy")
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

                Spacer(Modifier.height(10.dp))
                FooterBrand()
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
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = PTColor.TextMuted,
                        maxLines = 1
                    )
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(PTColor.PrimarySoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.FlashlightOn, null, tint = PTColor.White, modifier = Modifier.size(18.dp))
        }
        Text("PULSETORCH PRO", style = MaterialTheme.typography.labelMedium, color = PTColor.White.copy(alpha = 0.55f))
        Text("Version 2.4.1 (Build 4902)", style = MaterialTheme.typography.labelMedium, color = PTColor.TextMuted.copy(alpha = 0.6f))
    }
}
