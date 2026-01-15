package com.denysshulhin.pulsetorch.core.design.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PulseTorchScreen(
    background: Color = PTColor.Background,
    glowTop: Color = PTColor.CardBlue,
    glowBottom: Color = PTColor.CardBlue,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(background, background)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowTop.copy(alpha = 0.28f), Color.Transparent),
                        radius = 900f
                    )
                )
                .then(Modifier.blur(70.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowBottom.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(900f, 1700f),
                        radius = 1000f
                    )
                )
                .then(Modifier.blur(80.dp))
        )

        content()
    }
}
