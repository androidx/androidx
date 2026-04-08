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

package androidx.compose.material3

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.internal.ripple.RippleNodeConfig
import androidx.compose.material3.internal.ripple.createRippleModifierNode
import androidx.compose.material3.tokens.StateTokens
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a Ripple using the provided values and values inferred from the theme.
 *
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s by
 * drawing ripple animations and state layers.
 *
 * A Ripple responds to [PressInteraction.Press] by starting a new ripple animation, and responds to
 * other [Interaction]s by showing a fixed state layer with varying alpha values depending on the
 * [Interaction].
 *
 * [MaterialTheme] provides Ripples using [androidx.compose.foundation.LocalIndication], so a Ripple
 * will be used as the default [Indication] inside components such as
 * [androidx.compose.foundation.clickable] and [androidx.compose.foundation.indication], in addition
 * to Material provided components that use a Ripple as well.
 *
 * You can also explicitly create a Ripple and provide it to custom components in order to change
 * the parameters from the default, such as to create an unbounded ripple with a fixed size.
 *
 * To create a Ripple with a manually defined color that can change over time, see the other
 * [ripple] overload with a [ColorProducer] parameter. This will avoid unnecessary recompositions
 * when changing the color, and preserve existing ripple state when the color changes.
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param color the color of the ripple state layers. This color is usually the same color used by
 *   the text or iconography in the component. This color will then have
 *   [RippleDefaults.RippleAlpha] applied to calculate the final color used to draw the ripple. If
 *   [Color.Unspecified] is provided the color used will be [LocalContentColor] instead. If inset
 *   focus rings are enabled, their colors will be provided by the theme or by an overridden
 *   [LocalRippleConfiguration].
 * @param focusRingShape if specified, the shape of the ripple that the focus ring indication will
 *   use, if inset focus ring indications are enabled. If left `null`, a default shape will be used,
 *   which will be a rounded rectangle based on the [radius] if specified, otherwise it will be just
 *   a [RectangleShape]. This [Shape] instance must remain the same during the lifetime of the
 *   ripple in composition. If the [Shape] needs to change, delegate from a single instance to the
 *   changing shape to preserve the instance requirement.
 * @param enablePressIndication if true, this ripple will draw the indication for press
 *   interactions. Set this to `false` to disable drawing any visuals for press interactions in this
 *   ripple.
 * @param enableFocusIndication if true, this ripple will draw the indication for focus
 *   interactions. Set this to `false` to disable drawing any visuals for focus interactions in this
 *   ripple.
 * @param enableHoverIndication if true, this ripple will draw the indication for hover
 *   interactions. Set this to `false` to disable drawing any visuals for hover interactions in this
 *   ripple.
 * @param enableDragIndication if true, this ripple will draw the indication for drag interactions.
 *   Set this to `false` to disable drawing any visuals for drag interactions in this ripple.
 */
@ExperimentalMaterial3Api
@Stable
fun ripple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    focusRingShape: Shape? = null,
    enablePressIndication: Boolean = true,
    enableFocusIndication: Boolean = true,
    enableHoverIndication: Boolean = true,
    enableDragIndication: Boolean = true,
): IndicationNodeFactory {
    return if (
        radius == Dp.Unspecified &&
            color == Color.Unspecified &&
            focusRingShape == null &&
            enablePressIndication &&
            enableFocusIndication &&
            enableHoverIndication &&
            enableDragIndication
    ) {
        if (bounded) DefaultBoundedRipple else DefaultUnboundedRipple
    } else {
        RippleNodeFactory(
            bounded = bounded,
            radius = radius,
            color = color,
            focusRingShape = focusRingShape,
            enablePressIndication = enablePressIndication,
            enableFocusIndication = enableFocusIndication,
            enableHoverIndication = enableHoverIndication,
            enableDragIndication = enableDragIndication,
        )
    }
}

