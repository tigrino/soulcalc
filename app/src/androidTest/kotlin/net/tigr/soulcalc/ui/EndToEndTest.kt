/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.tigr.soulcalc.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for full user flows.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun waitForKeyboard() {
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        composeTestRule.waitForIdle()
    }

    private fun clearSheet() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Clear Sheet").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()
    }

    private fun extractText(raw: String): String {
        // AnnotatedString.toString() returns "[text]", extract the inner text
        return if (raw.startsWith("[") && raw.endsWith("]")) {
            raw.substring(1, raw.length - 1)
        } else raw
    }

    private fun getFirstInputText(): String {
        val node = composeTestRule.onAllNodesWithTag("line_input").onFirst()
        val raw = node.fetchSemanticsNode().config
            .filter { it.key.name == "EditableText" }
            .firstOrNull()?.value?.toString() ?: ""
        return extractText(raw)
    }

    private fun getFirstResultText(): String {
        val node = composeTestRule.onAllNodesWithTag("line_result").onFirst()
        val raw = node.fetchSemanticsNode().config
            .filter { it.key.name == "Text" }
            .firstOrNull()?.value?.toString() ?: ""
        return extractText(raw)
    }

    private fun getLastInputText(): String {
        val node = composeTestRule.onAllNodesWithTag("line_input").onLast()
        val raw = node.fetchSemanticsNode().config
            .filter { it.key.name == "EditableText" }
            .firstOrNull()?.value?.toString() ?: ""
        return extractText(raw)
    }

    private fun getLastResultText(): String {
        val node = composeTestRule.onAllNodesWithTag("line_result").onLast()
        val raw = node.fetchSemanticsNode().config
            .filter { it.key.name == "Text" }
            .firstOrNull()?.value?.toString() ?: ""
        return extractText(raw)
    }

    @Before
    fun setUp() {
        waitForKeyboard()
        clearSheet()
    }

    @Test
    fun flow1_launchApp_typeExpression_seeResult() {
        composeTestRule.onNodeWithText("SoulCalc").assertIsDisplayed()

        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_+", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getFirstResultText()
        assert(result == "4") { "Expected result '4' but got '$result'" }
    }

    @Test
    fun flow2_complexCalculation_withParentheses() {
        composeTestRule.onNodeWithTag("key_(", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_+", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_5", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_)", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_×", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getFirstResultText()
        assert(result == "30") { "Expected result '30' but got '$result'" }
    }

    @Test
    fun flow3_exponentiation() {
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_^", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_8", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getFirstResultText()
        assert(result == "256") { "Expected result '256' but got '$result'" }
    }

    @Test
    fun flow4_clearSheet() {
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_3", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Clear is already done in setUp, so just verify sheet has the typed content
        val input = getFirstInputText()
        assert(input == "123") { "Expected input '123' but got '$input'" }

        // Now clear again
        clearSheet()

        // After clearing, input should be empty
        val clearedInput = getFirstInputText()
        assert(clearedInput.isEmpty()) { "Expected empty input after clear but got '$clearedInput'" }
    }

    @Test
    fun flow5_multiLineCalculation() {
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("key_⏎", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)

        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        // First line should have "100" input and result
        val firstInput = getFirstInputText()
        val firstResult = getFirstResultText()
        assert(firstInput == "100") { "Expected first input '100' but got '$firstInput'" }
        assert(firstResult == "100") { "Expected first result '100' but got '$firstResult'" }

        // Second line should have "200" input and result
        val lastInput = getLastInputText()
        val lastResult = getLastResultText()
        assert(lastInput == "200") { "Expected last input '200' but got '$lastInput'" }
        assert(lastResult == "200") { "Expected last result '200' but got '$lastResult'" }
    }

    @Test
    fun flow6_openSettingsDialog() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("System default").assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun flow7_selectDarkTheme() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Dark").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SoulCalc").assertIsDisplayed()
    }

    @Test
    fun flow8_decimalNumbers() {
        composeTestRule.onNodeWithTag("key_3", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_.", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_4", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val input = getFirstInputText()
        val result = getFirstResultText()
        assert(input == "3.14") { "Expected input '3.14' but got '$input'" }
        assert(result == "3.14") { "Expected result '3.14' but got '$result'" }
    }

    @Test
    fun flow9_negativeResult() {
        composeTestRule.onNodeWithTag("key_5", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_−", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getFirstResultText()
        assert(result == "-5") { "Expected result '-5' but got '$result'" }
    }
}
