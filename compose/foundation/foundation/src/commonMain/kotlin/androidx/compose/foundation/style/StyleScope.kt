/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit

/**
 * A [StyleScope] is the receiver scope of a [Style] lambda. It allows defining the properties of a
 * style that will be used by a [styleable] modifier to allow customizing the look of a composable
 * component in its default state and in states like hover, pressed, or focused readable from the
 * [state] property.
 *
 * The properties set on a style scope affect the styled region of a component. A component creates
 * a styled region by using the [styleable] modifier.
 *
 * @see Style
 */
@ExperimentalFoundationStyleApi
sealed interface StyleScope : CompositionLocalAccessorScope, Density {
    /**
     * The state of the component. applying this style. For example, if a component is pressed the
     * [StyleState.isPressed] will be `true`.
     *
     * Custom states can be read from the state using the [StyleStateKey] for the state.
     */
    val state: StyleState

    /**
     * Sets the padding for the start edge of the component's content. Content padding is the space
     * between the component's border (if any) and its content. The width/height of the component
     * includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the start edge.
     * @see contentPaddingEnd
     * @see contentPaddingTop
     * @see contentPaddingBottom
     * @see contentPaddingHorizontal
     * @see contentPaddingVertical
     * @see contentPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPaddingStart(value: Dp)

    /**
     * Sets the padding for the end edge of the component's content. Content padding is the space
     * between the component's border (if any) and its content. The width/height of the component
     * includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the end edge.
     * @see contentPaddingStart
     * @see contentPaddingTop
     * @see contentPaddingBottom
     * @see contentPaddingHorizontal
     * @see contentPaddingVertical
     * @see contentPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPaddingEnd(value: Dp)

    /**
     * Sets the padding for the top edge of the component's content. Content padding is the space
     * between the component's border (if any) and its content. The width/height of the component
     * includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the top edge.
     * @see contentPaddingStart
     * @see contentPaddingEnd
     * @see contentPaddingBottom
     * @see contentPaddingHorizontal
     * @see contentPaddingVertical
     * @see contentPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPaddingTop(value: Dp)

    /**
     * Sets the padding for the bottom edge of the component's content. Content padding is the space
     * between the component's border (if any) and its content. The width/height of the component
     * includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the bottom edge.
     * @see contentPaddingStart
     * @see contentPaddingEnd
     * @see contentPaddingTop
     * @see contentPaddingHorizontal
     * @see contentPaddingVertical
     * @see contentPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPaddingBottom(value: Dp)

    /**
     * Sets the padding for the horizontal (start and end) edges of the component's content. Content
     * padding is the space between the component's border (if any) and its content. The
     * width/height of the component includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to both start and end edges.
     * @see contentPaddingStart
     * @see contentPaddingEnd
     * @see contentPaddingVertical
     * @see contentPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPaddingHorizontal(value: Dp)

    /**
     * Sets the padding for the vertical (start and end) edges of the component's content. Content
     * padding is the space between the component's border (if any) and its content. The
     * width/height of the component includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to both top and bottom edges.
     * @see contentPaddingTop
     * @see contentPaddingBottom
     * @see contentPaddingHorizontal
     * @see contentPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPaddingVertical(value: Dp)

    /**
     * Sets the padding for all four edges (top, end, bottom, start) edges of the component's
     * content. Content padding is the space between the component's border (if any) and its
     * content. The width/height of the component includes content padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to all four edges.
     * @see contentPaddingStart
     * @see contentPaddingEnd
     * @see contentPaddingTop
     * @see contentPaddingBottom
     * @see contentPaddingHorizontal
     * @see contentPaddingVertical
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPadding(value: Dp)

    /**
     * Sets the padding for all four edges (top, end, bottom, start) edges of the component's
     * content. Content padding is the space between the component's border (if any) and its
     * content. The width/height of the component includes content padding.
     *
     * This property is *not* inherited
     *
     * @param start The padding for the start edge.
     * @param top The padding for the top edge.
     * @param end The padding for the end edge.
     * @param bottom The padding for the bottom edge.
     * @see contentPaddingStart
     * @see contentPaddingEnd
     * @see contentPaddingTop
     * @see contentPaddingBottom
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPadding(start: Dp, top: Dp, end: Dp, bottom: Dp)

    /**
     * Sets the padding for the vertical (top and bottom) and horizontal (start and end) edges of
     * the component's content. Content padding is the space between the component's border (if any)
     * and its content. The width/height of the component includes content padding.
     *
     * This property is *not* inherited
     *
     * @param vertical The padding for the top and bottom edges.
     * @param horizontal The padding for the start and end edges.
     * @see contentPaddingVertical
     * @see contentPaddingHorizontal
     * @see androidx.compose.foundation.layout.padding
     */
    fun contentPadding(horizontal: Dp, vertical: Dp)

