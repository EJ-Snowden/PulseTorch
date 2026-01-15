package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.denysshulhin.pulsetorch.core.design.theme.PTColor

@Composable
fun PTSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PTColor.SurfaceDark, RoundedCornerShape(14.dp))
            .border(1.dp, PTColor.BorderSofter, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                style = if (selected) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium,
                color = if (selected) PTColor.Primary else PTColor.TextSilver,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) PTColor.Background.copy(alpha = 0.50f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) PTColor.Primary.copy(alpha = 0.45f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelected(index) }
                    .padding(top = 10.dp),
                maxLines = 1
            )
        }
    }
}
