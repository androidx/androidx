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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.foundation.style

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit

/**
 * An interface that is the base interface for all styles scopes.
 *
 * [CustomStyleScope] is a constraint on the [CustomStyle]'s `ScopeT` parameter which is the
 * receiver scope of the style. At minimum all style scopes used as `ScopeT` must implement
 * [CustomStyleScope] typically by delegation to a [StyleScope] received in a [Style] lambda.
 *
 * @sample androidx.compose.foundation.samples.StyleStateKeySample
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi interface CustomStyleScope : Density, CompositionLocalAccessorScope

/**
 * An interface that introduces the [state] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface StyleStateScope {
    /**
     * The state of the component. applying this style. For example, if a component is pressed the
     * [StyleState.isPressed] will be `true`.
     *
     * Custom states can be read from the state using the [StyleStateKey] for the state.
     */
    val state: StyleState

    /**
     * A helper function to implement state reading extension functions such as
     * [StyleScope.pressed].
     *
     * Custom style states can use this function to implement start reading functions to be
     * consistent with the predefined state reading functions.
     *
     * @param key the [StyleStateKey] for the custom state.
     * @param block the block to execute when [active] returns `true`.
     * @param active an expression that should return `true` when the state is active and [block]
     *   should be called.
     * @sample androidx.compose.foundation.samples.StyleStateKeySample
     */
    fun <T> state(
        key: StyleStateKey<T>,
        block: () -> Unit,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    )
}

/**
 * A helper function to implement state reading extension functions such as [StyleScope.pressed] for
 * use with `Boolean` states.
 *
 * Custom style states can use this function to implement start reading functions to be consistent
 * with the predefined state reading functions.
 *
 * @param key the [StyleStateKey] for the custom state.
 * @param block the block to execute when the style state is `true`.
 * @sample androidx.compose.foundation.samples.StyleStateKeySample
 */
@ExperimentalFoundationStyleApi
fun StyleStateScope.state(key: StyleStateKey<Boolean>, block: () -> Unit) =
    state(key, block) { key, state -> state[key] }

