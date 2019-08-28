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

package androidx.ui.core

/**
 * A [Placeable] corresponds to a child layout that can be positioned by its
 * parent layout. Most [Placeable]s are the result of a [Measurable.measure] call.
 *
 * A `Placeable` should never be stored between measure calls.
 */
abstract class Placeable {
    /**
     * The width, in pixels, of the measured layout. This is the `width`
     * value passed into [MeasureBlockScope.layout].
     */
    abstract val width: IntPx

    /**
     * The height, in pixels, of the measured layout. This is the `height`
     * value passed into [MeasureBlockScope.layout].
     */
    abstract val height: IntPx

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or [null] if the line is not provided.
     */
    abstract operator fun get(line: AlignmentLine): IntPx?

    /**
     * Positions the layout within its parent. This must be called or the
     * layout won't be shown within its parent. This should be called within
     * the [MeasureBlockScope.layout]'s lambda. Public access to this method
     * is available within the [MeasureBlockScope.layout] `positioningBlock`
     * lambda.
     */
    protected abstract fun place(x: IntPx, y: IntPx)

    /**
     * Called from [PositioningBlockScope] to place a Placeable.
     */
    internal fun placeInternal(x: IntPx, y: IntPx) {
        this.place(x, y)
    }
}