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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.samples.AlertDialogSample
import androidx.wear.compose.material.samples.AlertWithButtons
import androidx.wear.compose.material.samples.AlertWithChips
import androidx.wear.compose.material.samples.AppCardWithIcon
import androidx.wear.compose.material.samples.ButtonWithIcon
import androidx.wear.compose.material.samples.ButtonWithText
import androidx.wear.compose.material.samples.ChipWithIconAndLabels
import androidx.wear.compose.material.samples.CircularProgressIndicatorFullscreenWithGap
import androidx.wear.compose.material.samples.CircularProgressIndicatorWithAnimation
import androidx.wear.compose.material.samples.CompactButtonWithIcon
import androidx.wear.compose.material.samples.CompactChipWithIconAndLabel
import androidx.wear.compose.material.samples.ConfirmationDialogSample
import androidx.wear.compose.material.samples.ConfirmationWithAnimation
import androidx.wear.compose.material.samples.CurvedTextDemo
import androidx.wear.compose.material.samples.HorizontalPageIndicatorSample
import androidx.wear.compose.material.samples.IndeterminateCircularProgressIndicator
import androidx.wear.compose.material.samples.InlineSliderSample
import androidx.wear.compose.material.samples.InlineSliderSegmentedSample
import androidx.wear.compose.material.samples.InlineSliderWithIntegerSample
import androidx.wear.compose.material.samples.OptionChangePicker
import androidx.wear.compose.material.samples.ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo
import androidx.wear.compose.material.samples.ScalingLazyColumnWithHeaders
import androidx.wear.compose.material.samples.ScalingLazyColumnWithHeadersReversed
import androidx.wear.compose.material.samples.SimplePicker
import androidx.wear.compose.material.samples.SimpleScaffoldWithScrollIndicator
import androidx.wear.compose.material.samples.SimpleScalingLazyColumn
import androidx.wear.compose.material.samples.SimpleScalingLazyColumnWithContentPadding
import androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material.samples.SplitToggleChipWithCheckbox
import androidx.wear.compose.material.samples.StatefulSwipeToDismissBox
import androidx.wear.compose.material.samples.StepperSample
import androidx.wear.compose.material.samples.StepperWithIntegerSample
import androidx.wear.compose.material.samples.TimeTextWithCustomSeparator
import androidx.wear.compose.material.samples.TimeTextWithFullDateAndTimeFormat
import androidx.wear.compose.material.samples.TitleCardStandard
import androidx.wear.compose.material.samples.TitleCardWithImage
import androidx.wear.compose.material.samples.ToggleButtonWithIcon
import androidx.wear.compose.material.samples.ToggleChipWithIcon

// Declare the swipe to dismiss demos so that we can use this variable as the background composable
// for the SwipeToDismissDemo itself.
@ExperimentalWearMaterialApi
internal val SwipeToDismissDemos =
    DemoCategory(
        "Swipe to Dismiss",
        listOf(
            DemoCategory(
                "Samples",
                listOf(
                    ComposableDemo("Simple") { navBack ->
                        SimpleSwipeToDismissBox(navBack)
                    },
                    ComposableDemo("Stateful") { StatefulSwipeToDismissBox() },
                )
            ),
            DemoCategory(
                "Demos",
                listOf(
                    ComposableDemo("Demo") { navigateBack ->
                        val state = remember { mutableStateOf(SwipeDismissDemoState.List) }
                        SwipeToDismissDemo(navigateBack = navigateBack, demoState = state)
                    },
                    ComposableDemo("Stateful Demo") { navigateBack ->
                        SwipeToDismissBoxWithState(navigateBack)
                    },
                )
            )
        )
    )

