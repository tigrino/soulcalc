# SoulCalc for iOS & macOS

Native iOS/macOS port of [SoulCalc](https://github.com/tigrino/soulcalc) — a notepad-style calculator inspired by [Soulver](https://soulver.app). Built with SwiftUI, full feature parity with the Android version.

## Requirements

- iOS 17.0+ / macOS 14.0+ (Mac Catalyst)
- Xcode 15.0+
- Swift 5.9+

## Build & Run

**Xcode (recommended):**
```bash
open SoulCalc.xcodeproj
# Select scheme "SoulCalc", pick destination, Cmd+R
```

**Command line:**
```bash
# iOS Simulator
xcodebuild -project SoulCalc.xcodeproj -scheme SoulCalc \
  -destination 'platform=iOS Simulator,name=iPhone 16' build

# Run domain tests via SPM (no Xcode required)
swift test
```

## Features

### Expressions
Type mathematical expressions, get instant results:

| Operation      | Example            | Result |
|----------------|--------------------|--------|
| Addition       | `10 + 5`           | 15     |
| Subtraction    | `10 - 5`           | 5      |
| Multiplication | `10 × 5`           | 50     |
| Division       | `10 ÷ 5`           | 2      |
| Power          | `2 ^ 8`            | 256    |
| Square root    | `sqrt(16)`         | 4      |
| Percentage     | `10%`              | 0.1    |
| Contextual %   | `100 + 10%`        | 110    |
| Parentheses    | `(2 + 3) × 4`     | 20     |

### Variables
```
$tax = 0.08
$price = 100
$price × (1 + $tax)    → 108
```

### Line References
```
100                     → 100
200                     → 200
$1 + $2                 → 300
```
References update automatically when lines are inserted or removed.

### Comments
Lines starting with `#` are treated as comments and not evaluated.

### Custom Keyboard

```
 ^    #    $    =    ⌫
 7    8    9    ÷    (
 4    5    6    ×    )
 1    2    3    −    ⏎ (tall)
 %    0    .    +    ⏎ (tall)
```

Long-press shortcuts:
- `^` → insert `sqrt(`
- `⌫` → clear entire line
- `$` → pick from defined variables

Toggle between custom and system keyboard via toolbar icon.

### Persistence
- Auto-save with 500ms debounce
- Focused line restored on app restart
- JSON file storage (`soulcalc_sheet.json`)

### Themes
Dark / Light / System — configurable in Settings.

## Architecture

MVVM with clean domain separation. The domain layer is pure Swift with no UI dependencies, testable via SPM.

```
SoulCalc/
├── Domain/                     # Pure Swift, no UI deps
│   ├── Parser/
│   │   ├── Token.swift         # Token types
│   │   ├── Lexer.swift         # String → Tokens
│   │   ├── AstNode.swift       # AST node types
│   │   ├── Parser.swift        # Tokens → AST (recursive descent)
│   │   └── LineClassifier.swift
│   ├── Evaluator/
│   │   └── Evaluator.swift     # AST → Double
│   ├── Engine/
│   │   └── SheetEngine.swift   # Multi-line eval, cascade updates
│   └── Model/
│       ├── Line.swift
│       ├── Result.swift
│       └── Scope.swift         # Variables + line references
├── Data/
│   ├── SheetStore.swift        # JSON file persistence
│   └── ThemePreferences.swift  # UserDefaults
└── UI/
    ├── ViewModel/
    │   └── MainViewModel.swift # @MainActor, @Published state
    ├── Screen/
    │   └── MainScreen.swift
    └── Component/
        ├── CalculatorKeyboard.swift
        ├── LineRow.swift
        ├── SettingsView.swift
        ├── GuideView.swift
        ├── AboutView.swift
        └── VariablePickerView.swift
```

### Expression Grammar

```
expression  = assignment | additive
assignment  = "$" identifier "=" additive
additive    = multiplicative (("+"|"-") multiplicative)*
multiplicative = power (("*"|"/") power)*
power       = unary ("^" power)?          # right-associative
unary       = "-"? postfix
postfix     = primary "%"?
primary     = number | variable | lineRef | "(" expression ")" | function
function    = "sqrt" "(" expression ")"
```

Contextual percentage: `A + B%` evaluates as `A + A×(B/100)`, not `A + B/100`.

## Tests

141 unit tests across 6 files:

| File                      | Tests | Coverage                                    |
|---------------------------|-------|---------------------------------------------|
| EvaluatorTests.swift      | 42    | Arithmetic, %, sqrt, variables, errors       |
| MainViewModelTests.swift  | 35    | Lines, focus, keyboard, copy, persistence    |
| ParserTests.swift         | 32    | Parsing, precedence, functions, errors        |
| SheetEngineTests.swift    | 27    | Cascade, insert/remove, refs, scope           |
| LexerTests.swift          | 23    | Tokenization, Unicode ops, edge cases         |
| LineClassifierTests.swift | 17    | Comments, empty, expressions                  |

```bash
# Run domain tests (no Xcode)
swift test

# Run all tests (requires Xcode + Simulator)
xcodebuild test -project SoulCalc.xcodeproj -scheme SoulCalc \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```

Note: `MainViewModelTests` requires SwiftUI and is excluded from SPM builds.

## License

MIT License. Copyright (c) 2026 Albert Zenkoff.
