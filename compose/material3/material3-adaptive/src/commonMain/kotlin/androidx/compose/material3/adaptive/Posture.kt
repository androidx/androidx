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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.fastDistinctBy

/**
 * Posture info that can help make layout adaptation decisions. For example when
 * [Posture.separatingVerticalHingeBounds] is not empty, the layout may want to avoid putting any
 * content over those hinge area. We suggest to use [calculatePosture] to retrieve instances of this
 * class in applications, unless you have a strong need of customization that cannot be fulfilled by
 * the default implementation.
 *
 * Note that the hinge bounds will be represent as [Rect] with window coordinates, instead of layout
 * coordinate.
 *
 * @constructor create an instance of [Posture]
 * @param isTabletop `true` if the current window is considered as in the table top mode, i.e. there
 *        is one half-opened horizontal hinge in the middle of the current window. When this is
 *        `true` it usually means it's hard for users to interact with the window area around
 *        the hinge and developers may consider separating the layout along the hinge and show
 *        software keyboard or other controls in the bottom half of the window.
 * @param separatingVerticalHingeBounds the list of vertical hinge bounds that are separating.
 *        A separating hinge means it creates two logical display areas.
 * @param occludingVerticalHingeBounds the list of vertical hinge bounds that are occluding.
 *        An occluding hinge means it conceals part of the display.
 * @param allVerticalHingeBounds the list of all vertical hinge bounds.
 * @param separatingHorizontalHingeBounds the list of horizontal hinge bounds that are separating.
 *        A separating hinge means it creates two logical display areas.
 * @param occludingHorizontalHingeBounds the list of horizontal hinge bounds that are occluding.
 *        An occluding hinge means it conceals part of the display.
 * @param allHorizontalHingeBounds the list of all horizontal hinge bounds.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class Posture(
    val isTabletop: Boolean = false,
    val separatingVerticalHingeBounds: List<Rect> = emptyList(),
    val occludingVerticalHingeBounds: List<Rect> = emptyList(),
    val allVerticalHingeBounds: List<Rect> =
        combineList(separatingVerticalHingeBounds, occludingVerticalHingeBounds)
            .fastDistinctBy { it },
    val separatingHorizontalHingeBounds: List<Rect> = emptyList(),
    val occludingHorizontalHingeBounds: List<Rect> = emptyList(),
    val allHorizontalHingeBounds: List<Rect> =
        combineList(separatingHorizontalHingeBounds, occludingHorizontalHingeBounds)
            .fastDistinctBy { it },
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Posture) return false
        if (isTabletop != other.isTabletop) return false
        if (separatingVerticalHingeBounds != other.separatingVerticalHingeBounds) return false
        if (occludingVerticalHingeBounds != other.occludingVerticalHingeBounds) return false
        if (allVerticalHingeBounds != other.allVerticalHingeBounds) return false
        if (separatingHorizontalHingeBounds != other.separatingHorizontalHingeBounds) return false
        if (occludingHorizontalHingeBounds != other.occludingHorizontalHingeBounds) return false
        if (allHorizontalHingeBounds != other.allHorizontalHingeBounds) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isTabletop.hashCode()
        result = 31 * result + separatingVerticalHingeBounds.hashCode()
        result = 31 * result + occludingVerticalHingeBounds.hashCode()
        result = 31 * result + allVerticalHingeBounds.hashCode()
        result = 31 * result + separatingHorizontalHingeBounds.hashCode()
        result = 31 * result + occludingHorizontalHingeBounds.hashCode()
        result = 31 * result + allHorizontalHingeBounds.hashCode()
        return result
    }
}

private fun <T> combineList(first: List<T>, second: List<T>): List<T> {
    return when {
        first.isEmpty() -> second
        second.isEmpty() -> first
        else -> {
            val list = ArrayList<T>(first.size + second.size)
            list.addAll(first)
            list.addAll(second)
            list
        }
    }
}
