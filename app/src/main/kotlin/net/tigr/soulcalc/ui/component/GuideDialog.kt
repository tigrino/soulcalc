/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.tigr.soulcalc.R

@Composable
fun GuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.menu_guide))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                GuideSection(
                    title = stringResource(R.string.guide_basics_title),
                    content = stringResource(R.string.guide_basics_content)
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideSection(
                    title = stringResource(R.string.guide_variables_title),
                    content = stringResource(R.string.guide_variables_content)
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideSection(
                    title = stringResource(R.string.guide_references_title),
                    content = stringResource(R.string.guide_references_content)
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideSection(
                    title = stringResource(R.string.guide_keyboard_title),
                    content = stringResource(R.string.guide_keyboard_content)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok))
            }
        }
    )
}

@Composable
private fun GuideSection(
    title: String,
    content: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium
    )
}