    /**
     * Sets the external padding for the start edge of the component. The external padding is the
     * space between the edge of the component and its border (if any). The width/height of the
     * component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the start edge.
     * @see externalPaddingEnd
     * @see externalPaddingTop
     * @see externalPaddingBottom
     * @see externalPaddingHorizontal
     * @see externalPaddingVertical
     * @see externalPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPaddingStart(value: Dp)

    /**
     * Sets the external padding for the end edge of the component. The external padding is the
     * space between the edge of the component and its border (if any). The width/height of the
     * component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the end edge.
     * @see externalPaddingStart
     * @see externalPaddingTop
     * @see externalPaddingBottom
     * @see externalPaddingHorizontal
     * @see externalPaddingVertical
     * @see externalPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPaddingEnd(value: Dp)

    /**
     * Sets the external padding for the top edge of the component. The external padding is the
     * space between the edge of the component and its border (if any). The width/height of the
     * component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the top edge.
     * @see externalPaddingStart
     * @see externalPaddingEnd
     * @see externalPaddingBottom
     * @see externalPaddingHorizontal
     * @see externalPaddingVertical
     * @see externalPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPaddingTop(value: Dp)

    /**
     * Sets the external padding for the bottom edge of the component. The external padding is the
     * space between the edge of the component and its border (if any). The width/height of the
     * component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to the bottom edge.
     * @see externalPaddingStart
     * @see externalPaddingEnd
     * @see externalPaddingTop
     * @see externalPaddingHorizontal
     * @see externalPaddingVertical
     * @see externalPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPaddingBottom(value: Dp)

    /**
     * Sets the external padding for the horizontal (start and end) edges of the component. The
     * external padding is the space between the edge of the component and its border (if any). The
     * width/height of the component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to both start and end edges.
     * @see externalPaddingStart
     * @see externalPaddingEnd
     * @see externalPaddingVertical
     * @see externalPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPaddingHorizontal(value: Dp)

    /**
     * Sets the external padding for the vertical (start and end) edges of the component. The
     * external padding is the space between the edge of the component and its border (if any). The
     * width/height of the component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to both top and bottom edges.
     * @see externalPaddingTop
     * @see externalPaddingBottom
     * @see externalPaddingHorizontal
     * @see externalPadding
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPaddingVertical(value: Dp)

    /**
     * Sets the external padding for all four edges (top, end, bottom, start) of the component. The
     * external padding is the space between the edge of the component and its border (if any). The
     * width/height of the component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param value The amount of padding to apply to all four edges.
     * @see externalPaddingStart
     * @see externalPaddingEnd
     * @see externalPaddingTop
     * @see externalPaddingBottom
     * @see externalPaddingHorizontal
     * @see externalPaddingVertical
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPadding(value: Dp)

    /**
     * Sets the external padding for all four edges (top, end, bottom, start) of the component. The
     * external padding is the space between the edge of the component and its border (if any). The
     * width/height of the component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param start The padding for the start edge.
     * @param top The padding for the top edge.
     * @param end The padding for the end edge.
     * @param bottom The padding for the bottom edge.
     * @see externalPaddingStart
     * @see externalPaddingEnd
     * @see externalPaddingTop
     * @see externalPaddingBottom
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPadding(start: Dp, top: Dp, end: Dp, bottom: Dp)

    /**
     * Sets the external padding for the vertical (top and bottom) and horizontal (start and end)
     * edges of the component. The external padding is the space between the edge of the component
     * and its border (if any). The width/height of the component includes external padding.
     *
     * This property is *not* inherited
     *
     * @param vertical The padding for the top and bottom edges.
     * @param horizontal The padding for the start and end edges.
     * @see externalPaddingVertical
     * @see externalPaddingHorizontal
     * @see androidx.compose.foundation.layout.padding
     */
    fun externalPadding(horizontal: Dp, vertical: Dp)

