/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.demos.text2.BasicSecureTextFieldDemos
import androidx.compose.foundation.demos.text2.BasicTextFieldCustomPinFieldDemo
import androidx.compose.foundation.demos.text2.BasicTextFieldDemos
import androidx.compose.foundation.demos.text2.BasicTextFieldInScrollableDemo
import androidx.compose.foundation.demos.text2.BasicTextFieldInputTransformationDemos
import androidx.compose.foundation.demos.text2.BasicTextFieldLongTextDemo
import androidx.compose.foundation.demos.text2.BasicTextFieldOutputTransformationDemos
import androidx.compose.foundation.demos.text2.BasicTextFieldValueCallbackDemo
import androidx.compose.foundation.demos.text2.BasicTextFieldsInDialogDemo
import androidx.compose.foundation.demos.text2.DecorationBoxDemos
import androidx.compose.foundation.demos.text2.KeyboardActionsDemos
import androidx.compose.foundation.demos.text2.KeyboardOptionsDemos
import androidx.compose.foundation.demos.text2.NestedReceiveContentDemo
import androidx.compose.foundation.demos.text2.ScrollableDemos
import androidx.compose.foundation.demos.text2.ScrollableDemosRtl
import androidx.compose.foundation.demos.text2.SwapFieldSameStateDemo
import androidx.compose.foundation.demos.text2.TextFieldCursorNotBlinkingInUnfocusedWindowDemo
import androidx.compose.foundation.demos.text2.TextFieldLineLimitsDemos
import androidx.compose.foundation.demos.text2.TextFieldReceiveContentDemo
import androidx.compose.foundation.layout.samples.windowInsetsNestedScrollDemo
import androidx.compose.foundation.samples.BasicTextFieldUndoSample
import androidx.compose.foundation.samples.HandwritingDetectorSample
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.ui.text.samples.AnnotatedStringFromHtml

