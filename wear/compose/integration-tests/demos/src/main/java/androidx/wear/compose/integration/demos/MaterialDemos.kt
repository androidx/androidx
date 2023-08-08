/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material.samples.AlertDialogSample
import androidx.wear.compose.material.samples.AlertWithButtons
import androidx.wear.compose.material.samples.AlertWithChips
import androidx.wear.compose.material.samples.AnimateOptionChangePicker
import androidx.wear.compose.material.samples.AppCardWithIcon
import androidx.wear.compose.material.samples.AutoCenteringPickerGroup
import androidx.wear.compose.material.samples.ButtonWithIcon
import androidx.wear.compose.material.samples.ButtonWithText
import androidx.wear.compose.material.samples.ChipWithIconAndLabel
import androidx.wear.compose.material.samples.ChipWithIconAndLabelAndPlaceholders
import androidx.wear.compose.material.samples.ChipWithIconAndLabels
import androidx.wear.compose.material.samples.ChipWithIconAndLabelsAndOverlaidPlaceholder
import androidx.wear.compose.material.samples.CircularProgressIndicatorFullscreenWithGap
import androidx.wear.compose.material.samples.CircularProgressIndicatorWithAnimation
import androidx.wear.compose.material.samples.CompactButtonWithIcon
import androidx.wear.compose.material.samples.CompactChipWithIcon
import androidx.wear.compose.material.samples.CompactChipWithIconAndLabel
import androidx.wear.compose.material.samples.CompactChipWithLabel
import androidx.wear.compose.material.samples.ConfirmationDialogSample
import androidx.wear.compose.material.samples.ConfirmationWithAnimation
import androidx.wear.compose.material.samples.CurvedTextDemo
import androidx.wear.compose.material.samples.CurvedTextProviderDemo
import androidx.wear.compose.material.samples.EdgeSwipeForSwipeToDismiss
import androidx.wear.compose.material.samples.FixedFontSize
import androidx.wear.compose.material.samples.HorizontalPageIndicatorSample
import androidx.wear.compose.material.samples.IndeterminateCircularProgressIndicator
import androidx.wear.compose.material.samples.InlineSliderSample
import androidx.wear.compose.material.samples.InlineSliderSegmentedSample
import androidx.wear.compose.material.samples.InlineSliderWithIntegerSample
import androidx.wear.compose.material.samples.LargeButtonWithIcon
import androidx.wear.compose.material.samples.OutlinedButtonWithIcon
import androidx.wear.compose.material.samples.OutlinedChipWithIconAndLabel
import androidx.wear.compose.material.samples.OutlinedCompactButtonWithIcon
import androidx.wear.compose.material.samples.OutlinedCompactChipWithIconAndLabel
import androidx.wear.compose.material.samples.PickerGroup24Hours
import androidx.wear.compose.material.samples.ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo
import androidx.wear.compose.material.samples.SimplePicker
import androidx.wear.compose.material.samples.SimpleScaffoldWithScrollIndicator
import androidx.wear.compose.material.samples.SimpleScalingLazyColumn
import androidx.wear.compose.material.samples.SimpleScalingLazyColumnWithContentPadding
import androidx.wear.compose.material.samples.SimpleScalingLazyColumnWithSnap
import androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material.samples.SplitToggleChipWithCheckbox
import androidx.wear.compose.material.samples.StatefulSwipeToDismissBox
import androidx.wear.compose.material.samples.StepperSample
import androidx.wear.compose.material.samples.StepperWithCustomSemanticsSample
import androidx.wear.compose.material.samples.StepperWithIntegerSample
import androidx.wear.compose.material.samples.StepperWithoutRangeSemanticsSample
import androidx.wear.compose.material.samples.TextPlaceholder
import androidx.wear.compose.material.samples.TimeTextAnimation
import androidx.wear.compose.material.samples.TimeTextWithFullDateAndTimeFormat
import androidx.wear.compose.material.samples.TimeTextWithStatus
import androidx.wear.compose.material.samples.TitleCardStandard
import androidx.wear.compose.material.samples.TitleCardWithImage
import androidx.wear.compose.material.samples.ToggleButtonWithIcon
import androidx.wear.compose.material.samples.ToggleChipWithRadioButton
import androidx.wear.compose.material.samples.ToggleChipWithSwitch
import java.time.LocalDate
import java.time.LocalTime