    /**
     * Sets the width of the border around the component. The border is drawn on top of the
     * background and the padded content. The border's width does not contribute to the component's
     * layout size (width/height); it is rendered within the component's bounds This method only
     * sets the width; color or brush must be set separately.
     *
     * This property is *not* inherited
     *
     * @param value The width of the border.
     * @see borderColor
     * @see borderBrush
     * @see border
     * @see shape
     * @see androidx.compose.foundation.border
     */
    fun borderWidth(value: Dp)

    /**
     * Sets the color of the border around the component. The border is drawn on top of the
     * background and the padded content. This method only sets the color; width must be set
     * separately. The border's presence and appearance do not affect the component's layout size.
     *
     * This property is *not* inherited
     *
     * @param value The color of the border.
     * @see borderWidth
     * @see borderBrush
     * @see shape
     * @see androidx.compose.foundation.border
     */
    fun borderColor(value: Color)

    /**
     * Sets the brush used to paint the border around the component. The border is drawn on top of
     * the background and the padded content. This method only sets the brush; width must be set
     * separately. The border's presence and appearance do not affect the component's layout size.
     *
     * This property is *not* inherited
     *
     * @param value The brush to paint the border (e.g., for gradients).
     * @see borderWidth
     * @see borderColor
     * @see border(Dp, Brush)
     * @see androidx.compose.foundation.border
     */
    fun borderBrush(value: Brush)

    /**
     * Sets a solid border for the component with the specified width and color. The border is drawn
     * on top of the background and the padded content. The border itself does not contribute to the
     * component's layout size (width/height).
     *
     * This property is *not* inherited
     *
     * @param width The width of the border.
     * @param color The color of the border.
     * @see borderWidth
     * @see borderColor
     * @see borderBrush
     * @see androidx.compose.foundation.border
     */
    fun border(width: Dp, color: Color)

    /**
     * Sets a border for the component with the specified width and brush. The border is drawn on
     * top of the background and the padded content. The border itself does not contribute to the
     * component's layout size (width/height).
     *
     * This property is *not* inherited
     *
     * @param width The width of the border.
     * @param brush The brush to paint the border (e.g., for gradients).
     * @see borderWidth
     * @see borderColor
     * @see borderBrush
     * @see androidx.compose.foundation.border
     */
    fun border(width: Dp, brush: Brush)

    /**
     * Sets the preferred width of the component. The actual size will also depend on the parent's
     * constraints and other modifiers. The specified width includes both [contentPadding] and
     * [externalPadding].
     *
     * This property is *not* inherited
     *
     * @param value The preferred width in Dp.
     * @see height
     * @see size
     * @see width(Float)
     * @see fillWidth
     * @see minWidth
     * @see maxWidth
     * @see androidx.compose.foundation.layout.width
     */
    fun width(value: Dp)

    /**
     * Sets the preferred height of the component. The actual size will also depend on the parent's
     * constraints and other modifiers. The specified height includes both [contentPadding] and
     * [externalPadding].
     *
     * This property is *not* inherited
     *
     * @param value The preferred height in Dp.
     * @see width
     * @see size
     * @see fillHeight
     * @see minHeight
     * @see maxHeight
     * @see androidx.compose.foundation.layout.height
     */
    fun height(value: Dp)

    /**
     * Sets the preferred width and height of the component. The actual size will also depend on the
     * parent's constraints and other modifiers. The specified dimensions includes both
     * [contentPadding] and [externalPadding].
     *
     * This property is *not* inherited
     *
     * @param width The preferred width in Dp.
     * @param height The preferred height in Dp.
     * @see androidx.compose.foundation.layout.size
     */
    fun size(width: Dp, height: Dp)

    /**
     * Sets the preferred width and height of the component to the same value. The actual size will
     * also depend on the parent's constraints and other modifiers. The specified size includes any
     * padding.
     *
     * This property is *not* inherited
     *
     * @param value The preferred width and height in Dp.
     * @see androidx.compose.foundation.layout.size
     */
    fun size(value: Dp)

    /**
     * Sets the preferred width and height of the component using a [DpSize] object. The actual size
     * will also depend on the parent's constraints and other modifiers. The specified size includes
     * any padding.
     *
     * This property is *not* inherited
     *
     * @param value The preferred size.
     * @see androidx.compose.foundation.layout.size
     */
    fun size(value: DpSize)

