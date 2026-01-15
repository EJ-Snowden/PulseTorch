package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PTStereoMeter() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("L", style = MaterialTheme.typography.labelMedium, color = PTColor.TextSecondary)
            Text("R", style = MaterialTheme.typography.labelMedium, color = PTColor.TextSecondary)
        }

        Spacer(Modifier.height(8.dp))
        PTMeterRow(active = listOf(0.30f, 0.40f, 0.60f, 0.80f, 0.60f, 0.30f, 0.10f))
        Spacer(Modifier.height(10.dp))
        PTMeterRow(active = listOf(0.30f, 0.40f, 0.60f, 0.75f, 0.55f, 0.25f, 0.05f))

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-60db", style = MaterialTheme.typography.labelMedium, color = PTColor.TextSecondary.copy(alpha = 0.6f))
            Text("-20db", style = MaterialTheme.typography.labelMedium, color = PTColor.TextSecondary.copy(alpha = 0.6f))
            Text("0db", style = MaterialTheme.typography.labelMedium, color = PTColor.TextSecondary.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun PTMeterRow(active: List<Float>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(PTColor.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .border(1.dp, PTColor.BorderSofter, RoundedCornerShape(10.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PTColor.Background.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val bars = listOf(0.18f, 0.26f, 0.40f, 0.55f, 0.75f, 0.90f, 1f, 0.60f)
            bars.forEachIndexed { i, h ->
                val isHot = i >= 6
                val color =
                    if (isHot) PTColor.PrimarySoft
                    else PTColor.StatusActive.copy(alpha = 0.55f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((20.dp * (active.getOrNull(i)?.coerceIn(0.0f, 1.0f) ?: h)).coerceAtLeast(2.dp))
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
