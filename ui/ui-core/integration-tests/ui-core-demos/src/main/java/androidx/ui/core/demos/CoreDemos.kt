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

package androidx.ui.core.demos

import androidx.ui.core.demos.autofill.ExplicitAutofillTypesDemo
import androidx.ui.core.demos.focus.FocusableDemo
import androidx.ui.core.demos.gestures.DoubleTapGestureFilterDemo
import androidx.ui.core.demos.gestures.DoubleTapInTapDemo
import androidx.ui.core.demos.gestures.DragAndScaleGestureDetectorDemo
import androidx.ui.core.demos.gestures.DragSlopExceededGestureFilterDemo
import androidx.ui.core.demos.gestures.LongPressGestureDetectorDemo
import androidx.ui.core.demos.gestures.NestedLongPressDemo
import androidx.ui.core.demos.gestures.NestedPressingDemo
import androidx.ui.core.demos.gestures.NestedScalingDemo
import androidx.ui.core.demos.gestures.NestedScrollingDemo
import androidx.ui.core.demos.gestures.PopupDragDemo
import androidx.ui.core.demos.gestures.PressIndicatorGestureFilterDemo
import androidx.ui.core.demos.gestures.TapGestureFilterDemo
import androidx.ui.core.demos.gestures.RawDragGestureFilterDemo
import androidx.ui.core.demos.gestures.ScaleGestureFilterDemo
import androidx.ui.core.demos.gestures.DragGestureFilterDemo
import androidx.ui.core.demos.gestures.LongPressDragGestureFilterDemo
import androidx.ui.core.demos.keyinput.KeyInputDemo
import androidx.ui.core.demos.viewinterop.ViewInComposeDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory

private val GestureDemos = DemoCategory("Gestures", listOf(
    DemoCategory("Common Gestures", listOf(
        ComposableDemo("Press Indication") { PressIndicatorGestureFilterDemo() },
        ComposableDemo("Tap") { TapGestureFilterDemo() },
        ComposableDemo("Double Tap") { DoubleTapGestureFilterDemo() },
        ComposableDemo("Long Press") { LongPressGestureDetectorDemo() },
        ComposableDemo("Drag") { DragGestureFilterDemo() },
        ComposableDemo("Long Press Drag") { LongPressDragGestureFilterDemo() },
        ComposableDemo("Scale") { ScaleGestureFilterDemo() }
    )),
    DemoCategory("Building Block Gestures", listOf(
        ComposableDemo("Drag Slop Exceeded") { DragSlopExceededGestureFilterDemo() },
        ComposableDemo("Raw Drag") { RawDragGestureFilterDemo() }
    )),
    DemoCategory(
        "Combinations / Case Studies", listOf(
            ComposableDemo("Nested Pressing") { NestedPressingDemo() },
            ComposableDemo("Nested Scrolling") { NestedScrollingDemo() },
            ComposableDemo("Nested Scaling") { NestedScalingDemo() },
            ComposableDemo("Drag and Scale") { DragAndScaleGestureDetectorDemo() },
            ComposableDemo("Popup Drag") { PopupDragDemo() },
            ComposableDemo("Double Tap in Tap") { DoubleTapInTapDemo() },
            ComposableDemo("Nested Long Press") { NestedLongPressDemo() }
        ))
))

private val GraphicsDemos = DemoCategory("Graphics", listOf(
    ComposableDemo("VectorGraphicsDemo") { VectorGraphicsDemo() },
    ComposableDemo("DeclarativeGraphicsDemo") { DeclarativeGraphicsDemo() }
))

val CoreDemos = DemoCategory("Framework", listOf(
    ComposableDemo("Explicit autofill types") { ExplicitAutofillTypesDemo() },
    ComposableDemo("Focus") { FocusableDemo() },
    ComposableDemo("KeyInput") { KeyInputDemo() },
    ComposableDemo("Multiple collects measure") { MultipleCollectTest() },
    ComposableDemo("Popup") { PopupDemo() },
    GraphicsDemos,
    GestureDemos,
    ComposableDemo("View in compose") { ViewInComposeDemo() }
))