    /**
     * Sets the width of the component to a fraction of the parent's available width. The specified
     * width includes any padding.
     *
     * This property is *not* inherited
     *
     * @param fraction The fraction of the available width (e.g., 0.5f for 50%).
     * @see fillWidth
     * @see androidx.compose.foundation.layout.fillMaxWidth
     */
    fun width(@FloatRange(from = 0.0, to = 1.0) fraction: Float)

    /**
     * Sets the height of the component to a fraction of the parent's available height. The
     * specified height includes any padding.
     *
     * This property is *not* inherited
     *
     * @param fraction The fraction of the available height (e.g., 0.5f for 50%).
     * @see fillHeight
     * @see androidx.compose.foundation.layout.fillMaxHeight
     */
    fun height(@FloatRange(from = 0.0, to = 1.0) fraction: Float)

    /**
     * Offsets the component horizontally from its original calculated left position. Positive
     * values shift the component to the right, negative to the left.
     *
     * This property is *not* inherited
     *
     * @param value The amount to offset from the left edge.
     * @see top
     * @see right
     * @see bottom
     * @see androidx.compose.foundation.layout.offset
     */
    fun left(value: Dp)

    /**
     * Offsets the component vertically from its original calculated top position. Positive values
     * shift the component downwards, negative upwards.
     *
     * This property is *not* inherited
     *
     * @param value The amount to offset from the top edge.
     * @see left
     * @see right
     * @see bottom
     * @see androidx.compose.foundation.layout.offset
     */
    fun top(value: Dp)

    /**
     * Offsets the component horizontally from its original calculated right position. Positive
     * values shift the component to the left (further from the right edge), negative to the right.
     *
     * This property is *not* inherited
     *
     * @param value The amount to offset from the right edge.
     * @see left
     * @see top
     * @see bottom
     * @see androidx.compose.foundation.layout.offset
     */
    fun right(value: Dp)

    /**
     * Offsets the component vertically from its original calculated bottom position. Positive
     * values shift the component upwards (further from the bottom edge), negative downwards.
     *
     * This property is *not* inherited
     *
     * @param value The amount to offset from the bottom edge.
     * @see left
     * @see top
     * @see right
     * @see androidx.compose.foundation.layout.offset
     */
    fun bottom(value: Dp)

    /**
     * Constrains the minimum width of the component. The component's width, including padding, will
     * be at least this value.
     *
     * This property is *not* inherited
     *
     * @param value The minimum width.
     * @see minHeight
     * @see minSize
     * @see maxWidth
     * @see androidx.compose.foundation.layout.widthIn
     */
    fun minWidth(value: Dp)

    /**
     * Constrains the minimum height of the component. The component's height, including padding,
     * will be at least this value.
     *
     * This property is *not* inherited
     *
     * @param value The minimum height.
     * @see minWidth
     * @see minSize
     * @see maxHeight
     * @see androidx.compose.foundation.layout.heightIn
     */
    fun minHeight(value: Dp)

    /**
     * Constrains the minimum size (width and height) of the component. The component's dimensions,
     * including padding, will be at least these values.
     *
     * This property is *not* inherited
     *
     * @param size The minimum size ([DpSize]).
     * @see minWidth
     * @see minHeight
     * @see androidx.compose.foundation.layout.sizeIn
     */
    fun minSize(size: DpSize)

    /**
     * Constrains the minimum width and height of the component. The component's dimensions,
     * including padding, will be at least these values.
     *
     * This property is *not* inherited
     *
     * @param width The minimum width.
     * @param height The minimum height.
     * @see minWidth
     * @see minHeight
     * @see androidx.compose.foundation.layout.sizeIn
     */
    fun minSize(width: Dp, height: Dp)

    /**
     * Constrains the maximum width of the component. The component's width, including padding, will
     * be at most this value.
     *
     * This property is *not* inherited
     *
     * @param value The maximum width.
     * @see maxHeight
     * @see maxSize
     * @see minWidth
     * @see androidx.compose.foundation.layout.widthIn
     */
    fun maxWidth(value: Dp)

    /**
     * Constrains the maximum height of the component. The component's height, including padding,
     * will be at most this value.
     *
     * This property is *not* inherited
     *
     * @param value The maximum height.
     * @see maxWidth
     * @see maxSize
     * @see minHeight
     * @see androidx.compose.foundation.layout.heightIn
     */
    fun maxHeight(value: Dp)