/**
 * Creates a Ripple using the provided values and values inferred from the theme.
 *
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s by
 * drawing ripple animations and state layers.
 *
 * A Ripple responds to [PressInteraction.Press] by starting a new ripple animation, and responds to
 * other [Interaction]s by showing a fixed state layer with varying alpha values depending on the
 * [Interaction].
 *
 * [MaterialTheme] provides Ripples using [androidx.compose.foundation.LocalIndication], so a Ripple
 * will be used as the default [Indication] inside components such as
 * [androidx.compose.foundation.clickable] and [androidx.compose.foundation.indication], in addition
 * to Material provided components that use a Ripple as well.
 *
 * You can also explicitly create a Ripple and provide it to custom components in order to change
 * the parameters from the default, such as to create an unbounded ripple with a fixed size.
 *
 * To create a Ripple with a manually defined color that can change over time, see the other
 * [ripple] overload with a [ColorProducer] parameter. This will avoid unnecessary recompositions
 * when changing the color, and preserve existing ripple state when the color changes.
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have [RippleDefaults.RippleAlpha] applied to
 *   calculate the final color used to draw the ripple. If [Color.Unspecified] is provided the color
 *   used will be [LocalContentColor] instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
fun ripple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
): IndicationNodeFactory =
    ripple(
        bounded = bounded,
        radius = radius,
        color = color,
        focusRingShape = null,
        enablePressIndication = true,
        enableFocusIndication = true,
        enableHoverIndication = true,
        enableDragIndication = true,
    )

/**
 * Creates a Ripple using the provided values and values inferred from the theme.
 *
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s by
 * drawing ripple animations and state layers.
 *
 * A Ripple responds to [PressInteraction.Press] by starting a new ripple animation, and responds to
 * other [Interaction]s by showing a fixed state layer with varying alpha values depending on the
 * [Interaction].
 *
 * [MaterialTheme] provides Ripples using [androidx.compose.foundation.LocalIndication], so a Ripple
 * will be used as the default [Indication] inside components such as
 * [androidx.compose.foundation.clickable] and [androidx.compose.foundation.indication], in addition
 * to Material provided components that use a Ripple as well.
 *
 * You can also explicitly create a Ripple and provide it to custom components in order to change
 * the parameters from the default, such as to create an unbounded ripple with a fixed size.
 *
 * To create a Ripple with a static color, see the [ripple] overload with a [Color] parameter. This
 * overload is optimized for Ripples that have dynamic colors that change over time, to reduce
 * unnecessary recompositions.
 *
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have [RippleDefaults.RippleAlpha] applied to
 *   calculate the final color used to draw the ripple. If you are creating this [ColorProducer]
 *   outside of composition (where it will be automatically remembered), make sure that its instance
 *   is stable (such as by remembering the object that holds it), or remember the returned [ripple]
 *   object to make sure that ripple nodes are not being created each recomposition. If inset focus
 *   rings are enabled, their colors will
 *     * be provided by the theme or by an overridden [LocalRippleConfiguration].
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param focusRingShape if specified, the shape of the ripple that the focus ring indication will
 *   use, if inset focus ring indications are enabled. If left `null`, a default shape will be used,
 *   which will be a rounded rectangle based on the [radius] if specified, otherwise it will be just
 *   a [RectangleShape]. This [Shape] instance must remain the same during the lifetime of the
 *   ripple in composition. If the [Shape] needs to change, delegate from a single instance to the
 *   changing shape to preserve the instance requirement.
 * @param enablePressIndication if true, this ripple will draw the indication for press
 *   interactions. Set this to `false` to disable drawing any visuals for press interactions in this
 *   ripple.
 * @param enableFocusIndication if true, this ripple will draw the indication for focus
 *   interactions. Set this to `false` to disable drawing any visuals for focus interactions in this
 *   ripple.
 * @param enableHoverIndication if true, this ripple will draw the indication for hover
 *   interactions. Set this to `false` to disable drawing any visuals for hover interactions in this
 *   ripple.
 * @param enableDragIndication if true, this ripple will draw the indication for drag interactions.
 *   Set this to `false` to disable drawing any visuals for drag interactions in this ripple.
 */
@ExperimentalMaterial3Api
@Stable
fun ripple(
    color: ColorProducer,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    focusRingShape: Shape? = null,
    enablePressIndication: Boolean = true,
    enableFocusIndication: Boolean = true,
    enableHoverIndication: Boolean = true,
    enableDragIndication: Boolean = true,
): IndicationNodeFactory {
    return RippleNodeFactory(
        bounded = bounded,
        radius = radius,
        colorProducer = color,
        focusRingShape = focusRingShape,
        enablePressIndication = enablePressIndication,
        enableFocusIndication = enableFocusIndication,
        enableHoverIndication = enableHoverIndication,
        enableDragIndication = enableDragIndication,
    )
}

