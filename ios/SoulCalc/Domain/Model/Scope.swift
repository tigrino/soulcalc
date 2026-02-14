// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Holds variable bindings and line results for expression evaluation.
struct Scope {
    let variables: [String: Double]
    let lineResults: [Int: Double]

    init(variables: [String: Double] = [:], lineResults: [Int: Double] = [:]) {
        self.variables = variables
        self.lineResults = lineResults
    }

    /// Returns a new Scope with the variable added or updated.
    func withVariable(_ name: String, _ value: Double) -> Scope {
        var newVars = variables
        newVars[name] = value
        return Scope(variables: newVars, lineResults: lineResults)
    }

    /// Returns a new Scope with the line result added.
    func withLineResult(_ lineNumber: Int, _ value: Double) -> Scope {
        var newResults = lineResults
        newResults[lineNumber] = value
        return Scope(variables: variables, lineResults: newResults)
    }

    /// Resolves a variable by name, returns nil if not found.
    func resolveVariable(_ name: String) -> Double? {
        variables[name]
    }

    /// Resolves a line reference by number, returns nil if not found.
    func resolveLineRef(_ lineNumber: Int) -> Double? {
        lineResults[lineNumber]
    }
}
