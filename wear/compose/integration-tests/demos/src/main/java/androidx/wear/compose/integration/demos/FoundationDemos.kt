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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.samples.CurvedAndNormalText
import androidx.wear.compose.foundation.samples.CurvedBackground
import androidx.wear.compose.foundation.samples.CurvedBottomLayout
import androidx.wear.compose.foundation.samples.CurvedFixedSize
import androidx.wear.compose.foundation.samples.CurvedFontHeight
import androidx.wear.compose.foundation.samples.CurvedFontWeight
import androidx.wear.compose.foundation.samples.CurvedFonts
import androidx.wear.compose.foundation.samples.CurvedRowAndColumn
import androidx.wear.compose.foundation.samples.CurvedWeight
import androidx.wear.compose.foundation.samples.EdgeSwipeForSwipeToDismiss
import androidx.wear.compose.foundation.samples.ExpandableTextSample
import androidx.wear.compose.foundation.samples.ExpandableWithItemsSample
import androidx.wear.compose.foundation.samples.HierarchicalFocusCoordinatorSample
import androidx.wear.compose.foundation.samples.OversizeComposable
import androidx.wear.compose.foundation.samples.RotaryScrollSample
import androidx.wear.compose.foundation.samples.RotarySnapSample
import androidx.wear.compose.foundation.samples.ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo
import androidx.wear.compose.foundation.samples.SimpleCurvedWorld
import androidx.wear.compose.foundation.samples.SimpleHorizontalPagerSample
import androidx.wear.compose.foundation.samples.SimpleScalingLazyColumn
import androidx.wear.compose.foundation.samples.SimpleScalingLazyColumnWithContentPadding
import androidx.wear.compose.foundation.samples.SimpleScalingLazyColumnWithSnap
import androidx.wear.compose.foundation.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.foundation.samples.SimpleVerticalPagerSample
import androidx.wear.compose.foundation.samples.StatefulSwipeToDismissBox
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material.samples.SwipeToRevealCardSample
import androidx.wear.compose.material.samples.SwipeToRevealChipSample

// Declare the swipe to dismiss demos so that we can use this variable as the background composable
// for the SwipeToDismissDemo itself.
internal val SwipeToDismissDemos =
    DemoCategory(
        "Swipe to Dismiss",
        listOf(
            DemoCategory(
                "Samples",
                listOf(
                    ComposableDemo("Simple") { params ->
                        SimpleSwipeToDismissBox(params.navigateBack)
                    },
                    ComposableDemo("Stateful") { StatefulSwipeToDismissBox() },
                    ComposableDemo("Edge swipe") { params ->
                        EdgeSwipeForSwipeToDismiss(params.navigateBack)
                    },
                )
            ),
            DemoCategory(
                "Demos",
                listOf(
                    ComposableDemo("Demo") { params ->
                        val state = remember { mutableStateOf(SwipeDismissDemoState.List) }
                        SwipeToDismissDemo(navigateBack = params.navigateBack, demoState = state)
                    },
                    ComposableDemo("Stateful Demo") { params ->
                        SwipeToDismissBoxWithState(params.navigateBack)
                    },
                    ComposableDemo("EdgeSwipeToDismiss modifier") { params ->
                        EdgeSwipeDemo(params.swipeToDismissBoxState)
                    },
                    ComposableDemo("Nested SwipeToDismissBox") { NestedSwipeToDismissDemo() }
                )
            )
        )
    )

val WearFoundationDemos =
    DemoCategory(
        "Foundation",
        listOf(
            DemoCategory(
                "Expandables",
                listOf(
                    ComposableDemo("Items in SLC") { ExpandableListItems() },
                    ComposableDemo("Multiple Items") { ExpandableMultipleItems() },
                    ComposableDemo("Expandable Text") { ExpandableText() },
                    ComposableDemo("Items Sample") { ExpandableWithItemsSample() },
                    ComposableDemo("Text Sample") { ExpandableTextSample() },
                )
            ),
            DemoCategory(
                "CurvedLayout",
                listOf(
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
                    ComposableDemo("Font Height") { CurvedFontHeight() },
                    ComposableDemo("Fonts") { CurvedFonts() },
                    ComposableDemo("Curved Icons") { CurvedIconsDemo() },
                    ComposableDemo("Letter Spacing (em)") { CurvedSpacingEmDemo() },
                    ComposableDemo("Letter Spacing (sp)") { CurvedSpacingSpDemo() },
                )
            ),
            DemoCategory(
                "Pagers",
                listOf(
                    ComposableDemo("Horizontal Pager") { SimpleHorizontalPagerSample() },
                    ComposableDemo("Vertical Pager") { SimpleVerticalPagerSample() },
                )
            ),
            ComposableDemo("Scrollable Column") { ScrollableColumnDemo() },
            ComposableDemo("Scrollable Row") { ScrollableRowDemo() },
            DemoCategory(
                "Rotary Input",
                listOf(
                    DemoCategory(
                        "Samples",
                        listOf(
                            ComposableDemo(".rotary with scroll") { RotaryScrollSample() },
                            ComposableDemo(".rotary with snap") { RotarySnapSample() },
                            ComposableDemo("RotaryEvent") { ScrollUsingRotatingCrownDemo() },
                            ComposableDemo("PreRotaryEvent") { InterceptScrollDemo() }
                        )
                    ),
                    DemoCategory("Demos", listOf())
                )
            ),
            ComposableDemo("Focus Sample") { HierarchicalFocusCoordinatorSample() },
            DemoCategory(
                "Scaling Lazy Column",
                listOf(
                    ComposableDemo("Defaults", "Basic ScalingLazyColumn using default values") {
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
            SwipeToDismissDemos,
            DemoCategory(
                "Swipe To Reveal",
                listOf(
                    DemoCategory(
                        "Samples",
                        listOf(
                            ComposableDemo("Material S2R Chip") { params ->
                                Centralize {
                                    SwipeToRevealChipSample(params.swipeToDismissBoxState)
                                }
                            },
                            ComposableDemo("Material S2R Card") { params ->
                                Centralize {
                                    SwipeToRevealCardSample(params.swipeToDismissBoxState)
                                }
                            },
                            ComposableDemo("Material S2R Cards Inside SLC") { params ->
                                Centralize {
                                    ScalingLazyColumn {
                                        item {
                                            SwipeToRevealCardSample(params.swipeToDismissBoxState)
                                        }
                                        item {
                                            SwipeToRevealCardSample(params.swipeToDismissBoxState)
                                        }
                                        item {
                                            SwipeToRevealCardSample(params.swipeToDismissBoxState)
                                        }
                                        item {
                                            SwipeToRevealCardSample(params.swipeToDismissBoxState)
                                        }
                                    }
                                }
                            },
                        )
                    ),
                    DemoCategory(
                        "Demos",
                        listOf(
                            ComposableDemo("S2R Chip, 2 actions") { params ->
                                SwipeToRevealChips(
                                    params.swipeToDismissBoxState,
                                    includeSecondaryAction = true
                                )
                            },
                            ComposableDemo("S2R Chip, 1 action") { params ->
                                SwipeToRevealChips(
                                    params.swipeToDismissBoxState,
                                    includeSecondaryAction = false
                                )
                            },
                            ComposableDemo("S2R Card") { params ->
                                SwipeToRevealCards(params.swipeToDismissBoxState)
                            },
                        )
                    )
                )
            )
        ),
    )