/**
 * An interface that introduces the [contentPadding] property to a [Style] receiver scope interface.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ContentPaddingScope {
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
}

/**
 * Sets the padding for the horizontal (start and end) edges of the component's content. Content
 * padding is the space between the component's border (if any) and its content. The width/height of
 * the component includes content padding.
 *
 * This property is *not* inherited
 *
 * @param value The amount of padding to apply to both start and end edges.
 * @see ContentPaddingScope.contentPaddingStart
 * @see ContentPaddingScope.contentPaddingEnd
 * @see contentPaddingVertical
 * @see contentPadding
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ContentPaddingScope.contentPaddingHorizontal(value: Dp) {
    contentPaddingStart(value)
    contentPaddingEnd(value)
}

/**
 * Sets the padding for the vertical (start and end) edges of the component's content. Content
 * padding is the space between the component's border (if any) and its content. The width/height of
 * the component includes content padding.
 *
 * This property is *not* inherited
 *
 * @param value The amount of padding to apply to both top and bottom edges.
 * @see ContentPaddingScope.contentPaddingTop
 * @see ContentPaddingScope.contentPaddingBottom
 * @see contentPaddingHorizontal
 * @see contentPadding
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ContentPaddingScope.contentPaddingVertical(value: Dp) {
    contentPaddingTop(value)
    contentPaddingBottom(value)
}

/**
 * Sets the padding for all four edges (top, end, bottom, start) edges of the component's content.
 * Content padding is the space between the component's border (if any) and its content. The
 * width/height of the component includes content padding.
 *
 * This property is *not* inherited
 *
 * @param value The amount of padding to apply to all four edges.
 * @see ContentPaddingScope.contentPaddingStart
 * @see ContentPaddingScope.contentPaddingEnd
 * @see ContentPaddingScope.contentPaddingTop
 * @see ContentPaddingScope.contentPaddingBottom
 * @see contentPaddingHorizontal
 * @see contentPaddingVertical
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ContentPaddingScope.contentPadding(value: Dp) {
    contentPaddingStart(value)
    contentPaddingEnd(value)
    contentPaddingTop(value)
    contentPaddingBottom(value)
}

/**
 * Sets the padding for all four edges (top, end, bottom, start) edges of the component's content.
 * Content padding is the space between the component's border (if any) and its content. The
 * width/height of the component includes content padding.
 *
 * This property is *not* inherited
 *
 * @param start The padding for the start edge.
 * @param top The padding for the top edge.
 * @param end The padding for the end edge.
 * @param bottom The padding for the bottom edge.
 * @see ContentPaddingScope.contentPaddingStart
 * @see ContentPaddingScope.contentPaddingEnd
 * @see ContentPaddingScope.contentPaddingTop
 * @see ContentPaddingScope.contentPaddingBottom
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ContentPaddingScope.contentPadding(start: Dp, top: Dp, end: Dp, bottom: Dp) {
    contentPaddingStart(start)
    contentPaddingTop(top)
    contentPaddingEnd(end)
    contentPaddingBottom(bottom)
}

/**
 * Sets the padding for the vertical (top and bottom) and horizontal (start and end) edges of the
 * component's content. Content padding is the space between the component's border (if any) and its
 * content. The width/height of the component includes content padding.
 *
 * This property is *not* inherited
 *
 * @param vertical The padding for the top and bottom edges.
 * @param horizontal The padding for the start and end edges.
 * @see contentPaddingVertical
 * @see contentPaddingHorizontal
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ContentPaddingScope.contentPadding(horizontal: Dp, vertical: Dp) {
    contentPaddingHorizontal(horizontal)
    contentPaddingVertical(vertical)
}

/**
 * Sets the padding for the component's content. Content padding is the space between the
 * component's border (if any) and its content. The width/height of the component includes content
 * padding.
 *
 * This property is *not* inherited
 *
 * @param paddingValues The [PaddingValues] to apply to the content.
 * @see contentPadding
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun <T> T.contentPadding(paddingValues: PaddingValues)
    where T : ContentPaddingScope, T : CompositionLocalAccessorScope {
    contentPadding(
        start = paddingValues.calculateStartPadding(LocalLayoutDirection.currentValue),
        top = paddingValues.calculateTopPadding(),
        end = paddingValues.calculateEndPadding(LocalLayoutDirection.currentValue),
        bottom = paddingValues.calculateBottomPadding(),
    )
}

/**
 * An interface that introduces the [externalPadding] properties to a [Style] receiver scope
 * interface.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ExternalPaddingScope {
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
}

/**
 * Sets the external padding for the horizontal (start and end) edges of the component. The external
 * padding is the space between the edge of the component and its border (if any). The width/height
 * of the component includes external padding.
 *
 * This property is *not* inherited
 *
 * @param value The amount of padding to apply to both start and end edges.
 * @see ExternalPaddingScope.externalPaddingStart
 * @see ExternalPaddingScope.externalPaddingEnd
 * @see externalPaddingVertical
 * @see externalPadding
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ExternalPaddingScope.externalPaddingHorizontal(value: Dp) {
    externalPaddingStart(value)
    externalPaddingEnd(value)
}

/**
 * Sets the external padding for the vertical (start and end) edges of the component. The external
 * padding is the space between the edge of the component and its border (if any). The width/height
 * of the component includes external padding.
 *
 * This property is *not* inherited
 *
 * @param value The amount of padding to apply to both top and bottom edges.
 * @see ExternalPaddingScope.externalPaddingTop
 * @see ExternalPaddingScope.externalPaddingBottom
 * @see externalPaddingHorizontal
 * @see externalPadding
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ExternalPaddingScope.externalPaddingVertical(value: Dp) {
    externalPaddingTop(value)
    externalPaddingBottom(value)
}

/**
 * Sets the external padding for all four edges (top, end, bottom, start) of the component. The
 * external padding is the space between the edge of the component and its border (if any). The
 * width/height of the component includes external padding.
 *
 * This property is *not* inherited
 *
 * @param value The amount of padding to apply to all four edges.
 * @see ExternalPaddingScope.externalPaddingStart
 * @see ExternalPaddingScope.externalPaddingEnd
 * @see ExternalPaddingScope.externalPaddingTop
 * @see ExternalPaddingScope.externalPaddingBottom
 * @see externalPaddingHorizontal
 * @see externalPaddingVertical
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ExternalPaddingScope.externalPadding(value: Dp) {
    externalPaddingStart(value)
    externalPaddingEnd(value)
    externalPaddingTop(value)
    externalPaddingBottom(value)
}

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
 * @see ExternalPaddingScope.externalPaddingStart
 * @see ExternalPaddingScope.externalPaddingEnd
 * @see ExternalPaddingScope.externalPaddingTop
 * @see ExternalPaddingScope.externalPaddingBottom
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ExternalPaddingScope.externalPadding(start: Dp, top: Dp, end: Dp, bottom: Dp) {
    externalPaddingStart(start)
    externalPaddingTop(top)
    externalPaddingEnd(end)
    externalPaddingBottom(bottom)
}

/**
 * Sets the external padding for the vertical (top and bottom) and horizontal (start and end) edges
 * of the component. The external padding is the space between the edge of the component and its
 * border (if any). The width/height of the component includes external padding.
 *
 * This property is *not* inherited
 *
 * @param vertical The padding for the top and bottom edges.
 * @param horizontal The padding for the start and end edges.
 * @see externalPaddingVertical
 * @see externalPaddingHorizontal
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun ExternalPaddingScope.externalPadding(horizontal: Dp, vertical: Dp) {
    externalPaddingHorizontal(horizontal)
    externalPaddingVertical(vertical)
}

/**
 * Sets the external padding for the component. The external padding is the space between the edge
 * of the component and its border (if any). The width/height of the component includes external
 * padding.
 *
 * This property is *not* inherited
 *
 * @param paddingValues The [PaddingValues] to apply to the external padding.
 * @see externalPadding
 * @see androidx.compose.foundation.layout.padding
 */
