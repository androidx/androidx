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

package androidx.compose.ui.platform

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * This class represents platform insets.
 */
@ExperimentalComposeUiApi
@Immutable
class PlatformInsets(
    @Stable
    val top: Dp = 0.dp,
    @Stable
    val bottom: Dp = 0.dp,
    @Stable
    val left: Dp = 0.dp,
    @Stable
    val right: Dp = 0.dp,
) {
    companion object {
        val Zero = PlatformInsets(0.dp, 0.dp, 0.dp, 0.dp)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformInsets) return false

        if (top != other.top) return false
        if (bottom != other.bottom) return false
        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode(): Int {
        var result = top.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlatformInsets(top=$top, bottom=$bottom, left=$left, right=$right)"
    }
}

internal fun PlatformInsets.exclude(insets: PlatformInsets) = PlatformInsets(
    left = (left - insets.left).coerceAtLeast(0.dp),
    top = (top - insets.top).coerceAtLeast(0.dp),
    right = (right - insets.right).coerceAtLeast(0.dp),
    bottom = (bottom - insets.bottom).coerceAtLeast(0.dp)
)