/**
 * Creates a Ripple using the provided values and values inferred from the theme.
 *
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s by
 * drawing ripple animations and state layers.
 *
 * A Ripple responds to [PressInteraction.Press] by starting a new ripple animation, and responds to
 * other [Interaction]s by showing a fixed state layer with varying alpha values depending on the
 * [Interaction].
 *
 * [MaterialTheme] provides Ripples using [androidx.compose.foundation.LocalIndication], so a Ripple
 * will be used as the default [Indication] inside components such as
 * [androidx.compose.foundation.clickable] and [androidx.compose.foundation.indication], in addition
 * to Material provided components that use a Ripple as well.
 *
 * You can also explicitly create a Ripple and provide it to custom components in order to change
 * the parameters from the default, such as to create an unbounded ripple with a fixed size.
 *
 * To create a Ripple with a static color, see the [ripple] overload with a [Color] parameter. This
 * overload is optimized for Ripples that have dynamic colors that change over time, to reduce
 * unnecessary recompositions.
 *
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have [RippleDefaults.RippleAlpha] applied to
 *   calculate the final color used to draw the ripple. If you are creating this [ColorProducer]
 *   outside of composition (where it will be automatically remembered), make sure that its instance
 *   is stable (such as by remembering the object that holds it), or remember the returned [ripple]
 *   object to make sure that ripple nodes are not being created each recomposition.
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
fun ripple(
    color: ColorProducer,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
): IndicationNodeFactory =
    ripple(
        color = color,
        bounded = bounded,
        radius = radius,
        focusRingShape = null,
        enablePressIndication = true,
        enableFocusIndication = true,
        enableHoverIndication = true,
        enableDragIndication = true,
    )

/** Default values used by [ripple]. */
object RippleDefaults {
    /**
     * Represents the default [RippleAlpha] that will be used for a ripple to indicate different
     * states.
     */
    val RippleAlpha: RippleAlpha =
        RippleAlpha(
            pressedAlpha = StateTokens.PressedStateLayerOpacity,
            focusedAlpha = StateTokens.FocusStateLayerOpacity,
            draggedAlpha = StateTokens.DraggedStateLayerOpacity,
            hoveredAlpha = StateTokens.HoverStateLayerOpacity,
        )

    /**
     * The default [RippleNodeConfig] that corresponds to a [RippleThemeConfiguration] opacity based
     * indication style.
     */
    @Suppress("ExperimentalPropertyAnnotation")
    @ExperimentalMaterial3Api
    val OpacityFocusRippleThemeConfiguration =
        RippleThemeConfiguration(RippleThemeConfiguration.Focus.Opacity())

    /**
     * The default [RippleNodeConfig] that corresponds to a [RippleThemeConfiguration] inset focus
     * ring based indication style.
     */
    @Suppress("ExperimentalPropertyAnnotation")
    @ExperimentalMaterial3Api
    val InsetFocusRingRippleThemeConfiguration =
        RippleThemeConfiguration(
            RippleThemeConfiguration.Focus.InsetRing(
                outerStrokeInset = 0.dp,
                outerStrokeWidth = 2.dp,
                innerStrokeInset = 1.dp,
                innerStrokeWidth = 3.dp,
            )
        )

    /** The default [RippleThemeConfiguration]. */
    @Suppress("ExperimentalPropertyAnnotation")
    @ExperimentalMaterial3Api
    val ThemeConfiguration: RippleThemeConfiguration = OpacityFocusRippleThemeConfiguration
}

/**
 * The overall [RippleThemeConfiguration] in use by all built-in components and [ripple].
 *
 * By default, this will be [RippleDefaults.ThemeConfiguration].
 *
 * The ripple configuration is resolved as follows:
 * - [LocalRippleThemeConfiguration] provides the highest-level theming configuration for ripples,
 *   including whether the focus indication is drawn by inset focus rings.
 * - [LocalRippleConfiguration] provides hierarchical per-ripple configuration for [ripple],
 *   including disabling ripples and their indications. This takes next highest priority.
 * - [ripple] parameters allow specifying configuration for individual indication callsites in
 *   components.
 */
@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalMaterial3Api
val LocalRippleThemeConfiguration: ProvidableCompositionLocal<RippleThemeConfiguration> =
    compositionLocalOf {
        RippleDefaults.ThemeConfiguration
    }

