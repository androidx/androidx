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

package androidx.ui.framework.demos

import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.framework.demos.autofill.ExplicitAutofillTypesDemo
import androidx.ui.framework.demos.focus.FocusableDemo
import androidx.ui.framework.demos.gestures.DoubleTapGestureDetectorDemo
import androidx.ui.framework.demos.gestures.DragScaleGestureDetectorDemo
import androidx.ui.framework.demos.gestures.LongPressDragGestureDetectorDemo
import androidx.ui.framework.demos.gestures.LongPressGestureDetectorDemo
import androidx.ui.framework.demos.gestures.NestedLongPressDemo
import androidx.ui.framework.demos.gestures.NestedPressDemo
import androidx.ui.framework.demos.gestures.NestedScalingDemo
import androidx.ui.framework.demos.gestures.NestedScrollingDemo
import androidx.ui.framework.demos.gestures.PopupDragDemo
import androidx.ui.framework.demos.gestures.PressIndicatorGestureDetectorDemo
import androidx.ui.framework.demos.gestures.PressReleasedGestureDetectorDemo
import androidx.ui.framework.demos.gestures.RawDragGestureDetectorDemo
import androidx.ui.framework.demos.gestures.ScaleGestureDetectorDemo
import androidx.ui.framework.demos.gestures.TouchSlopDragGestureDetectorDemo
import androidx.ui.framework.demos.gestures.TouchSlopExceededGestureDetectorDemo

private val GestureDemos = DemoCategory("Gestures", listOf(
    DemoCategory("Simple - Non-Movement", listOf(
        ComposableDemo("PressIndicatorGestureDetector") { PressIndicatorGestureDetectorDemo() },
        ComposableDemo("PressReleasedGestureDetector") { PressReleasedGestureDetectorDemo() },
        ComposableDemo("LongPressGestureDetector") { LongPressGestureDetectorDemo() },
        ComposableDemo("DoubleTapGestureDetector") { DoubleTapGestureDetectorDemo() }
    )),
    DemoCategory("Simple - Movement", listOf(
        ComposableDemo("TouchSlopDragGestureDetector") { TouchSlopDragGestureDetectorDemo() },
        ComposableDemo("LongPressDragGestureDetector") { LongPressDragGestureDetectorDemo() },
        ComposableDemo("RawDragGestureDetector") { RawDragGestureDetectorDemo() },
        ComposableDemo("TouchSlopExceededGestureDetector") {
            TouchSlopExceededGestureDetectorDemo()
        },
        ComposableDemo("ScaleGestureDetector") { ScaleGestureDetectorDemo() }
    )),
    DemoCategory("Complex", listOf(
        ComposableDemo("Nested scrolling") { NestedScrollingDemo() },
        ComposableDemo("Nested press") { NestedPressDemo() },
        ComposableDemo("Nested long press") { NestedLongPressDemo() },
        ComposableDemo("Nested scaling") { NestedScalingDemo() },
        ComposableDemo("Popup drag") { PopupDragDemo() },
        ComposableDemo("Drag and scale") { DragScaleGestureDetectorDemo() }
    ))
))

val FrameworkDemos = DemoCategory("Framework Demos", listOf(
    ComposableDemo("Animations, gestures, and semantics") { AnimationGestureSemanticsDemo() },
    ComposableDemo("Explicit autofill types") { ExplicitAutofillTypesDemo() },
    ComposableDemo("Focus") { FocusableDemo() },
    ComposableDemo("Multiple collects measure") { MultipleCollectTest() },
    ComposableDemo("Popup") { PopupDemo() },
    ComposableDemo("Vector graphics") { VectorGraphicsDemo() },
    GestureDemos
))
