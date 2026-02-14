// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

/// Main screen of the calculator application.
struct MainScreen: View {
    @ObservedObject var viewModel: MainViewModel
    @Binding var themeMode: ThemeMode

    @State private var showMenu = false
    @State private var showSettings = false
    @State private var showGuide = false
    @State private var showAbout = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Lines list
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(Array(viewModel.state.lines.enumerated()), id: \.offset) { index, line in
                                LineRow(
                                    line: line,
                                    lineNumber: index + 1,
                                    isFocused: index == viewModel.state.focusedLineIndex,
                                    pendingInsertion: index == viewModel.state.focusedLineIndex
                                        ? viewModel.state.pendingInsertion : nil,
                                    pendingBackspace: index == viewModel.state.focusedLineIndex
                                        && viewModel.state.pendingBackspace,
                                    pendingClearLine: index == viewModel.state.focusedLineIndex
                                        && viewModel.state.pendingClearLine,
                                    showSystemKeyboard: !viewModel.state.useCustomKeyboard,
                                    onTextChanged: { text in
                                        viewModel.lineTextChanged(index, text)
                                    },
                                    onNewLine: {
                                        viewModel.newLineRequested(index)
                                    },
                                    onDeleteLine: {
                                        viewModel.deleteLineRequested(index)
                                    },
                                    onResultTap: {
                                        if let result = viewModel.copyResult(index) {
                                            #if canImport(UIKit)
                                            UIPasteboard.general.string = result
                                            #elseif canImport(AppKit)
                                            NSPasteboard.general.clearContents()
                                            NSPasteboard.general.setString(result, forType: .string)
                                            #endif
                                        }
                                    },
                                    onFocused: {
                                        viewModel.lineFocused(index)
                                    },
                                    onInsertionConsumed: {
                                        viewModel.insertionConsumed()
                                    },
                                    onBackspaceConsumed: {
                                        viewModel.backspaceConsumed()
                                    },
                                    onClearLineConsumed: {
                                        viewModel.clearLineConsumed()
                                    }
                                )
                                .id(index)
                            }
                        }
                    }
                    .onChange(of: viewModel.state.focusedLineIndex) { _, newIndex in
                        withAnimation {
                            proxy.scrollTo(newIndex, anchor: .center)
                        }
                    }
                    .onChange(of: viewModel.state.lines.count) { _, _ in
                        withAnimation {
                            proxy.scrollTo(viewModel.state.focusedLineIndex, anchor: .center)
                        }
                    }
                }

                // Custom keyboard
                if viewModel.state.useCustomKeyboard {
                    CalculatorKeyboard(
                        onKeyPress: { key in viewModel.keyPressed(key) },
                        onEnter: { viewModel.enterKeyPressed() },
                        onBackspace: { viewModel.backspacePressed() },
                        onClearLine: { viewModel.clearLinePressed() },
                        onDollarKey: { viewModel.dollarKeyPressed() },
                        onDollarLongPress: { viewModel.showVariablePicker() },
                        onHashKey: { viewModel.hashKeyPressed() }
                    )
                }
            }
            .navigationTitle("SoulCalc")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    HStack(spacing: 4) {
                        // Keyboard toggle
                        Button {
                            viewModel.toggleKeyboard()
                        } label: {
                            Image(systemName: viewModel.state.useCustomKeyboard
                                  ? "keyboard" : "keyboard.chevron.compact.down")
                        }

                        // Menu
                        Menu {
                            Button {
                                let text = viewModel.copyAll()
                                if !text.isEmpty {
                                    #if canImport(UIKit)
                                    UIPasteboard.general.string = text
                                    #elseif canImport(AppKit)
                                    NSPasteboard.general.clearContents()
                                    NSPasteboard.general.setString(text, forType: .string)
                                    #endif
                                }
                            } label: {
                                Label("Copy All", systemImage: "doc.on.doc")
                            }

                            Button(role: .destructive) {
                                viewModel.clearSheet()
                            } label: {
                                Label("Clear Sheet", systemImage: "trash")
                            }

                            Divider()

                            Button {
                                showGuide = true
                            } label: {
                                Label("Guide", systemImage: "questionmark.circle")
                            }

                            Button {
                                showSettings = true
                            } label: {
                                Label("Settings", systemImage: "gearshape")
                            }

                            Button {
                                showAbout = true
                            } label: {
                                Label("About", systemImage: "info.circle")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView(currentThemeMode: $themeMode)
                    .presentationDetents([.medium])
            }
            .sheet(isPresented: $showGuide) {
                GuideView()
            }
            .sheet(isPresented: $showAbout) {
                AboutView()
                    .presentationDetents([.medium])
            }
            .sheet(isPresented: Binding(
                get: { viewModel.state.showVariablePicker },
                set: { if !$0 { viewModel.dismissVariablePicker() } }
            )) {
                VariablePickerView(
                    variables: viewModel.state.availableVariables,
                    onVariableSelected: { variable in
                        viewModel.variableSelected(variable)
                    },
                    onDismiss: {
                        viewModel.dismissVariablePicker()
                    }
                )
                .presentationDetents([.medium])
            }
        }
        .overlay(alignment: .bottom) {
            // Toast
            if let message = viewModel.state.toastMessage {
                Text(message)
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.ultraThinMaterial, in: Capsule())
                    .padding(.bottom, viewModel.state.useCustomKeyboard ? 300 : 60)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation {
                                viewModel.toastShown()
                            }
                        }
                    }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: viewModel.state.toastMessage != nil)
    }
}