/**
 * The overall ripple theme in use by all built-in components and [ripple].
 *
 * This is controlled by [LocalRippleThemeConfiguration].
 *
 * The ripple configuration is resolved as follows:
 * - [LocalRippleThemeConfiguration] provides the highest-level theming configuration for ripples,
 *   including whether the focus indication is drawn by inset focus rings.
 * - [LocalRippleConfiguration] provides hierarchical per-ripple configuration for [ripple],
 *   including disabling ripples and their indications. This takes next highest priority.
 * - [ripple] parameters allow specifying configuration for individual indication callsites in
 *   components.
 *
 * @param focus the themable configuration for the focus indication.
 */
@ExperimentalMaterial3Api
class RippleThemeConfiguration(val focus: Focus) {
    /** The configuration options for the focus indication for [RippleThemeConfiguration]. */
    @ExperimentalMaterial3Api
    abstract class Focus private constructor() {
        /** An opacity-based focus indication. */
        @ExperimentalMaterial3Api
        class Opacity : Focus() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Opacity) return false

                return true
            }

            override fun hashCode(): Int = 1
        }

        /**
         * An inset ring focus indication. This is drawn using two strokes, an outer stroke and an
         * inner stroke.
         *
         * The inner stroke is drawn first, followed by the outer stroke.
         */
        @ExperimentalMaterial3Api
        class InsetRing(
            val outerStrokeInset: Dp,
            val outerStrokeWidth: Dp,
            val innerStrokeInset: Dp,
            val innerStrokeWidth: Dp,
        ) : Focus() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is InsetRing) return false

                if (outerStrokeInset != other.outerStrokeInset) return false
                if (outerStrokeWidth != other.outerStrokeWidth) return false
                if (innerStrokeInset != other.innerStrokeInset) return false
                if (innerStrokeWidth != other.innerStrokeWidth) return false

                return true
            }

            override fun hashCode(): Int {
                var result = outerStrokeInset.hashCode()
                result = 31 * result + outerStrokeWidth.hashCode()
                result = 31 * result + innerStrokeInset.hashCode()
                result = 31 * result + innerStrokeWidth.hashCode()
                return result
            }
        }
    }
}

/**
 * CompositionLocal used for providing [RippleConfiguration] down the tree. This acts as a
 * tree-local 'override' for ripples used inside components that you cannot directly control, such
 * as to change the color of a specific component's ripple, or disable it entirely by providing
 * `null`.
 *
 * In most cases you should rely on the default theme behavior for consistency with other
 * components. This exists as an escape hatch for individual components and is not intended to be
 * used for full theme customization across an application. For this use case you should instead
 * build your own custom ripple that queries your design system theme values directly using
 * [createRippleModifierNode].
 *
 * The ripple configuration is resolved as follows:
 * - [LocalRippleThemeConfiguration] provides the highest-level theming configuration for ripples,
 *   including whether the focus indication is drawn by inset focus rings.
 * - [LocalRippleConfiguration] provides hierarchical per-ripple configuration for [ripple],
 *   including disabling ripples and their indications. This takes next highest priority.
 * - [ripple] parameters allow specifying configuration for individual indication callsites in
 *   components.
 */
val LocalRippleConfiguration: ProvidableCompositionLocal<RippleConfiguration?> =
    compositionLocalWithComputedDefaultOf {
        RippleConfiguration()
    }

/**
 * Local per-ripple configuration for the [ripple] appearance, provided using
 * [LocalRippleConfiguration]. In most cases the default values should be used, for custom design
 * system use cases you should instead build your own custom ripple using
 * [createRippleModifierNode]. To disable the ripple completely, provide `null` using
 * [LocalRippleConfiguration].
 *
 * The ripple configuration is resolved as follows:
 * - [LocalRippleThemeConfiguration] provides the highest-level theming configuration for ripples,
 *   including whether the focus indication is drawn by inset focus rings.
 * - [LocalRippleConfiguration] provides hierarchical per-ripple configuration for [ripple],
 *   including disabling ripples and their indications. This takes next highest priority.
 * - [ripple] parameters allow specifying configuration for individual indication callsites in
 *   components.
 */
