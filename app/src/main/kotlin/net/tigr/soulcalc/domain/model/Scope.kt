/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.model

/**
 * Holds variable bindings and line results for expression evaluation.
 *
 * @property variables Named variable values (e.g., "tax" -> 0.08)
 * @property lineResults Results indexed by 1-based line number
 */
data class Scope(
    val variables: Map<String, Double> = emptyMap(),
    val lineResults: Map<Int, Double> = emptyMap()
) {
    /** Returns a new Scope with the variable added or updated. */
    fun withVariable(name: String, value: Double): Scope =
        copy(variables = variables + (name to value))

    /** Returns a new Scope with the line result added. */
    fun withLineResult(lineNumber: Int, value: Double): Scope =
        copy(lineResults = lineResults + (lineNumber to value))

    /** Resolves a variable by name, returns null if not found. */
    fun resolveVariable(name: String): Double? = variables[name]

    /** Resolves a line reference by number, returns null if not found. */
    fun resolveLineRef(lineNumber: Int): Double? = lineResults[lineNumber]
}
