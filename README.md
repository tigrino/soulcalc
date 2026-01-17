# SoulCalc — Notepad Calculator for Android

## Overview

SoulCalc is an open-source notepad-style calculator for Android. Users type mathematical expressions line by line, and results appear instantly in a column on the right. The app supports variables, line references, and persistent history.

## Author

Copyright (c) Albert 'Tigr' Zenkoff <albert@tigr.net>

## Acknowledgment

SoulCalc is inspired by [Soulver](https://soulver.app/) by Zac Cohan, who pioneered the notepad calculator concept in 2005. We gratefully acknowledge his innovative work in creating a better way to work with numbers.

---

## Functional Requirements

### Core Calculation

**FR-01: Multiline Input**
Each line in the editor is an independent expression. Users can have unlimited lines per sheet.

**FR-02: Real-time Evaluation**
Results appear instantly as the user types. No "equals" button or explicit evaluation trigger.

**FR-03: Answer Column**
Each line displays its result aligned to the right. The input and result are visually distinct.

**FR-04: Basic Arithmetic**
Supported operators:
- Addition: `+`
- Subtraction: `-`
- Multiplication: `×` or `*`
- Division: `÷` or `/`
- Parentheses: `(` `)`

**FR-05: Percentage Operations**
- Standalone: `10%` → `0.1`
- Additive: `100 + 10%` → `110`
- Subtractive: `100 - 10%` → `90`
- Multiplicative: `100 × 10%` → `10`

**FR-06: Power and Roots**
- Power: `2^8` → `256`
- Square root: `sqrt(16)` → `4`

### Variables and References

**FR-07: Named Variables**
Users can define variables using `$name = value` syntax:
```
$tax = 0.08
$price = 100
$price × $tax
```
Variable names must start with a letter after the `$` prefix and contain only alphanumeric characters and underscores.

**FR-08: Line References**
Users can reference previous line results using `$n` where `n` is the line number:
```
100 + 50        → 150
$1 × 2          → 300
$1 + $2         → 450
```
Line numbers are 1-indexed.

**FR-09: Recalculation Cascade**
When a line is modified, all lines below it that depend on it (directly or indirectly) are recalculated automatically.

**FR-10: Scope Rules**
- Variables are available to all lines below their definition
- Redefining a variable updates its value for all subsequent lines
- Line references always point to the result of that line number, regardless of variable definitions

### Editing

**FR-11: Line Editing**
Users can insert, delete, or edit any line at any time. The editor behaves like a standard text editor.

**FR-12: Comments**
Lines starting with `#` are treated as comments. They are not evaluated and show no result.
```
# Shopping calculation
$price = 49.99
$qty = 3
$price × $qty
```

**FR-13: Blank Lines**
Empty lines are allowed for visual separation. They show no result.

### Persistence

**FR-14: Auto-save**
The current sheet is saved automatically after each change. No manual save action required.

**FR-15: Survive Restart**
On app launch, the sheet is restored exactly as it was left, including cursor position.

### UI/UX

**FR-17: Custom Keyboard**
A custom 5×5 calculator keyboard is the default input method. See Keyboard Specification section for layout.

**FR-18: System Keyboard Switching**
The system keyboard replaces the custom keyboard when:
- User presses `$` (variable/reference input)
- User presses `#` (comment input)

The custom keyboard returns when:
- User presses Enter/newline

**FR-19: Manual Keyboard Toggle**
A toggle button allows manual switching between custom and system keyboards at any time.

**FR-20: Variable Quick-pick**
Long-pressing the `$` key shows a popup list of recently used variables for quick insertion.

**FR-21: Copy Result**
Tapping a result copies it to the clipboard. Visual feedback confirms the action.

**FR-22: Copy All**
A menu action copies the entire sheet as formatted text:
```
100 + 50 = 150
$1 × 2 = 300
```

**FR-23: Clear Sheet**
A menu action clears the current sheet after confirmation.

**FR-24: Theme Support**
The app supports light and dark themes, following system preference by default with manual override option.

### Error Handling

**FR-25: Syntax Errors**
Invalid expressions show an error indicator (e.g., "?" or error icon) in the result column instead of crashing.

**FR-26: Division by Zero**
Division by zero shows "∞" or "Error" in the result column.

**FR-27: Undefined Reference**
Referencing an undefined variable or invalid line number shows an error indicator.

**FR-28: Circular Reference Prevention**
Line references can only point to lines above, preventing circular dependencies by design.

---

## Keyboard Specification

### Layout (5×5 Grid)

```
┌─────┬─────┬─────┬─────┬─────┐
│  7  │  8  │  9  │  ÷  │  ⌫  │
├─────┼─────┼─────┼─────┼─────┤
│  4  │  5  │  6  │  ×  │  (  │
├─────┼─────┼─────┼─────┼─────┤
│  1  │  2  │  3  │  −  │  )  │
├─────┼─────┼─────┼─────┼─────┤
│  0  │  .  │  %  │  +  │  ^  │
├─────┼─────┼─────┼─────┼─────┤
│  $  │  #  │sqrt │  =  │  ⏎  │
└─────┴─────┴─────┴─────┴─────┘
```

### Key Behaviors

| Key | Tap Action | Long-press Action |
|-----|------------|-------------------|
| `0-9` | Insert digit | — |
| `.` | Insert decimal point | — |
| `+ − × ÷` | Insert operator | — |
| `%` | Insert percent | — |
| `^` | Insert power operator | — |
| `( )` | Insert parenthesis | — |
| `sqrt` | Insert `sqrt(` | — |
| `=` | Insert assignment operator | — |
| `⌫` | Delete character before cursor | Clear entire line |
| `⏎` | Insert newline, return to custom keyboard | — |
| `$` | Insert `$`, switch to system keyboard | Show variable picker |
| `#` | Insert `#`, switch to system keyboard | — |

### Toggle Button

A small keyboard icon appears in the corner of the active keyboard. Tapping it switches between custom and system keyboards.

---

## Data Model

### Line

| Field | Type | Description |
|-------|------|-------------|
| id | Int | Unique identifier |
| position | Int | Order in sheet (0-indexed) |
| input | String | Raw user input |
| result | Result | Evaluation result |

### Result (sealed type)

- `Success(value: Double)` — valid numeric result
- `Error(message: String)` — evaluation error
- `Empty` — blank line or comment

### Sheet

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| name | String | Optional display name |
| lines | List<Line> | Ordered lines |
| createdAt | Timestamp | Creation time |
| updatedAt | Timestamp | Last modification time |

### Scope

| Field | Type | Description |
|-------|------|-------------|
| variables | Map<String, Double> | Named variable values |
| lineResults | Map<Int, Double> | Results indexed by line number |

---

## Architecture

### Layer Diagram

```
┌─────────────────────────────────────────┐
│               UI Layer                  │
│         (Jetpack Compose)               │
├─────────────────────────────────────────┤
│             ViewModel                   │
│    (State management, UI events)        │
├─────────────────────────────────────────┤
│            Domain Layer                 │
│  ┌───────────┐  ┌───────────────────┐   │
│  │   Parser  │  │    Evaluator      │   │
│  │           │  │                   │   │
│  │ Tokenize  │  │ Execute AST       │   │
│  │ Build AST │  │ Manage scope      │   │
│  └───────────┘  └───────────────────┘   │
│  ┌───────────────────────────────────┐  │
│  │         Sheet Engine              │  │
│  │   Coordinate parse/evaluate       │  │
│  │   Handle cascade updates          │  │
│  └───────────────────────────────────┘  │
├─────────────────────────────────────────┤
│             Data Layer                  │
│   (Room Database, DataStore)            │
└─────────────────────────────────────────┘
```

### Component Responsibilities

**Parser**
- Tokenize input string into tokens (Number, Operator, Variable, Function, etc.)
- Build Abstract Syntax Tree (AST)
- Handle operator precedence
- Identify comments and empty lines

**Evaluator**
- Walk AST and compute numeric result
- Resolve variable references from scope
- Resolve line references from scope
- Handle percentage context logic
- Return Result type

**Sheet Engine**
- Maintain current sheet state
- On line change: re-evaluate affected lines
- Rebuild scope during evaluation pass
- Debounce rapid input (100ms)

**ViewModel**
- Expose UI state as StateFlow
- Handle UI events (line changed, copy, clear, etc.)
- Coordinate with Sheet Engine and Repository

**Repository**
- Abstract data persistence
- Save and load sheet
- Manage auto-save logic

### File Structure

```
app/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── SheetDao.kt
│   │   └── LineDao.kt
│   ├── model/
│   │   ├── LineEntity.kt
│   │   └── SheetEntity.kt
│   └── repository/
│       └── SheetRepository.kt
├── domain/
│   ├── model/
│   │   ├── Line.kt
│   │   ├── Sheet.kt
│   │   ├── Result.kt
│   │   └── Scope.kt
│   ├── parser/
│   │   ├── Token.kt
│   │   ├── Lexer.kt
│   │   ├── AstNode.kt
│   │   └── Parser.kt
│   ├── evaluator/
│   │   └── Evaluator.kt
│   └── engine/
│       └── SheetEngine.kt
├── ui/
│   ├── screen/
│   │   └── MainScreen.kt
│   ├── component/
│   │   ├── LineRow.kt
│   │   ├── CalculatorKeyboard.kt
│   │   ├── KeyboardToggle.kt
│   │   └── VariablePicker.kt
│   ├── viewmodel/
│   │   └── MainViewModel.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Typography.kt
└── SoulCalcApp.kt
```

---

## Expression Grammar

```
expression    → assignment | computation

assignment    → VARIABLE "=" computation

computation   → term (("+"|"-") term)*

term          → factor (("×"|"*"|"÷"|"/") factor)*

factor        → power ("%")?

power         → unary ("^" unary)*

unary         → ("-")? primary

primary       → NUMBER
              | VARIABLE
              | LINE_REF
              | "(" computation ")"
              | function

function      → "sqrt" "(" computation ")"

NUMBER        → [0-9]+ ("." [0-9]+)?
VARIABLE      → "$" [a-zA-Z][a-zA-Z0-9_]*
LINE_REF      → "$" [0-9]+
```

### Operator Precedence (lowest to highest)

1. Assignment `=`
2. Addition, Subtraction `+` `-`
3. Multiplication, Division `×` `÷`
4. Percentage `%` (postfix)
5. Power `^`
6. Unary minus `-`
7. Function call, Parentheses

---

## Percentage Semantics

The `%` operator behavior depends on context:

| Expression | Interpretation | Result |
|------------|---------------|--------|
| `10%` | Standalone percentage | `0.1` |
| `100 + 10%` | Add 10% of 100 | `110` |
| `100 - 10%` | Subtract 10% of 100 | `90` |
| `100 × 10%` | Multiply by 10% | `10` |
| `100 ÷ 10%` | Divide by 10% | `1000` |
| `(5 + 5)%` | Percentage of result | `0.1` |

Implementation: When `%` follows a value that is itself the right operand of `+` or `-`, apply percentage to the left operand of that addition/subtraction.

---

## UI Specifications

### Main Screen Layout

```
┌─────────────────────────────────────┐
│ ≡  SoulCalc                    ⋮   │  ← Top bar with menu
├─────────────────────────────────────┤
│ # Monthly budget                    │
│ $income = 5000              5000   │
│ $rent = 1500                1500   │
│ $utilities = 200             200   │
│ $income - $rent - $utilities       │
│                              3300   │
│                                     │
│                                     │  ← Scrollable line list
│                                     │
│                                     │
├─────────────────────────────────────┤
│ ┌─────┬─────┬─────┬─────┬─────┐    │
│ │  7  │  8  │  9  │  ÷  │  ⌫  │    │
│ ├─────┼─────┼─────┼─────┼─────┤    │
│ │  4  │  5  │  6  │  ×  │  (  │    │
│ ├─────┼─────┼─────┼─────┼─────┤    │  ← Custom keyboard
│ │  1  │  2  │  3  │  −  │  )  │    │
│ ├─────┼─────┼─────┼─────┼─────┤    │
│ │  0  │  .  │  %  │  +  │  ^  │    │
│ ├─────┼─────┼─────┼─────┼─────┤    │
│ │  $  │  #  │sqrt │  =  │  ⏎  │ ⌨  │  ← Toggle button
│ └─────┴─────┴─────┴─────┴─────┘    │
└─────────────────────────────────────┘
```

### Line Row Layout

```
┌────────────────────────────┬────────┐
│ $price × $qty              │  149.97│
└────────────────────────────┴────────┘
  ↑ Input (editable)           ↑ Result (tap to copy)
```

- Input area takes ~70% width
- Result area takes ~30% width
- Result right-aligned
- Different background or separator between input and result

### Menu Options

**Overflow menu (⋮)**
- Copy all
- Clear sheet
- Settings

**Settings**
- Theme (System / Light / Dark)
- About

---

## Technical Notes

### Debouncing

Evaluation is debounced by 100ms to avoid excessive computation during rapid typing.

### Number Formatting

- Results display up to 10 significant digits
- Very large or small numbers use scientific notation
- Trailing zeros after decimal are trimmed

### Clipboard Format

**Copy single result:** Plain number (e.g., `149.97`)

**Copy all:** Each line formatted as `expression = result`, one per line:
```
$price = 49.99 = 49.99
$qty = 3 = 3
$price × $qty = 149.97
```

### Error Messages

| Error | Display |
|-------|---------|
| Syntax error | `?` |
| Division by zero | `∞` |
| Undefined variable | `? $name` |
| Invalid line reference | `? $n` |

---

## Future Considerations (Out of Scope)

The following features are explicitly not included in this version but may be considered for future releases:

- Multiple sheets / sheet history
- Unit conversion
- Currency conversion
- Scientific functions beyond sqrt
- Export to file
- Cloud sync
- Landscape keyboard layout
- Tablet-optimized layout