    /**
     * Constrains the maximum size (width and height) of the component. The component's dimensions,
     * including padding, will be at most these values.
     *
     * This property is *not* inherited
     *
     * @param size The maximum size ([DpSize]).
     * @see maxWidth
     * @see maxHeight
     * @see androidx.compose.foundation.layout.sizeIn
     */
    fun maxSize(size: DpSize)

    /**
     * Constrains the maximum width and height of the component. The component's dimensions,
     * including padding, will be at most these values.
     *
     * This property is *not* inherited
     *
     * @param width The maximum width.
     * @param height The maximum height.
     * @see maxWidth
     * @see maxHeight
     * @see androidx.compose.foundation.layout.sizeIn
     */
    fun maxSize(width: Dp, height: Dp)

    /**
     * Sets the opacity of the component. A value of 1.0f means fully opaque, 0.0f means fully
     * transparent.
     *
     * This property is *not* inherited
     *
     * @param value The alpha value (0.0f to 1.0f).
     * @see androidx.compose.ui.draw.alpha
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun alpha(@FloatRange(from = 0.0, to = 1.0) value: Float)

    /**
     * Scales the component horizontally around its center pivot point.
     *
     * Setting scaleX will override the horizontal scaling set by a previous call to [scale].
     *
     * This property is *not* inherited
     *
     * @param value The scaling factor for the X-axis. 1.0f is no scale.
     * @see scaleY
     * @see scale
     * @see androidx.compose.ui.draw.scale
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun scaleX(@FloatRange(from = 0.0) value: Float)

    /**
     * Scales the component vertically around its center pivot point.
     *
     * Setting scaleX will override the vertical scaling set by a previous call to [scale].
     *
     * This property is *not* inherited
     *
     * @param value The scaling factor for the Y-axis. 1.0f is no scale.
     * @see scaleX
     * @see scale
     * @see androidx.compose.ui.draw.scale
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun scaleY(@FloatRange(from = 0.0) value: Float)

    /**
     * Scales the component uniformly (both horizontally and vertically) around its center pivot
     * point.
     *
     * Setting [scale] will override the horizontal scaling set by a previous call to [scaleX] and
     * the previous vertical scaling set by calling [scaleY].
     *
     * This property is *not* inherited
     *
     * @param value The scaling factor for both X and Y axes. 1.0f is no scale.
     * @see scaleX
     * @see scaleY
     * @see androidx.compose.ui.draw.scale
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun scale(@FloatRange(from = 0.0) value: Float)

    /**
     * Translates (moves) the component horizontally. Positive values move it to the right, negative
     * values to the left.
     *
     * This property is *not* inherited
     *
     * @param value The translation amount on the X-axis in pixels.
     * @see translationY
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun translationX(@FloatRange(from = 0.0) value: Float)

    /**
     * Translates (moves) the component vertically. Positive values move it down, negative values
     * up.
     *
     * This property is *not* inherited
     *
     * @param value The translation amount on the Y-axis in pixels.
     * @see translationX
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun translationY(@FloatRange(from = 0.0) value: Float)

    /**
     * Translates (moves) the component horizontally and vertically.
     *
     * This property is *not* inherited
     *
     * @param x The translation amount on the X-axis in pixels.
     * @param y The translation amount on the Y-axis in pixels.
     * @see translationX
     * @see translationY
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun translation(@FloatRange(from = 0.0) x: Float, @FloatRange(from = 0.0) y: Float)

    /**
     * Translates (moves) the component by the given [Offset].
     *
     * This property is *not* inherited
     *
     * @param offset The translation offset in pixels.
     * @see translationX
     * @see translationY
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun translation(offset: Offset)

    /**
     * Rotates the component around the X-axis through its center.
     *
     * This property is *not* inherited
     *
     * @param value The rotation angle in degrees.
     * @see rotationY
     * @see rotationZ
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun rotationX(value: Float)

    /**
     * Rotates the component around the Y-axis through its center.
     *
     * This property is *not* inherited
     *
     * @param value The rotation angle in degrees.
     * @see rotationX
     * @see rotationZ
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun rotationY(value: Float)

    /**
     * Rotates the component around the Z-axis (perpendicular to the screen) through its center.
     *
     * This property is *not* inherited
     *
     * @param value The rotation angle in degrees.
     * @see rotationX
     * @see rotationY
     * @see androidx.compose.ui.draw.rotate
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun rotationZ(value: Float)

    /**
     * Offset percentage along the x and y axis for which contents are rotated and scaled. The
     * default value of 0.5f, 0.5f indicates the pivot point will be at the midpoint of the left and
     * right as well as the top and bottom bounds of the layer. Default value is
     * [TransformOrigin.Center].
     *
     * @param value The origin of the transform
     * @see [androidx.compose.ui.graphics.GraphicsLayerScope]
     */
    fun transformOrigin(value: TransformOrigin)

