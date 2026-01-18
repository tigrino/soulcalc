/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tigr.soulcalc.R

/**
 * Custom 5x5 calculator keyboard.
 *
 * Layout:
 * ```
 * sqrt  ^   #   $   =
 *  7    8   9   ÷   (
 *  4    5   6   ×   )
 *  1    2   3   −   ⌫
 *  0    .   %   +   ⏎
 * ```
 */
@Composable
fun CalculatorKeyboard(
    onKeyPress: (String) -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit,
    onClearLine: () -> Unit,
    onDollarKey: () -> Unit,
    onDollarLongPress: () -> Unit,
    onHashKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf(
            KeyDef("sqrt", KeyType.FUNCTION),
            KeyDef("^", KeyType.OPERATOR),
            KeyDef("#", KeyType.SPECIAL_HASH),
            KeyDef("$", KeyType.SPECIAL_DOLLAR),
            KeyDef("=", KeyType.OPERATOR)
        ),
        listOf(
            KeyDef("7", KeyType.DIGIT),
            KeyDef("8", KeyType.DIGIT),
            KeyDef("9", KeyType.DIGIT),
            KeyDef("÷", KeyType.OPERATOR),
            KeyDef("(", KeyType.OPERATOR)
        ),
        listOf(
            KeyDef("4", KeyType.DIGIT),
            KeyDef("5", KeyType.DIGIT),
            KeyDef("6", KeyType.DIGIT),
            KeyDef("×", KeyType.OPERATOR),
            KeyDef(")", KeyType.OPERATOR)
        ),
        listOf(
            KeyDef("1", KeyType.DIGIT),
            KeyDef("2", KeyType.DIGIT),
            KeyDef("3", KeyType.DIGIT),
            KeyDef("−", KeyType.OPERATOR),
            KeyDef("⌫", KeyType.BACKSPACE)
        ),
        listOf(
            KeyDef("0", KeyType.DIGIT),
            KeyDef(".", KeyType.DIGIT),
            KeyDef("%", KeyType.OPERATOR),
            KeyDef("+", KeyType.OPERATOR),
            KeyDef("⏎", KeyType.ENTER)
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { keyDef ->
                        KeyButton(
                            keyDef = keyDef,
                            onTap = {
                                when (keyDef.type) {
                                    KeyType.DIGIT, KeyType.OPERATOR -> onKeyPress(keyDef.label)
                                    KeyType.FUNCTION -> onKeyPress("sqrt(")
                                    KeyType.BACKSPACE -> onBackspace()
                                    KeyType.ENTER -> onEnter()
                                    // Special keys: only call their dedicated handler which handles
                                    // both text insertion AND keyboard switching atomically.
                                    // Do NOT call onKeyPress here - that would cause a race condition.
                                    KeyType.SPECIAL_DOLLAR -> onDollarKey()
                                    KeyType.SPECIAL_HASH -> onHashKey()
                                }
                            },
                            onLongPress = when (keyDef.type) {
                                KeyType.BACKSPACE -> onClearLine
                                KeyType.SPECIAL_DOLLAR -> onDollarLongPress
                                else -> null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private enum class KeyType {
    DIGIT,
    OPERATOR,
    FUNCTION,
    BACKSPACE,
    ENTER,
    SPECIAL_DOLLAR,
    SPECIAL_HASH
}

private data class KeyDef(
    val label: String,
    val type: KeyType
)

/**
 * Returns the accessibility content description for a key.
 */
@Composable
private fun keyContentDescription(keyDef: KeyDef): String {
    return when (keyDef.label) {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" ->
            stringResource(R.string.key_digit, keyDef.label)
        "+" -> stringResource(R.string.key_add)
        "−" -> stringResource(R.string.key_subtract)
        "×" -> stringResource(R.string.key_multiply)
        "÷" -> stringResource(R.string.key_divide)
        "^" -> stringResource(R.string.key_power)
        "%" -> stringResource(R.string.key_percent)
        "=" -> stringResource(R.string.key_equals)
        "(" -> stringResource(R.string.key_open_paren)
        ")" -> stringResource(R.string.key_close_paren)
        "." -> stringResource(R.string.key_decimal)
        "sqrt" -> stringResource(R.string.key_sqrt)
        "⌫" -> stringResource(R.string.key_backspace)
        "⏎" -> stringResource(R.string.key_enter)
        "$" -> stringResource(R.string.key_dollar)
        "#" -> stringResource(R.string.key_hash)
        else -> keyDef.label
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(
    keyDef: KeyDef,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val keyDescription = keyContentDescription(keyDef)

    val backgroundColor = when (keyDef.type) {
        KeyType.DIGIT -> MaterialTheme.colorScheme.surface
        KeyType.OPERATOR, KeyType.FUNCTION -> MaterialTheme.colorScheme.secondaryContainer
        KeyType.BACKSPACE -> MaterialTheme.colorScheme.errorContainer
        KeyType.ENTER -> MaterialTheme.colorScheme.tertiaryContainer
        KeyType.SPECIAL_DOLLAR, KeyType.SPECIAL_HASH -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when (keyDef.type) {
        KeyType.DIGIT -> MaterialTheme.colorScheme.onSurface
        KeyType.OPERATOR, KeyType.FUNCTION -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyType.BACKSPACE -> MaterialTheme.colorScheme.onErrorContainer
        KeyType.ENTER -> MaterialTheme.colorScheme.onTertiaryContainer
        KeyType.SPECIAL_DOLLAR, KeyType.SPECIAL_HASH -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .testTag("key_${keyDef.label}")
            .semantics { contentDescription = keyDescription }
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTap()
                },
                onLongClick = if (onLongPress != null) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                } else null
            ),
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = keyDef.label,
                color = textColor,
                fontSize = if (keyDef.type == KeyType.FUNCTION) 14.sp else 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
