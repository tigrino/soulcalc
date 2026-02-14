// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "SoulCalc",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    targets: [
        // Domain logic only - pure Swift, no SwiftUI dependency, testable on all platforms
        .target(
            name: "SoulCalcDomain",
            path: "SoulCalc/Domain"
        ),
        .testTarget(
            name: "SoulCalcTests",
            dependencies: ["SoulCalcDomain"],
            path: "Tests/SoulCalcTests",
            exclude: ["MainViewModelTests.swift"]
        ),
    ]
)