    /**
     * Clips the component to its bounds. If a [shape] is also applied, it clips to the shape. When
     * clip is `true` content that overflows the component's bounds is not drawn.
     *
     * This property is *not* inherited
     *
     * @param value `true` to clip (default), `false` to allow drawing outside bounds.
     * @see shape
     * @see androidx.compose.ui.draw.clip
     * @see androidx.compose.ui.draw.clipToBounds
     */
    fun clip(value: Boolean = true)

    /**
     * Sets the Z-index of the component. Higher Z-index components are drawn on top of lower
     * Z-index components within the same parent. This affects drawing order, not layout.
     *
     * This property is *not* inherited
     *
     * @param value The Z-index value.
     * @see androidx.compose.ui.zIndex
     */
    fun zIndex(@FloatRange(from = 0.0) value: Float)

    /**
     * Sets the background color of the component. If a [shape] is applied, the background will fill
     * that shape.
     *
     * Setting a background color will override any previously set background brush.
     *
     * This property is *not* inherited
     *
     * @param color The background color.
     * @see shape
     * @see androidx.compose.foundation.background
     */
    fun background(color: Color)

    /**
     * Sets the background of the component using a [Brush]. This allows for gradient backgrounds or
     * other complex fills. If a [shape] is applied, the background will fill that shape.
     *
     * Setting a background brush will override any previously set background color.
     *
     * This property is *not* inherited
     *
     * @param value The brush to use for the background.
     * @see shape
     * @see androidx.compose.foundation.background
     */
    fun background(value: Brush)

    /**
     * Sets the foreground color for the component. This can be used to overlay a color on top of
     * the component's content. It is important that this brush be partially transparent (e.g. alpha
     * less than 1.0) or it will obscure the content. If a [shape] is applied, the [foreground] will
     * fill that shape.
     *
     * This property is *not* inherited
     *
     * @param value The brush to use for the foreground.
     * @see contentColor
     * @see contentBrush
     */
    fun foreground(value: Color)

    /**
     * Sets the foreground brush for the component. This can be used to overlay a color or gradient
     * on top of the component's content. It is important that this brush be partially transparent
     * (e.g. alpha less than 1.0) or it will obscure the content. If a [shape] is applied, the
     * [foreground] will fill that shape.
     *
     * This property is *not* inherited
     *
     * @param value The brush to use for the foreground.
     * @see contentColor
     * @see contentBrush
     */
    fun foreground(value: Brush)

    /**
     * Sets the [Shape] for the component. This shape is used for clipping ([clip]), background
     * rendering ([background]), and border rendering.
     *
     * If [shape] is not specified then a [androidx.compose.ui.graphics.RectangleShape] is used.
     *
     * This property is *not* inherited
     *
     * @param value The shape to apply.
     * @see clip
     * @see background
     * @see border
     * @see androidx.compose.ui.draw.clip
     * @see androidx.compose.foundation.background
     * @see androidx.compose.foundation.border
     */
    fun shape(value: Shape)

    /**
     * Applies a drop shadow effect directly to the component, often used for text or specific
     * graphics. This is distinct from `shadowElevation` which is specific to platform elevation
     * shadows. Multiple drop shadows can be applied by calling this function multiple times or
     * using the vararg overload. The border and overall layout size are not affected by this
     * shadow.
     *
     * If [shape] is set, the shadow will be applied to the shape's bounds.
     *
     * This property is *not* inherited.
     *
     * @param value The [Shadow] properties (color, offset, blurRadius) for the drop shadow.
     * @see innerShadow
     * @see Shadow
     * @see androidx.compose.ui.draw.dropShadow
     */
    fun dropShadow(value: Shadow)

    /**
     * Applies one or more drop shadow effects directly to the component. This is distinct from
     * `shadowElevation`. The border and overall layout size are not affected by these shadows.
     *
     * This property is *not* inherited.
     *
     * @param value A vararg of [Shadow] properties to apply as drop shadows.
     * @see innerShadow
     * @see Shadow
     * @see androidx.compose.ui.draw.dropShadow
     */
    fun dropShadow(vararg value: Shadow)

