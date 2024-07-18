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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Represents the [Color] of Card in different interaction states.
 */
@ExperimentalTvMaterial3Api
@Immutable
class CardColors internal constructor(
    internal val containerColor: Color,
    internal val contentColor: Color,
    internal val focusedContainerColor: Color,
    internal val focusedContentColor: Color,
    internal val pressedContainerColor: Color,
    internal val pressedContentColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CardColors

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (focusedContainerColor != other.focusedContainerColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (pressedContainerColor != other.pressedContainerColor) return false
        if (pressedContentColor != other.pressedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + focusedContainerColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + pressedContainerColor.hashCode()
        result = 31 * result + pressedContentColor.hashCode()

        return result
    }

    override fun toString(): String {
        return "CardColors(" +
            "containerColor=$containerColor, " +
            "contentColor=$contentColor, " +
            "focusedContainerColor=$focusedContainerColor, " +
            "focusedContentColor=$focusedContentColor, " +
            "pressedContainerColor=$pressedContainerColor, " +
            "pressedContentColor=$pressedContentColor)"
    }
}

/**
 * Represents the [Shape] of Card in different interaction states.
 */
@ExperimentalTvMaterial3Api
@Immutable
class CardShape internal constructor(
    internal val shape: Shape,
    internal val focusedShape: Shape,
    internal val pressedShape: Shape
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CardShape

        if (shape != other.shape) return false
        if (focusedShape != other.focusedShape) return false
        if (pressedShape != other.pressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + focusedShape.hashCode()
        result = 31 * result + pressedShape.hashCode()

        return result
    }

    override fun toString(): String {
        return "CardShape(shape=$shape, focusedShape=$focusedShape, pressedShape=$pressedShape)"
    }
}

/**
 * Represents the scaleFactor of Card in different interaction states.
 * Note: This scaleFactor must always be a non-negative float.
 */
@ExperimentalTvMaterial3Api
@Immutable
class CardScale internal constructor(
    @FloatRange(from = 0.0) internal val scale: Float,
    @FloatRange(from = 0.0) internal val focusedScale: Float,
    @FloatRange(from = 0.0) internal val pressedScale: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CardScale

        if (scale != other.scale) return false
        if (focusedScale != other.focusedScale) return false
        if (pressedScale != other.pressedScale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scale.hashCode()
        result = 31 * result + focusedScale.hashCode()
        result = 31 * result + pressedScale.hashCode()

        return result
    }

    override fun toString(): String {
        return "CardScale(scale=$scale, focusedScale=$focusedScale, pressedScale=$pressedScale)"
    }

    companion object {
        /**
         * Signifies the absence of a [ScaleIndication] in Card component.
         */
        val None = CardScale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f
        )
    }
}

/**
 * Represents the [Border] of Card in different interaction states.
 */
@ExperimentalTvMaterial3Api
@Immutable
class CardBorder internal constructor(
    internal val border: Border,
    internal val focusedBorder: Border,
    internal val pressedBorder: Border
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CardBorder

        if (border != other.border) return false
        if (focusedBorder != other.focusedBorder) return false
        if (pressedBorder != other.pressedBorder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = border.hashCode()
        result = 31 * result + focusedBorder.hashCode()
        result = 31 * result + pressedBorder.hashCode()

        return result
    }

    override fun toString(): String {
        return "CardBorder(border=$border, focusedBorder=$focusedBorder, " +
            "pressedBorder=$pressedBorder)"
    }
}

/**
 * Represents the [Glow] of Card in different interaction states.
 */
@ExperimentalTvMaterial3Api
@Immutable
class CardGlow internal constructor(
    internal val glow: Glow,
    internal val focusedGlow: Glow,
    internal val pressedGlow: Glow
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CardGlow

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
        return "CardGlow(glow=$glow, focusedGlow=$focusedGlow, pressedGlow=$pressedGlow)"
    }
}
