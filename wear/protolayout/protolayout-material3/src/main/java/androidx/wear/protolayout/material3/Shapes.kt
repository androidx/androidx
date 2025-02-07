/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.material3

import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.material3.tokens.ShapeTokens

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container, offering a range of curved shapes. The default
 * [Shapes] theme for Material3 is rounded rectangles, with various degrees of corner roundness:
 * - None
 * - Extra Small
 * - Small
 * - Medium
 * - Large
 * - Extra Large
 * - Full
 *
 * You can customize the shape of any component by overriding the shape parameter for that
 * component. For example, by default, buttons use the shape style "large". If your product requires
 * a smaller amount of roundness, you can override the shape parameter with a different shape value
 * like [Shapes.small].
 */
public class Shapes
private constructor(
    /** No corner radius, i.e. square shape. */
    public val none: Corner,

    /** The extra small, mostly square corner with `4dp` corner radius. */
    public val extraSmall: Corner,

    /** The small, almost square corner with `8dp` corner radius. */
    public val small: Corner,

    /** The medium corner with `18dp` corner radius. */
    public val medium: Corner,

    /** The large, mostly round corner with `26dp` corner radius. */
    public val large: Corner,

    /** The extra large, almost round corner with `36dp` corner radius. */
    public val extraLarge: Corner,

    /** Full corner radius, i.e. round shape. */
    public val full: Corner
) {
    /** Default Shape theme. */
    public constructor() :
        this(
            none = ShapeTokens.CORNER_NONE,
            small = ShapeTokens.CORNER_SMALL,
            extraSmall = ShapeTokens.CORNER_EXTRA_SMALL,
            medium = ShapeTokens.CORNER_MEDIUM,
            large = ShapeTokens.CORNER_LARGE,
            extraLarge = ShapeTokens.CORNER_EXTRA_LARGE,
            full = ShapeTokens.CORNER_FULL,
        )
}