    /**
     * Applies an inner shadow effect to the component. This shadow is drawn inside the bounds of
     * the component. Multiple inner shadows can be applied by calling this function multiple times
     * or using the vararg overload. The border and overall layout size are not affected by this
     * shadow.
     *
     * If [shape] is set, the shadow will be applied to the shape's bounds.
     *
     * This property is *not* inherited.
     *
     * @param value The [Shadow] properties (color, offset, blurRadius) for the inner shadow.
     * @see dropShadow
     * @see Shadow
     * @see androidx.compose.ui.draw.innerShadow
     */
    fun innerShadow(value: Shadow)

    /**
     * Applies one or more inner shadow effects to the component. These shadows are drawn inside the
     * bounds of the component. The border and overall layout size are not affected by these
     * shadows.
     *
     * This property is *not* inherited.
     *
     * @param value A vararg of [Shadow] properties to apply as inner shadows.
     * @see dropShadow
     * @see Shadow
     * @see androidx.compose.ui.draw.innerShadow
     */
    fun innerShadow(vararg value: Shadow)

    /**
     * Specifies a [Style] whose properties will be animated to when they change. This uses a
     * default animation specification. This allows for smooth transitions between style states
     * without manual animation setup. A [androidx.compose.animation.core.spring] will be used for
     * the animation for both animating to and from the style.
     *
     * @param value The target [Style] whose properties should be animated.
     * @see Style
     */
    fun animate(value: Style)

    /**
     * Specifies a [Style] whose properties will be animated to when they change, using the provided
     * [AnimationSpec]. This allows for smooth transitions between style states with custom
     * animation curves. The same animation [spec] will be used for animating both to and from the
     * style.
     *
     * @param spec The [AnimationSpec] to use for the animation.
     * @param value The target [Style] whose properties should be animated.
     * @see Style
     * @see androidx.compose.animation.core.AnimationSpec
     */
    fun animate(spec: AnimationSpec<Float>, value: Style)

    /**
     * Specifies a [Style] whose properties will be animated to when they change, using the provided
     * [AnimationSpec]. This allows for smooth transitions between style states with custom
     * animation curves.
     *
     * @param toSpec The [AnimationSpec] to use for the animation to the values set in [value]. This
     *   animation is used when the [animate] call is added to the style.
     * @param fromSpec The [AnimationSpec] to use for the animation from the values set in [value].
     *   This animation is used when the [animate] call is removed from the style.
     * @param value The target [Style] whose properties should be animated.
     * @see Style
     * @see androidx.compose.animation.core.AnimationSpec
     */
    fun animate(toSpec: AnimationSpec<Float>, fromSpec: AnimationSpec<Float>, value: Style)

