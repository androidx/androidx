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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines [Shape] for all TV [Interaction] states of a Clickable Surface.
 * @param shape [Shape] to be applied when Clickable Surface is in the default state.
 * @param focusedShape [Shape] to be applied when Clickable Surface is focused.
 * @param pressedShape [Shape] to be applied when Clickable Surface is pressed.
 * @param disabledShape [Shape] to be applied when Clickable Surface is disabled.
 * @param focusedDisabledShape [Shape] to be applied when Clickable Surface is focused in the
 * default state.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceShape internal constructor(
    val shape: Shape,
    val focusedShape: Shape,
    val pressedShape: Shape,
    val disabledShape: Shape,
    val focusedDisabledShape: Shape
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClickableSurfaceShape

        if (shape != other.shape) return false
        if (focusedShape != other.focusedShape) return false
        if (pressedShape != other.pressedShape) return false
        if (disabledShape != other.disabledShape) return false
        if (focusedDisabledShape != other.focusedDisabledShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + focusedShape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + disabledShape.hashCode()
        result = 31 * result + focusedDisabledShape.hashCode()

        return result
    }

    override fun toString(): String {
        return "ClickableSurfaceShape(shape=$shape, focusedShape=$focusedShape, " +
            "pressedShape=$pressedShape, disabledShape=$disabledShape, " +
            "focusedDisabledShape=$focusedDisabledShape)"
    }
}

/**
 * Defines [Color] for all TV [Interaction] states of a Clickable Surface.
 * @param color [Color] to be applied when Clickable Surface is in the default state.
 * @param focusedColor [Color] to be applied when Clickable Surface is focused.
 * @param pressedColor [Color] to be applied when Clickable Surface is pressed.
 * @param disabledColor [Color] to be applied when Clickable Surface is disabled.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceColor internal constructor(
    val color: Color,
    val focusedColor: Color,
    val pressedColor: Color,
    val disabledColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClickableSurfaceColor

        if (color != other.color) return false
        if (focusedColor != other.focusedColor) return false
        if (pressedColor != other.pressedColor) return false
        if (disabledColor != other.disabledColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + focusedColor.hashCode()
        result = 31 * result + pressedColor.hashCode()
        result = 31 * result + disabledColor.hashCode()

        return result
    }

    override fun toString(): String {
        return "ClickableSurfaceColor(color=$color, focusedColor=$focusedColor, " +
            "pressedColor=$pressedColor, disabledColor=$disabledColor)"
    }
}

/**
 * Defines [Glow] for all TV states of [Surface].
 * @param glow [Glow] to be applied when [Surface] is in the default state.
 * @param focusedGlow [Glow] to be applied when [Surface] is focused.
 * @param pressedGlow [Glow] to be applied when [Surface] is pressed.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceGlow internal constructor(
    val glow: Glow,
    val focusedGlow: Glow,
    val pressedGlow: Glow
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClickableSurfaceGlow

        if (glow != other.glow) return false
        if (focusedGlow != other.focusedGlow) return false
        if (pressedGlow != other.pressedGlow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = glow.hashCode()
        result = 31 * result + focusedGlow.hashCode()
        result = 31 * result + pressedGlow.hashCode()

        return result
    }

    override fun toString(): String {
        return "ClickableSurfaceGlow(glow=$glow, focusedGlow=$focusedGlow, " +
            "pressedGlow=$pressedGlow)"
    }
}

/**
 * Defines the shadow for a TV component.
 * @param elevationColor [Color] to be applied on the shadow
 * @param elevation defines how strong should be the shadow. Larger its value, further the
 * shadow goes from the center of the component.
 */
@ExperimentalTvMaterial3Api
@Immutable
class Glow(
    val elevationColor: Color,
    val elevation: Dp
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Glow

        if (elevationColor != other.elevationColor) return false
        if (elevation != other.elevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elevationColor.hashCode()
        result = 31 * result + elevation.hashCode()
        return result
    }

    override fun toString(): String {
        return "Glow(elevationColor=$elevationColor, elevation=$elevation)"
    }

    fun copy(
        glowColor: Color? = null,
        glowElevation: Dp? = null
    ): Glow = Glow(
        elevationColor = glowColor ?: this.elevationColor,
        elevation = glowElevation ?: this.elevation
    )

    companion object {
        /**
         * Signifies the absence of a glow in TV Components. Use this if you do not want to display
         * a glow indication in any of the Leanback TV Components.
         */
        val None = Glow(
            elevationColor = Color.Transparent,
            elevation = 0.dp
        )
    }
}
