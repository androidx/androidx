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
 * @property isTabletop `true` if the current window is considered as in the table top mode, i.e.
 *           there is one half-opened horizontal hinge in the middle of the current window. When
 *           this is `true` it usually means it's hard for users to interact with the window area
 *           around the hinge and developers may consider separating the layout along the hinge and
 *           show software keyboard or other controls in the bottom half of the window.
 * @property hingeList a list of all hinges that are relevant to the posture.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class Posture(
    val isTabletop: Boolean = false,
    val hingeList: List<HingeInfo> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Posture) return false
        if (isTabletop != other.isTabletop) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isTabletop.hashCode()
        result = 31 * result + hingeList.hashCode()
        return result
    }

    override fun toString(): String {
        @Suppress("ListIterator")
        return "Posture(isTabletop=$isTabletop, " +
            "hinges=[${hingeList.joinToString(", ")}])"
    }
}

/**
 * Returns the list of vertical hinge bounds that are separating.
 */
@ExperimentalMaterial3AdaptiveApi
val Posture.separatingVerticalHingeBounds get() = hingeList.getBounds { isVertical && isSeparating }

/**
 *  Returns the list of vertical hinge bounds that are occluding.
 */
@ExperimentalMaterial3AdaptiveApi
val Posture.occludingVerticalHingeBounds get() = hingeList.getBounds { isVertical && isOccluding }

/**
 *  Returns the list of all vertical hinge bounds.
 */
@ExperimentalMaterial3AdaptiveApi
val Posture.allVerticalHingeBounds get() = hingeList.getBounds { isVertical }

/**
 * Returns the list of horizontal hinge bounds that are separating.
 */
@ExperimentalMaterial3AdaptiveApi
val Posture.separatingHorizontalHingeBounds
    get() = hingeList.getBounds { !isVertical && isSeparating }

/**
 * Returns the list of horizontal hinge bounds that are occluding.
 */
@ExperimentalMaterial3AdaptiveApi
val Posture.occludingHorizontalHingeBounds
    get() = hingeList.getBounds { !isVertical && isOccluding }

/**
 *  Returns the list of all horizontal hinge bounds.
 */
@ExperimentalMaterial3AdaptiveApi
val Posture.allHorizontalHingeBounds
    get() = hingeList.getBounds { !isVertical }

/**
 * A class that contains the info of a hinge relevant to a [Posture].
 *
 * @param bounds the bounds of the hinge in the relevant viewport.
 * @param isVertical `true` if the hinge is a vertical one, i.e., it separates the viewport into
 *        left and right; `false` if the hinge is horizontal, i.e., it separates the viewport
 *        into top and bottom.
 * @param isSeparating `true` if the hinge creates two logical display areas.
 * @param isOccluding `true` if the hinge conceals part of the display.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class HingeInfo(
    val bounds: Rect,
    val isVertical: Boolean,
    val isSeparating: Boolean,
    val isOccluding: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HingeInfo) return false
        if (bounds != other.bounds) return false
        if (isVertical != other.isVertical) return false
        if (isSeparating != other.isSeparating) return false
        if (isOccluding != other.isOccluding) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bounds.hashCode()
        result = 31 * result + isVertical.hashCode()
        result = 31 * result + isSeparating.hashCode()
        result = 31 * result + isOccluding.hashCode()
        return result
    }

    override fun toString(): String {
        return "HingeInfo(bounds=$bounds, " +
            "isVertical=$isVertical, " +
            "isSeparating=$isSeparating, " +
            "isOccluding=$isOccluding)"
    }
}

@ExperimentalMaterial3AdaptiveApi
private inline fun List<HingeInfo>.getBounds(predicate: HingeInfo.() -> Boolean): List<Rect> =
    @Suppress("ListIterator")
    mapNotNull { if (it.predicate()) it.bounds else null }