@ExperimentalFoundationStyleApi
fun <T> T.externalPadding(paddingValues: PaddingValues)
    where T : ExternalPaddingScope, T : CompositionLocalAccessorScope {
    externalPadding(
        start = paddingValues.calculateStartPadding(LocalLayoutDirection.currentValue),
        top = paddingValues.calculateTopPadding(),
        end = paddingValues.calculateEndPadding(LocalLayoutDirection.currentValue),
        bottom = paddingValues.calculateBottomPadding(),
    )
}

/**
 * An interface that introduces border properties to a [Style] receiver scope interface.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface BorderScope {
    /**
     * Sets the width of the border around the component. The border is drawn on top of the
     * background and the padded content. The border's width does not contribute to the component's
     * layout size (width/height); it is rendered within the component's bounds This method only
     * sets the width; color or brush must be set separately.
     *
     * Specifying a [Dp.Unspecified] value will remove the border.
     *
     * Specifying a [Dp.Hairline] or 0.dp value will create 1 pixel border regardless of density.
     *
     * This property is *not* inherited
     *
     * @param value The width of the border.
     * @see borderColor
     * @see borderBrush
     * @see border
     * @see ShapeScope.shape
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
     * @see StyleScope.shape
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
}

/**
 * An interface that introduces size properties to a [Style] receiver scope interface.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface SizeScope {
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
     * @see MinSizeScope.minWidth
     * @see MaxSizeScope.maxWidth
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
     * @see MinSizeScope.minWidth
     * @see MaxSizeScope.maxWidth
     * @see androidx.compose.foundation.layout.height
     */
    fun height(value: Dp)

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
}

/**
 * Sets the preferred width and height of the component. The actual size will also depend on the
 * parent's constraints and other modifiers. The specified dimensions includes both [contentPadding]
 * and [externalPadding].
 *
 * This property is *not* inherited
 *
 * @param width The preferred width in Dp.
 * @param height The preferred height in Dp.
 * @see androidx.compose.foundation.layout.size
 */
