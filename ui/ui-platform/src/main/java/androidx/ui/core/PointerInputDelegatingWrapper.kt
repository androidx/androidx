/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.unit.PxPosition

internal class PointerInputDelegatingWrapper(
    wrapped: LayoutNodeWrapper,
    pointerInputModifier: PointerInputModifier
) : DelegatingLayoutNodeWrapper<PointerInputModifier>(wrapped, pointerInputModifier) {

    init {
        pointerInputModifier.pointerInputFilter.layoutCoordinates = this
    }

    override fun hitTest(
        pointerPositionRelativeToScreen: PxPosition,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ): Boolean {
        if (isGlobalPointerInBounds(pointerPositionRelativeToScreen)) {
            // If we were hit, add the pointerInputFilter and keep looking to see if anything
            // further down the tree is also hit and return true.
            hitPointerInputFilters.add(modifier.pointerInputFilter)
            super.hitTest(pointerPositionRelativeToScreen, hitPointerInputFilters)
            return true
        } else {
            // Anything out of bounds of ourselves can't be hit.
            return false
        }
    }
}