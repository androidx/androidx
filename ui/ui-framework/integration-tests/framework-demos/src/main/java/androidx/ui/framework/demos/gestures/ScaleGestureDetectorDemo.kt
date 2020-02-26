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
import androidx.ui.core.gesture.ScaleGestureDetector
import androidx.ui.core.gesture.ScaleObserver
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.unit.dp
import androidx.ui.unit.px

/**
 * Simple demo that shows off ScaleGestureDetectorDemo.
 */
class ScaleGestureDetectorDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val width = state { 96.dp }
            val height = state { 96.dp }

            val scaleObserver = object : ScaleObserver {
                override fun onScale(scaleFactor: Float) {
                    width.value *= scaleFactor
                    height.value *= scaleFactor
                }
            }

            ScaleGestureDetector(scaleObserver) {
                DrawingBox(0.px, 0.px, width.value, height.value, Color(0xFF9e9e9e.toInt()))
            }
        }
    }
}