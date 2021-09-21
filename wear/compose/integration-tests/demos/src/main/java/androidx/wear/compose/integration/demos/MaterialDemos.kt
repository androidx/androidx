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
import androidx.wear.compose.material.samples.CurvedTextDemo
import androidx.wear.compose.material.samples.ScalingLazyColumnWithHeaders
import androidx.wear.compose.material.samples.ScalingLazyColumnWithHeadersReversed
import androidx.wear.compose.material.samples.SimpleScaffoldWithScrollIndicator
import androidx.wear.compose.material.samples.SimpleScalingLazyColumn
import androidx.wear.compose.material.samples.SimpleScalingLazyColumnWithContentPadding
import androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material.samples.TimeTextWithCustomSeparator
import androidx.wear.compose.material.samples.TimeTextWithFullDateAndTimeFormat

// Declare the swipe to dismiss demos so that we can use this variable as the background composable
// for the SwipeToDismissDemo itself.
@ExperimentalWearMaterialApi
internal val SwipeToDismissDemos =
    DemoCategory(
        "Swipe to Dismiss",
        listOf(
            ComposableDemo("Sample") { navigateBack ->
                SimpleSwipeToDismissBox(navigateBack = navigateBack)
            },
            ComposableDemo("Demo") { navigateBack ->
                val state = remember { mutableStateOf(SwipeDismissDemoState.List) }
                SwipeToDismissDemo(navigateBack = navigateBack, demoState = state)
            },
        )
    )

@ExperimentalWearMaterialApi
val WearMaterialDemos = DemoCategory(
    "Material",
    listOf(
        DemoCategory(
            "TimeText",
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
                ComposableDemo("Clock with custom separator") {
                    TimeTextWithCustomSeparator()
                },
                ComposableDemo("Clock with full date and time format") {
                    TimeTextWithFullDateAndTimeFormat()
                },
                ComposableDemo("Clock with padding") {
                    TimeTextWithPadding()
                },
            )
        ),
        DemoCategory(
            "Button",
            listOf(
                ComposableDemo("Button Sizes") { ButtonSizes() },
                ComposableDemo("Button Styles") { ButtonStyles() },
            )
        ),
        ComposableDemo("Toggle Button") { ToggleButtons() },
        DemoCategory(
            "Chips",
            listOf(
                ComposableDemo("Chip") { StandardChips() },
                ComposableDemo("Compact chip") { SmallChips() },
                ComposableDemo("Avatar chip") { AvatarChips() },
                ComposableDemo("Rtl chips") { RtlChips() },
                ComposableDemo("Custom chips") { CustomChips() },
                ComposableDemo("Image background chips") { ImageBackgroundChips() },
            )
        ),
        DemoCategory(
            "Toggle Chip",
            listOf(
                ComposableDemo("Toggle chip") { ToggleChips() },
                ComposableDemo("RTL Toggle chip") { RtlToggleChips() },
            )
        ),
        ComposableDemo("Card") { CardDemo() },
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
