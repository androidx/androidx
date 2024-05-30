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

import android.content.Context
import android.widget.Toast
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Material3DemoCategory
import androidx.wear.compose.material3.samples.EdgeSwipeForSwipeToDismiss
import androidx.wear.compose.material3.samples.FixedFontSize
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorSample
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorWithPagerSample
import androidx.wear.compose.material3.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material3.samples.StatefulSwipeToDismissBox
import androidx.wear.compose.material3.samples.StepperSample
import androidx.wear.compose.material3.samples.StepperWithIntegerSample
import androidx.wear.compose.material3.samples.StepperWithRangeSemanticsSample

val WearMaterial3Demos =
    Material3DemoCategory(
        "Material 3",
        listOf(
            Material3DemoCategory(
                "Button",
                listOf(
                    ComposableDemo("Button") { ButtonDemo() },
                    ComposableDemo("Filled Tonal Button") { FilledTonalButtonDemo() },
                    ComposableDemo("Outlined Button") { OutlinedButtonDemo() },
                    ComposableDemo("Child Button") { ChildButtonDemo() },
                    ComposableDemo("Compact Button") { CompactButtonDemo() },
                    ComposableDemo("Multiline Button") { MultilineButtonDemo() },
                    ComposableDemo("Avatar Button") { AvatarButtonDemo() },
                )
            ),
            ComposableDemo("List Header") { Centralize { ListHeaderDemo() } },
            Material3DemoCategory("Time Text", TimeTextDemos),
            ComposableDemo("Card") { CardDemo() },
            ComposableDemo("Text Button") { TextButtonDemo() },
            ComposableDemo("Icon Button") { IconButtonDemo() },
            ComposableDemo("Text Toggle Button") { TextToggleButtonDemo() },
            ComposableDemo("Icon Toggle Button") { IconToggleButtonDemo() },
            ComposableDemo("Switch") { SwitchDemos() },
            ComposableDemo("Checkbox Button") { CheckboxButtonDemo() },
            ComposableDemo("Split Checkbox Button") { SplitCheckboxButtonDemo() },
            ComposableDemo("Radio Button") { RadioButtonDemo() },
            ComposableDemo("Split Radio Button") { SplitRadioButtonDemo() },
            ComposableDemo("Toggle Button") { ToggleButtonDemo() },
            ComposableDemo("Split Toggle Button") { SplitToggleButtonDemo() },
            Material3DemoCategory(
                "Stepper",
                listOf(
                    Material3DemoCategory(
                        "Samples",
                        listOf(
                            ComposableDemo("Stepper") { Centralize { StepperSample() } },
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
            Material3DemoCategory("Slider", SliderDemos),
            Material3DemoCategory("Progress Indicator", ProgressIndicatorDemos),
            Material3DemoCategory("Scroll Indicator", ScrollIndicatorDemos),
            Material3DemoCategory("Placeholder", PlaceholderDemos),
            ComposableDemo(title = "Fixed Font Size") { Centralize { FixedFontSize() } },
            Material3DemoCategory(
                title = "Swipe To Dismiss",
                listOf(
                    ComposableDemo("Simple") { SimpleSwipeToDismissBox(it.navigateBack) },
                    ComposableDemo("Stateful") { StatefulSwipeToDismissBox() },
                    ComposableDemo("Edge swipe") { EdgeSwipeForSwipeToDismiss(it.navigateBack) },
                )
            ),
            Material3DemoCategory(
                title = "Horizontal Page Indicator",
                listOf(
                    ComposableDemo("Simple HorizontalPageIndicator") {
                        HorizontalPageIndicatorSample()
                    },
                    ComposableDemo("HorizontalPageIndicator with Pager") {
                        HorizontalPageIndicatorWithPagerSample(it.swipeToDismissBoxState)
                    },
                )
            ),
            ComposableDemo("Settings Demo") { SettingsDemo() }
        )
    )

internal fun showOnClickToast(context: Context) {
    Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
}

internal fun showOnLongClickToast(context: Context) {
    Toast.makeText(context, "Long clicked", Toast.LENGTH_SHORT).show()
}