@Immutable
class RippleConfiguration
@OptIn(ExperimentalMaterial3Api::class)
internal constructor(val color: Color, val focus: Focus?, rippleAlpha: RippleAlpha?) {
    val rippleAlpha: RippleAlpha? = rippleAlpha

    /**
     * Local per-ripple configuration for [ripple] appearance, provided using
     * [LocalRippleConfiguration]. In most cases the default values should be used, for custom
     * design system use cases you should instead build your own custom ripple using
     * [createRippleModifierNode]. To disable the ripple completely, provide `null` using
     * [LocalRippleConfiguration].
     *
     * @param color the color override for the ripple. If [Color.Unspecified], then the default
     *   color from the theme will be used instead. Note that if the ripple has a color explicitly
     *   set with the parameter on [ripple], that will always be used instead of this value.
     */
    constructor(color: Color = Color.Unspecified) : this(color, null, null)

    /**
     * Local per-ripple configuration for [ripple] appearance, provided using
     * [LocalRippleConfiguration]. In most cases the default values should be used, for custom
     * design system use cases you should instead build your own custom ripple using
     * [createRippleModifierNode]. To disable the ripple completely, provide `null` using
     * [LocalRippleConfiguration].
     *
     * @param focus the focus indication override for this ripple. If null, then the default
     *   indication style will be used instead. This will only be applied if the style of focus
     *   indication matches the one set by [LocalRippleThemeConfiguration] (opacity vs inset focus
     *   ring)
     * @param color the color override for the ripple. If [Color.Unspecified], then the default
     *   color from the theme will be used instead. Note that if the ripple has a color explicitly
     *   set with the parameter on [ripple], that will always be used instead of this value.
     */
    @ExperimentalMaterial3Api
    constructor(
        focus: Focus?,
        color: Color = Color.Unspecified,
    ) : this(color = color, focus = focus, rippleAlpha = null)

    /**
     * Configuration for [ripple] appearance, provided using [LocalRippleConfiguration]. In most
     * cases the default values should be used, for custom design system use cases you should
     * instead build your own custom ripple using [createRippleModifierNode]. To disable the ripple
     * completely, provide `null` using [LocalRippleConfiguration].
     *
     * @param color the color override for the ripple. If [Color.Unspecified], then the default
     *   color from the theme will be used instead. Note that if the ripple has a color explicitly
     *   set with the parameter on [ripple], that will always be used instead of this value.
     * @param rippleAlpha the [RippleAlpha] override for this ripple. If null, then the default
     *   alpha will be used instead.
     */
    constructor(
        color: Color = Color.Unspecified,
        rippleAlpha: RippleAlpha? = null,
    ) : this(color = color, focus = null, rippleAlpha = rippleAlpha)

    /** The configuration options for the focus indication for [RippleConfiguration]. */
    @ExperimentalMaterial3Api
    abstract class Focus private constructor() {
        /** An opacity-based focus indication. */
        @ExperimentalMaterial3Api
        class Opacity : Focus() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Opacity) return false

                return true
            }

            override fun hashCode(): Int = 1
        }

        /**
         * An inset ring focus indication. This is drawn using two strokes, an outer stroke and an
         * inner stroke.
         *
         * The inner stroke is drawn first, followed by the outer stroke.
         *
         * @param outerStrokeColor the color of the outer stroke.
         * @param innerStrokeColor the color of the inner stroke.
         */
        @ExperimentalMaterial3Api
        class InsetRing(val outerStrokeColor: Color, val innerStrokeColor: Color) : Focus() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is InsetRing) return false

                if (outerStrokeColor != other.outerStrokeColor) return false
                if (innerStrokeColor != other.innerStrokeColor) return false

                return true
            }

            override fun hashCode(): Int {
                var result = outerStrokeColor.hashCode()
                result = 31 * result + innerStrokeColor.hashCode()
                return result
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleConfiguration) return false

        if (color != other.color) return false
        if (focus != other.focus) return false
        if (rippleAlpha != other.rippleAlpha) return false

        return true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + (focus?.hashCode() ?: 0)
        result = 31 * result + (rippleAlpha?.hashCode() ?: 0)
        return result
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun toString(): String {
        return "RippleConfiguration(color=$color, focus=$focus, rippleAlpha=$rippleAlpha)"
    }
}