@SuppressLint("ClassVerificationFailure")
val WearMaterialDemos = DemoCategory(
    "Material",
    listOf(
        DemoCategory(
            "ScrollAway",
            listOf(
                ComposableDemo("Column") { ScrollAwayColumnDemo() },
                ComposableDemo("Column (delay)") { ScrollAwayColumnDelayDemo() },
                ComposableDemo("Lazy Column") { ScrollAwayLazyColumnDemo() },
                ComposableDemo("Lazy Column offset<0") { ScrollAwayLazyColumnDemo2() },
                ComposableDemo("Lazy Column offset>0") { ScrollAwayLazyColumnDelayDemo() },
                ComposableDemo("SLC Cards") {
                    ScrollAwayScalingLazyColumnCardDemo()
                },
                ComposableDemo("SLC Cards offset<0") {
                    ScrollAwayScalingLazyColumnCardDemo2()
                },
                ComposableDemo("SLC Cards offset>0") {
                    ScrollAwayScalingLazyColumnCardDemoMismatch()
                },
                ComposableDemo("Out of range") {
                    ScrollAwayScalingLazyColumnCardDemoOutOfRange()
                },
                ComposableDemo("SLC Chips") {
                    ScrollAwayScalingLazyColumnChipDemo()
                },
                ComposableDemo("SLC Chips offset<0") {
                    ScrollAwayScalingLazyColumnChipDemo2()
                },
            )
        ),
        DemoCategory(
            "Picker",
            if (Build.VERSION.SDK_INT > 25) {
                listOf(
                    ComposableDemo("Time HH:MM:SS") { params ->
                        var timePickerTime by remember { mutableStateOf(LocalTime.now()) }
                        TimePicker(
                            onTimeConfirm = {
                                timePickerTime = it
                                params.navigateBack()
                            },
                            time = timePickerTime,
                        )
                    },
                    ComposableDemo("Time 12 Hour") { params ->
                        var timePickerTime by remember { mutableStateOf(LocalTime.now()) }
                        TimePickerWith12HourClock(
                            onTimeConfirm = {
                                timePickerTime = it
                                params.navigateBack()
                            },
                            time = timePickerTime,
                        )
                    },
                    ComposableDemo("Date Picker") { params ->
                        var datePickerDate by remember { mutableStateOf(LocalDate.now()) }
                        DatePicker(
                            onDateConfirm = {
                                datePickerDate = it
                                params.navigateBack()
                            },
                            date = datePickerDate
                        )
                    },
                    ComposableDemo("From Date Picker") { params ->
                        var datePickerDate by remember { mutableStateOf(LocalDate.now()) }
                        DatePicker(
                            onDateConfirm = {
                                datePickerDate = it
                                params.navigateBack()
                            },
                            date = datePickerDate,
                            fromDate = datePickerDate
                        )
                    },
                    ComposableDemo("To Date Picker") { params ->
                        var datePickerDate by remember { mutableStateOf(LocalDate.now()) }
                        DatePicker(
                            onDateConfirm = {
                                datePickerDate = it
                                params.navigateBack()
                            },
                            date = datePickerDate,
                            toDate = datePickerDate
                        )
                    },
                    ComposableDemo("Simple Picker") { SimplePicker() },
                    ComposableDemo("No gradient") { PickerWithoutGradient() },
                    ComposableDemo("Animate picker change") { AnimateOptionChangePicker() },
                    ComposableDemo("Sample Picker Group") { PickerGroup24Hours() },
                    ComposableDemo("Autocentering Picker Group") { AutoCenteringPickerGroup() }
                )
            } else {
                listOf(
                    ComposableDemo("Simple Picker") { SimplePicker() },
                )
            }
        ),
        DemoCategory(
            "Slider",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Inline slider") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                InlineSliderSample()
                            }
                        },
                        ComposableDemo("Segmented inline slider") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                InlineSliderSegmentedSample()
                            }
                        },
                        ComposableDemo("Integer inline slider") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                InlineSliderWithIntegerSample()
                            }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Inline slider") { InlineSliderDemo() },
                        ComposableDemo("RTL Inline slider") { InlineSliderRTLDemo() },
                        ComposableDemo("With custom color") { InlineSliderCustomColorsDemo() },
                        ComposableDemo("Inline slider segmented") { InlineSliderSegmented() },
                        ComposableDemo("Inline slider with integers") {
                            InlineSliderWithIntegersDemo()
                        },
                    )
                )
            )
        ),
        DemoCategory(
            "Stepper",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Stepper") { Centralize { StepperSample() } },
                        ComposableDemo("Integer Stepper") {
                            Centralize { StepperWithIntegerSample() }
                        },
                        ComposableDemo("Stepper without RangeSemantics") {
                            Centralize { StepperWithoutRangeSemanticsSample() }
                        },
                        ComposableDemo("Stepper with customSemantics") {
                            Centralize { StepperWithCustomSemanticsSample() }
                        }
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Simple stepper") { StepperDemo() },
                        ComposableDemo("Stepper with integer") { StepperWithIntegerDemo() },
                        ComposableDemo("With scrollbar") { StepperWithScrollBarDemo() },
                        ComposableDemo("With custom color") { StepperWithCustomColors() },
                    )
                )
            )
        ),
        DemoCategory(
            "Time Text",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Clock with Status") { TimeTextWithStatus() },
                        ComposableDemo("Clock with custom time format") {
                            TimeTextWithFullDateAndTimeFormat()
                        },
                        ComposableDemo("Clock with animated status",
                            "A TimeText with status that animates in/out when tapping " +
                                "the central button."
                        ) {
                            TimeTextAnimation()
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Clock only") { TimeTextClockOnly() },
                        ComposableDemo("Clock with leading text") { TimeTextWithLeadingText() },
                        ComposableDemo("Clock with shadow") { TimeTextWithShadow() },
                        ComposableDemo("Clock with localised format") {
                            TimeTextWithLocalisedFormat()
                        },
                    )
                ),
            )
        ),
        DemoCategory(
            "Dialogs",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Alert Dialog") { AlertDialogSample() },
                        ComposableDemo("Confirmation Dialog") { ConfirmationDialogSample() },
                        ComposableDemo("Alert - Buttons") { AlertWithButtons() },
                        ComposableDemo("Alert - Chips") { AlertWithChips() },
                        ComposableDemo("Confirmation") { ConfirmationWithAnimation() },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Power Off") { DialogPowerOff() },
                        ComposableDemo("Access Location") { DialogAccessLocation() },
                        ComposableDemo("Grant Permission") { DialogGrantPermission() },
                        ComposableDemo("Long Chips") { DialogLongChips() },
                        ComposableDemo("Dialog Background") {
                            DialogBackground(Color.Green)
                        },
                        ComposableDemo("Confirmation") { DialogSuccessConfirmation() },
                    )
                )
            )
        ),
        DemoCategory(
            "Button",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Button With Icon") { Centralize { ButtonWithIcon() } },
                        ComposableDemo("Button With Large Icon") {
                            Centralize { LargeButtonWithIcon() }
                        },
                        ComposableDemo("Button With Text") { Centralize { ButtonWithText() } },
                        ComposableDemo("Outlined Button With Icon") {
                            Centralize { OutlinedButtonWithIcon() }
                        },
                        ComposableDemo("Compact Button With Icon") {
                            Centralize { CompactButtonWithIcon() }
                        },
                        ComposableDemo("Outline Compact Button With Icon") {
                            Centralize { OutlinedCompactButtonWithIcon() }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Button Gallery") { ButtonGallery() },
                        ComposableDemo("Button Sizes") { ButtonSizes() },
                        ComposableDemo("Button Styles") { ButtonStyles() },
                    )
                )
            )
        ),
        DemoCategory(
            "Toggle Button",
            listOf(
                ComposableDemo("Sample") { Centralize { ToggleButtonWithIcon() } },
                ComposableDemo("Demos") { ToggleButtons() },
            )
        ),
        DemoCategory(
            "Chips",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Chip With Icon And long Label") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                ChipWithIconAndLabel()
                            }
                        },
                        ComposableDemo("Chip With Icon And Labels") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                ChipWithIconAndLabels()
                            }
                        },
                        ComposableDemo("Outlined Chip With Icon And long Label") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                OutlinedChipWithIconAndLabel()
                            }
                        },
                        ComposableDemo("Compact Chip With Icon And Label") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                CompactChipWithIconAndLabel()
                            }
                        },
                        ComposableDemo("Compact Chip With Label Only") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                CompactChipWithLabel()
                            }
                        },
                        ComposableDemo("Compact Chip Icon Only") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                CompactChipWithIcon()
                            }
                        },
                        ComposableDemo("Outlined Compact Chip With Icon and Label") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                OutlinedCompactChipWithIconAndLabel()
                            }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Chip") { StandardChips() },
                        ComposableDemo("Compact chip") { SmallChips() },
                        ComposableDemo("Avatar chip") { AvatarChips() },
                        ComposableDemo("Rtl chips") { RtlChips() },
                        ComposableDemo("Custom chips") { CustomChips() },
                        ComposableDemo("Image background chips") { ImageBackgroundChips() },
                    )
                )
            )
        ),
        DemoCategory(
            "Placeholders",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Content Placeholders") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                ChipWithIconAndLabelAndPlaceholders()
                            }
                        },
                        ComposableDemo("Overlaid Placeholder") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                ChipWithIconAndLabelsAndOverlaidPlaceholder()
                            }
                        },
                        ComposableDemo("Simple Text Placeholder") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                TextPlaceholder()
                            }
                        },
                     )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Chips") { PlaceholderChips() },
                        ComposableDemo("Cards") { PlaceholderCards() },
                    )
                )
            )
        ),
        DemoCategory(
            "Toggle Chip",
            listOf(
                DemoCategory("Samples",
                    listOf(
                        ComposableDemo("ToggleChip With Switch") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                ToggleChipWithSwitch()
                            }
                        },
                        ComposableDemo("ToggleChip With RadioButton") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                ToggleChipWithRadioButton()
                            }
                        },
                        ComposableDemo("SplitToggleChip With Checkbox") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                SplitToggleChipWithCheckbox()
                            }
                        }
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Toggle chip") { ToggleChips() },
                        ComposableDemo("RTL Toggle chip") {
                            ToggleChips(
                                layoutDirection = LayoutDirection.Rtl,
                                description = "RTL ToggleChips"
                            )
                        },
                    )
                )
            )
        ),
        DemoCategory(
            "Card",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("AppCard") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                AppCardWithIcon()
                            }
                        },
                        ComposableDemo("TitleCard") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                TitleCardStandard()
                            }
                        },
                        ComposableDemo("TitleCard With Image") {
                            Centralize(Modifier.padding(horizontal = 10.dp)) {
                                TitleCardWithImage()
                            }
                        },
                    )
                ),
                ComposableDemo("Demos") { CardDemo() },
            )
        ),
        DemoCategory(
            "Page Indicator",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Sample with InlineSlider") {
                            Centralize { HorizontalPageIndicatorSample() }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Customized PageIndicator") {
                            CustomizedHorizontalPageIndicator()
                        },
                    )
                )
            )
        ),
        DemoCategory(
            "Progress Indicator",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Indeterminate") {
                            Centralize { IndeterminateCircularProgressIndicator() }
                        },
                        ComposableDemo("Animation") {
                            Centralize { CircularProgressIndicatorWithAnimation() }
                        },
                        ComposableDemo("Fullscreen with a gap") {
                            Centralize { CircularProgressIndicatorFullscreenWithGap() }
                        }
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Indeterminate progress") {
                            Centralize { IndeterminateProgress() }
                        },
                        ComposableDemo("Custom angles") {
                            Centralize { ProgressWithCustomAngles() }
                        },
                        ComposableDemo("Media controls") { Centralize { ProgressWithMedia() } },
                        ComposableDemo("Transforming progress indicator") {
                            Centralize { TransformingCustomProgressIndicator() }
                        },
                    )
                )
            )
        ),
        DemoCategory(
            title = "Swipe To Dismiss",
            listOf(
                ComposableDemo("Simple") { SimpleSwipeToDismissBox(it.navigateBack) },
                ComposableDemo("Stateful") { StatefulSwipeToDismissBox() },
                ComposableDemo("Edge swipe") { EdgeSwipeForSwipeToDismiss(it.navigateBack) },
            )
        ),
        DemoCategory(
            "List (Scaling Lazy Column)",
            listOf(
                ComposableDemo(
                    "Defaults",
                    "Basic ScalingLazyColumn using default values"
                ) {
                    SimpleScalingLazyColumn()
                },
                ComposableDemo(
                    "With Content Padding",
                    "Basic ScalingLazyColumn with autoCentering disabled and explicit " +
                        "content padding of top = 20.dp, bottom = 20.dp"
                ) {
                    SimpleScalingLazyColumnWithContentPadding()
                },
                ComposableDemo(
                    "With Snap",
                    "Basic ScalingLazyColumn, center aligned with snap enabled"
                ) {
                    SimpleScalingLazyColumnWithSnap()
                },
                ComposableDemo(
                    "Edge Anchor",
                    "A ScalingLazyColumn with Edge (rather than center) item anchoring. " +
                        "If you click on an item there will be an animated scroll of the " +
                        "items edge to the center"
                ) {
                    ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo()
                },
                ComposableDemo(
                    "Edge Anchor (G)",
                    "A ScalingLazyColumn with Edge (rather than center) item anchoring. " +
                        "If you click on an item there will be an animated scroll of the " +
                        "items edge to the center and guidelines drawn on top"
                ) {
                    ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo()
                    GuideLines()
                },
                ComposableDemo(
                    "Scaling Details (G)",
                    "A ScalingLazyColumn with items that show their position and size as" +
                        "well as guidelines"
                ) {
                    ScalingLazyColumnDetail()
                    GuideLines()
                },
                ComposableDemo(
                    "Stress Test",
                    "A ScalingLazyColumn with a mixture of different types of items"
                ) {
                    ScalingLazyColumnMixedTypes()
                },
                ComposableDemo(
                    "Stress Test [G]",
                    "A ScalingLazyColumn with a mixture of different types of items with " +
                        "guidelines"
                ) {
                    ScalingLazyColumnMixedTypes()
                    GuideLines()
                },
            )
        ),
        DemoCategory(
            "Scaffold",
            listOf(
                ComposableDemo("Scaffold with Scrollbar") {
                    SimpleScaffoldWithScrollIndicator()
                },
            )
        ),
        DemoCategory(
            "Position Indicator",
            listOf(
                ComposableDemo("Hide when no scrollable") { HideWhenFullDemo() },
                ComposableDemo("Hide when no scrollable on ScalingLazyColumn") {
                    HideWhenFullSLCDemo()
                },
                ComposableDemo("Controllable PI") { ControllablePositionIndicator() },
                ComposableDemo("Shared PI") { SharedPositionIndicator() }
            )
        ),
        DemoCategory(
            "Curved Text",
            listOf(
                ComposableDemo("Basic Styling") { CurvedTextDemo() },
                ComposableDemo("Provider Styling") { CurvedTextProviderDemo() },
            )
        ),
        DemoCategory(
            "Theme",
            listOf(
                ComposableDemo("Fonts") { ThemeFonts() },
                ComposableDemo(
                    title = "Fixed Font Size",
                    description =
                    "Display1 font size not impacted by changes to user font selection",
                ) { Centralize { FixedFontSize() } },
                ComposableDemo("Colors") { ThemeColors() },
            )
        ),
    ),
)
