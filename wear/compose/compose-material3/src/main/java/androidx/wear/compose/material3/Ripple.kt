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

package androidx.wear.compose.material3

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ripple.RippleNodeConfiguration
import androidx.compose.material3.ripple.createRippleModifierNode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Creates a Ripple using the provided values and values inferred from the [MaterialTheme].
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
 * Examples of enabling and customizing inset focus rings for these default Ripples:
 *
 * @sample androidx.wear.compose.material3.samples.EnableInsetFocusRingSample
 * @sample androidx.wear.compose.material3.samples.CustomizeInsetFocusRingSample
 *
 * Examples of explicitly created Ripples drawing inset focus rings with a [focusRingShape]:
 *
 * @sample androidx.wear.compose.material3.samples.ClickableWithFocusRingShapeSample
 * @sample androidx.wear.compose.material3.samples.FocusableWithFocusRingShapeSample
 * @sample androidx.wear.compose.material3.samples.BorderedClickableWithFocusRingShapeSample
 * @sample androidx.wear.compose.material3.samples.ClickableWithDynamicFocusRingShapeSample
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 *   This bounding only applies to any opacity-based layers that are drawn, and doesn't affect inset
 *   focus rings, if in use.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have alpha applied to calculate the final
 *   color used to draw the ripple. If [Color.Unspecified] is provided the color used will be
 *   [LocalContentColor] instead. If inset focus rings are enabled, their colors will be provided by
 *   the theme or by an overridden [LocalRippleConfiguration].
 * @param focusRingShape this parameter is only used if inset focus rings are enabled, specifically
 *   when the [RippleThemeConfiguration.focus] from [LocalRippleThemeConfiguration] is
 *   [RippleThemeConfiguration.Focus.InsetRing]. When inset focus rings are enabled, the focus ring
 *   shape will define the shape of the ripple that the focus ring indication will use if specified.
 *   If left `null`, a default shape will be used, which will be a rounded rectangle based on the
 *   [radius] if specified, otherwise it will be just a [RectangleShape]. This [Shape] instance must
 *   remain the same during the lifetime of the ripple in composition. If the [Shape] needs to
 *   change, delegate from a single instance to the changing shape to preserve the instance
 *   requirement.
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
@Stable
public fun ripple(
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
 * Creates a Ripple using the provided values and values inferred from the [MaterialTheme].
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
 *   This bounding only applies to any opacity-based layers that are drawn, and doesn't affect inset
 *   focus rings, if in use.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have alpha applied to calculate the final
 *   color used to draw the ripple. If [Color.Unspecified] is provided the color used will be
 *   [LocalContentColor] instead.
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Stable
public fun ripple(
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
 * Creates a Ripple using the provided values and values inferred from the [MaterialTheme].
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
 * @sample androidx.wear.compose.material3.samples.ClickableWithDynamicFocusRingShapeSample
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have alpha applied to calculate the final
 *   color used to draw the ripple. If you are creating this [ColorProducer] outside of composition
 *   (where it will be automatically remembered), make sure that its instance is stable (such as by
 *   remembering the object that holds it), or remember the returned [ripple] object to make sure
 *   that ripple nodes are not being created each recomposition. If inset focus rings are enabled,
 *   their colors will be provided by the theme or by an overridden [LocalRippleConfiguration].
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 *   This bounding only applies to any opacity-based layers that are drawn, and doesn't affect inset
 *   focus rings, if in use.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param focusRingShape this parameter is only used if inset focus rings are enabled, specifically
 *   when the [RippleThemeConfiguration.focus] from [LocalRippleThemeConfiguration] is
 *   [RippleThemeConfiguration.Focus.InsetRing]. When inset focus rings are enabled, the focus ring
 *   shape will define the shape of the ripple that the focus ring indication will use if specified.
 *   If left `null`, a default shape will be used, which will be a rounded rectangle based on the
 *   [radius] if specified, otherwise it will be just a [RectangleShape]. This [Shape] instance must
 *   remain the same during the lifetime of the ripple in composition. If the [Shape] needs to
 *   change, delegate from a single instance to the changing shape to preserve the instance
 *   requirement.
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
@Stable
public fun ripple(
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
 * Creates a Ripple using the provided values and values inferred from the [MaterialTheme].
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
 *   iconography in the component. This color will then have alpha applied to calculate the final
 *   color used to draw the ripple. If you are creating this [ColorProducer] outside of composition
 *   (where it will be automatically remembered), make sure that its instance is stable (such as by
 *   remembering the object that holds it), or remember the returned [ripple] object to make sure
 *   that ripple nodes are not being created each recomposition.
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 *   This bounding only applies to any opacity-based layers that are drawn, and doesn't affect inset
 *   focus rings, if in use.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Stable
public fun ripple(
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
public object RippleDefaults {
    /** A [RippleThemeConfiguration] that corresponds to an opacity based indication style. */
    public val OpacityFocusThemeConfiguration: RippleThemeConfiguration =
        RippleThemeConfiguration(RippleThemeConfiguration.Focus.Opacity)

    /**
     * A [RippleThemeConfiguration] that corresponds to an inset focus ring based indication style.
     */
    public val InsetFocusRingThemeConfiguration: RippleThemeConfiguration =
        RippleThemeConfiguration(
            RippleThemeConfiguration.Focus.InsetRing(
                outerStrokeInset = 0.dp,
                outerStrokeWidth = 2.dp,
                innerStrokeInset = 0.dp,
                innerStrokeWidth = 4.dp,
            )
        )

    /** The default [RippleThemeConfiguration]. */
    public val ThemeConfiguration: RippleThemeConfiguration = OpacityFocusThemeConfiguration
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
 *
 * @sample androidx.wear.compose.material3.samples.EnableInsetFocusRingSample
 * @sample androidx.wear.compose.material3.samples.CustomizeInsetFocusRingSample
 */
public val LocalRippleThemeConfiguration: ProvidableCompositionLocal<RippleThemeConfiguration> =
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
@Immutable
public class RippleThemeConfiguration(public val focus: Focus) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleThemeConfiguration) return false
        return focus == other.focus
    }

    override fun hashCode(): Int = focus.hashCode()

    override fun toString(): String = "RippleThemeConfiguration(focus=$focus)"

    /** The configuration options for the focus indication for [RippleThemeConfiguration]. */
    @Immutable
    public sealed class Focus private constructor() {
        /** An opacity-based focus indication. */
        public object Opacity : Focus() {
            override fun toString(): String = "RippleThemeConfiguration.Focus.Opacity"
        }

        /**
         * An inset ring focus indication. This is drawn using two strokes, an outer stroke and an
         * inner stroke. The inner stroke is drawn first, followed by the outer stroke.
         *
         * All stroke insets and widths must be non-negative, and the outer and inner strokes must
         * overlap such that the outer stroke is not inset further than the inner stroke
         * (`outerStrokeInset <= innerStrokeInset`), the inner stroke starts before or at the inner
         * edge of the outer stroke (`innerStrokeInset <= outerStrokeInset + outerStrokeWidth`), and
         * the inner stroke extends at least as far inward as the outer stroke (`outerStrokeInset +
         * outerStrokeWidth <= innerStrokeInset + innerStrokeWidth`).
         *
         * As this describes the overall ripple theme, and the inset ring colors are use-site
         * specific, the colors are configured in [RippleConfiguration.Focus.InsetRing] instead.
         *
         * @param outerStrokeInset the amount the outer stroke is inset
         * @param outerStrokeWidth the width of the outer stroke
         * @param innerStrokeInset the amount the inner stroke is inset
         * @param innerStrokeWidth the width of the inner stroke
         */
        public class InsetRing(
            public val outerStrokeInset: Dp,
            public val outerStrokeWidth: Dp,
            public val innerStrokeInset: Dp,
            public val innerStrokeWidth: Dp,
        ) : Focus() {
            init {
                require(outerStrokeInset >= 0.dp) {
                    "outerStrokeInset must be non-negative: outerStrokeInset=$outerStrokeInset"
                }
                require(outerStrokeWidth >= 0.dp) {
                    "outerStrokeWidth must be non-negative: outerStrokeWidth=$outerStrokeWidth"
                }
                require(innerStrokeInset >= 0.dp) {
                    "innerStrokeInset must be non-negative: innerStrokeInset=$innerStrokeInset"
                }
                require(innerStrokeWidth >= 0.dp) {
                    "innerStrokeWidth must be non-negative: innerStrokeWidth=$innerStrokeWidth"
                }
                require(outerStrokeInset <= innerStrokeInset) {
                    "outerStrokeInset ($outerStrokeInset) must be <= innerStrokeInset " +
                        "($innerStrokeInset) so that the outer stroke does not start further " +
                        "inward than the inner stroke"
                }
                require(innerStrokeInset <= outerStrokeInset + outerStrokeWidth) {
                    "innerStrokeInset ($innerStrokeInset) must be <= outerStrokeInset + " +
                        "outerStrokeWidth (${outerStrokeInset + outerStrokeWidth}) to prevent a " +
                        "gap between the inner and outer strokes"
                }
                require(
                    outerStrokeInset + outerStrokeWidth <= innerStrokeInset + innerStrokeWidth
                ) {
                    "outerStrokeInset + outerStrokeWidth (${outerStrokeInset + outerStrokeWidth}) " +
                        "must be <= innerStrokeInset + innerStrokeWidth " +
                        "(${innerStrokeInset + innerStrokeWidth}) so that the inner stroke " +
                        "extends at least as far inward as the outer stroke"
                }
            }

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

            override fun toString(): String =
                "RippleThemeConfiguration.Focus.InsetRing(" +
                    "outerStrokeInset=$outerStrokeInset, " +
                    "outerStrokeWidth=$outerStrokeWidth, " +
                    "innerStrokeInset=$innerStrokeInset, " +
                    "innerStrokeWidth=$innerStrokeWidth)"
        }

        private object NonExhaustive : Focus()
    }
}

/**
 * The [RippleThemeConfiguration.Focus.InsetRing] to draw focus rings with, or null if inset focus
 * rings are not in use, either because they are disabled by
 * [WearComposeMaterial3Flags.isInsetFocusRingEnabled], or because this configuration uses a
 * different style of focus indication.
 */
@OptIn(ExperimentalWearComposeMaterial3Api::class)
internal val RippleThemeConfiguration.insetFocusRing: RippleThemeConfiguration.Focus.InsetRing?
    get() =
        if (WearComposeMaterial3Flags.isInsetFocusRingEnabled) {
            focus as? RippleThemeConfiguration.Focus.InsetRing
        } else {
            null
        }

/**
 * Draws an inset focus ring indication for [shape] when inset focus rings are enabled by
 * [LocalRippleThemeConfiguration], preserving active focus when [shape] changes dynamically.
 */
@Composable
internal fun Modifier.insetFocusRing(
    interactionSource: InteractionSource?,
    shape: Shape,
): Modifier {
    if (interactionSource == null || LocalRippleThemeConfiguration.current.insetFocusRing == null) {
        return this
    }
    return this.indication(
        interactionSource = interactionSource,
        indication =
            ripple(
                focusRingShape = rememberDelegatingShape(shape),
                enablePressIndication = false,
                enableFocusIndication = true,
                enableDragIndication = false,
                enableHoverIndication = false,
            ),
    )
}

/**
 * Remembers a [Shape] with stable identity that delegates outline creation to the latest [shape]
 * State.
 *
 * Passing a dynamically changing [Shape] (such as an animated shape or a split-section shape that
 * updates on press) directly to [ripple] would create a new [IndicationNodeFactory] instance on
 * each change, causing `Modifier.indication` to recreate the underlying ripple node and lose active
 * focus or press state. Wrapping [shape] with [rememberDelegatingShape] keeps the [Shape] reference
 * stable across recompositions while still reading the updated [shape] during draw.
 */
@Composable
internal fun rememberDelegatingShape(shape: Shape): Shape {
    val currentShape by rememberUpdatedState(shape)
    // Intentionally not keyed on [shape]: the returned Shape instance must keep the same identity
    // across shape changes so RippleNodeFactory.equals remains true and does not recreate the
    // RippleNode.
    return remember {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline = currentShape.createOutline(size, layoutDirection, density)
        }
    }
}

/**
 * CompositionLocal used for providing [RippleConfiguration] down the tree. This acts as a
 * tree-local 'override' for ripples used inside components that you cannot directly control, such
 * as to change the color of a specific component's ripple, or disable it entirely by providing
 * `null`.
 *
 * In most cases you should rely on the default theme behavior for consistency with other components
 * - this exists as an escape hatch for individual components and is not intended to be used for
 *   full theme customization across an application. For this use case you should instead build your
 *   own custom ripple that queries your design system theme values directly using
 *   [createRippleModifierNode].
 *
 * The ripple configuration is resolved as follows:
 * - [LocalRippleThemeConfiguration] provides the highest-level theming configuration for ripples,
 *   including whether the focus indication is drawn by inset focus rings.
 * - [LocalRippleConfiguration] provides hierarchical per-ripple configuration for [ripple],
 *   including disabling ripples and their indications. This takes next highest priority.
 * - [ripple] parameters allow specifying configuration for individual indication callsites in
 *   components.
 */
public val LocalRippleConfiguration: ProvidableCompositionLocal<RippleConfiguration?> =
    compositionLocalOf {
        RippleConfiguration()
    }

/**
 * Configuration for [ripple] appearance, provided using [LocalRippleConfiguration]. In most cases
 * the default values should be used, for custom design system use cases you should instead build
 * your own custom ripple using [createRippleModifierNode]. To disable the ripple, provide `null`
 * using [LocalRippleConfiguration].
 *
 * The ripple configuration is resolved as follows:
 * - [LocalRippleThemeConfiguration] provides the highest-level theming configuration for ripples,
 *   including whether the focus indication is drawn by inset focus rings.
 * - [LocalRippleConfiguration] provides hierarchical per-ripple configuration for [ripple],
 *   including disabling ripples and their indications. This takes next highest priority.
 * - [ripple] parameters allow specifying configuration for individual indication callsites in
 *   components.
 *
 * @sample androidx.wear.compose.material3.samples.CustomizeInsetFocusRingSample
 * @property color the color override for the ripple. If [Color.Unspecified], then the default color
 *   from the theme will be used instead. Note that if the ripple has a color explicitly set with
 *   the parameter on [ripple], that will always be used instead of this value.
 * @property focus the focus indication override for this ripple. If null, then the default
 *   indication style will be used instead. This will only be applied if the style of focus
 *   indication matches the one set by [LocalRippleThemeConfiguration] (opacity vs inset focus
 *   ring). If the style doesn't match, then this configuration will be ignored and the default
 *   indication style will be used instead.
 */
@Immutable
public class RippleConfiguration
internal constructor(
    public val color: Color,
    public val focus: Focus?,
    rippleAlpha: RippleAlpha?,
) {
    /** The [RippleAlpha] used to configure the opacity levels of the per-ripple configuration. */
    @Deprecated(
        "Use other configuration options in RippleThemeConfiguration, RippleConfiguration and ripple. " +
            "Changing whether focus is displayed by opacity or inset focus rings is changed by providing " +
            "different values with LocalRippleThemeConfiguration. Disabling individual types of interactions " +
            "are displayed by a specific ripple can be done with the boolean parameters on ripple."
    )
    public val rippleAlpha: RippleAlpha? = rippleAlpha

    /**
     * Configuration for [ripple] appearance, provided using [LocalRippleConfiguration]. In most
     * cases the default values should be used, for custom design system use cases you should
     * instead build your own custom ripple using [createRippleModifierNode]. To disable the ripple,
     * provide `null` using [LocalRippleConfiguration].
     *
     * @param color the color override for the ripple. If [Color.Unspecified], then the default
     *   color from the theme will be used instead. Note that if the ripple has a color explicitly
     *   set with the parameter on [ripple], that will always be used instead of this value.
     */
    public constructor(color: Color = Color.Unspecified) : this(color, null, null)

    /**
     * Configuration for [ripple] appearance, provided using [LocalRippleConfiguration]. In most
     * cases the default values should be used, for custom design system use cases you should
     * instead build your own custom ripple using [createRippleModifierNode]. To disable the ripple,
     * provide `null` using [LocalRippleConfiguration].
     *
     * @param focus the focus indication override for this ripple. If null, then the default
     *   indication style will be used instead. This will only be applied if the style of focus
     *   indication matches the one set by [LocalRippleThemeConfiguration] (opacity vs inset focus
     *   ring).
     * @param color the color override for the ripple. If [Color.Unspecified], then the default
     *   color from the theme will be used instead. Note that if the ripple has a color explicitly
     *   set with the parameter on [ripple], that will always be used instead of this value.
     */
    public constructor(
        focus: Focus?,
        color: Color = Color.Unspecified,
    ) : this(color = color, focus = focus, rippleAlpha = null)

    /**
     * Configuration for [ripple] appearance, provided using [LocalRippleConfiguration]. In most
     * cases the default values should be used, for custom design system use cases you should
     * instead build your own custom ripple using [createRippleModifierNode]. To disable the ripple,
     * provide `null` using [LocalRippleConfiguration].
     *
     * @param color the color override for the ripple. If [Color.Unspecified], then the default
     *   color from the theme will be used instead. Note that if the ripple has a color explicitly
     *   set with the parameter on [ripple], that will always be used instead of this value.
     * @param rippleAlpha the [RippleAlpha] override for this ripple. If null, then the default
     *   alpha will be used instead.
     */
    @Deprecated(
        "Use other configuration options in RippleThemeConfiguration, RippleConfiguration and ripple. " +
            "Changing whether focus is displayed by opacity or inset focus rings is changed by providing " +
            "different values with LocalRippleThemeConfiguration. Disabling individual types of interactions " +
            "are displayed by a specific ripple can be done with the boolean parameters on ripple."
    )
    public constructor(
        color: Color = Color.Unspecified,
        rippleAlpha: RippleAlpha? = null,
    ) : this(color = color, focus = null, rippleAlpha = rippleAlpha)

    /** The configuration options for the focus indication for [RippleConfiguration]. */
    @Immutable
    public sealed class Focus private constructor() {
        /**
         * An opacity-based focus indication.
         *
         * This describes a use-site specific override for when opacity layer-based focus visuals
         * are used. Currently, there are no configuration options for opacity.
         */
        public object Opacity : Focus() {
            override fun toString(): String = "RippleConfiguration.Focus.Opacity"
        }

        /**
         * An inset ring focus indication. This is drawn using two strokes, an outer stroke and an
         * inner stroke. The inner stroke is drawn first, followed by the outer stroke.
         *
         * As this describes a use-site specific override, this provides coloring overrides for an
         * inset focus ring being drawn by an individual component. The inset ring dimensions are
         * controlled by [RippleThemeConfiguration.Focus.InsetRing] instead.
         *
         * @param outerStrokeColor the color of the outer stroke. If [Color.Unspecified], then the
         *   default color from the theme will be used instead.
         * @param innerStrokeColor the color of the inner stroke. If [Color.Unspecified], then the
         *   default color from the theme will be used instead.
         */
        public class InsetRing(
            public val outerStrokeColor: Color = Color.Unspecified,
            public val innerStrokeColor: Color = Color.Unspecified,
        ) : Focus() {
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

            override fun toString(): String =
                "RippleConfiguration.Focus.InsetRing(" +
                    "outerStrokeColor=$outerStrokeColor, " +
                    "innerStrokeColor=$innerStrokeColor)"
        }

        private object NonExhaustive : Focus()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleConfiguration) return false

        if (color != other.color) return false
        if (focus != other.focus) return false
        @Suppress("DEPRECATION") if (rippleAlpha != other.rippleAlpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + (focus?.hashCode() ?: 0)
        @Suppress("DEPRECATION")
        result = 31 * result + (rippleAlpha?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        @Suppress("DEPRECATION")
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

    // Cache variables for RippleNodeConfig and its component objects
    private var cachedRippleConfiguration: RippleConfiguration? = null
    private var cachedRippleThemeConfiguration: RippleThemeConfiguration? = null
    private var cachedInsetFocusRing: RippleThemeConfiguration.Focus.InsetRing? = null
    private var cachedMotionScheme: MotionScheme? = null

    private var cachedRippleNodeConfiguration: RippleNodeConfiguration? = null

    // State for fading the outer stroke of the focus ring after a period of inactivity
    private var isFaded by mutableStateOf(false)
    private var interactionsJob: Job? = null
    private var inactivityJob: Job? = null
    private val focuses = mutableListOf<FocusInteraction.Focus>()
    private val presses = mutableListOf<PressInteraction.Press>()

    // Calculation lambdas for the RippleConfiguration
    private val calculateColor = ColorProducer {
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
    private val calculateOuterStrokeColor = ColorProducer {
        val insetRing =
            currentValueOf(LocalRippleConfiguration)?.focus as? RippleConfiguration.Focus.InsetRing
        (insetRing?.outerStrokeColor ?: Color.Unspecified).takeOrElse {
            currentValueOf(LocalColorScheme).complementaryColor
        }
    }
    private val calculateInnerStrokeColor = ColorProducer {
        val insetRing =
            currentValueOf(LocalRippleConfiguration)?.focus as? RippleConfiguration.Focus.InsetRing
        (insetRing?.innerStrokeColor ?: Color.Unspecified).takeOrElse {
            currentValueOf(LocalColorScheme).background
        }
    }
    // The outer stroke color, faded while the focus ring has been inactive for a while.
    private val calculateFadedOuterStrokeColor = ColorProducer {
        val outerStrokeColor = calculateOuterStrokeColor()
        if (isFaded) {
            outerStrokeColor.copy(alpha = outerStrokeColor.alpha * FocusRingFadedAlpha)
        } else {
            outerStrokeColor
        }
    }

    private val calculateRippleNodeConfiguration = {
        val motionScheme = currentValueOf(LocalMotionScheme)
        val rippleThemeConfiguration = currentValueOf(LocalRippleThemeConfiguration)
        val insetFocusRing = rippleThemeConfiguration.insetFocusRing

        // If this is null, the ripple will be removed, so this should always be non-null in
        // normal use
        val rippleConfiguration = currentValueOf(LocalRippleConfiguration)

        val currentCachedRippleNodeConfiguration = cachedRippleNodeConfiguration

        // Simple caching: we pull out all immutable objects that could change the resulting
        // ripple node config. If we have a cached ripple node config and none of the immutable
        // inputs changed, we can just keep using the cached ripple node config.
        // Otherwise, we recreate the ripple node config and all of its components.
        // Note that for some inputs (shape, bounded, radius, enabled***Indication) we recreate
        // the entire node, so we don't have to worry about comparing those inputs.
        if (
            currentCachedRippleNodeConfiguration != null &&
                cachedRippleThemeConfiguration == rippleThemeConfiguration &&
                cachedInsetFocusRing == insetFocusRing &&
                cachedRippleConfiguration == rippleConfiguration &&
                cachedMotionScheme == motionScheme
        ) {
            currentCachedRippleNodeConfiguration
        } else {
            val pressConfiguration =
                if (enablePressIndication) {
                    RippleNodeConfiguration.PressConfiguration.Opacity(
                        @Suppress("DEPRECATION") rippleConfiguration?.rippleAlpha?.pressedAlpha
                            ?: RippleAlpha.pressedAlpha
                    )
                } else {
                    RippleNodeConfiguration.PressConfiguration.None
                }

            val focusConfiguration =
                if (enableFocusIndication) {
                    when (insetFocusRing) {
                        null -> {
                            @Suppress("DEPRECATION")
                            val focusedAlpha =
                                rippleConfiguration?.rippleAlpha?.focusedAlpha
                                    ?: RippleAlpha.focusedAlpha
                            RippleNodeConfiguration.FocusConfiguration.Opacity(focusedAlpha)
                        }
                        else -> {
                            val outerStrokeInset = insetFocusRing.outerStrokeInset
                            val outerStrokeWidth = insetFocusRing.outerStrokeWidth
                            val innerStrokeInset = insetFocusRing.innerStrokeInset
                            val innerStrokeWidth = insetFocusRing.innerStrokeWidth
                            val focusingAnimationSpec = motionScheme.fastSpatialSpec<Float>()
                            val unfocusingAnimationSpec = motionScheme.fastEffectsSpec<Float>()

                            RippleNodeConfiguration.FocusConfiguration.InsetRing(
                                shape = focusRingShape,
                                outerStrokeInset = outerStrokeInset,
                                outerStrokeWidth = outerStrokeWidth,
                                outerStrokeColor = calculateFadedOuterStrokeColor,
                                innerStrokeInset = innerStrokeInset,
                                innerStrokeWidth = innerStrokeWidth,
                                innerStrokeColor = calculateInnerStrokeColor,
                                focusingAnimationSpec = focusingAnimationSpec,
                                unfocusingAnimationSpec = unfocusingAnimationSpec,
                            )
                        }
                    }
                } else {
                    RippleNodeConfiguration.FocusConfiguration.None
                }

            val hoverConfiguration =
                if (enableHoverIndication) {
                    RippleNodeConfiguration.HoverConfiguration.Opacity(
                        @Suppress("DEPRECATION") rippleConfiguration?.rippleAlpha?.hoveredAlpha
                            ?: RippleAlpha.hoveredAlpha
                    )
                } else {
                    RippleNodeConfiguration.HoverConfiguration.None
                }

            val dragConfiguration =
                if (enableDragIndication) {
                    RippleNodeConfiguration.DragConfiguration.Opacity(
                        @Suppress("DEPRECATION") rippleConfiguration?.rippleAlpha?.draggedAlpha
                            ?: RippleAlpha.draggedAlpha
                    )
                } else {
                    RippleNodeConfiguration.DragConfiguration.None
                }

            val newConfig =
                RippleNodeConfiguration(
                    isBounded = bounded,
                    radius = radius,
                    color = calculateColor,
                    pressConfiguration = pressConfiguration,
                    focusConfiguration = focusConfiguration,
                    hoverConfiguration = hoverConfiguration,
                    dragConfiguration = dragConfiguration,
                )

            // Update the caches
            cachedRippleNodeConfiguration = newConfig
            cachedRippleThemeConfiguration = rippleThemeConfiguration
            cachedInsetFocusRing = insetFocusRing
            cachedRippleConfiguration = rippleConfiguration
            cachedMotionScheme = motionScheme

            newConfig
        }
    }

    override fun onAttach() {
        updateConfiguration()
    }

    override fun onObservedReadsChanged() {
        updateConfiguration()
    }

    override fun onDetach() {
        stopTrackingInteractions()
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
                // Interactions are only tracked for inset focus rings, so we skip this otherwise.
                // Note: LocalRippleThemeConfiguration is not expected to change while an element
                // is actively focused, as InteractionSource.interactions does not replay active
                // interactions.
                if (
                    enableFocusIndication &&
                        currentValueOf(LocalRippleThemeConfiguration).insetFocusRing != null
                ) {
                    startTrackingInteractions()
                } else {
                    stopTrackingInteractions()
                }
            }
        }
    }

    private fun attachNewRipple() {
        rippleNode =
            delegate(
                createRippleModifierNode(
                    interactionSource = interactionSource,
                    rippleNodeConfiguration = calculateRippleNodeConfiguration,
                )
            )
    }

    private fun removeRipple() {
        stopTrackingInteractions()
        rippleNode?.let { undelegate(it) }
        rippleNode = null
    }

    private fun startTrackingInteractions() {
        if (interactionsJob != null) return

        interactionsJob = coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> {
                        focuses.add(interaction)
                        isFaded = false
                        restartInactivityTimer()
                    }
                    is FocusInteraction.Unfocus -> {
                        focuses.remove(interaction.focus)
                        // Stay faded, so the stroke doesn't flash back while the ring animates
                        // out.
                        if (focuses.isEmpty()) cancelInactivityTimer()
                    }
                    is PressInteraction.Press -> {
                        presses.add(interaction)
                        isFaded = false
                        cancelInactivityTimer()
                    }
                    is PressInteraction.Release -> onPressEnded(interaction.press)
                    is PressInteraction.Cancel -> onPressEnded(interaction.press)
                }
            }
        }
    }

    private fun stopTrackingInteractions() {
        if (interactionsJob == null) return

        interactionsJob?.cancel()
        interactionsJob = null
        cancelInactivityTimer()
        focuses.clear()
        presses.clear()
        isFaded = false
    }

    private fun onPressEnded(press: PressInteraction.Press) {
        presses.remove(press)
        restartInactivityTimer()
    }

    private fun restartInactivityTimer() {
        cancelInactivityTimer()
        if (focuses.isNotEmpty() && presses.isEmpty()) {
            inactivityJob = coroutineScope.launch {
                delay(FocusRingFadeTimeout)
                isFaded = true
            }
        }
    }

    private fun cancelInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
    }
}

@Suppress("DEPRECATION")
private val RippleAlpha: RippleAlpha =
    RippleAlpha(
        pressedAlpha = 0.10f,
        focusedAlpha = 0.10f,
        draggedAlpha = 0.16f,
        hoveredAlpha = 0.08f,
    )

/** The alpha of the outer stroke of the focus ring, when faded due to inactivity. */
private const val FocusRingFadedAlpha = 0.5f

/** The duration of inactivity while focused, after which the outer stroke of the ring fades. */
internal val FocusRingFadeTimeout: Duration = 600.milliseconds

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
