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

import androidx.compose.Composable
import androidx.ui.graphics.Canvas
import androidx.ui.tooling.InspectionMode
import androidx.ui.unit.DensityScope
import androidx.ui.unit.PxSize

/**
 * Use Draw to get a [Canvas] to paint into the parent.
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.DrawSample
 *
 *  The [onPaint] lambda uses a [DensityScope] receiver scope, to allow easy translation
 *  between [Dp], [Sp], and [Px]. The `parentSize` parameter indicates the layout size of
 *  the parent.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Draw(
    noinline onPaint: DensityScope.(canvas: Canvas, parentSize: PxSize) -> Unit
) {
    // Hide the internals of DrawNode
    if (InspectionMode.current) {
        RepaintBoundaryNode(name = null) {
            DrawNode(onPaint = onPaint)
        }
    } else {
        DrawNode(onPaint = onPaint)
    }
}

/**
 * A Draw scope that accepts children to allow modifying the canvas for children.
 * The [children] are drawn when [DrawReceiver.drawChildren] is called.
 * If the [onPaint] does not call [DrawReceiver.drawChildren] then it will be called
 * after the lambda.
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.DrawWithChildrenSample
 */
@Composable
inline fun Draw(
    crossinline children: @Composable() () -> Unit,
    noinline onPaint: DrawReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit
) {
    // Hide the internals of DrawNode
    if (InspectionMode.current) {
        RepaintBoundaryNode(name = null) {
            DrawNode(onPaintWithChildren = onPaint) {
                children()
            }
        }
    } else {
        DrawNode(onPaintWithChildren = onPaint) {
            children()
        }
    }
}
