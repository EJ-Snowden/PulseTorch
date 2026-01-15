package com.denysshulhin.pulsetorch.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PTModeTabs(
    selectedIndex: Int, // 0 File, 1 System, 2 Mic
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PTSegmentedControl(
        items = listOf("File", "System", "Mic"),
        selectedIndex = selectedIndex,
        onSelected = onSelect,
        modifier = modifier
    )
}