@ExperimentalFoundationStyleApi
fun SizeScope.size(width: Dp, height: Dp) {
    width(width)
    height(height)
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
fun SizeScope.fillHeight() {
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
fun SizeScope.fillSize() {
    width(1.0f)
    height(1.0f)
}

/**
 * Sets the preferred width and height of the component to the same value. The actual size will also
 * depend on the parent's constraints and other modifiers. The specified size includes any padding.
 *
 * This property is *not* inherited
 *
 * @param value The preferred width and height in Dp.
 * @see androidx.compose.foundation.layout.size
 */
@ExperimentalFoundationStyleApi
fun SizeScope.size(value: Dp) {
    width(value)
    height(value)
}

/**
 * Sets the preferred width and height of the component using a [DpSize] object. The actual size
 * will also depend on the parent's constraints and other modifiers. The specified size includes any
 * padding.
 *
 * This property is *not* inherited
 *
 * @param value The preferred size.
 * @see androidx.compose.foundation.layout.size
 */
@ExperimentalFoundationStyleApi
fun SizeScope.size(value: DpSize) {
    width(value.width)
    height(value.height)
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
fun SizeScope.fillWidth() {
    width(1.0f)
}

/**
 * Sets a solid border for the component with the specified width and color. The border is drawn on
 * top of the background and the padded content. The border itself does not contribute to the
 * component's layout size (width/height).
 *
 * Specifying a [Dp.Unspecified] width will remove the border.
 *
 * Specifying a [Dp.Hairline] or 0.dp width will create 1 pixel border regardless of density.
 *
 * This property is *not* inherited
 *
 * @param width The width of the border.
 * @param color The color of the border.
 * @see BorderScope.borderWidth
 * @see BorderScope.borderColor
 * @see BorderScope.borderBrush
 * @see androidx.compose.foundation.border
 */
@ExperimentalFoundationStyleApi
fun BorderScope.border(width: Dp, color: Color) {
    borderWidth(width)
    borderColor(color)
}

/**
 * Sets a border for the component with the specified width and brush. The border is drawn on top of
 * the background and the padded content. The border itself does not contribute to the component's
 * layout size (width/height).
 *
 * Specifying a [Dp.Unspecified] width will remove the border.
 *
 * Specifying a [Dp.Hairline] or 0.dp width will create 1 pixel border regardless of density.
 *
 * This property is *not* inherited
 *
 * @param width The width of the border.
 * @param brush The brush to paint the border (e.g., for gradients).
 * @see BorderScope.borderWidth
 * @see BorderScope.borderColor
 * @see BorderScope.borderBrush
 * @see androidx.compose.foundation.border
 */
@ExperimentalFoundationStyleApi
fun BorderScope.border(width: Dp, brush: Brush) {
    borderWidth(width)
    borderBrush(brush)
}

/**
 * An interface that introduces position properties to a [Style] receiver scope interface.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface PositionScope {
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
}

/**
 * An interface that introduces min size properties to a [Style] receiver scope interface.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface MinSizeScope {
    /**
     * Constrains the minimum width of the component. The component's width, including padding, will
     * be at least this value.
     *
     * This property is *not* inherited
     *
     * @param value The minimum width.
     * @see minHeight
     * @see minSize
     * @see MaxSizeScope.maxWidth
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
     * @see MaxSizeScope.maxHeight
     * @see androidx.compose.foundation.layout.heightIn
     */
    fun minHeight(value: Dp)
}

/**
 * Constrains the minimum size (width and height) of the component. The component's dimensions,
 * including padding, will be at least these values.
 *
 * This property is *not* inherited
 *
 * @param size The minimum size ([DpSize]).
 * @see MinSizeScope.minWidth
 * @see MinSizeScope.minHeight
 * @see androidx.compose.foundation.layout.sizeIn
 */
@ExperimentalFoundationStyleApi
fun MinSizeScope.minSize(size: DpSize) {
    minWidth(size.width)
    minHeight(size.height)
}

/**
 * Constrains the minimum width and height of the component. The component's dimensions, including
 * padding, will be at least these values.
 *
 * This property is *not* inherited
 *
 * @param width The minimum width.
 * @param height The minimum height.
 * @see MinSizeScope.minWidth
 * @see MinSizeScope.minHeight
 * @see androidx.compose.foundation.layout.sizeIn
 */
@ExperimentalFoundationStyleApi
fun MinSizeScope.minSize(width: Dp, height: Dp) {
    minWidth(width)
    minHeight(height)
}

/**
 * An interface that introduces [maxWidth] and [maxHeight] properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface MaxSizeScope {
    /**
     * Constrains the maximum width of the component. The component's width, including padding, will
     * be at most this value.
     *
     * This property is *not* inherited
     *
     * @param value The maximum width.
     * @see maxHeight
     * @see maxSize
     * @see MinSizeScope.minWidth
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
     * @see MinSizeScope.minHeight
     * @see androidx.compose.foundation.layout.heightIn
     */
    fun maxHeight(value: Dp)
}

/**
 * Constrains the maximum size (width and height) of the component. The component's dimensions,
 * including padding, will be at most these values.
 *
 * This property is *not* inherited
 *
 * @param size The maximum size ([DpSize]).
 * @see MaxSizeScope.maxWidth
 * @see MaxSizeScope.maxHeight
 * @see androidx.compose.foundation.layout.sizeIn
 */
@ExperimentalFoundationStyleApi
fun MaxSizeScope.maxSize(size: DpSize) {
    maxWidth(size.width)
    maxHeight(size.height)
}

/**
 * Constrains the maximum width and height of the component. The component's dimensions, including
 * padding, will be at most these values.
 *
 * This property is *not* inherited
 *
 * @param width The maximum width.
 * @param height The maximum height.
 * @see MaxSizeScope.maxWidth
 * @see MaxSizeScope.maxHeight
 * @see androidx.compose.foundation.layout.sizeIn
 */
@ExperimentalFoundationStyleApi
fun MaxSizeScope.maxSize(width: Dp, height: Dp) {
    maxWidth(width)
    maxHeight(height)
}

/**
 * An interface that introduces the [alpha] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface AlphaScope {
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
}

/**
 * An interface that introduces the [scaleX] and [scaleY] properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ScaleScope {
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
}

/**
 * Scales the component uniformly (both horizontally and vertically) around its center pivot point.
 *
 * Setting [scale] will override the horizontal scaling set by a previous call to
 * [ScaleScope.scaleX] and the previous vertical scaling set by calling [ScaleScope.scaleY].
 *
 * This property is *not* inherited
 *
 * @param value The scaling factor for both X and Y axes. 1.0f is no scale.
 * @see ScaleScope.scaleX
 * @see ScaleScope.scaleY
 * @see androidx.compose.ui.draw.scale
 * @see androidx.compose.ui.graphics.graphicsLayer
 */
@ExperimentalFoundationStyleApi
fun ScaleScope.scale(@FloatRange(from = 0.0) value: Float) {
    scaleX(value)
    scaleY(value)
}

/**
 * Scales the component uniformly (both horizontally and vertically) around its center pivot point.
 *
 * Setting [scale] will override the horizontal scaling set by a previous call to
 * [ScaleScope.scaleX] and the previous vertical scaling set by calling [ScaleScope.scaleY].
 *
 * This property is *not* inherited
 *
 * @param x The scaling factor for X axes. 1.0f is no scale.
 * @param y The scaling factor for Y axes. 1.0f is no scale.
 * @see ScaleScope.scaleX
 * @see ScaleScope.scaleY
 * @see androidx.compose.ui.draw.scale
 * @see androidx.compose.ui.graphics.graphicsLayer
 */
@ExperimentalFoundationStyleApi
fun ScaleScope.scale(@FloatRange(from = 0.0) x: Float, @FloatRange(from = 0.0) y: Float) {
    scaleX(x)
    scaleY(y)
}

/**
 * An interface that introduces the translation properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TranslationScope {
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
}

/**
 * Translates (moves) the component horizontally and vertically.
 *
 * This property is *not* inherited
 *
 * @param x The translation amount on the X-axis in pixels.
 * @param y The translation amount on the Y-axis in pixels.
 * @see TranslationScope.translationX
 * @see TranslationScope.translationY
 * @see androidx.compose.ui.graphics.graphicsLayer
 */
@ExperimentalFoundationStyleApi
fun TranslationScope.translation(
    @FloatRange(from = 0.0) x: Float,
    @FloatRange(from = 0.0) y: Float,
) {
    translationX(x)
    translationY(y)
}

/**
 * Translates (moves) the component by the given [Offset].
 *
 * This property is *not* inherited
 *
 * @param offset The translation offset in pixels.
 * @see TranslationScope.translationX
 * @see TranslationScope.translationY
 * @see androidx.compose.ui.graphics.graphicsLayer
 */
@ExperimentalFoundationStyleApi
fun TranslationScope.translation(offset: Offset) {
    translationX(offset.x)
    translationY(offset.y)
}

/**
 * An interface that introduces the rotation properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface RotationScope {
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
}

/**
 * Rotates the component around the X-axis, Y-axis and Z-axis (perpendicular to the screen) through
 * its center.
 *
 * This property is *not* inherited
 *
 * @param x The x-axis rotation angle in degrees.
 * @param y The x-axis rotation angle in degrees.
 * @param z The x-axis rotation angle in degrees.
 * @see RotationScope.rotationX
 * @see RotationScope.rotationY
 * @see RotationScope.rotationZ
 * @see androidx.compose.ui.draw.rotate
 * @see androidx.compose.ui.graphics.graphicsLayer
 */
@ExperimentalFoundationStyleApi
fun RotationScope.rotation(x: Float, y: Float, z: Float) {
    rotationX(x)
    rotationY(y)
    rotationZ(z)
}

/**
 * An interface the introduces the [colorFilter] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ColorFilterScope {
    /**
     * Sets the [ColorFilter] to apply to the component.
     *
     * This property is *not* inherited
     *
     * @param value The color filter to apply.
     * @see androidx.compose.ui.graphics.graphicsLayer
     */
    fun colorFilter(value: ColorFilter?)
}

