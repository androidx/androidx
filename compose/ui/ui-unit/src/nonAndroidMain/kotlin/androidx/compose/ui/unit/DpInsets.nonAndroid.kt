/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi


/**
 * Represents a set of insets in [Dp] units.
 */
@ExperimentalComposeUiApi
@Immutable
class DpInsets(
    val top: Dp,
    val left: Dp,
    val bottom: Dp,
    val right: Dp
) {

    /**
     * Returns the sum of the insets.
     */
    operator fun plus(other: DpInsets) = DpInsets(
        top = top + other.top,
        left = left + other.left,
        bottom = bottom + other.bottom,
        right = right + other.right
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DpInsets) return false

        if (top != other.top) return false
        if (left != other.left) return false
        if (bottom != other.bottom) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode(): Int {
        var result = top.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}

/**
 * Returns the rectangle remaining after applying the given insets.
 */
@ExperimentalComposeUiApi
operator fun DpRect.minus(insets: DpInsets): DpRect =
    DpRect(
        top = top + insets.top,
        left = left + insets.left,
        bottom = bottom - insets.bottom,
        right = right - insets.right
    )

/**
 * Returns the size after adding the given insets.
 */
@ExperimentalComposeUiApi
operator fun DpSize.plus(insets: DpInsets): DpSize =
    DpSize(
        width = width + insets.left + insets.right,
        height = height + insets.top + insets.bottom
    )
