/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalFoundationApi::class)
internal class PositionedPage(
    override val index: Int,
    override val offset: Int,
    val key: Any,
    val orientation: Orientation,
    val wrappers: MutableList<PagerPlaceableWrapper>,
    val visualOffset: IntOffset
) : PageInfo {
    fun place(scope: Placeable.PlacementScope) = with(scope) {
        repeat(wrappers.size) { index ->
            val placeable = wrappers[index].placeable
            val offset = wrappers[index].offset
            if (orientation == Orientation.Vertical) {
                placeable.placeWithLayer(offset + visualOffset)
            } else {
                placeable.placeRelativeWithLayer(offset + visualOffset)
            }
        }
    }
}