/**
 * An interface that introduces the transform origin properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TransformOriginScope {
    /**
     * Offset percentage along the x-axis for which contents are rotated and scaled. The default
     * value of 0.5f indicates the pivot point will be at the midpoint of the left and right bounds
     * of the layer. Default value is [TransformOrigin.Center].
     *
     * @param value The origin of the transform
     * @see [androidx.compose.ui.graphics.GraphicsLayerScope]
     */
    fun transformOriginX(value: Float)

    /**
     * Offset percentage along the y-axis for which contents are rotated and scaled. The default
     * value of 0.5f indicates the pivot point will be at the midpoint of the top and bottom bounds
     * of the layer. Default value is [TransformOrigin.Center].
     *
     * @param value The origin of the transform
     * @see [androidx.compose.ui.graphics.GraphicsLayerScope]
     */
    fun transformOriginY(value: Float)
}

/**
 * Offset percentage along the x and y-axis for which contents are rotated and scaled. The default
 * value of 0.5f, 0.5f indicates the pivot point will be at the midpoint of the left and right as
 * well as the top and bottom bounds of the layer. Default value is [TransformOrigin.Center].
 *
 * @param value The origin of the transform
 * @see [androidx.compose.ui.graphics.GraphicsLayerScope]
 */
@ExperimentalFoundationStyleApi
fun TransformOriginScope.transformOrigin(value: TransformOrigin) {
    transformOriginX(value.pivotFractionX)
    transformOriginY(value.pivotFractionY)
}

