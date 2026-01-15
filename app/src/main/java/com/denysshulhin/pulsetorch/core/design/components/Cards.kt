package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PTSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(PTColor.CardBlue, RoundedCornerShape(18.dp))
            .border(1.dp, PTColor.BorderSofter, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun PTPanelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(PTColor.SurfaceDark.copy(alpha = 0.50f), RoundedCornerShape(18.dp))
            .border(1.dp, PTColor.BorderSofter, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        content()
    }
}