val TextDemos =
    DemoCategory(
        "Text",
        listOf(
            ComposableDemo("Text Accessibility") { TextAccessibilityDemo() },
            DemoCategory(
                "Text Canvas",
                listOf(
                    ComposableDemo("Brush") { TextBrushDemo() },
                    ComposableDemo("Brush Span") { BrushAnimatingSpanDemo() },
                    ComposableDemo("drawText") { DrawTextDemo() },
                    ComposableDemo("Stroke") { TextStrokeDemo() }
                )
            ),
            DemoCategory(
                "Animation",
                listOf(
                    ComposableDemo("color = { animatedColor.value }") { TextColorAnimation() },
                    ComposableDemo("GraphicsLayer (skew, scale, etc)") { TextAnimationDemo() },
                )
            ),
            DemoCategory(
                "Text Layout",
                listOf(
                    ComposableDemo("Static text") { TextDemo() },
                    DemoCategory(
                        "Line breaking",
                        listOf(
                            ComposableDemo("Line Break") { TextLineBreakDemo() },
                            ComposableDemo("Hyphens") { TextDemoHyphens() },
                            ComposableDemo("Ellipsize") { EllipsizeDemo() },
                            ComposableDemo("Ellipsize and letterspacing") {
                                EllipsizeWithLetterSpacing()
                            },
                            ComposableDemo("Letterspacing") { LetterSpacingDemo() }
                        )
                    ),
                    DemoCategory(
                        "Text Overflow",
                        listOf(
                            ComposableDemo("TextOverflow demo") { TextOverflowDemo() },
                            ComposableDemo("Visible overflow in drawText") {
                                TextOverflowVisibleInDrawText()
                            },
                            ComposableDemo("Visible overflow in Popup") {
                                TextOverflowVisibleInPopupDemo()
                            },
                            ComposableDemo("Min/max lines") { BasicTextMinMaxLinesDemo() },
                            ComposableDemo("Get last character after clip") {
                                LastClippedCharacterDemo()
                            }
                        )
                    ),
                    ComposableDemo("IncludeFontPadding & Clip") { TextFontPaddingDemo() },
                    ComposableDemo("Line Height Behavior") { TextLineHeightDemo() },
                    ComposableDemo("Layout Reuse") { TextReuseLayoutDemo() },
                    ComposableDemo("Multi paragraph") { MultiParagraphDemo() },
                )
            ),
            DemoCategory(
                "Fonts",
                listOf(
                    ComposableDemo("Typeface") { TypefaceDemo() },
                    ComposableDemo("Variable Fonts") { VariableFontsDemo() },
                    ComposableDemo("FontFamily fallback") { FontFamilyDemo() },
                    ComposableDemo("All system font families") { SystemFontFamilyDemo() },
                    ComposableDemo("Emoji Compat") { EmojiCompatDemo() },
                )
            ),
            DemoCategory(
                "Legacy Text Input (BasicTextFieldv1)",
                listOf(
                    ComposableDemo("Basic input fields") { InputFieldDemo() },
                    ComposableDemo("Capitalization/AutoCorrect") {
                        CapitalizationAutoCorrectDemo()
                    },
                    DemoCategory(
                        "Cursor",
                        listOf(
                            ComposableDemo("Cursor configuration") {
                                TextFieldCursorBlinkingDemo()
                            },
                            ComposableDemo("Unfocused window") {
                                CursorNotBlinkingInUnfocusedWindowDemo()
                            }
                        )
                    ),
                    DemoCategory(
                        "Focus",
                        listOf(
                            ComposableDemo("Focus transition") { TextFieldFocusTransition() },
                            ComposableDemo("Focus keyboard interaction") {
                                TextFieldFocusKeyboardInteraction()
                            },
                            ComposableDemo("Focus immediately") { FocusTextFieldImmediatelyDemo() },
                            ComposableDemo("TextField focus") { TextFieldFocusDemo() },
                        )
                    ),
                    ComposableDemo("Full-screen field") { FullScreenTextFieldDemo() },
                    ComposableDemo("Ime Action") { ImeActionDemo() },
                    ComposableDemo("Ime SingleLine") { ImeSingleLineDemo() },
                    ComposableDemo("Inside Dialog") { TextFieldsInDialogDemo() },
                    ComposableDemo("Inside scrollable") { TextFieldsInScrollableDemo() },
                    ComposableDemo("Keyboard Types") { KeyboardTypeDemo() },
                    ComposableDemo("Min/Max Lines") { BasicTextFieldMinMaxDemo() },
                    ComposableDemo("Reject Text Change") { RejectTextChangeDemo() },
                    ComposableDemo("Scrollable text fields") { ScrollableTextFieldDemo() },
                    ComposableDemo("Visual Transformation") { VisualTransformationDemo() },
                    ComposableDemo("TextFieldValue") { TextFieldValueDemo() },
                    ComposableDemo("Tail Following Text Field") { TailFollowingTextFieldDemo() },
                    ComposableDemo("Secondary input system") { PlatformTextInputAdapterDemo() },
                    ComposableDemo("Read-only field") { ReadOnlyTextFieldDemo() },
                    ComposableDemo("Interop") { InteropTextFieldDemo() }
                )
            ),
            DemoCategory(
                "Text Input (BasicTextFieldv2)",
                listOf(
                    ComposableDemo("Basic text input") { BasicTextFieldDemos() },
                    ComposableDemo("Value/callback overload") { BasicTextFieldValueCallbackDemo() },
                    ComposableDemo("Keyboard Options") { KeyboardOptionsDemos() },
                    ComposableDemo("Keyboard Actions") { KeyboardActionsDemos() },
                    ComposableDemo("Decoration Box") { DecorationBoxDemos() },
                    ComposableDemo("Line limits") { TextFieldLineLimitsDemos() },
                    DemoCategory(
                        "Scroll",
                        listOf(
                            ComposableDemo("Ltr") { ScrollableDemos() },
                            ComposableDemo("Rtl") { ScrollableDemosRtl() },
                        )
                    ),
                    ComposableDemo("Inside Scrollable") { BasicTextFieldInScrollableDemo() },
                    ComposableDemo("Input Transformation") {
                        BasicTextFieldInputTransformationDemos()
                    },
                    DemoCategory(
                        "Receive Content",
                        listOf(
                            ComposableDemo("Basic") { TextFieldReceiveContentDemo() },
                            ComposableDemo("Nested") { NestedReceiveContentDemo() },
                        )
                    ),
                    ComposableDemo("Output Transformation") {
                        BasicTextFieldOutputTransformationDemos()
                    },
                    ComposableDemo("Secure Field") { BasicSecureTextFieldDemos() },
                    ComposableDemo("Inside Dialog") { BasicTextFieldsInDialogDemo() },
                    ComposableDemo("Swap the field but reuse the state") {
                        SwapFieldSameStateDemo()
                    },
                    ComposableDemo("Custom PIN field") { BasicTextFieldCustomPinFieldDemo() },
                    ComposableDemo("Undo/Redo") { BasicTextFieldUndoSample() },
                    ComposableDemo("Long text") { BasicTextFieldLongTextDemo() },
                    ComposableDemo("Cursor") { TextFieldCursorNotBlinkingInUnfocusedWindowDemo() },
                    ComposableDemo("Handwriting detector") { HandwritingDetectorSample() }
                )
            ),
            DemoCategory(
                "Selection",
                listOf(
                    ComposableDemo("Text selection") { TextSelectionDemo() },
                    ComposableDemo("Text selection sample") { TextSelectionSample() },
                    ComposableDemo("Overflowed Selection") { TextOverflowedSelectionDemo() },
                    ComposableDemo("Scrollable Column Text Selection") {
                        TextScrollableColumnSelectionDemo()
                    },
                    ComposableDemo("Selection Minimum Touch Target") {
                        MinTouchTargetTextSelection()
                    },
                    ComposableDemo("Selection & DropdownMenu") { DropdownMenuSelection() },
                    ComposableDemo("Selection in Popup") { SelectionPopupDemo() },
                )
            ),
            DemoCategory(
                "\uD83D\uDD75️️️ Memory allocs",
                listOf(
                    ComposableDemo("\uD83D\uDD75️ SetText") { MemoryAllocsSetText() },
                    ComposableDemo("\uD83D\uDD75️ IfNotEmptyText") { MemoryAllocsIfNotEmptyText() },
                    ComposableDemo("\uD83E\uDDA5 LazyList reuse") { MemoryAllocsLazyList() }
                )
            ),
            ComposableDemo("\uD83D\uDD17 Hyperlinks") { Hyperlinks() },
            ComposableDemo("Text Pointer Icon") { TextPointerIconDemo() },
            ComposableDemo("Html") { AnnotatedStringFromHtml() },
            ComposableDemo("Show/hide keyboard via scroll") { windowInsetsNestedScrollDemo() },
        )
    )
