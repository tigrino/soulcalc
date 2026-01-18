/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

/**
 * Recursive descent parser for calculator expressions.
 *
 * Grammar:
 * ```
 * expression    → assignment | computation
 * assignment    → VARIABLE "=" computation
 * computation   → term (("+"|"-") term)*
 * term          → factor (("×"|"*"|"÷"|"/") factor)*
 * factor        → power ("%")?
 * power         → unary ("^" unary)*
 * unary         → ("-")? primary
 * primary       → NUMBER | VARIABLE | LINE_REF | "(" computation ")" | function
 * function      → "sqrt" "(" computation ")"
 * ```
 */
class Parser(private val tokens: List<Token>) {
    private var position: Int = 0

    private val currentToken: Token?
        get() = if (position < tokens.size) tokens[position] else null

    private val currentType: TokenType?
        get() = currentToken?.type

    /**
     * Result of parsing: either a valid AST or an error message.
     */
    sealed class ParseResult {
        data class Success(val node: AstNode) : ParseResult()
        data class Error(val message: String, val position: Int) : ParseResult()
        data object Empty : ParseResult()
    }

    /**
     * Parses the token stream and returns a ParseResult.
     */
    fun parse(): ParseResult {
        if (tokens.isEmpty() || (tokens.size == 1 && tokens[0].type == TokenType.EOF)) {
            return ParseResult.Empty
        }

        return try {
            val node = parseExpression()
            if (currentType != TokenType.EOF) {
                ParseResult.Error("Unexpected token: ${currentToken?.value}", currentToken?.position ?: 0)
            } else {
                ParseResult.Success(node)
            }
        } catch (e: ParseException) {
            ParseResult.Error(e.message ?: "Parse error", e.position)
        }
    }

    private fun parseExpression(): AstNode {
        // Check for assignment: VARIABLE "=" computation
        if (currentType == TokenType.VARIABLE) {
            val varToken = currentToken!!
            val nextPos = position + 1
            if (nextPos < tokens.size && tokens[nextPos].type == TokenType.EQUALS) {
                position++ // consume variable
                position++ // consume equals
                val expr = parseComputation()
                return AssignmentNode(varToken.value, expr)
            }
        }
        return parseComputation()
    }

    private fun parseComputation(): AstNode {
        var left = parseTerm()

        while (currentType == TokenType.PLUS || currentType == TokenType.MINUS) {
            val opType = currentType!!
            position++ // consume operator

            val right = parseTerm()

            // Handle percentage context: if right side ends with %, it uses left as base
            val adjustedRight = applyPercentageContext(right, left)

            val op = if (opType == TokenType.PLUS) BinaryOp.ADD else BinaryOp.SUBTRACT
            left = BinaryOpNode(left, op, adjustedRight)
        }

        return left
    }

    private fun parseTerm(): AstNode {
        var left = parseFactor()

        while (currentType == TokenType.MULTIPLY || currentType == TokenType.DIVIDE) {
            val op = if (currentType == TokenType.MULTIPLY) BinaryOp.MULTIPLY else BinaryOp.DIVIDE
            position++ // consume operator
            val right = parseFactor()
            left = BinaryOpNode(left, op, right)
        }

        return left
    }

    private fun parseFactor(): AstNode {
        val base = parsePower()

        // Check for postfix %
        if (currentType == TokenType.PERCENT) {
            position++ // consume %
            // Standalone percentage (context will be applied by parseComputation if needed)
            return PercentNode(base, null)
        }

        return base
    }

    private fun parsePower(): AstNode {
        val base = parseUnary()

        // Power is right-associative: 2^3^4 = 2^(3^4)
        if (currentType == TokenType.POWER) {
            position++ // consume ^
            val exponent = parsePower() // right-recursive for right associativity
            return BinaryOpNode(base, BinaryOp.POWER, exponent)
        }

        return base
    }

    private fun parseUnary(): AstNode {
        if (currentType == TokenType.MINUS) {
            position++ // consume -
            val operand = parseUnary()
            return UnaryMinusNode(operand)
        }
        return parsePrimary()
    }

    private fun parsePrimary(): AstNode {
        val token = currentToken ?: throw ParseException("Unexpected end of input", position)

        return when (token.type) {
            TokenType.NUMBER -> {
                position++
                NumberNode(token.value.toDouble())
            }

            TokenType.VARIABLE -> {
                position++
                VariableNode(token.value)
            }

            TokenType.LINE_REF -> {
                position++
                val lineNum = token.value.toIntOrNull()
                    ?: throw ParseException("Invalid line reference: \$${token.value}", token.position)
                LineRefNode(lineNum)
            }

            TokenType.LPAREN -> {
                position++ // consume (
                val expr = parseComputation()
                if (currentType != TokenType.RPAREN) {
                    throw ParseException("Expected ')'", currentToken?.position ?: position)
                }
                position++ // consume )
                expr
            }

            TokenType.SQRT -> {
                position++ // consume sqrt
                if (currentType != TokenType.LPAREN) {
                    throw ParseException("Expected '(' after sqrt", currentToken?.position ?: position)
                }
                position++ // consume (
                val arg = parseComputation()
                if (currentType != TokenType.RPAREN) {
                    throw ParseException("Expected ')'", currentToken?.position ?: position)
                }
                position++ // consume )
                FunctionNode("sqrt", arg)
            }

            TokenType.EOF -> {
                throw ParseException("Unexpected end of input", token.position)
            }

            TokenType.ERROR -> {
                throw ParseException("Invalid token: ${token.value}", token.position)
            }

            else -> {
                throw ParseException("Unexpected token: ${token.value}", token.position)
            }
        }
    }

    /**
     * Applies percentage context to a node.
     * If the node is a PercentNode without a base, applies the given base.
     */
    private fun applyPercentageContext(node: AstNode, base: AstNode): AstNode {
        return when (node) {
            is PercentNode -> {
                if (node.base == null) {
                    PercentNode(node.operand, base)
                } else {
                    node
                }
            }
            else -> node
        }
    }

    private class ParseException(message: String, val position: Int) : Exception(message)
}

/**
 * Convenience function to parse an input string.
 */
fun parseExpression(input: String): Parser.ParseResult {
    val tokens = Lexer(input).tokenize()
    return Parser(tokens).parse()
}
