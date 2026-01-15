package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PTSignalRing() {
    Box(contentAlignment = Alignment.Center) {
        // blue ambient glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PTColor.AccentBlue.copy(alpha = 0.14f), PTColor.Background.copy(alpha = 0f)),
                        radius = 600f
                    )
                )
        )

        Canvas(modifier = Modifier.size(280.dp)) {
            val c = center
            // outer subtle circle
            drawCircle(
                color = PTColor.White.copy(alpha = 0.06f),
                radius = size.minDimension / 2f,
                style = Stroke(width = 2.5f)
            )

            // dashed ring
            drawCircle(
                color = PTColor.AccentBlue.copy(alpha = 0.20f),
                radius = size.minDimension / 2f - 18f,
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            // main ring base
            drawCircle(
                color = PTColor.SurfaceDark,
                radius = size.minDimension / 2f - 40f,
                style = Stroke(width = 10f)
            )

            // conic arc
            val sweep = Brush.sweepGradient(
                listOf(
                    PTColor.AccentBlue.copy(alpha = 0f),
                    PTColor.AccentBlue,
                    PTColor.Primary,
                    PTColor.AccentBlue,
                    PTColor.AccentBlue.copy(alpha = 0f),
                )
            )
            drawArc(
                brush = sweep,
                startAngle = 180f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(40f, 40f),
                size = Size(size.width - 80f, size.height - 80f),
                style = Stroke(width = 10f)
            )
        }

        // inner core
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(PTColor.SurfaceDark, RoundedCornerShape(999.dp))
                .border(1.dp, PTColor.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(42.dp)
                ) {
                    Bar(h = 16, color = PTColor.AccentBlue.copy(alpha = 0.50f))
                    Bar(h = 22, color = PTColor.AccentBlue.copy(alpha = 0.75f))
                    Bar(h = 34, color = PTColor.Primary)
                    Bar(h = 24, color = PTColor.AccentBlue.copy(alpha = 0.75f))
                    Bar(h = 14, color = PTColor.AccentBlue.copy(alpha = 0.50f))
                }
                Text(
                    text = "SIGNAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = PTColor.TextSilver.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun Bar(h: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 4.dp, height = h.dp)
            .background(color, RoundedCornerShape(99.dp))
    )
}
