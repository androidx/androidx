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
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.samples.AppCardWithIcon
import androidx.wear.compose.material.samples.ButtonWithIcon
import androidx.wear.compose.material.samples.ButtonWithText
import androidx.wear.compose.material.samples.ChipWithIconAndLabels
import androidx.wear.compose.material.samples.CompactButtonWithIcon
import androidx.wear.compose.material.samples.CompactChipWithIconAndLabel
import androidx.wear.compose.material.samples.CurvedTextDemo
import androidx.wear.compose.material.samples.InlineSliderSample
import androidx.wear.compose.material.samples.InlineSliderSegmentedSample
import androidx.wear.compose.material.samples.ScalingLazyColumnWithHeaders
import androidx.wear.compose.material.samples.ScalingLazyColumnWithHeadersReversed
import androidx.wear.compose.material.samples.SimpleScaffoldWithScrollIndicator
import androidx.wear.compose.material.samples.SimpleScalingLazyColumn
import androidx.wear.compose.material.samples.SimpleScalingLazyColumnWithContentPadding
import androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material.samples.SplitToggleChipWithCheckbox
import androidx.wear.compose.material.samples.StatefulSwipeToDismissBox
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
            "Slider",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Inline slider") { Centralize { InlineSliderSample() } },
                        ComposableDemo("Segmented inline slider") {
                            Centralize { InlineSliderSegmentedSample() }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        ComposableDemo("Inline slider") { InlineSliderDemo() },
                        ComposableDemo("RTL Inline slider") { InlineSliderRTLDemo() },
                        ComposableDemo("Inline slider segmented") { InlineSliderSegmented() },
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
                    )
                ),
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
                        ComposableDemo("RTL Toggle chip") { RtlToggleChips() },
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
                        ComposableDemo("AppCard") { Centralize({ AppCardWithIcon() }) },
                        ComposableDemo("TitleCard") { Centralize({ TitleCardStandard() }) },
                        ComposableDemo("TitleCardWithImage") {
                            Centralize({ TitleCardWithImage() })
                        },
                    )
                ),
                ComposableDemo("Demos") { CardDemo() },
            )
        ),
        SwipeToDismissDemos,
        DemoCategory(
            "List",
            listOf(
                ComposableDemo("Scaling Lazy Column") { SimpleScalingLazyColumn() },
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
        ComposableDemo("Curved Text") { CurvedTextDemo() },
    ),
)
