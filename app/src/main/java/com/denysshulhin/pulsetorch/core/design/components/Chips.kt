package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PTChipRow(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(PTColor.SurfaceDark, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (selected) PTColor.Primary.copy(alpha = 0.85f) else PTColor.BorderSofter,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                if (selected && label.lowercase() == "strobe") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(PTColor.Primary, RoundedCornerShape(99.dp))
                        )
                        Text(
                            text = "  $label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PTColor.Primary
                        )
                    }
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) PTColor.Primary else PTColor.TextSilver
                    )
                }
            }
        }
    }
}
