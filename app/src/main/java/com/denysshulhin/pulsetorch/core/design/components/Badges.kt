package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PTStatusBadge(text: String) {
    Row(
        modifier = Modifier
            .background(PTColor.StatusActive.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
            .border(1.dp, PTColor.StatusActive.copy(alpha = 0.20f), RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(10.dp)) {
            drawCircle(color = PTColor.StatusActive)
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = PTColor.StatusActive
        )
    }
}
