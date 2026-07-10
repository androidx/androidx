/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.ink.authoring

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.UiThread

/** Called to render a [CompletedShapeT] instance to an [android.graphics.Canvas]. */
@ExperimentalInkCustomShapeWorkflowApi
public interface CompletedShapeRenderer<in CompletedShapeT : Any> {

    /**
     * Draw an instance of [CompletedShapeT] to a [Canvas].
     *
     * @param canvas The output [Canvas] to draw to.
     * @param shape The object to be drawn.
     * @param strokeToScreenTransform The full transform that has already been applied to the
     *   [Canvas] in order to draw this object. Most implementations do not need to use this data,
     *   but it is provided for certain rare circumstances (such as analytical anti-aliasing) that
     *   require it.
     * @param animatorClockStateMillis The current timestamp to be used for animation calculations.
     *   Note that the animation timing may not proceed at the same rate as system elapsed time - it
     *   will by default, but may be paused and resumed, or sped up, or slowed down.
     */
    @UiThread
    public fun draw(
        canvas: Canvas,
        shape: CompletedShapeT,
        strokeToScreenTransform: Matrix,
        animatorClockStateMillis: Long,
    )

    /**
     * Whether calls to [draw] with a new timestamp value results in different visual output. In
     * other words, return `true` if and only if [shape] is animated.
     */
    @UiThread public fun changesWithTime(shape: CompletedShapeT): Boolean = false
}
