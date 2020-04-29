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

import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.core.demos.autofill.ExplicitAutofillTypesDemo
import androidx.ui.core.demos.focus.FocusableDemo
import androidx.ui.core.demos.gestures.DoubleTapGestureDetectorDemo
import androidx.ui.core.demos.gestures.DoubleTapInTapDemo
import androidx.ui.core.demos.gestures.DragScaleGestureDetectorDemo
import androidx.ui.core.demos.gestures.LongPressDragGestureDetectorDemo
import androidx.ui.core.demos.gestures.LongPressGestureDetectorDemo
import androidx.ui.core.demos.gestures.NestedLongPressDemo
import androidx.ui.core.demos.gestures.NestedPressDemo
import androidx.ui.core.demos.gestures.NestedScalingDemo
import androidx.ui.core.demos.gestures.NestedScrollingDemo
import androidx.ui.core.demos.gestures.PopupDragDemo
import androidx.ui.core.demos.gestures.PressIndicatorGestureDetectorDemo
import androidx.ui.core.demos.gestures.PressReleasedGestureDetectorDemo
import androidx.ui.core.demos.gestures.RawDragGestureDetectorDemo
import androidx.ui.core.demos.gestures.ScaleGestureDetectorDemo
import androidx.ui.core.demos.gestures.TouchSlopDragGestureDetectorDemo
import androidx.ui.core.demos.gestures.TouchSlopExceededGestureDetectorDemo
import androidx.ui.core.demos.viewinterop.ViewInComposeDemo

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
        ComposableDemo("Drag and scale") { DragScaleGestureDetectorDemo() },
        ComposableDemo("Double tap in tap") { DoubleTapInTapDemo() }
    ))
))

private val GraphicsDemos = DemoCategory("Graphics", listOf(
    ComposableDemo("VectorGraphicsDemo") { VectorGraphicsDemo() },
    ComposableDemo("DeclarativeGraphicsDemo") { DeclarativeGraphicsDemo() }
))

val CoreDemos = DemoCategory("Framework", listOf(
    ComposableDemo("Explicit autofill types") { ExplicitAutofillTypesDemo() },
    ComposableDemo("Focus") { FocusableDemo() },
    ComposableDemo("Multiple collects measure") { MultipleCollectTest() },
    ComposableDemo("Popup") { PopupDemo() },
    GraphicsDemos,
    GestureDemos,
    ComposableDemo("View in compose") { ViewInComposeDemo() }
))
