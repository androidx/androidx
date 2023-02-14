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

package androidx.tv.material3

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Defines [Shape] for all TV [Interaction] states of a Clickable Surface.
 * @param defaultShape [Shape] to be applied when Clickable Surface is in the default state.
 * @param focusedShape [Shape] to be applied when Clickable Surface is focused.
 * @param pressedShape [Shape] to be applied when Clickable Surface is pressed.
 * @param disabledShape [Shape] to be applied when Clickable Surface is disabled.
 * @param focusedDisabledShape [Shape] to be applied when Clickable Surface is focused in the
 * default state.
 */
@ExperimentalTvMaterial3Api
class ClickableSurfaceShape internal constructor(
    val defaultShape: Shape,
    val focusedShape: Shape,
    val pressedShape: Shape,
    val disabledShape: Shape,
    val focusedDisabledShape: Shape
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClickableSurfaceShape

        if (defaultShape != other.defaultShape) return false
        if (focusedShape != other.focusedShape) return false
        if (pressedShape != other.pressedShape) return false
        if (disabledShape != other.disabledShape) return false
        if (focusedDisabledShape != other.focusedDisabledShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultShape.hashCode()
        result = 31 * result + focusedShape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + disabledShape.hashCode()
        result = 31 * result + focusedDisabledShape.hashCode()

        return result
    }

    override fun toString(): String {
        return "ClickableSurfaceShape(defaultShape=$defaultShape, focusedShape=$focusedShape, " +
            "pressedShape=$pressedShape, disabledShape=$disabledShape, " +
            "focusedDisabledShape=$focusedDisabledShape)"
    }
}

/**
 * Defines [Color] for all TV [Interaction] states of a Clickable Surface.
 * @param defaultColor [Color] to be applied when Clickable Surface is in the default state.
 * @param focusedColor [Color] to be applied when Clickable Surface is focused.
 * @param pressedColor [Color] to be applied when Clickable Surface is pressed.
 * @param disabledColor [Color] to be applied when Clickable Surface is disabled.
 */
@ExperimentalTvMaterial3Api
class ClickableSurfaceColor internal constructor(
    val defaultColor: Color,
    val focusedColor: Color,
    val pressedColor: Color,
    val disabledColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClickableSurfaceColor

        if (defaultColor != other.defaultColor) return false
        if (focusedColor != other.focusedColor) return false
        if (pressedColor != other.pressedColor) return false
        if (disabledColor != other.disabledColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultColor.hashCode()
        result = 31 * result + focusedColor.hashCode()
        result = 31 * result + pressedColor.hashCode()
        result = 31 * result + disabledColor.hashCode()

        return result
    }

    override fun toString(): String {
        return "ClickableSurfaceColor(defaultColor=$defaultColor, focusedColor=$focusedColor, " +
            "pressedColor=$pressedColor, disabledColor=$disabledColor)"
    }
}
