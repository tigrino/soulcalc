/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.tigr.soulcalc.R

/**
 * Custom calculator keyboard.
 *
 * Layout:
 * ```
 *  ^    #   $   =   ⌫
 *  7    8   9   ÷   (
 *  4    5   6   ×   )
 *  1    2   3   −   ⏎ (tall)
 *  0    .   %   +   ⏎ (tall)
 * ```
 *
 * Long-press ^ for sqrt(
 * Long-press ⌫ to clear line
 * Long-press $ to insert result reference
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
    val topRows = listOf(
        listOf(
            KeyDef("^", KeyType.POWER),
            KeyDef("#", KeyType.SPECIAL_HASH),
            KeyDef("$", KeyType.SPECIAL_DOLLAR),
            KeyDef("=", KeyType.OPERATOR),
            KeyDef("⌫", KeyType.BACKSPACE)
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
        )
    )

    val bottomRows = listOf(
        listOf(
            KeyDef("1", KeyType.DIGIT),
            KeyDef("2", KeyType.DIGIT),
            KeyDef("3", KeyType.DIGIT),
            KeyDef("−", KeyType.OPERATOR)
        ),
        listOf(
            KeyDef("0", KeyType.DIGIT),
            KeyDef(".", KeyType.DIGIT),
            KeyDef("%", KeyType.OPERATOR),
            KeyDef("+", KeyType.OPERATOR)
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
            // Top 3 rows (5 buttons each)
            topRows.forEach { row ->
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
                                    KeyType.POWER -> onKeyPress("^")
                                    KeyType.BACKSPACE -> onBackspace()
                                    KeyType.ENTER -> onEnter()
                                    KeyType.SPECIAL_DOLLAR -> onDollarKey()
                                    KeyType.SPECIAL_HASH -> onHashKey()
                                }
                            },
                            onLongPress = when (keyDef.type) {
                                KeyType.BACKSPACE -> onClearLine
                                KeyType.SPECIAL_DOLLAR -> onDollarLongPress
                                KeyType.POWER -> { { onKeyPress("sqrt(") } }
                                else -> null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bottom 2 rows with tall Enter button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Left side: 4 buttons per row, stacked
                Column(
                    modifier = Modifier.weight(4f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bottomRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { keyDef ->
                                KeyButton(
                                    keyDef = keyDef,
                                    onTap = { onKeyPress(keyDef.label) },
                                    onLongPress = null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Right side: tall Enter button
                KeyButton(
                    keyDef = KeyDef("⏎", KeyType.ENTER),
                    onTap = onEnter,
                    onLongPress = null,
                    modifier = Modifier
                        .weight(1f)
                        .height(116.dp)  // 2 * 56dp + 4dp gap
                )
            }
        }
    }
}

private enum class KeyType {
    DIGIT,
    OPERATOR,
    POWER,
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
        "^" -> stringResource(R.string.key_power_sqrt)
        "%" -> stringResource(R.string.key_percent)
        "=" -> stringResource(R.string.key_equals)
        "(" -> stringResource(R.string.key_open_paren)
        ")" -> stringResource(R.string.key_close_paren)
        "." -> stringResource(R.string.key_decimal)
        "⌫" -> stringResource(R.string.key_backspace)
        "⏎" -> stringResource(R.string.key_enter)
        "$" -> stringResource(R.string.key_dollar)
        "#" -> stringResource(R.string.key_hash)
        else -> keyDef.label
    }
}

private const val LONG_PRESS_THRESHOLD_MS = 400L

@Composable
private fun KeyButton(
    keyDef: KeyDef,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val keyDescription = keyContentDescription(keyDef)
    var showAltLabel by remember { mutableStateOf(false) }

    val backgroundColor = when (keyDef.type) {
        KeyType.DIGIT -> MaterialTheme.colorScheme.surface
        KeyType.OPERATOR, KeyType.POWER -> MaterialTheme.colorScheme.secondaryContainer
        KeyType.BACKSPACE, KeyType.ENTER -> MaterialTheme.colorScheme.errorContainer
        KeyType.SPECIAL_DOLLAR, KeyType.SPECIAL_HASH -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when (keyDef.type) {
        KeyType.DIGIT -> MaterialTheme.colorScheme.onSurface
        KeyType.OPERATOR, KeyType.POWER -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyType.BACKSPACE, KeyType.ENTER -> MaterialTheme.colorScheme.onErrorContainer
        KeyType.SPECIAL_DOLLAR, KeyType.SPECIAL_HASH -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val displayLabel = if (showAltLabel && keyDef.type == KeyType.POWER) "√" else keyDef.label

    Surface(
        modifier = modifier
            .height(56.dp)
            .testTag("key_${keyDef.label}")
            .semantics { contentDescription = keyDescription }
            .pointerInput(keyDef, onTap, onLongPress) {
                detectTapGestures(
                    onPress = { _ ->
                        if (onLongPress != null) {
                            coroutineScope {
                                val longPressJob = launch {
                                    delay(LONG_PRESS_THRESHOLD_MS)
                                    showAltLabel = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                val released = tryAwaitRelease()
                                longPressJob.cancel()
                                if (released && showAltLabel) {
                                    onLongPress()
                                } else if (released) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTap()
                                }
                                showAltLabel = false
                            }
                        } else {
                            val released = tryAwaitRelease()
                            if (released) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTap()
                            }
                        }
                    }
                )
            },
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
