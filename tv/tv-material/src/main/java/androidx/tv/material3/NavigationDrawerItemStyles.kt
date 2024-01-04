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
import androidx.compose.foundation.Indication
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Defines [Shape] for all TV [Indication] states of a [NavigationDrawerItem]
 *
 * @constructor create an instance with arbitrary shape. See [NavigationDrawerItemDefaults.shape]
 * for the default shape used in a [NavigationDrawerItem]
 *
 * @param shape the default shape used when the [NavigationDrawerItem] is enabled
 * @param focusedShape the shape used when the [NavigationDrawerItem] is enabled and focused
 * @param pressedShape the shape used when the [NavigationDrawerItem] is enabled and pressed
 * @param selectedShape the shape used when the [NavigationDrawerItem] is enabled and selected
 * @param disabledShape the shape used when the [NavigationDrawerItem] is not enabled
 * @param focusedSelectedShape the shape used when the [NavigationDrawerItem] is enabled,
 * focused and selected
 * @param focusedDisabledShape the shape used when the [NavigationDrawerItem] is not enabled
 * and focused
 * @param pressedSelectedShape the shape used when the [NavigationDrawerItem] is enabled,
 * pressed and selected
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Immutable
class NavigationDrawerItemShape(
    val shape: Shape,
    val focusedShape: Shape,
    val pressedShape: Shape,
    val selectedShape: Shape,
    val disabledShape: Shape,
    val focusedSelectedShape: Shape,
    val focusedDisabledShape: Shape,
    val pressedSelectedShape: Shape
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationDrawerItemShape

        if (shape != other.shape) return false
        if (focusedShape != other.focusedShape) return false
        if (pressedShape != other.pressedShape) return false
        if (selectedShape != other.selectedShape) return false
        if (disabledShape != other.disabledShape) return false
        if (focusedSelectedShape != other.focusedSelectedShape) return false
        if (focusedDisabledShape != other.focusedDisabledShape) return false
        return pressedSelectedShape == other.pressedSelectedShape
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + focusedShape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + selectedShape.hashCode()
        result = 31 * result + disabledShape.hashCode()
        result = 31 * result + focusedSelectedShape.hashCode()
        result = 31 * result + focusedDisabledShape.hashCode()
        result = 31 * result + pressedSelectedShape.hashCode()

        return result
    }

    override fun toString(): String {
        return "NavigationDrawerItemShape(shape=$shape, " +
            "focusedShape=$focusedShape," +
            "pressedShape=$pressedShape, " +
            "selectedShape=$selectedShape, " +
            "disabledShape=$disabledShape, " +
            "focusedSelectedShape=$focusedSelectedShape, " +
            "focusedDisabledShape=$focusedDisabledShape, " +
            "pressedSelectedShape=$pressedSelectedShape)"
    }
}

