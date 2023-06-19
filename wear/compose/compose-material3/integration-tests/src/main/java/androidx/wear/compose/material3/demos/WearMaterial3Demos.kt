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

import androidx.compose.ui.Alignment
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material3.samples.AppCardSample
import androidx.wear.compose.material3.samples.AppCardWithIconSample
import androidx.wear.compose.material3.samples.CardSample
import androidx.wear.compose.material3.samples.FixedFontSize
import androidx.wear.compose.material3.samples.OutlinedAppCardSample
import androidx.wear.compose.material3.samples.OutlinedCardSample
import androidx.wear.compose.material3.samples.OutlinedTitleCardSample
import androidx.wear.compose.material3.samples.StepperSample
import androidx.wear.compose.material3.samples.StepperWithIntegerSample
import androidx.wear.compose.material3.samples.StepperWithRangeSemanticsSample
import androidx.wear.compose.material3.samples.TitleCardSample
import androidx.wear.compose.material3.samples.TitleCardWithImageSample

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
                }
            )
        ),
        DemoCategory(
            "Card",
            listOf(
                ComposableDemo("Samples") {
                    ScalingLazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        autoCentering = AutoCenteringParams(itemIndex = 0)
                    ) {
                        item { CardSample() }
                        item { AppCardSample() }
                        item { AppCardWithIconSample() }
                        item { TitleCardSample() }
                        item { TitleCardWithImageSample() }
                        item { OutlinedCardSample() }
                        item { OutlinedAppCardSample() }
                        item { OutlinedTitleCardSample() }
                    }
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