@ExperimentalWearMaterialApi
val WearMaterialDemos = DemoCategory(
    "Material",
    listOf(
        DemoCategory(
            "Picker",
            listOf(
                ComposableDemo("Time HH:MM:SS") {
                    TimePickerWithHoursMinutesSeconds()
                },
                ComposableDemo("Time 12 Hour") {
                    TimePickerWith12HourClock()
                },
                ComposableDemo("Date Picker") {
                    DatePicker()
                },
                ComposableDemo("Simple Picker") {
                    SimplePicker()
                },
                ComposableDemo("Change Selected Option Picker") {
                    OptionChangePicker()
                },
            )
        ),
        DemoCategory(
            "Slider",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Inline slider") { Centralize { InlineSliderSample() } },
                        ComposableDemo("Segmented inline slider") {
                            Centralize { InlineSliderSegmentedSample() }
                        },
                        ComposableDemo("Integer inline slider") {
                            Centralize { InlineSliderWithIntegerSample() }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Inline slider") { InlineSliderDemo() },
                        ComposableDemo("RTL Inline slider") { InlineSliderRTLDemo() },
                        ComposableDemo("With custom colors") { InlineSliderCustomColorsDemo() },
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
                        ComposableDemo("Stepper") {
                            Centralize({ StepperSample() })
                        },
                        ComposableDemo("Integer Stepper") {
                            Centralize({ StepperWithIntegerSample() })
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Simple stepper") { StepperDemo() },
                        ComposableDemo("Stepper with integer") { StepperWithIntegerDemo() },
                        ComposableDemo("With scrollbar") {
                            StepperWithScrollBarDemo()
                        },
                        ComposableDemo("With custom colors") {
                            StepperWithCustomColors()
                        },
                    )
                )
            )
        ),
        DemoCategory(
            "TimeText",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Clock with custom separator") {
                            TimeTextWithCustomSeparator()
                        },
                        ComposableDemo("Clock with full date and time format") {
                            TimeTextWithFullDateAndTimeFormat()
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Clock only") {
                            TimeTextClockOnly()
                        },
                        ComposableDemo("Clock with leading text") {
                            TimeTextWithLeadingText()
                        },
                        ComposableDemo("Clock with trailing text") {
                            TimeTextWithTrailingText()
                        },
                        ComposableDemo("Clock with leading and trailing text") {
                            TimeTextWithLeadingAndTrailingText()
                        },
                        ComposableDemo("Clock with padding") {
                            TimeTextWithPadding()
                        },
                        ComposableDemo("Clock with yyyy.MM.dd HH:mm:ss format") {
                            TimeTextWithLongDateTime()
                        },
                        ComposableDemo("Clock with custom format and color") {
                            TimeTextWithCustomFormatAndColor()
                        },
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
                        ComposableDemo("Alert Dialog") {
                            AlertDialogSample()
                        },
                        ComposableDemo("Confirmation Dialog") {
                            ConfirmationDialogSample()
                        },
                        ComposableDemo("Alert - Buttons") {
                            AlertWithButtons()
                        },
                        ComposableDemo("Alert - Chips") {
                            AlertWithChips()
                        },
                        ComposableDemo("Confirmation") {
                            ConfirmationWithAnimation()
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Power Off") { navBack ->
                            DialogPowerOff(navBack)
                        },
                        ComposableDemo("Access Location") { navBack ->
                            DialogAccessLocation(navBack)
                        },
                        ComposableDemo("Grant Permission") { navBack ->
                            DialogGrantPermission(navBack)
                        },
                        ComposableDemo("Long Chips") { navBack ->
                            DialogLongChips(navBack)
                        },
                        ComposableDemo("Confirmation") { navBack ->
                            DialogSuccessConfirmation(navBack)
                        },
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
                        ComposableDemo("ButtonWithIcon") { Centralize({ ButtonWithIcon() }) },
                        ComposableDemo("ButtonWithText") { Centralize({ ButtonWithText() }) },
                        ComposableDemo("CompactButtonWithIcon") {
                            Centralize({ CompactButtonWithIcon() })
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
            "ToggleButton",
            listOf(
                ComposableDemo("Sample") { Centralize({ ToggleButtonWithIcon() }) },
                ComposableDemo("Demos") { ToggleButtons() },
            )
        ),
        DemoCategory(
            "Chips",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("ChipWithIconAndLabels") {
                            Centralize({ ChipWithIconAndLabels() })
                        },
                        ComposableDemo("CompactChipWithIconAndLabel") {
                            Centralize({ CompactChipWithIconAndLabel() })
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
            "PageIndicator",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Sample with InlineSlider") {
                            Centralize({ HorizontalPageIndicatorSample() })
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
            "Toggle Chip",
            listOf(
                DemoCategory("Samples",
                    listOf(
                        ComposableDemo("ToggleChipWithIcon") {
                            Centralize({ ToggleChipWithIcon() })
                        },
                        ComposableDemo("SplitToggleChipWithCheckbox") {
                            Centralize({ SplitToggleChipWithCheckbox() })
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
                        ComposableDemo("AppCard") { Centralize { AppCardWithIcon() } },
                        ComposableDemo("TitleCard") { Centralize { TitleCardStandard() } },
                        ComposableDemo("TitleCardWithImage") {
                            Centralize({ TitleCardWithImage() })
                        },
                    )
                ),
                ComposableDemo("Demos") { CardDemo() },
            )
        ),
        DemoCategory(
            "ProgressIndicator",
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
                        ComposableDemo("Full screen indeterminate progress") {
                            Centralize { FullscreenIndeterminateProgress() }
                        },
                        ComposableDemo("Small indeterminate progress") {
                            Centralize { SmallIndeterminateProgress() }
                        },
                        ComposableDemo("Custom angles") {
                            Centralize { ProgressWithCustomAngles() }
                        },
                        ComposableDemo("Media controls") {
                            Centralize { ProgressWithMedia() }
                        },
                        ComposableDemo("Multiple progress indicators") {
                            Centralize { MultipleProgressIndicators() }
                        },
                        ComposableDemo("Transforming progress indicator") {
                            Centralize { TransformingCustomProgressIndicator() }
                        },
                    )
                )
            )
        ),
        SwipeToDismissDemos,
        DemoCategory(
            "List",
            listOf(
                ComposableDemo("Scaling Lazy Column [SLC]") { SimpleScalingLazyColumn() },
                ComposableDemo("SLC with Edge Anchoring and Animated Scroll") {
                    ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo()
                },
                ComposableDemo("SLC with Content Padding") {
                    SimpleScalingLazyColumnWithContentPadding()
                },
                ComposableDemo("List Headers") { ScalingLazyColumnWithHeaders() },
                ComposableDemo("Reverse Layout") { ScalingLazyColumnWithHeadersReversed() },
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
            "PositionIndicator",
            listOf(
                ComposableDemo("Hide when no scrollable") {
                    HideWhenFullDemo()
                },
                ComposableDemo("Hide when no scrollable on ScalingLazyColumn") {
                    HideWhenFullSLCDemo()
                },
                ComposableDemo("Controllable PI") {
                    ControllablePositionIndicator()
                },
            )
        ),
        ComposableDemo("Curved Text") { CurvedTextDemo() },
        DemoCategory(
            "Theme",
            listOf(
                ComposableDemo("Fonts") {
                    ThemeFonts()
                },
                ComposableDemo("Colors") {
                    ThemeColors()
                },
            )
        ),
    ),
)
