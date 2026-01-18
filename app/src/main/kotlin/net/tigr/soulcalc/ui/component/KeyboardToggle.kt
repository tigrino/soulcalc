/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.tigr.soulcalc.R

/**
 * Toggle button to switch between custom calculator keyboard and system keyboard.
 */
@Composable
fun KeyboardToggle(
    useCustomKeyboard: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (useCustomKeyboard) {
                Icons.Default.Keyboard
            } else {
                Icons.Default.Calculate
            },
            contentDescription = if (useCustomKeyboard) {
                stringResource(R.string.content_description_switch_to_system_keyboard)
            } else {
                stringResource(R.string.content_description_switch_to_calculator_keyboard)
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
