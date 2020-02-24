/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.framework.demos.gestures

import android.app.Activity
import android.os.Bundle
import androidx.compose.state
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.core.setContent
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.px

/**
 * Simple DragGestureDetector demo.
 */
class RawDragGestureDetectorDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val xOffset = state { 0.px }
            val yOffset = state { 0.px }

            val dragObserver = object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    xOffset.value += dragDistance.x
                    yOffset.value += dragDistance.y
                    return dragDistance
                }
            }

            RawDragGestureDetector(dragObserver = dragObserver) {
                DrawingBox(xOffset.value, yOffset.value, 96.dp, 96.dp, Grey)
            }
        }
    }
}