    /**
     * Applies a complete [TextStyle] object to the component. This is a convenient way to set
     * multiple text-related properties at once. Text properties are inherited by child text
     * components unless overridden.
     *
     * This property is inherited.
     *
     * @param value The [TextStyle] to apply.
     * @see contentColor
     * @see fontFamily
     * @see fontSize
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textStyle(value: TextStyle)

    /**
     * Sets the preferred content color, primarily affecting text color. This property is inherited
     * by child text components if not overridden. This affects drawing only and is often a
     * component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The color for the content, typically text.
     * @see contentBrush
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun contentColor(value: Color)

    /**
     * Sets the preferred brush for rendering content, primarily affecting text. This allows for
     * gradient text or other brush-based text effects. This property is inherited by child text
     * components if not overridden. This affects drawing only and is often a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The brush for the content, typically text.
     * @see contentColor
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun contentBrush(value: Brush)

    /**
     * Sets the text decoration (e.g., underline, line-through). This property is inherited by child
     * text components if not overridden. This affects drawing only and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextDecoration] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textDecoration(value: TextDecoration) // int enum (mask?) 2 possible values

    /**
     * Sets the font family for the text. This property is inherited by child text components if not
     * overridden. This affects text layout and rendering, and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontFamily] to use.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontFamily(value: FontFamily) // reference class

    /**
     * Sets the text indent (e.g., for the first line or subsequent lines). This property is
     * inherited by child text components if not overridden. This affects text layout and is a
     * component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextIndent] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textIndent(value: TextIndent) // ref class of two longs

    /**
     * Sets the font size for the text. This property is inherited by child text components if not
     * overridden. This affects text layout and rendering, and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The font size in [TextUnit] (e.g., `16.sp`).
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontSize(value: TextUnit) // Long value class

    /**
     * Sets the line height for the text. This property is inherited by child text components if not
     * overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The line height in [TextUnit] (e.g., `20.sp`) or `TextUnit.Unspecified`.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun lineHeight(value: TextUnit) // long value class

    /**
     * Sets the letter spacing for the text. This property is inherited by child text components if
     * not overridden. This affects text layout and rendering, and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The letter spacing in [TextUnit] (e.g., `0.5.sp`).
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun letterSpacing(value: TextUnit) // long value class

    /**
     * Sets the baseline shift for the text (e.g., for superscript or subscript). This property is
     * inherited by child text components if not overridden. This affects text layout and rendering,
     * and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [BaselineShift] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun baselineShift(value: BaselineShift) // float value class

    /**
     * Sets the font weight for the text (e.g., bold, normal). This property is inherited by child
     * text components if not overridden. This affects text rendering and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontWeight] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontWeight(value: FontWeight) // Int enum, 9 values, 4 bits

    /**
     * Sets the font style for the text (e.g., italic, normal). This property is inherited by child
     * text components if not overridden. This affects text rendering and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontStyle] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontStyle(
        value: FontStyle
    ) // enum int value class, two possible values + unspecified, 2 bits

    /**
     * Sets the text alignment (e.g., start, end, center). This property is inherited by child text
     * components if not overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextAlign] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textAlign(value: TextAlign) // int enum of 6 values + unspecified, 3 bits

    /**
     * Sets the text direction (e.g., LTR, RTL, content-based). This property is inherited by child
     * text components if not overridden. This affects text layout and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextDirection] to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textDirection(value: TextDirection) // int enum of 5 values + unspecified, 3 bits

    /**
     * Sets the line breaking strategy for text. This property is inherited by child text components
     * if not overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [LineBreak] strategy to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun lineBreak(value: LineBreak) // int enum of 3 values + unspecified, 2 bits

    /**
     * Sets the hyphenation strategy for text. This property is inherited by child text components
     * if not overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [Hyphens] strategy to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun hyphens(value: Hyphens) // int enum of 2 values + unspecified, 2 bits

    /**
     * Sets the font synthesis strategy, determining if and how bold/italic styles are synthesized
     * when the font family does not natively support them. This property is inherited by child text
     * components if not overridden. This affects text rendering and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontSynthesis] strategy to apply.
     * @see textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontSynthesis(value: FontSynthesis) // enum int value, 4 possible values,

    /**
     * A helper function to implement state reading extension functions such as
     * [StyleScope.pressed].
     *
     * Custom style states can use this function to implement start reading functions to be
     * consistent with the predefined state reading functions.
     */
    fun <T> state(
        key: StyleStateKey<T>,
        value: Style,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    )
}

/**
 * Sets the width of the component to fill the parent's available width (equivalent to
 * `width(1.0f)`). The specified width includes any padding.
 *
 * This property is *not* inherited
 *
 * @see fillHeight
 * @see fillSize
 * @see androidx.compose.foundation.layout.fillMaxWidth
 */
@ExperimentalFoundationStyleApi
fun StyleScope.fillWidth() {
    width(1.0f)
}

/**
 * Sets the height of the component to fill the parent's available height (equivalent to
 * `height(1.0f)`). The specified height includes any padding.
 *
 * This property is *not* inherited
 *
 * @see fillWidth
 * @see fillSize
 * @see androidx.compose.foundation.layout.fillMaxHeight
 */
@ExperimentalFoundationStyleApi
fun StyleScope.fillHeight() {
    height(1.0f)
}

/**
 * Sets the width and height of the component to fill the parent's available space (equivalent to
 * `width(1.0f)` and `height(1.0f)`). The specified size includes any padding.
 *
 * This property is *not* inherited
 *
 * @see fillWidth
 * @see fillHeight
 * @see androidx.compose.foundation.layout.fillMaxSize
 */
@ExperimentalFoundationStyleApi
fun StyleScope.fillSize() {
    width(1.0f)
    height(1.0f)
}

/**
 * Apply [style] to the current [StyleScope]. Applying the scope calls the scope applies any
 * properties set in the scope directly.
 *
 * @param style the style to apply.
 */
@ExperimentalFoundationStyleApi
fun StyleScope.apply(style: Style) {
    with(style) { this@apply.applyStyle() }
}
