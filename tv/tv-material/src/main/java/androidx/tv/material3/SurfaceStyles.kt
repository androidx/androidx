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

import androidx.annotation.FloatRange
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines [Shape] for all TV [Interaction] states of a Clickable Surface.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceShape internal constructor(
    internal val shape: Shape,
    internal val focusedShape: Shape,
    internal val pressedShape: Shape,
    internal val disabledShape: Shape,
    internal val focusedDisabledShape: Shape
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
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceColor internal constructor(
    internal val color: Color,
    internal val focusedColor: Color,
    internal val pressedColor: Color,
    internal val disabledColor: Color
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
 * Defines the scale for all TV indication states of Surface. Note: This scale must be
 * a non-negative float.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceScale internal constructor(
    @FloatRange(from = 0.0) internal val scale: Float,
    @FloatRange(from = 0.0) internal val focusedScale: Float,
    @FloatRange(from = 0.0) internal val pressedScale: Float,
    @FloatRange(from = 0.0) internal val disabledScale: Float,
    @FloatRange(from = 0.0) internal val focusedDisabledScale: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClickableSurfaceScale

        if (scale != other.scale) return false
        if (focusedScale != other.focusedScale) return false
        if (pressedScale != other.pressedScale) return false
        if (disabledScale != other.disabledScale) return false
        if (focusedDisabledScale != other.focusedDisabledScale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scale.hashCode()
        result = 31 * result + focusedScale.hashCode()
        result = 31 * result + pressedScale.hashCode()
        result = 31 * result + disabledScale.hashCode()
        result = 31 * result + focusedDisabledScale.hashCode()

        return result
    }

    override fun toString(): String {
        return "ClickableSurfaceScale(scale=$scale, focusedScale=$focusedScale," +
            "pressedScale=$pressedScale, disabledScale=$disabledScale, " +
            "focusedDisabledScale=$focusedDisabledScale)"
    }

    companion object {
        /**
         * Signifies the absence of a scale in TV Components. Use this if you do not want to
         * display a [ScaleIndication] in any of the Leanback TV Components.
         */
        val None = ClickableSurfaceScale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f,
            disabledScale = 1f,
            focusedDisabledScale = 1f
        )
    }
}

/**
 * Defines [Glow] for all TV states of [Surface].
 */
@ExperimentalTvMaterial3Api
@Immutable
class ClickableSurfaceGlow internal constructor(
    internal val glow: Glow,
    internal val focusedGlow: Glow,
    internal val pressedGlow: Glow
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
