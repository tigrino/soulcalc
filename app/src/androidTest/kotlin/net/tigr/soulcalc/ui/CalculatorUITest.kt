/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
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
 * UI tests for the calculator keyboard and basic interactions.
 */
@RunWith(AndroidJUnit4::class)
class CalculatorUITest {

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

    private fun getInputText(): String {
        val node = composeTestRule.onAllNodesWithTag("line_input").onFirst()
        val raw = node.fetchSemanticsNode().config
            .filter { it.key.name == "EditableText" }
            .firstOrNull()?.value?.toString() ?: ""
        // AnnotatedString.toString() returns "[text]", extract the inner text
        return if (raw.startsWith("[") && raw.endsWith("]")) {
            raw.substring(1, raw.length - 1)
        } else raw
    }

    private fun getResultText(): String {
        val node = composeTestRule.onAllNodesWithTag("line_result").onFirst()
        val raw = node.fetchSemanticsNode().config
            .filter { it.key.name == "Text" }
            .firstOrNull()?.value?.toString() ?: ""
        // AnnotatedString.toString() returns "[text]", extract the inner text
        return if (raw.startsWith("[") && raw.endsWith("]")) {
            raw.substring(1, raw.length - 1)
        } else raw
    }

    @Before
    fun setUp() {
        waitForKeyboard()
        clearSheet()
    }

    @Test
    fun appLaunches_showsTitle() {
        composeTestRule.onNodeWithText("SoulCalc").assertIsDisplayed()
    }

    @Test
    fun keyboardIsDisplayed() {
        composeTestRule.onNodeWithTag("key_7", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_8", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_9", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_+", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_−", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_×", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_÷", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_sqrt", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_⏎", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_⌫", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun digitButtonPress_insertsDigit() {
        composeTestRule.onNodeWithTag("key_5", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val input = getInputText()
        val result = getResultText()
        assert(input == "5") { "Expected input '5' but got '$input'" }
        assert(result == "5") { "Expected result '5' but got '$result'" }
    }

    @Test
    fun multipleDigitPresses_buildsNumber() {
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_3", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val input = getInputText()
        val result = getResultText()
        assert(input == "123") { "Expected input '123' but got '$input'" }
        assert(result == "123") { "Expected result '123' but got '$result'" }
    }

    @Test
    fun simpleAddition_showsResult() {
        composeTestRule.onNodeWithTag("key_5", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_+", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_3", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getResultText()
        assert(result == "8") { "Expected result '8' but got '$result'" }
    }

    @Test
    fun simpleSubtraction_showsResult() {
        composeTestRule.onNodeWithTag("key_9", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_−", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_4", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getResultText()
        assert(result == "5") { "Expected result '5' but got '$result'" }
    }

    @Test
    fun simpleMultiplication_showsResult() {
        composeTestRule.onNodeWithTag("key_6", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_×", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_7", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getResultText()
        assert(result == "42") { "Expected result '42' but got '$result'" }
    }

    @Test
    fun simpleDivision_showsResult() {
        composeTestRule.onNodeWithTag("key_8", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_÷", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getResultText()
        assert(result == "4") { "Expected result '4' but got '$result'" }
    }

    @Test
    fun backspaceButton_deletesLastCharacter() {
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_2", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_3", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("key_⌫", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val input = getInputText()
        val result = getResultText()
        assert(input == "12") { "Expected input '12' but got '$input'" }
        assert(result == "12") { "Expected result '12' but got '$result'" }
    }

    @Test
    fun enterButton_createsNewLine() {
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("key_⏎", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        // After enter, we should have 2 lines
        val inputNodes = composeTestRule.onAllNodesWithTag("line_input")
        val count = inputNodes.fetchSemanticsNodes().size
        assert(count >= 2) { "Expected at least 2 lines but got $count" }
    }

    @Test
    fun percentageOperation_evaluatesCorrectly() {
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_+", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_1", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_0", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("key_%", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        composeTestRule.waitForIdle()

        val result = getResultText()
        assert(result == "110") { "Expected result '110' but got '$result'" }
    }
}