/**
 * Defines container & content color [Color] for all TV [Indication] states of a
 * [NavigationDrawerItem]
 *
 * @constructor create an instance with arbitrary colors. See [NavigationDrawerItemDefaults.colors]
 * for the default colors used in a [NavigationDrawerItem]
 *
 * @param containerColor the default container color used when the [NavigationDrawerItem] is
 * enabled
 * @param contentColor the default content color used when the [NavigationDrawerItem] is enabled
 * @param inactiveContentColor the content color used when none of the navigation items have
 * focus
 * @param focusedContainerColor the container color used when the [NavigationDrawerItem] is
 * enabled and focused
 * @param focusedContentColor the content color used when the [NavigationDrawerItem] is enabled
 * and focused
 * @param pressedContainerColor the container color used when the [NavigationDrawerItem] is
 * enabled and pressed
 * @param pressedContentColor the content color used when the [NavigationDrawerItem] is enabled
 * and pressed
 * @param selectedContainerColor the container color used when the [NavigationDrawerItem] is
 * enabled and selected
 * @param selectedContentColor the content color used when the [NavigationDrawerItem] is
 * enabled and selected
 * @param disabledContainerColor the container color used when the [NavigationDrawerItem] is
 * not enabled
 * @param disabledContentColor the content color used when the [NavigationDrawerItem] is not
 * enabled
 * @param disabledInactiveContentColor the content color used when none of the navigation items
 * have focus and this item is disabled
 * @param focusedSelectedContainerColor the container color used when the
 * [NavigationDrawerItem] is enabled, focused and selected
 * @param focusedSelectedContentColor the content color used when the [NavigationDrawerItem]
 * is enabled, focused and selected
 * @param pressedSelectedContainerColor the container color used when the
 * [NavigationDrawerItem] is enabled, pressed and selected
 * @param pressedSelectedContentColor the content color used when the [NavigationDrawerItem] is
 * enabled, pressed and selected
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Immutable
class NavigationDrawerItemColors(
    val containerColor: Color,
    val contentColor: Color,
    val inactiveContentColor: Color,
    val focusedContainerColor: Color,
    val focusedContentColor: Color,
    val pressedContainerColor: Color,
    val pressedContentColor: Color,
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val disabledInactiveContentColor: Color,
    val focusedSelectedContainerColor: Color,
    val focusedSelectedContentColor: Color,
    val pressedSelectedContainerColor: Color,
    val pressedSelectedContentColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationDrawerItemColors

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (inactiveContentColor != other.inactiveContentColor) return false
        if (focusedContainerColor != other.focusedContainerColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (pressedContainerColor != other.pressedContainerColor) return false
        if (pressedContentColor != other.pressedContentColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledInactiveContentColor != other.disabledInactiveContentColor) return false
        if (focusedSelectedContainerColor != other.focusedSelectedContainerColor) return false
        if (focusedSelectedContentColor != other.focusedSelectedContentColor) return false
        if (pressedSelectedContainerColor != other.pressedSelectedContainerColor) return false
        return pressedSelectedContentColor == other.pressedSelectedContentColor
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + inactiveContentColor.hashCode()
        result = 31 * result + focusedContainerColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + pressedContainerColor.hashCode()
        result = 31 * result + pressedContentColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledInactiveContentColor.hashCode()
        result = 31 * result + focusedSelectedContainerColor.hashCode()
        result = 31 * result + focusedSelectedContentColor.hashCode()
        result = 31 * result + pressedSelectedContainerColor.hashCode()
        result = 31 * result + pressedSelectedContentColor.hashCode()
        return result
    }

    override fun toString(): String {
        return "NavigationDrawerItemColors(containerColor=$containerColor, " +
            "contentColor=$contentColor, " +
            "focusedContainerColor=$focusedContainerColor, " +
            "focusedContentColor=$focusedContentColor, " +
            "pressedContainerColor=$pressedContainerColor, " +
            "pressedContentColor=$pressedContentColor, " +
            "selectedContainerColor=$selectedContainerColor, " +
            "selectedContentColor=$selectedContentColor, " +
            "disabledContainerColor=$disabledContainerColor, " +
            "disabledContentColor=$disabledContentColor, " +
            "focusedSelectedContainerColor=$focusedSelectedContainerColor, " +
            "focusedSelectedContentColor=$focusedSelectedContentColor, " +
            "pressedSelectedContainerColor=$pressedSelectedContainerColor, " +
            "pressedSelectedContentColor=$pressedSelectedContentColor)"
    }
}

/**
 * Defines the scale for all TV [Indication] states of a [NavigationDrawerItem]
 *
 * @constructor create an instance with arbitrary scale. See [NavigationDrawerItemDefaults.scale]
 * for the default scale used in a [NavigationDrawerItem]
 *
 * @param scale the scale used when the [NavigationDrawerItem] is enabled
 * @param focusedScale the scale used when the [NavigationDrawerItem] is enabled and focused
 * @param pressedScale the scale used when the [NavigationDrawerItem] is enabled and pressed
 * @param selectedScale the scale used when the [NavigationDrawerItem] is enabled and selected
 * @param disabledScale the scale used when the [NavigationDrawerItem] is not enabled
 * @param focusedSelectedScale the scale used when the [NavigationDrawerItem] is enabled,
 * focused and selected
 * @param focusedDisabledScale the scale used when the [NavigationDrawerItem] is not enabled and
 * focused
 * @param pressedSelectedScale the scale used when the [NavigationDrawerItem] is enabled,
 * pressed and selected
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Immutable
class NavigationDrawerItemScale(
    @FloatRange(from = 0.0) val scale: Float,
    @FloatRange(from = 0.0) val focusedScale: Float,
    @FloatRange(from = 0.0) val pressedScale: Float,
    @FloatRange(from = 0.0) val selectedScale: Float,
    @FloatRange(from = 0.0) val disabledScale: Float,
    @FloatRange(from = 0.0) val focusedSelectedScale: Float,
    @FloatRange(from = 0.0) val focusedDisabledScale: Float,
    @FloatRange(from = 0.0) val pressedSelectedScale: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationDrawerItemScale

        if (scale != other.scale) return false
        if (focusedScale != other.focusedScale) return false
        if (pressedScale != other.pressedScale) return false
        if (selectedScale != other.selectedScale) return false
        if (disabledScale != other.disabledScale) return false
        if (focusedSelectedScale != other.focusedSelectedScale) return false
        if (focusedDisabledScale != other.focusedDisabledScale) return false
        return pressedSelectedScale == other.pressedSelectedScale
    }

    override fun hashCode(): Int {
        var result = scale.hashCode()
        result = 31 * result + focusedScale.hashCode()
        result = 31 * result + pressedScale.hashCode()
        result = 31 * result + selectedScale.hashCode()
        result = 31 * result + disabledScale.hashCode()
        result = 31 * result + focusedSelectedScale.hashCode()
        result = 31 * result + focusedDisabledScale.hashCode()
        result = 31 * result + pressedSelectedScale.hashCode()

        return result
    }

    override fun toString(): String {
        return "NavigationDrawerItemScale(scale=$scale, " +
            "focusedScale=$focusedScale, " +
            "pressedScale=$pressedScale, " +
            "selectedScale=$selectedScale, " +
            "disabledScale=$disabledScale, " +
            "focusedSelectedScale=$focusedSelectedScale, " +
            "focusedDisabledScale=$focusedDisabledScale, " +
            "pressedSelectedScale=$pressedSelectedScale)"
    }

    companion object {
        /**
         * Signifies the absence of a [ScaleIndication] in [NavigationDrawerItem]
         */
        val None = NavigationDrawerItemScale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f,
            selectedScale = 1f,
            disabledScale = 1f,
            focusedSelectedScale = 1f,
            focusedDisabledScale = 1f,
            pressedSelectedScale = 1f
        )
    }
}

