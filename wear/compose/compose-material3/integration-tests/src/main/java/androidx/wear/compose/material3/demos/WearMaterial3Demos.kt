/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material3.samples.EdgeSwipeForSwipeToDismiss
import androidx.wear.compose.material3.samples.FixedFontSize
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorSample
import androidx.wear.compose.material3.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material3.samples.StatefulSwipeToDismissBox
import androidx.wear.compose.material3.samples.StepperSample
import androidx.wear.compose.material3.samples.StepperWithIntegerSample
import androidx.wear.compose.material3.samples.StepperWithRangeSemanticsSample

val WearMaterial3Demos = DemoCategory(
    "Material 3",
    listOf(
        DemoCategory(
            "Button",
            listOf(
                ComposableDemo("Button") {
                    ButtonDemo()
                },
                ComposableDemo("Filled Tonal Button") {
                    FilledTonalButtonDemo()
                },
                ComposableDemo("Outlined Button") {
                    OutlinedButtonDemo()
                },
                ComposableDemo("Child Button") {
                    ChildButtonDemo()
                },
                ComposableDemo("Multiline Button") {
                    MultilineButtonDemo()
                },
                ComposableDemo("Avatar Button") {
                    AvatarButtonDemo()
                },
            )
        ),
        ComposableDemo("List Header") {
            Centralize {
                ListHeaderDemo()
            }
        },
        ComposableDemo("Card") {
            CardDemo()
        },
        ComposableDemo("Text Button") {
            TextButtonDemo()
        },
        ComposableDemo("Icon Button") {
            IconButtonDemo()
        },
        ComposableDemo("Text Toggle Button") {
            TextToggleButtonDemo()
        },
        ComposableDemo("Icon Toggle Button") {
            IconToggleButtonDemo()
        },
        ComposableDemo("Checkbox") {
            CheckboxDemos()
        },
        ComposableDemo("Switch") {
            SwitchDemos()
        },
        ComposableDemo("Radio Button") {
            RadioButtonDemos()
        },
        DemoCategory(
            title = "Toggle Button",
            toggleButtonDemos
        ),
        DemoCategory(
            "Stepper",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Stepper") {
                            Centralize { StepperSample() }
                        },
                        ComposableDemo("Integer Stepper") {
                            Centralize { StepperWithIntegerSample() }
                        },
                        ComposableDemo("Stepper with rangeSemantics") {
                            Centralize { StepperWithRangeSemanticsSample() }
                        }
                    )
                )
            )
        ),
        DemoCategory(
            "Slider",
            SliderDemos
        ),
        ComposableDemo(
            title = "Fixed Font Size"
        ) {
            Centralize { FixedFontSize() }
        },
        DemoCategory(
            title = "Swipe To Dismiss",
            listOf(
                ComposableDemo("Simple") { SimpleSwipeToDismissBox(it.navigateBack) },
                ComposableDemo("Stateful") { StatefulSwipeToDismissBox() },
                ComposableDemo("Edge swipe") { EdgeSwipeForSwipeToDismiss(it.navigateBack) },
            )
        ),
        ComposableDemo("HorizontalPageIndicator") {
            Centralize { HorizontalPageIndicatorSample() }
        },
    )
)