@Stable
private class RippleNodeFactory
private constructor(
    private val bounded: Boolean,
    private val radius: Dp,
    private val colorProducer: ColorProducer?,
    private val color: Color,
    private val focusRingShape: Shape,
    private val enablePressIndication: Boolean,
    private val enableFocusIndication: Boolean,
    private val enableHoverIndication: Boolean,
    private val enableDragIndication: Boolean,
) : IndicationNodeFactory {
    constructor(
        bounded: Boolean,
        radius: Dp,
        colorProducer: ColorProducer,
        focusRingShape: Shape?,
        enablePressIndication: Boolean,
        enableFocusIndication: Boolean,
        enableHoverIndication: Boolean,
        enableDragIndication: Boolean,
    ) : this(
        bounded = bounded,
        radius = radius,
        colorProducer = colorProducer,
        color = Color.Unspecified,
        focusRingShape =
            focusRingShape
                ?: radius.takeIf { it != Dp.Unspecified }?.let { RoundedCornerShape(it) }
                ?: RectangleShape,
        enablePressIndication = enablePressIndication,
        enableFocusIndication = enableFocusIndication,
        enableHoverIndication = enableHoverIndication,
        enableDragIndication = enableDragIndication,
    )

    constructor(
        bounded: Boolean,
        radius: Dp,
        color: Color,
        focusRingShape: Shape?,
        enablePressIndication: Boolean,
        enableFocusIndication: Boolean,
        enableHoverIndication: Boolean,
        enableDragIndication: Boolean,
    ) : this(
        bounded = bounded,
        radius = radius,
        colorProducer = null,
        color = color,
        focusRingShape =
            focusRingShape
                ?: radius.takeIf { it != Dp.Unspecified }?.let { RoundedCornerShape(it) }
                ?: RectangleShape,
        enablePressIndication = enablePressIndication,
        enableFocusIndication = enableFocusIndication,
        enableHoverIndication = enableHoverIndication,
        enableDragIndication = enableDragIndication,
    )

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        val colorProducer = colorProducer ?: ColorProducer { color }
        return DelegatingThemeAwareRippleNode(
            interactionSource,
            bounded,
            radius,
            colorProducer,
            focusRingShape,
            enablePressIndication,
            enableFocusIndication,
            enableHoverIndication,
            enableDragIndication,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleNodeFactory) return false

        if (bounded != other.bounded) return false
        if (radius != other.radius) return false
        if (colorProducer != other.colorProducer) return false
        if (color != other.color) return false
        if (focusRingShape != other.focusRingShape) return false
        if (enablePressIndication != other.enablePressIndication) return false
        if (enableFocusIndication != other.enableFocusIndication) return false
        if (enableHoverIndication != other.enableHoverIndication) return false
        if (enableDragIndication != other.enableDragIndication) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bounded.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + colorProducer.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + focusRingShape.hashCode()
        result = 31 * result + enablePressIndication.hashCode()
        result = 31 * result + enableFocusIndication.hashCode()
        result = 31 * result + enableHoverIndication.hashCode()
        result = 31 * result + enableDragIndication.hashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class DelegatingThemeAwareRippleNode(
    private val interactionSource: InteractionSource,
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: ColorProducer,
    private val focusRingShape: Shape,
    private val enablePressIndication: Boolean,
    private val enableFocusIndication: Boolean,
    private val enableHoverIndication: Boolean,
    private val enableDragIndication: Boolean,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var rippleNode: DelegatableNode? = null

    override fun onAttach() {
        updateConfiguration()
    }

    override fun onObservedReadsChanged() {
        updateConfiguration()
    }

    /**
     * Handles [LocalRippleConfiguration] changing between null / non-null. Changes to
     * [RippleConfiguration.color] and [RippleConfiguration.rippleAlpha] are handled as part of the
     * ripple definition.
     */
    private fun updateConfiguration() {
        observeReads {
            val configuration = currentValueOf(LocalRippleConfiguration)
            if (configuration == null) {
                removeRipple()
            } else {
                if (rippleNode == null) attachNewRipple()
            }
        }
    }

    private fun attachNewRipple() {
        val calculateColor = ColorProducer {
            val userDefinedColor = color()
            if (userDefinedColor.isSpecified) {
                userDefinedColor
            } else {
                // If this is null, the ripple will be removed, so this should always be non-null in
                // normal use
                val rippleConfiguration = currentValueOf(LocalRippleConfiguration)
                if (rippleConfiguration?.color?.isSpecified == true) {
                    rippleConfiguration.color
                } else {
                    currentValueOf(LocalContentColor)
                }
            }
        }
        val calculateOuterStrokeColor = ColorProducer {
            val rippleConfiguration = currentValueOf(LocalRippleConfiguration)

            if (rippleConfiguration?.focus is RippleConfiguration.Focus.InsetRing) {
                rippleConfiguration.focus.outerStrokeColor
            } else {
                currentValueOf(MaterialTheme.LocalMaterialTheme).colorScheme.secondary
            }
        }
        val calculateInnerStrokeColor = ColorProducer {
            val rippleConfiguration = currentValueOf(LocalRippleConfiguration)

            if (rippleConfiguration?.focus is RippleConfiguration.Focus.InsetRing) {
                rippleConfiguration.focus.innerStrokeColor
            } else {
                currentValueOf(MaterialTheme.LocalMaterialTheme).colorScheme.onSecondary
            }
        }

        val calculateRippleNodeConfig = {
            val motionScheme = currentValueOf(MaterialTheme.LocalMaterialTheme).motionScheme
            val rippleThemeConfiguration = currentValueOf(LocalRippleThemeConfiguration)

            // If this is null, the ripple will be removed, so this should always be non-null in
            // normal use
            val rippleConfiguration = currentValueOf(LocalRippleConfiguration)

            RippleNodeConfig(
                press =
                    if (enablePressIndication) {
                        @Suppress("DEPRECATION")
                        RippleNodeConfig.Press.Opacity(
                            rippleConfiguration?.rippleAlpha?.pressedAlpha
                                ?: StateTokens.PressedStateLayerOpacity
                        )
                    } else {
                        RippleNodeConfig.Press.None
                    },
                focus =
                    if (enableFocusIndication) {
                        when (rippleThemeConfiguration.focus) {
                            is RippleThemeConfiguration.Focus.Opacity ->
                                @Suppress("DEPRECATION")
                                RippleNodeConfig.Focus.Opacity(
                                    rippleConfiguration?.rippleAlpha?.focusedAlpha
                                        ?: StateTokens.FocusStateLayerOpacity
                                )
                            is RippleThemeConfiguration.Focus.InsetRing ->
                                RippleNodeConfig.Focus.InsetRing(
                                    shape = focusRingShape,
                                    outerStrokeInset =
                                        rippleThemeConfiguration.focus.outerStrokeInset,
                                    outerStrokeWidth =
                                        rippleThemeConfiguration.focus.outerStrokeWidth,
                                    outerStrokeColor = calculateOuterStrokeColor,
                                    innerStrokeInset =
                                        rippleThemeConfiguration.focus.innerStrokeInset,
                                    innerStrokeWidth =
                                        rippleThemeConfiguration.focus.innerStrokeWidth,
                                    innerStrokeColor = calculateInnerStrokeColor,
                                    focusingAnimationSpec = motionScheme.fastSpatialSpec(),
                                    unfocusingAnimationSpec = motionScheme.fastSpatialSpec(),
                                )
                            else -> error("Unknown focus ripple theme configuration")
                        }
                    } else {
                        RippleNodeConfig.Focus.None
                    },
                hover =
                    if (enableHoverIndication) {
                        @Suppress("DEPRECATION")
                        RippleNodeConfig.Hover.Opacity(
                            rippleConfiguration?.rippleAlpha?.hoveredAlpha
                                ?: StateTokens.HoverStateLayerOpacity
                        )
                    } else {
                        RippleNodeConfig.Hover.None
                    },
                drag =
                    if (enableDragIndication) {
                        @Suppress("DEPRECATION")
                        RippleNodeConfig.Drag.Opacity(
                            rippleConfiguration?.rippleAlpha?.draggedAlpha
                                ?: StateTokens.DraggedStateLayerOpacity
                        )
                    } else {
                        RippleNodeConfig.Drag.None
                    },
            )
        }

        rippleNode =
            delegate(
                createRippleModifierNode(
                    interactionSource = interactionSource,
                    bounded = bounded,
                    radius = radius,
                    color = calculateColor,
                    rippleNodeConfig = calculateRippleNodeConfig,
                )
            )
    }

    private fun removeRipple() {
        rippleNode?.let { undelegate(it) }
        rippleNode = null
    }
}

private val DefaultBoundedRipple =
    RippleNodeFactory(
        bounded = true,
        radius = Dp.Unspecified,
        color = Color.Unspecified,
        focusRingShape = null,
        enablePressIndication = true,
        enableFocusIndication = true,
        enableHoverIndication = true,
        enableDragIndication = true,
    )

private val DefaultUnboundedRipple =
    RippleNodeFactory(
        bounded = false,
        radius = Dp.Unspecified,
        color = Color.Unspecified,
        focusRingShape = null,
        enablePressIndication = true,
        enableFocusIndication = true,
        enableHoverIndication = true,
        enableDragIndication = true,
    )
