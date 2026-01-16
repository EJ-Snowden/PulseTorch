package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kotlin.math.roundToInt

@Composable
fun PTSignalRing(level01: Float) {
    val lvl = level01.coerceIn(0f, 1f)

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PTColor.AccentBlue.copy(alpha = 0.10f + 0.10f * lvl),
                            PTColor.Background.copy(alpha = 0f)
                        ),
                        radius = 600f
                    )
                )
        )

        Canvas(modifier = Modifier.size(280.dp)) {
            drawCircle(
                color = PTColor.White.copy(alpha = 0.06f),
                radius = size.minDimension / 2f,
                style = Stroke(width = 2.5f)
            )

            drawCircle(
                color = PTColor.AccentBlue.copy(alpha = 0.15f + 0.10f * lvl),
                radius = size.minDimension / 2f - 18f,
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            drawCircle(
                color = PTColor.SurfaceDark,
                radius = size.minDimension / 2f - 40f,
                style = Stroke(width = 10f)
            )

            val sweep = Brush.sweepGradient(
                listOf(
                    PTColor.AccentBlue.copy(alpha = 0f),
                    PTColor.AccentBlue,
                    PTColor.Primary,
                    PTColor.AccentBlue,
                    PTColor.AccentBlue.copy(alpha = 0f),
                )
            )

            val sweepAngle = 40f + 260f * lvl
            drawArc(
                brush = sweep,
                startAngle = 180f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(40f, 40f),
                size = Size(size.width - 80f, size.height - 80f),
                style = Stroke(width = 10f)
            )
        }

        Box(
            modifier = Modifier
                .size(128.dp)
                .background(PTColor.SurfaceDark, RoundedCornerShape(999.dp))
                .border(1.dp, PTColor.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val bars = barsForLevel(lvl)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(42.dp)
                ) {
                    bars.forEach { (h, color) -> Bar(h = h, color = color) }
                }

                Text(
                    text = "${(lvl * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = PTColor.TextSilver.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun barsForLevel(level: Float): List<Pair<Int, Color>> {
    val base = listOf(14, 18, 26, 18, 12)
    val boost = (level * 18f).roundToInt()
    val heights = base.mapIndexed { i, h ->
        val extra = when (i) {
            2 -> boost
            1, 3 -> (boost * 0.65f).roundToInt()
            else -> (boost * 0.35f).roundToInt()
        }
        (h + extra).coerceIn(8, 46)
    }

    val hot = level > 0.75f
    return listOf(
        heights[0] to PTColor.AccentBlue.copy(alpha = 0.50f),
        heights[1] to PTColor.AccentBlue.copy(alpha = 0.75f),
        heights[2] to (if (hot) PTColor.PrimarySoft else PTColor.Primary),
        heights[3] to PTColor.AccentBlue.copy(alpha = 0.75f),
        heights[4] to PTColor.AccentBlue.copy(alpha = 0.50f),
    )
}

@Composable
private fun Bar(h: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 4.dp, height = h.dp)
            .background(color, RoundedCornerShape(99.dp))
    )
}