/**
 * An interface that introduces the [clip] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ClipScope {
    /**
     * Clips the component to its bounds. If a [ShapeScope.shape] is also applied, it clips to the
     * shape. When clip is `true` content that overflows the component's bounds is not drawn.
     *
     * This property is *not* inherited
     *
     * @param value `true` to clip (default), `false` to allow drawing outside bounds.
     * @see ShapeScope.shape
     * @see androidx.compose.ui.draw.clip
     * @see androidx.compose.ui.draw.clipToBounds
     */
    fun clip(value: Boolean = true)
}

/**
 * An interface that introduces the [zIndex] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ZIndexScope {
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
}

/**
 * An interface that introduces the [background] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface BackgroundScope {
    /**
     * Sets the background color of the component. If a [ShapeScope.shape] is applied, the
     * background will fill that shape.
     *
     * Setting a background color will override any previously set background brush.
     *
     * This property is *not* inherited
     *
     * @param color The background color.
     * @see ShapeScope.shape
     * @see androidx.compose.foundation.background
     */
    fun background(color: Color)

    /**
     * Sets the background of the component using a [Brush]. This allows for gradient backgrounds or
     * other complex fills. If a [ShapeScope.shape] is applied, the background will fill that shape.
     *
     * Setting a background brush will override any previously set background color.
     *
     * This property is *not* inherited
     *
     * @param value The brush to use for the background.
     * @see ShapeScope.shape
     * @see androidx.compose.foundation.background
     */
    fun background(value: Brush)
}

/**
 * An interface that introduces the [foreground] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ForegroundScope {
    /**
     * Sets the foreground color for the component. This can be used to overlay a color on top of
     * the component's content. It is important that this brush be partially transparent (e.g. alpha
     * less than 1.0) or it will obscure the content. If a [ShapeScope.shape] is applied, the
     * [foreground] will fill that shape.
     *
     * This property is *not* inherited
     *
     * @param value The brush to use for the foreground.
     * @see ContentColorScope.contentColor
     * @see ContentColorScope.contentBrush
     */
    fun foreground(value: Color)

    /**
     * Sets the foreground brush for the component. This can be used to overlay a color or gradient
     * on top of the component's content. It is important that this brush be partially transparent
     * (e.g. alpha less than 1.0) or it will obscure the content. If a [ShapeScope.shape] is
     * applied, the [foreground] will fill that shape.
     *
     * This property is *not* inherited
     *
     * @param value The brush to use for the foreground.
     * @see ContentColorScope.contentColor
     * @see ContentColorScope.contentBrush
     */
    fun foreground(value: Brush)
}

/**
 * An interface that introduces the [shape] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ShapeScope {
    /**
     * Sets the [Shape] for the component. This shape is used for clipping ([ClipScope.clip]),
     * background rendering ([BackgroundScope.background]), and border rendering.
     *
     * If [shape] is not specified then a [androidx.compose.ui.graphics.RectangleShape] is used.
     *
     * This property is *not* inherited
     *
     * @param value The shape to apply.
     * @see ClipScope.clip
     * @see BackgroundScope.background
     * @see border
     * @see androidx.compose.ui.draw.clip
     * @see androidx.compose.foundation.background
     * @see androidx.compose.foundation.border
     */
    fun shape(value: Shape)
}

/**
 * An interface that introduces the shadow properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ShadowScope {
    /**
     * Applies a drop shadow effect directly to the component, often used for text or specific
     * graphics. This is distinct from `shadowElevation` which is specific to platform elevation
     * shadows. Multiple drop shadows can be applied by calling this function multiple times or
     * using the vararg overload. The border and overall layout size are not affected by this
     * shadow.
     *
     * If [ShapeScope.shape] is set, the shadow will be applied to the shape's bounds.
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
     * If [ShapeScope.shape] is set, the shadow will be applied to the shape's bounds.
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
}

/**
 * An interface that introduces [animate] functions to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface AnimateStyleScope {

    /**
     * Specifies a [Style] whose properties will be animated to when they change, using the provided
     * [AnimationSpec]. This allows for smooth transitions between style states with custom
     * animation curves.
     *
     * @param toSpec The [AnimationSpec] to use for the animation to the values set in [block]. This
     *   animation is used when the [animate] call is added to the style.
     * @param fromSpec The [AnimationSpec] to use for the animation from the values set in [block].
     *   This animation is used when the [animate] call is removed from the style.
     * @param block The block containing the properties values to animate
     * @see Style
     * @see androidx.compose.animation.core.AnimationSpec
     */
    fun animate(toSpec: AnimationSpec<Float>, fromSpec: AnimationSpec<Float>, block: () -> Unit)
}

