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
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Use Draw to get a [Canvas] to paint into the parent.
 *
 *     Draw { canvas, parentSize ->
 *         val paint = Paint()
 *         paint.color = Color(0xFF000000.toInt())
 *         canvas.drawRect(Rect(0.0f, 0.0f, parentSize.width, parentSize.height, paint)
 *     }
 *
 *  The [onPaint] lambda uses a [DensityReceiver] receiver scope, to allow easy translation
 *  between [Dp], [Sp], and [Px]. The [parentSize] parameter indicates the layout size of
 *  the parent.
 */
@Composable
fun Draw(
    @Children(composable = false)
    onPaint: DensityReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit
) {
    // Hide the internals of DrawNode
    <DrawNode onPaint=onPaint/>
}

/**
 * A Draw scope that accepts children to allow modifying the canvas for children.
 * The [children] are drawn when [DrawReceiver.drawChildren] is called.
 * If the [onPaint] does not call [DrawReceiver.drawChildren] then it will be called
 * after the lambda.
 *
 *     Draw(children) { canvas, parentSize ->
 *         canvas.save()
 *         val circle = Path()
 *         circle.addOval(parentSize.toRect())
 *         canvas.clipPath(circle)
 *         drawChildren()
 *         canvas.restore()
 *     }
 */
@Composable
fun Draw(
    children: @Composable() () -> Unit,
    @Children(composable = false)
    onPaint: DrawReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit
) {
    // Hide the internals of DrawNode
    <DrawNode onPaintWithChildren=onPaint>
        children()
    </DrawNode>
}
