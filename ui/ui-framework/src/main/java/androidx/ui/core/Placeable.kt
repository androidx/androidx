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
 * A [Placeable] corresponds to a child which can be positioned by its parent container.
 * Most [Placeable]s are the result of a [Measureable.measure] call.
 */
// TODO(popam): investigate if this class is really needed, as it blocks making
//              MeasuredPlaceable an inline class
abstract class Placeable {
    abstract val width: IntPx
    abstract val height: IntPx
    protected abstract fun place(x: IntPx, y: IntPx)
    internal fun placeInternal(x: IntPx, y: IntPx) {
        place(x, y)
    }
}