package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
internal fun EmtTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
