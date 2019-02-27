/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.core

import androidx.ui.painting.Canvas
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Use Draw to get a [Canvas] to paint into the parent.
 *
 *     <Draw> canvas, parentSize ->
 *         val paint = Paint()
 *         paint.color = Color(0xFF000000.toInt())
 *         canvas.drawRect(Rect(0.0f, 0.0f, parentSize.width, parentSize.height, paint)
 *     </Draw>
 */
class Draw(@Children(composable = false) var onPaint: (canvas: Canvas, parentSize: PxSize) -> Unit) : Component() {

    override fun compose() {
        // Hide the internals of DrawNode
        <DrawNode onPaint />
    }
}

/**
 * Temporary needed to be able to use the component from the adapter module. b/120971484
 */
@Composable
fun DrawComposable(@Children(composable = false) onPaint: (canvas: Canvas, parentSize: PxSize) -> Unit) {
    <Draw onPaint />
}