/**
 * Defines [Border] for all TV [Indication] states of a [NavigationDrawerItem]
 *
 * @constructor create an instance with arbitrary border. See [NavigationDrawerItemDefaults.border]
 * for the default border used in a [NavigationDrawerItem]
 *
 * @param border the default [Border] used when the [NavigationDrawerItem] is enabled
 * @param focusedBorder the [Border] used when the [NavigationDrawerItem] is enabled and focused
 * @param pressedBorder the [Border] used when the [NavigationDrawerItem] is enabled and pressed
 * @param selectedBorder the [Border] used when the [NavigationDrawerItem] is enabled and
 * selected
 * @param disabledBorder the [Border] used when the [NavigationDrawerItem] is not enabled
 * @param focusedSelectedBorder the [Border] used when the [NavigationDrawerItem] is enabled,
 * focused and selected
 * @param focusedDisabledBorder the [Border] used when the [NavigationDrawerItem] is not
 * enabled and focused
 * @param pressedSelectedBorder the [Border] used when the [NavigationDrawerItem] is enabled,
 * pressed and selected
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Immutable
class NavigationDrawerItemBorder(
    val border: Border,
    val focusedBorder: Border,
    val pressedBorder: Border,
    val selectedBorder: Border,
    val disabledBorder: Border,
    val focusedSelectedBorder: Border,
    val focusedDisabledBorder: Border,
    val pressedSelectedBorder: Border
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationDrawerItemBorder

        if (border != other.border) return false
        if (focusedBorder != other.focusedBorder) return false
        if (pressedBorder != other.pressedBorder) return false
        if (selectedBorder != other.selectedBorder) return false
        if (disabledBorder != other.disabledBorder) return false
        if (focusedSelectedBorder != other.focusedSelectedBorder) return false
        if (focusedDisabledBorder != other.focusedDisabledBorder) return false
        return pressedSelectedBorder == other.pressedSelectedBorder
    }

    override fun hashCode(): Int {
        var result = border.hashCode()
        result = 31 * result + focusedBorder.hashCode()
        result = 31 * result + pressedBorder.hashCode()
        result = 31 * result + selectedBorder.hashCode()
        result = 31 * result + disabledBorder.hashCode()
        result = 31 * result + focusedSelectedBorder.hashCode()
        result = 31 * result + focusedDisabledBorder.hashCode()
        result = 31 * result + pressedSelectedBorder.hashCode()

        return result
    }

    override fun toString(): String {
        return "NavigationDrawerItemBorder(border=$border, " +
            "focusedBorder=$focusedBorder, " +
            "pressedBorder=$pressedBorder, " +
            "selectedBorder=$selectedBorder, " +
            "disabledBorder=$disabledBorder, " +
            "focusedSelectedBorder=$focusedSelectedBorder, " +
            "focusedDisabledBorder=$focusedDisabledBorder, " +
            "pressedSelectedBorder=$pressedSelectedBorder)"
    }
}

/**
 * Defines [Glow] for all TV [Indication] states of a [NavigationDrawerItem]
 *
 * @constructor create an instance with arbitrary glow. See [NavigationDrawerItemDefaults.glow]
 * for the default glow used in a [NavigationDrawerItem]
 *
 * @param glow the [Glow] used when the [NavigationDrawerItem] is enabled
 * @param focusedGlow the [Glow] used when the [NavigationDrawerItem] is enabled and focused
 * @param pressedGlow the [Glow] used when the [NavigationDrawerItem] is enabled and pressed
 * @param selectedGlow the [Glow] used when the [NavigationDrawerItem] is enabled and selected
 * @param focusedSelectedGlow the [Glow] used when the [NavigationDrawerItem] is enabled,
 * focused and selected
 * @param pressedSelectedGlow the [Glow] used when the [NavigationDrawerItem] is enabled,
 * pressed and selected
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Immutable
class NavigationDrawerItemGlow(
    val glow: Glow,
    val focusedGlow: Glow,
    val pressedGlow: Glow,
    val selectedGlow: Glow,
    val focusedSelectedGlow: Glow,
    val pressedSelectedGlow: Glow
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationDrawerItemGlow

        if (glow != other.glow) return false
        if (focusedGlow != other.focusedGlow) return false
        if (pressedGlow != other.pressedGlow) return false
        if (selectedGlow != other.selectedGlow) return false
        if (focusedSelectedGlow != other.focusedSelectedGlow) return false
        return pressedSelectedGlow == other.pressedSelectedGlow
    }

    override fun hashCode(): Int {
        var result = glow.hashCode()
        result = 31 * result + focusedGlow.hashCode()
        result = 31 * result + pressedGlow.hashCode()
        result = 31 * result + selectedGlow.hashCode()
        result = 31 * result + focusedSelectedGlow.hashCode()
        result = 31 * result + pressedSelectedGlow.hashCode()

        return result
    }

    override fun toString(): String {
        return "NavigationDrawerItemGlow(glow=$glow, " +
            "focusedGlow=$focusedGlow, " +
            "pressedGlow=$pressedGlow, " +
            "selectedGlow=$selectedGlow, " +
            "focusedSelectedGlow=$focusedSelectedGlow, " +
            "pressedSelectedGlow=$pressedSelectedGlow)"
    }
}
