package com.denysshulhin.pulsetorch.core.utils

import androidx.compose.runtime.Composable

@Composable
fun StopOnBackgroundEffect(onStop: () -> Unit) {
    // Foreground service owns the pipeline now.
    // Intentionally no-op.
}
