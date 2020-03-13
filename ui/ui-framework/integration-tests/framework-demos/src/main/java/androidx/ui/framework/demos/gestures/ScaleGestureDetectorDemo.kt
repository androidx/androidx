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

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.gesture.ScaleGestureDetector
import androidx.ui.core.gesture.ScaleObserver
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.dp

/**
 * Simple demo that shows off ScaleGestureDetectorDemo.
 */
@Composable
fun ScaleGestureDetectorDemo() {
    val size = state { 192.dp }

    val scaleObserver = object : ScaleObserver {
        override fun onScale(scaleFactor: Float) {
            size.value *= scaleFactor
        }
    }

    Box(
        LayoutSize.Fill +
                LayoutAlign.Center +
                ScaleGestureDetector(scaleObserver) +
                LayoutSize(size.value),
        backgroundColor = Color(0xFF9e9e9e.toInt())
    )
}
