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
import android.os.Build
import android.widget.Toast
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Material3DemoCategory
import androidx.wear.compose.material3.samples.AnimatedTextSample
import androidx.wear.compose.material3.samples.AnimatedTextSampleButtonResponse
import androidx.wear.compose.material3.samples.AnimatedTextSampleSharedFontRegistry
import androidx.wear.compose.material3.samples.ButtonGroupSample
import androidx.wear.compose.material3.samples.EdgeButtonListSample
import androidx.wear.compose.material3.samples.EdgeButtonSample
import androidx.wear.compose.material3.samples.EdgeSwipeForSwipeToDismiss
import androidx.wear.compose.material3.samples.FixedFontSize
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorSample
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorWithPagerSample
import androidx.wear.compose.material3.samples.ScaffoldSample
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
                    ComposableDemo("Base Button") { BaseButtonDemo() },
                    ComposableDemo("Filled Button") { ButtonDemo() },
                    ComposableDemo("Filled Tonal Button") { FilledTonalButtonDemo() },
                    ComposableDemo("Filled Variant Button") { FilledVariantButtonDemo() },
                    ComposableDemo("Outlined Button") { OutlinedButtonDemo() },
                    ComposableDemo("Child Button") { ChildButtonDemo() },
                    ComposableDemo("Multiline Button") { MultilineButtonDemo() },
                    ComposableDemo("Avatar Button") { AvatarButtonDemo() },
                    ComposableDemo("Button (Image Background)") { ButtonBackgroundImageDemo() },
                )
            ),
            ComposableDemo("Color Scheme") { ColorSchemeDemos() },
            Material3DemoCategory("Curved Text", CurvedTextDemos),
            Material3DemoCategory("Alert Dialog", AlertDialogs),
            Material3DemoCategory("Confirmation", Comfirmations),
            Material3DemoCategory("Open on phone Dialog", OpenOnPhoneDialogDemos),
            ComposableDemo("Scaffold") { ScaffoldSample() },
            Material3DemoCategory("ScrollAway", ScrollAwayDemos),
            ComposableDemo("Haptics") { Centralize { HapticsDemos() } },
            ComposableDemo("Compact Button") { CompactButtonDemo() },
            ComposableDemo("Icon Button") { IconButtonDemo() },
            ComposableDemo("Image Button") { ImageButtonDemo() },
            ComposableDemo("Text Button") { TextButtonDemo() },
            Material3DemoCategory(
                "Edge Button",
                listOf(
                    ComposableDemo("Simple Edge Button") { EdgeButtonSample() },
                    ComposableDemo("Sizes and Colors") { EdgeButtonMultiDemo() },
                    ComposableDemo("Configurable") { EdgeButtonConfigurableDemo() },
                    ComposableDemo("Simple Edge Button below SLC") { EdgeButtonListSample() },
                    ComposableDemo("Edge Button Below C") { EdgeButtonBelowColumnDemo() },
                    ComposableDemo("Edge Button Below LC") { EdgeButtonBelowLazyColumnDemo() },
                    ComposableDemo("Edge Button Below SLC") {
                        EdgeButtonBelowScalingLazyColumnDemo()
                    },
                )
            ),
            Material3DemoCategory(
                "Button Group",
                listOf(
                    ComposableDemo("Two buttons") { ButtonGroupSample() },
                    ComposableDemo("Three buttons") { ButtonGroupDemo() },
                )
            ),
            ComposableDemo("List Header") { Centralize { ListHeaderDemo() } },
            Material3DemoCategory("Time Text", TimeTextDemos),
            ComposableDemo("Card") { CardDemo() },
            ComposableDemo("Animated Shape Buttons") { AnimatedShapeButtonDemo() },
            ComposableDemo("Text Toggle Button") { TextToggleButtonDemo() },
            ComposableDemo("Icon Toggle Button") { IconToggleButtonDemo() },
            ComposableDemo("Checkbox Button") { CheckboxButtonDemo() },
            ComposableDemo("Split Checkbox Button") { SplitCheckboxButtonDemo() },
            ComposableDemo("Radio Button") { RadioButtonDemo() },
            ComposableDemo("Split Radio Button") { SplitRadioButtonDemo() },
            ComposableDemo("Switch Button") { SwitchButtonDemo() },
            ComposableDemo("Split Switch Button") { SplitSwitchButtonDemo() },
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
            Material3DemoCategory("Picker", PickerDemos),
            // Requires API level 26 or higher due to java.time dependency.
            *(if (Build.VERSION.SDK_INT >= 26)
                arrayOf(
                    Material3DemoCategory("TimePicker", TimePickerDemos),
                    Material3DemoCategory("DatePicker", DatePickerDemos)
                )
            else emptyArray<Material3DemoCategory>()),
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
            Material3DemoCategory(title = "Typography", TypographyDemos),
            Material3DemoCategory(
                "Animated Text",
                if (Build.VERSION.SDK_INT > 31) {
                    listOf(
                        ComposableDemo("Simple animation") { Centralize { AnimatedTextSample() } },
                        ComposableDemo("Animation with button click") {
                            Centralize { AnimatedTextSampleButtonResponse() }
                        },
                        ComposableDemo("Shared Font Registry") {
                            Centralize { AnimatedTextSampleSharedFontRegistry() }
                        },
                    )
                } else {
                    emptyList()
                }
            ),
            ComposableDemo("Settings Demo") { SettingsDemo() },
            Material3DemoCategory(
                title = "LazyColumn",
                listOf(ComposableDemo("Notifications") { LazyColumnNotificationsDemo() })
            )
        )
    )

internal fun showOnClickToast(context: Context) {
    Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
}

internal fun showOnLongClickToast(context: Context) {
    Toast.makeText(context, "Long clicked", Toast.LENGTH_SHORT).show()
}
