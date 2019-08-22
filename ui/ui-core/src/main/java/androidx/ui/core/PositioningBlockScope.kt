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
 * Receiver scope for the [MeasureBlockScope.layout] `positioningBlock` lambda
 * that gives access to [Placeable.place] to position child [Placeable]s
 */
object PositioningBlockScope {
    /**
     * Positions the layout within its parent. This must be called or the
     * layout won't be shown within its parent. This should be called within
     * the [MeasureBlockScope.layout]'s lambda.
     */
    fun Placeable.place(x: IntPx, y: IntPx) {
        this.placeInternal(x, y)
    }

    /**
     * Convenience [Placeable.place] function that accepts [Px] and rounds to nearest
     * [IntPx] using [Px.round].
     */
    fun Placeable.place(x: Px, y: Px) {
        this.placeInternal(x.round(), y.round())
    }
}