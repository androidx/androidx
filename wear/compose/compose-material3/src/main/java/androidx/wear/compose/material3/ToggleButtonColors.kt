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

package androidx.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

/**
 * Represents the different container and content colors used for toggle buttons
 * ([IconToggleButton] and [TextToggleButton]) in various states, that are checked, unchecked,
 * enabled and disabled.
 */
@Immutable
public class ToggleButtonColors public constructor(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
) {

    /**
     * Determines the container color based on whether the toggle button is [enabled]
     * and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    fun containerColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedContainerColor else uncheckedContainerColor
            } else {
                if (checked) disabledCheckedContainerColor else disabledUncheckedContainerColor
            }
        )
    }

    /**
     * Determines the content color based on whether the toggle button is [enabled]
     * and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedContentColor else uncheckedContentColor
            } else {
                if (checked) disabledCheckedContentColor else disabledUncheckedContentColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as ToggleButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        return result
    }
}
