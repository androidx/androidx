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

import androidx.wear.compose.foundation.samples.CurvedAndNormalText
import androidx.wear.compose.foundation.samples.CurvedBottomLayout
import androidx.wear.compose.foundation.samples.CurvedBackground
import androidx.wear.compose.foundation.samples.CurvedFixedSize
import androidx.wear.compose.foundation.samples.CurvedFontWeight
import androidx.wear.compose.foundation.samples.CurvedFonts
import androidx.wear.compose.foundation.samples.CurvedRowAndColumn
import androidx.wear.compose.foundation.samples.CurvedWeight
import androidx.wear.compose.foundation.samples.ExpandableTextSample
import androidx.wear.compose.foundation.samples.ExpandableWithItemsSample
import androidx.wear.compose.foundation.samples.HierarchicalFocusCoordinatorSample
import androidx.wear.compose.foundation.samples.OversizeComposable
import androidx.wear.compose.foundation.samples.ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo
import androidx.wear.compose.foundation.samples.SimpleCurvedWorld
import androidx.wear.compose.foundation.samples.SimpleScalingLazyColumn
import androidx.wear.compose.foundation.samples.SimpleScalingLazyColumnWithContentPadding
import androidx.wear.compose.foundation.samples.SimpleScalingLazyColumnWithSnap

val WearFoundationDemos = DemoCategory(
    "Foundation",
    listOf(
        DemoCategory(
            "Expandables",
            listOf(
                ComposableDemo("Items in SLC") { ExpandableListItems() },
                ComposableDemo("Expandable Text") { ExpandableText() },
                ComposableDemo("Items Sample") { ExpandableWithItemsSample() },
                ComposableDemo("Text Sample") { ExpandableTextSample() },
            )
        ),
        DemoCategory("CurvedLayout", listOf(
            ComposableDemo("Curved Row") { CurvedWorldDemo() },
            ComposableDemo("Curved Row and Column") { CurvedRowAndColumn() },
            ComposableDemo("Curved Box") { CurvedBoxDemo() },
            ComposableDemo("Simple") { SimpleCurvedWorld() },
            ComposableDemo("Alignment") { CurvedRowAlignmentDemo() },
            ComposableDemo("Curved Text") { BasicCurvedTextDemo() },
            ComposableDemo("Curved and Normal Text") { CurvedAndNormalText() },
            ComposableDemo("Fixed size") { CurvedFixedSize() },
            ComposableDemo("Oversize composable") { OversizeComposable() },
            ComposableDemo("Weights") { CurvedWeight() },
            ComposableDemo("Ellipsis Demo") { CurvedEllipsis() },
            ComposableDemo("Bottom layout") { CurvedBottomLayout() },
            ComposableDemo("Curved layout direction") { CurvedLayoutDirection() },
            ComposableDemo("Background") { CurvedBackground() },
            ComposableDemo("Font Weight") { CurvedFontWeight() },
            ComposableDemo("Fonts") { CurvedFonts() },
        )),
        ComposableDemo("Scrollable Column") { ScrollableColumnDemo() },
        ComposableDemo("Scrollable Row") { ScrollableRowDemo() },
        DemoCategory("Rotary Input", RotaryInputDemos),
        ComposableDemo("Focus Sample") { HierarchicalFocusCoordinatorSample() },
        DemoCategory("Scaling Lazy Column", listOf(
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
            ),
        ),
        DemoCategory(
            "Swipe To Reveal",
            listOf(
                ComposableDemo("Swipe To Reveal Chip") {
                    SwipeToRevealChips()
                },
                ComposableDemo("Swipe to Reveal Card") {
                    SwipeToRevealCards()
                },
                ComposableDemo("Swipe to Reveal - Custom") {
                    SwipeToRevealWithSingleAction()
                },
                ComposableDemo("Swipe to Reveal - RTL") {
                    SwipeToRevealInRtl()
                }
            )
        )
    ),
)
