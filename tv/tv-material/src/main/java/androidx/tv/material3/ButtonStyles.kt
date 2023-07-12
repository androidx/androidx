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

/**
 * Defines [Shape] for all TV [Interaction] states of Button.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ButtonShape internal constructor(
    internal val shape: Shape,
    internal val focusedShape: Shape,
    internal val pressedShape: Shape,
    internal val disabledShape: Shape,
    internal val focusedDisabledShape: Shape
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ButtonShape

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
        return "ButtonShape(shape=$shape, focusedShape=$focusedShape, pressedShape=$pressedShape," +
            " disabledShape=$disabledShape, focusedDisabledShape=$focusedDisabledShape)"
    }
}

/**
 * Defines [Color]s for all TV [Interaction] states of Button.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ButtonColors internal constructor(
    internal val containerColor: Color,
    internal val contentColor: Color,
    internal val focusedContainerColor: Color,
    internal val focusedContentColor: Color,
    internal val pressedContainerColor: Color,
    internal val pressedContentColor: Color,
    internal val disabledContainerColor: Color,
    internal val disabledContentColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ButtonColors

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (focusedContainerColor != other.focusedContainerColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (pressedContainerColor != other.pressedContainerColor) return false
        if (pressedContentColor != other.pressedContentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + focusedContainerColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + pressedContainerColor.hashCode()
        result = 31 * result + pressedContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }

    override fun toString(): String {
        return "ButtonColors(containerColor=$containerColor, contentColor=$contentColor, " +
            "focusedContainerColor=$focusedContainerColor, " +
            "focusedContentColor=$focusedContentColor, " +
            "pressedContainerColor=$pressedContainerColor, " +
            "pressedContentColor=$pressedContentColor, " +
            "disabledContainerColor=$disabledContainerColor, " +
            "disabledContentColor=$disabledContentColor)"
    }
}

/**
 * Defines [Color]s for all TV [Interaction] states of a WideButton
 */
@ExperimentalTvMaterial3Api
@Immutable
class WideButtonContentColor internal constructor(
    internal val contentColor: Color,
    internal val focusedContentColor: Color,
    internal val pressedContentColor: Color,
    internal val disabledContentColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WideButtonContentColor

        if (contentColor != other.contentColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (pressedContentColor != other.pressedContentColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + pressedContentColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }

    override fun toString(): String {
        return "WideButtonContentColor(contentColor=$contentColor, " +
            "focusedContentColor=$focusedContentColor, " +
            "pressedContentColor=$pressedContentColor, " +
            "disabledContentColor=$disabledContentColor)"
    }
}

/**
 * Defines the scale for all TV [Interaction] states of Button.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ButtonScale internal constructor(
    @FloatRange(from = 0.0) internal val scale: Float,
    @FloatRange(from = 0.0) internal val focusedScale: Float,
    @FloatRange(from = 0.0) internal val pressedScale: Float,
    @FloatRange(from = 0.0) internal val disabledScale: Float,
    @FloatRange(from = 0.0) internal val focusedDisabledScale: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ButtonScale

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
        return "ButtonScale(scale=$scale, focusedScale=$focusedScale, pressedScale=$pressedScale," +
            " disabledScale=$disabledScale, focusedDisabledScale=$focusedDisabledScale)"
    }
}

/**
 * Defines [Border] for all TV [Interaction] states of Button.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ButtonBorder internal constructor(
    internal val border: Border,
    internal val focusedBorder: Border,
    internal val pressedBorder: Border,
    internal val disabledBorder: Border,
    internal val focusedDisabledBorder: Border
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ButtonBorder

        if (border != other.border) return false
        if (focusedBorder != other.focusedBorder) return false
        if (pressedBorder != other.pressedBorder) return false
        if (disabledBorder != other.disabledBorder) return false
        if (focusedDisabledBorder != other.focusedDisabledBorder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = border.hashCode()
        result = 31 * result + focusedBorder.hashCode()
        result = 31 * result + pressedBorder.hashCode()
        result = 31 * result + disabledBorder.hashCode()
        result = 31 * result + focusedDisabledBorder.hashCode()

        return result
    }

    override fun toString(): String {
        return "ButtonBorder(border=$border, focusedBorder=$focusedBorder," +
            "pressedBorder=$pressedBorder, disabledBorder=$disabledBorder, " +
            "focusedDisabledBorder=$focusedDisabledBorder)"
    }
}

/**
 * Defines [Glow] for all TV [Interaction] states of Button.
 */
@ExperimentalTvMaterial3Api
@Immutable
class ButtonGlow internal constructor(
    internal val glow: Glow,
    internal val focusedGlow: Glow,
    internal val pressedGlow: Glow
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ButtonGlow

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
        return "ButtonGlow(glow=$glow, focusedGlow=$focusedGlow, pressedGlow=$pressedGlow)"
    }
}

private val WideButtonContainerColor = Color.Transparent

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun ButtonShape.toClickableSurfaceShape(): ClickableSurfaceShape = ClickableSurfaceShape(
    shape = shape,
    focusedShape = focusedShape,
    pressedShape = pressedShape,
    disabledShape = disabledShape,
    focusedDisabledShape = focusedDisabledShape
)

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun ButtonColors.toClickableSurfaceColors(): ClickableSurfaceColors =
    ClickableSurfaceColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun WideButtonContentColor.toClickableSurfaceColors(): ClickableSurfaceColors =
    ClickableSurfaceColors(
        containerColor = WideButtonContainerColor,
        contentColor = contentColor,
        focusedContainerColor = WideButtonContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = WideButtonContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = WideButtonContainerColor,
        disabledContentColor = disabledContentColor
    )

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun ButtonScale.toClickableSurfaceScale() = ClickableSurfaceScale(
    scale = scale,
    focusedScale = focusedScale,
    pressedScale = pressedScale,
    disabledScale = disabledScale,
    focusedDisabledScale = focusedDisabledScale
)

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun ButtonBorder.toClickableSurfaceBorder() = ClickableSurfaceBorder(
    border = border,
    focusedBorder = focusedBorder,
    pressedBorder = pressedBorder,
    disabledBorder = disabledBorder,
    focusedDisabledBorder = focusedDisabledBorder
)

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun ButtonGlow.toClickableSurfaceGlow() = ClickableSurfaceGlow(
    glow = glow,
    focusedGlow = focusedGlow,
    pressedGlow = pressedGlow
)
