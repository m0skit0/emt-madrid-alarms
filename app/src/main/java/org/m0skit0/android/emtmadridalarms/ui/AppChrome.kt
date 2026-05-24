package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.m0skit0.android.emtmadridalarms.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar() {
    TopAppBar(title = { Text(stringResource(R.string.app_title)) })
}
