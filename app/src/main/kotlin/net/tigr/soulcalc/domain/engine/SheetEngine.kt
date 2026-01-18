/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.engine

import net.tigr.soulcalc.domain.evaluator.Evaluator
import net.tigr.soulcalc.domain.model.Line
import net.tigr.soulcalc.domain.model.Result
import net.tigr.soulcalc.domain.model.Scope
import net.tigr.soulcalc.domain.parser.LineClassifier
import net.tigr.soulcalc.domain.parser.parseExpression

/**
 * Coordinates multi-line evaluation with scope management.
 *
 * The SheetEngine evaluates lines top-to-bottom, building up the scope
 * as it goes. Variables defined on earlier lines are available to later
 * lines. Line results are stored and can be referenced via $n syntax.
 *
 * When a line changes, all lines from that point forward are re-evaluated
 * to ensure cascade updates work correctly.
 *
 * Thread Safety: All public methods are synchronized to prevent race conditions
 * when accessed from multiple threads.
 */
class SheetEngine {

    private val lock = Any()
    private var lines: MutableList<Line> = mutableListOf()
    private var currentScope: Scope = Scope()

    /**
     * Evaluates a list of input strings and returns the resulting lines.
     *
     * @param inputs List of raw input strings, one per line
     * @return List of Line objects with evaluation results
     */
    fun evaluate(inputs: List<String>): List<Line> = synchronized(lock) {
        evaluateInternal(inputs)
    }

    /**
     * Internal evaluation without locking (must be called within synchronized block).
     */
    private fun evaluateInternal(inputs: List<String>): List<Line> {
        lines = mutableListOf()
        currentScope = Scope()

        inputs.forEachIndexed { index, input ->
            val line = evaluateLine(index, input)
            lines.add(line)
        }

        return lines.toList()
    }

    /**
     * Updates a single line and re-evaluates all affected lines.
     *
     * @param index The 0-based index of the line to update
     * @param input The new input string
     * @return The updated list of all lines
     */
    fun updateLine(index: Int, input: String): List<Line> = synchronized(lock) {
        if (index < 0) return@synchronized lines.toList()

        // Expand list if needed
        while (lines.size <= index) {
            lines.add(Line(id = lines.size, position = lines.size, input = "", result = Result.Empty))
        }

        // Re-evaluate from the changed line onwards
        val inputs = lines.mapIndexed { i, line ->
            if (i == index) input else line.input
        }

        evaluateInternal(inputs)
    }

    /**
     * Appends a new line and evaluates it.
     *
     * @param input The input string for the new line
     * @return The updated list of all lines
     */
    fun appendLine(input: String): List<Line> = synchronized(lock) {
        val newIndex = lines.size
        val line = evaluateLine(newIndex, input)
        lines.add(line)
        lines.toList()
    }

    /**
     * Removes a line and re-evaluates all subsequent lines.
     * Updates line references ($N) in all lines to maintain correct references.
     *
     * @param index The 0-based index of the line to remove
     * @return The updated list of all lines
     */
    fun removeLine(index: Int): List<Line> = synchronized(lock) {
        if (index < 0 || index >= lines.size) return@synchronized lines.toList()

        val removedLineNumber = index + 1
        val inputs = lines.mapIndexedNotNull { i, line ->
            if (i == index) null else updateLineReferencesAfterRemove(line.input, removedLineNumber)
        }

        evaluateInternal(inputs)
    }

    /**
     * Inserts a new line at the specified index and re-evaluates.
     * Updates line references ($N) in all lines to maintain correct references.
     *
     * @param index The 0-based index where to insert
     * @param input The input string for the new line
     * @return The updated list of all lines
     */
    fun insertLine(index: Int, input: String): List<Line> = synchronized(lock) {
        val safeIndex = index.coerceIn(0, lines.size)
        val insertedLineNumber = safeIndex + 1
        val inputs = lines.map { updateLineReferencesAfterInsert(it.input, insertedLineNumber) }.toMutableList()
        inputs.add(safeIndex, input)
        evaluateInternal(inputs)
    }

    /**
     * Updates line references ($N) after a line is inserted.
     * All references to lines >= insertedLineNumber are incremented by 1.
     */
    private fun updateLineReferencesAfterInsert(input: String, insertedLineNumber: Int): String {
        return LINE_REFERENCE_REGEX.replace(input) { match ->
            val refNumber = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            if (refNumber >= insertedLineNumber) {
                "\$${refNumber + 1}"
            } else {
                match.value
            }
        }
    }

    /**
     * Updates line references ($N) after a line is removed.
     * All references to lines > removedLineNumber are decremented by 1.
     * References to the removed line itself are left unchanged (will become invalid).
     */
    private fun updateLineReferencesAfterRemove(input: String, removedLineNumber: Int): String {
        return LINE_REFERENCE_REGEX.replace(input) { match ->
            val refNumber = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            if (refNumber > removedLineNumber) {
                "\$${refNumber - 1}"
            } else {
                match.value
            }
        }
    }

    companion object {
        private val LINE_REFERENCE_REGEX = Regex("""\$(\d+)""")
    }

    /**
     * Returns the current list of lines.
     */
    fun getLines(): List<Line> = synchronized(lock) { lines.toList() }

    /**
     * Returns the current scope after evaluation.
     */
    fun getScope(): Scope = synchronized(lock) { currentScope }

    /**
     * Clears all lines and resets to a single empty line.
     */
    fun clear(): List<Line> = synchronized(lock) {
        currentScope = Scope()
        lines = mutableListOf(Line(id = 0, position = 0, input = "", result = Result.Empty))
        lines.toList()
    }

    private fun evaluateLine(index: Int, input: String): Line {
        val lineNumber = index + 1 // 1-based for user display and references

        val result = when (LineClassifier.classify(input)) {
            LineClassifier.LineType.EMPTY -> Result.Empty
            LineClassifier.LineType.COMMENT -> Result.Empty
            LineClassifier.LineType.EXPRESSION -> {
                val parseResult = parseExpression(input)
                val evaluator = Evaluator(currentScope)
                val evalResult = evaluator.evaluate(parseResult)

                // Update scope with any new variables
                currentScope = evalResult.newScope

                // Store line result for future references
                if (evalResult.result is Result.Success) {
                    currentScope = currentScope.withLineResult(lineNumber, evalResult.result.value)
                }

                evalResult.result
            }
        }

        return Line(
            id = index,
            position = index,
            input = input,
            result = result
        )
    }
}