/**
 * Specifies a [Style] whose properties will be animated to when they change. This uses a default
 * animation specification. This allows for smooth transitions between style states without manual
 * animation setup. A [androidx.compose.animation.core.spring] will be used for the animation for
 * both animating to and from the style.
 *
 * @param block The target [Style] whose properties should be animated.
 * @see Style
 */
@ExperimentalFoundationStyleApi
fun AnimateStyleScope.animate(block: () -> Unit) {
    animate(DefaultSpringSpec, block)
}

/**
 * Specifies a [Style] whose properties will be animated to when they change, using the provided
 * [AnimationSpec]. This allows for smooth transitions between style states with custom animation
 * curves. The same animation [spec] will be used for animating both to and from the style.
 *
 * @param spec The [AnimationSpec] to use for the animation.
 * @param block The target [Style] whose properties should be animated.
 * @see Style
 * @see androidx.compose.animation.core.AnimationSpec
 */
@ExperimentalFoundationStyleApi
fun AnimateStyleScope.animate(spec: AnimationSpec<Float>, block: () -> Unit) {
    animate(spec, spec, block)
}

/**
 * An interface that introduces the [textStyle] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TextStyleScope {
    /**
     * Applies a complete [TextStyle] object to the component. This is a convenient way to set
     * multiple text-related properties at once. Text properties are inherited by child text
     * components unless overridden.
     *
     * This property is inherited.
     *
     * @param value The [TextStyle] to apply.
     * @see ContentColorScope.contentColor
     * @see FontFamilyScope.fontFamily
     * @see FontSizeScope.fontSize
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textStyle(value: TextStyle)
}

/**
 * An interface that introduces the [contentColor] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface ContentColorScope {
    /**
     * Sets the preferred content color, primarily affecting text color. This property is inherited
     * by child text components if not overridden. This affects drawing only and is often a
     * component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The color for the content, typically text.
     * @see contentBrush
     * @see TextStyleScope.textStyle
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
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun contentBrush(value: Brush)
}

/**
 * An interface that introduces the [textDecoration] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TextDecorationScope {
    /**
     * Sets the text decoration (e.g., underline, line-through). This property is inherited by child
     * text components if not overridden. This affects drawing only and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextDecoration] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textDecoration(value: TextDecoration) // int enum (mask?) 2 possible values
}

/**
 * An interface that introduces the [fontFamily] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface FontFamilyScope {
    /**
     * Sets the font family for the text. This property is inherited by child text components if not
     * overridden. This affects text layout and rendering, and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontFamily] to use.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontFamily(value: FontFamily) // reference class
}

/**
 * An interface that introduces the [textIndent] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TextIndentScope {
    /**
     * Sets the text indent (e.g., for the first line or subsequent lines). This property is
     * inherited by child text components if not overridden. This affects text layout and is a
     * component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextIndent] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textIndent(value: TextIndent) // ref class of two longs
}

/**
 * An interface that introduces the [fontSize] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface FontSizeScope {
    /**
     * Sets the font size for the text. This property is inherited by child text components if not
     * overridden. This affects text layout and rendering, and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The font size in [TextUnit] (e.g., `16.sp`).
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontSize(value: TextUnit)
}

/**
 * An interface that introduces the [lineHeight] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface LineHeightScope {
    /**
     * Sets the line height for the text. This property is inherited by child text components if not
     * overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The line height in [TextUnit] (e.g., `20.sp`) or `TextUnit.Unspecified`.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun lineHeight(value: TextUnit)
}

/**
 * An interface that introduces the [letterSpacing] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface LetterSpacingScope {
    /**
     * Sets the letter spacing for the text. This property is inherited by child text components if
     * not overridden. This affects text layout and rendering, and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The letter spacing in [TextUnit] (e.g., `0.5.sp`).
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun letterSpacing(value: TextUnit)
}

/**
 * An interface that introduces the [baselineShift] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface BaselineShiftScope {
    /**
     * Sets the baseline shift for the text (e.g., for superscript or subscript). This property is
     * inherited by child text components if not overridden. This affects text layout and rendering,
     * and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [BaselineShift] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun baselineShift(value: BaselineShift)
}

/**
 * An interface that introduces the [fontWeight] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface FontWeightScope {
    /**
     * Sets the font weight for the text (e.g., bold, normal). This property is inherited by child
     * text components if not overridden. This affects text rendering and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontWeight] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontWeight(value: FontWeight) // Int enum, 9 values, 4 bits
}

/**
 * An interface that introduces the [fontStyle] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface FontStyleScope {
    /**
     * Sets the font style for the text (e.g., italic, normal). This property is inherited by child
     * text components if not overridden. This affects text rendering and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontStyle] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontStyle(value: FontStyle)
}

/**
 * An interface that introduces the [textDirection] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TextDirectionScope {
    /**
     * Sets the text direction (e.g., LTR, RTL, content-based). This property is inherited by child
     * text components if not overridden. This affects text layout and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextDirection] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textDirection(value: TextDirection) // int enum of 5 values + unspecified, 3 bits
}

/**
 * An interface that introduces the [textAlign] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TextAlignScope {
    /**
     * Sets the text alignment (e.g., start, end, center). This property is inherited by child text
     * components if not overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextAlign] to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textAlign(value: TextAlign)
}

/**
 * An interface that introduces the [lineBreak] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface LineBreakScope {
    /**
     * Sets the line breaking strategy for text. This property is inherited by child text components
     * if not overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [LineBreak] strategy to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun lineBreak(value: LineBreak)
}

/**
 * An interface that introduces the [hyphens] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface HyphensScope {
    /**
     * Sets the hyphenation strategy for text. This property is inherited by child text components
     * if not overridden. This affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [Hyphens] strategy to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun hyphens(value: Hyphens) // int enum of 2 values + unspecified, 2 bits
}

/**
 * An interface that introduces the [fontSynthesis] property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface FontSynthesisScope {
    /**
     * Sets the font synthesis strategy, determining if and how bold/italic styles are synthesized
     * when the font family does not natively support them. This property is inherited by child text
     * components if not overridden. This affects text rendering and is a component of a
     * [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [FontSynthesis] strategy to apply.
     * @see TextStyleScope.textStyle
     * @see androidx.compose.ui.text.TextStyle
     */
    fun fontSynthesis(value: FontSynthesis) // enum int value, 4 possible values,
}

