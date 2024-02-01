/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.foundation.demos.draganddrop.DragAndDropMultiAppDemo
import androidx.compose.foundation.demos.draganddrop.DragAndDropNestedDemo
import androidx.compose.foundation.demos.focus.FocusGroupDemo
import androidx.compose.foundation.demos.pager.PagerDemos
import androidx.compose.foundation.demos.relocation.BringIntoViewAndroidInteropDemo
import androidx.compose.foundation.demos.relocation.BringIntoViewDemo
import androidx.compose.foundation.demos.relocation.BringIntoViewResponderDemo
import androidx.compose.foundation.demos.relocation.BringNestedIntoViewDemo
import androidx.compose.foundation.demos.relocation.BringRectangleIntoViewDemo
import androidx.compose.foundation.demos.relocation.RequestRectangleOnScreenDemo
import androidx.compose.foundation.demos.snapping.SnappingDemos
import androidx.compose.foundation.samples.CanScrollSample
import androidx.compose.foundation.samples.ControlledScrollableRowSample
import androidx.compose.foundation.samples.CustomTouchSlopSample
import androidx.compose.foundation.samples.InteractionSourceFlowSample
import androidx.compose.foundation.samples.SimpleInteractionSourceSample
import androidx.compose.foundation.samples.VerticalScrollExample
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

private val RelocationDemos = listOf(
    ComposableDemo("Bring Into View") { BringIntoViewDemo() },
    ComposableDemo("Bring Rectangle Into View") { BringRectangleIntoViewDemo() },
    ComposableDemo("Custom responder") { BringIntoViewResponderDemo() },
    ComposableDemo("Request Rectangle On Screen") { RequestRectangleOnScreenDemo() },
    ComposableDemo("Android view interop") { BringIntoViewAndroidInteropDemo() },
    ComposableDemo("Nested scrollables") { BringNestedIntoViewDemo() },
)

private val FocusDemos = listOf(
    ComposableDemo("Focus Group") { FocusGroupDemo() },
)

private val GestureDemos = listOf(
    ComposableDemo("AnchoredDraggable") { AnchoredDraggableDemo() },
    ComposableDemo("Draggable, Scrollable, Zoomable, Focusable") { HighLevelGesturesDemo() }
)

private val NestedScrollDemos = listOf(
    ComposableDemo("Nested Scroll") { NestedScrollDemo() },
    ComposableDemo("Nested Scroll Connection") { NestedScrollConnectionSample() },
    ComposableDemo("Nested Scroll Simple Column") { SimpleColumnNestedScrollSample() },
)

private val DragAndDropDemos = listOf(
    ComposableDemo("Multi app drag and drop") { DragAndDropMultiAppDemo() },
    ComposableDemo("Nested Drag and drop") { DragAndDropNestedDemo() }
)

val FoundationDemos = DemoCategory(
    "Foundation",
    listOf(
        DemoCategory("High-level Gestures", GestureDemos),
        DemoCategory("Drag and drop", DragAndDropDemos),
        ComposableDemo("Overscroll") { OverscrollDemo() },
        ComposableDemo("Can scroll forward / backward") { CanScrollSample() },
        ComposableDemo("Vertical scroll") { VerticalScrollExample() },
        ComposableDemo("Controlled Scrollable Row") { ControlledScrollableRowSample() },
        ComposableDemo("Draw Modifiers") { DrawModifiersDemo() },
        ComposableDemo("External Surfaces") { AndroidExternalSurfaceDemo() },
        DemoCategory("Lazy lists", LazyListDemos),
        DemoCategory("Snapping", SnappingDemos),
        DemoCategory("Pagers", PagerDemos),
        ComposableDemo("Simple InteractionSource") { SimpleInteractionSourceSample() },
        ComposableDemo("Flow InteractionSource") { InteractionSourceFlowSample() },
        DemoCategory("Suspending Gesture Detectors", CoroutineGestureDemos),
        DemoCategory("Nested Scroll", NestedScrollDemos),
        DemoCategory("Relocation Demos", RelocationDemos),
        DemoCategory("Focus Demos", FocusDemos),
        DemoCategory("Magnifier Demos", MagnifierDemos),
        ComposableDemo("Custom Touch Slop Demo - Composition Locals") { CustomTouchSlopSample() },
        ComposableDemo("Focused bounds") { FocusedBoundsDemo() },
        ComposableDemo("Scrollable with focused child") { ScrollableFocusedChildDemo() },
        ComposableDemo("Window insets") { WindowInsetsDemo() },
        ComposableDemo("Marquee") { BasicMarqueeDemo() },
        DemoCategory("Pointer Icon", PointerIconDemos)
    )
)
