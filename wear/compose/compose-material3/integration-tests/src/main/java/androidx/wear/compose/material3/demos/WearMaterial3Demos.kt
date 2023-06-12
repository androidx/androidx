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
import androidx.wear.compose.material3.samples.FixedFontSize
import androidx.wear.compose.material3.samples.StepperSample
import androidx.wear.compose.material3.samples.StepperWithIntegerSample
import androidx.wear.compose.material3.samples.StepperWithRangeSemanticsSample

val WearMaterial3Demos = DemoCategory(
    "Material 3",
    listOf(
        DemoCategory(
            "Buttons",
            listOf(
                ComposableDemo("Button") {
                    ButtonDemo()
                },
                ComposableDemo("FilledTonalButton") {
                    FilledTonalButtonDemo()
                },
                ComposableDemo("OutlinedButton") {
                    OutlinedButtonDemo()
                },
                ComposableDemo("ChildButton") {
                    ChildButtonDemo()
                }
            )
        ),
        ComposableDemo("Text Button") {
            TextButtonDemo()
        },
        ComposableDemo("Icon Button") {
            IconButtonDemo()
        },
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
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        // Add Stepper demos here
                    )
                )
            )
        ),
        ComposableDemo(
            title = "Fixed Font Size"
        ) {
            Centralize { FixedFontSize() }
        }
    )
)