/** An interface that introduces the [textMotion] property to a [Style] receiver scope. */
@ExperimentalFoundationStyleApi
interface TextMotionScope {
    /**
     * Sets the text motion strategy, which can be used to optimize for readability or for smooth
     * animations. This property is inherited by child text components if not overridden. This
     * affects text layout and is a component of a [TextStyle].
     *
     * This property is inherited.
     *
     * @param value The [TextMotion] strategy to apply.
     * @see androidx.compose.ui.text.TextStyle
     */
    fun textMotion(value: TextMotion)
}

/**
 * An interface that introduces the layout properties to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface LayoutStyleScope :
    ContentPaddingScope, ExternalPaddingScope, SizeScope, PositionScope, MinSizeScope, MaxSizeScope

/**
 * An interface that introduces the graphics layer property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface LayerStyleScope :
    AlphaScope,
    ScaleScope,
    TranslationScope,
    RotationScope,
    TransformOriginScope,
    ColorFilterScope,
    ClipScope,
    ZIndexScope

/**
 * An interface that introduces the drawing property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface DrawStyleScope : BorderScope, BackgroundScope, ForegroundScope, ShapeScope, ShadowScope

/**
 * An interface that introduces the text property to a [Style] receiver scope.
 *
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
interface TextStyleStyleScope :
    TextStyleScope,
    ContentColorScope,
    TextDecorationScope,
    FontFamilyScope,
    TextIndentScope,
    FontSizeScope,
    LineHeightScope,
    LetterSpacingScope,
    BaselineShiftScope,
    FontWeightScope,
    FontStyleScope,
    TextDirectionScope,
    TextAlignScope,
    LineBreakScope,
    HyphensScope,
    TextMotionScope,
    FontSynthesisScope

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
interface StyleScope :
    CustomStyleScope,
    StyleStateScope,
    AnimateStyleScope,
    LayoutStyleScope,
    LayerStyleScope,
    DrawStyleScope,
    TextStyleStyleScope

/**
 * Apply [style] to the current [StyleScope]. Applying the scope calls the scope applies any
 * properties set in the scope directly.
 *
 * @param style the style to apply.
 */
@ExperimentalFoundationStyleApi
fun <ScopeT : CustomStyleScope, StyleT : CustomStyle<ScopeT>> ScopeT.apply(style: StyleT) {
    with(style) { this@apply.applyStyle() }
}
