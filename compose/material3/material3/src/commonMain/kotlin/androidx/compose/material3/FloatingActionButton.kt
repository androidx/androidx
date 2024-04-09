/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.tokens.ExtendedFabPrimaryTokens
import androidx.compose.material3.tokens.FabPrimaryLargeTokens
import androidx.compose.material3.tokens.FabPrimarySmallTokens
import androidx.compose.material3.tokens.FabPrimaryTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external" target="_blank">Material Design floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![FAB image](https://developer.android.com/images/reference/androidx/compose/material3/fab.png)
 *
 * FAB typically contains an icon, for a FAB with text and an icon, see
 * [ExtendedFloatingActionButton].
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonSample
 *
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 * have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 * different states. This controls the size of the shadow below the FAB. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance
 * or preview the FAB in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation.tonalElevation(),
        shadowElevation = elevation.shadowElevation(interactionSource = interactionSource).value,
        interactionSource = interactionSource
    ) {
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = MaterialTheme.typography.fromToken(ExtendedFabPrimaryTokens.LabelTextFont)
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = FabPrimaryTokens.ContainerWidth,
                        minHeight = FabPrimaryTokens.ContainerHeight,
                    ),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

/**
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external" target="_blank">Material Design small floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![Small FAB image](https://developer.android.com/images/reference/androidx/compose/material3/small-fab.png)
 *
 * @sample androidx.compose.material3.samples.SmallFloatingActionButtonSample
 *
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 * have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 * different states. This controls the size of the shadow below the FAB. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance
 * or preview the FAB in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@Composable
fun SmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.smallShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = FabPrimarySmallTokens.ContainerWidth,
            minHeight = FabPrimarySmallTokens.ContainerHeight,
        ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external" target="_blank">Material Design large floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![Large FAB image](https://developer.android.com/images/reference/androidx/compose/material3/large-fab.png)
 *
 * @sample androidx.compose.material3.samples.LargeFloatingActionButtonSample
 *
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 * have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 * different states. This controls the size of the shadow below the FAB. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance
 * or preview the FAB in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@Composable
fun LargeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.largeShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = FabPrimaryLargeTokens.ContainerWidth,
            minHeight = FabPrimaryLargeTokens.ContainerHeight,
        ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external" target="_blank">Material Design extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * ![Extended FAB image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The other extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
 *
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 * have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 * different states. This controls the size of the shadow below the FAB. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance
 * or preview the FAB in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .sizeIn(minWidth = ExtendedFabMinimumWidth)
                .padding(horizontal = ExtendedFabTextPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external" target="_blank">Material Design extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * ![Extended FAB image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The other extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
 *
 * @param text label displayed inside this FAB
 * @param icon optional icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 * both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 * have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 * different states. This controls the size of the shadow below the FAB. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance
 * or preview the FAB in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        val startPadding = if (expanded) ExtendedFabStartIconPadding else 0.dp
        val endPadding = if (expanded) ExtendedFabTextPadding else 0.dp

        Row(
            modifier = Modifier
                .sizeIn(
                    minWidth = if (expanded) ExtendedFabMinimumWidth
                    else FabPrimaryTokens.ContainerWidth
                )
                .padding(start = startPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            icon()
            AnimatedVisibility(
                visible = expanded,
                enter = ExtendedFabExpandAnimation,
                exit = ExtendedFabCollapseAnimation,
            ) {
                Row(Modifier.clearAndSetSemantics {}) {
                    Spacer(Modifier.width(ExtendedFabEndIconPadding))
                    text()
                }
            }
        }
    }
}

/**
 * Contains the default values used by [FloatingActionButton]
 */
object FloatingActionButtonDefaults {
    /**
     * The recommended size of the icon inside a [LargeFloatingActionButton].
     */
    val LargeIconSize = FabPrimaryLargeTokens.IconSize

    /** Default shape for a floating action button. */
    val shape: Shape @Composable get() = FabPrimaryTokens.ContainerShape.value

    /** Default shape for a small floating action button. */
    val smallShape: Shape @Composable get() = FabPrimarySmallTokens.ContainerShape.value

    /** Default shape for a large floating action button. */
    val largeShape: Shape @Composable get() = FabPrimaryLargeTokens.ContainerShape.value

    /** Default shape for an extended floating action button. */
    val extendedFabShape: Shape @Composable get() =
        ExtendedFabPrimaryTokens.ContainerShape.value

    /** Default container color for a floating action button. */
    val containerColor: Color @Composable get() = FabPrimaryTokens.ContainerColor.value

    /**
     * Creates a [FloatingActionButtonElevation] that represents the elevation of a
     * [FloatingActionButton] in different states. For use cases in which a less prominent
     * [FloatingActionButton] is possible consider the [loweredElevation].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun elevation(
        defaultElevation: Dp = FabPrimaryTokens.ContainerElevation,
        pressedElevation: Dp = FabPrimaryTokens.PressedContainerElevation,
        focusedElevation: Dp = FabPrimaryTokens.FocusContainerElevation,
        hoveredElevation: Dp = FabPrimaryTokens.HoverContainerElevation,
    ): FloatingActionButtonElevation = FloatingActionButtonElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
    )

    /**
     * Use this to create a [FloatingActionButton] with a lowered elevation for less emphasis. Use
     * [elevation] to get a normal [FloatingActionButton].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun loweredElevation(
        defaultElevation: Dp = FabPrimaryTokens.LoweredContainerElevation,
        pressedElevation: Dp = FabPrimaryTokens.LoweredPressedContainerElevation,
        focusedElevation: Dp = FabPrimaryTokens.LoweredFocusContainerElevation,
        hoveredElevation: Dp = FabPrimaryTokens.LoweredHoverContainerElevation,
    ): FloatingActionButtonElevation = FloatingActionButtonElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
    )

    /**
     * Use this to create a [FloatingActionButton] that represents the default elevation of a
     * [FloatingActionButton] used for [BottomAppBar] in different states.
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    fun bottomAppBarFabElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp
    ): FloatingActionButtonElevation = FloatingActionButtonElevation(
        defaultElevation,
        pressedElevation,
        focusedElevation,
        hoveredElevation
    )
}

/**
 * Represents the tonal and shadow elevation for a floating action button in different states.
 *
 * See [FloatingActionButtonDefaults.elevation] for the default elevation used in a
 * [FloatingActionButton] and [ExtendedFloatingActionButton].
 */
@Stable
 open class FloatingActionButtonElevation internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
) {
    @Composable
    internal fun shadowElevation(interactionSource: InteractionSource): State<Dp> {
        return animateElevation(interactionSource = interactionSource)
    }

    internal fun tonalElevation(): Dp {
        return defaultElevation
    }

    @Composable
    private fun animateElevation(interactionSource: InteractionSource): State<Dp> {
        val animatable = remember(interactionSource) {
            FloatingActionButtonElevationAnimatable(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation
            )
        }

        LaunchedEffect(this) {
            animatable.updateElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation
            )
        }

        LaunchedEffect(interactionSource) {
            val interactions = mutableListOf<Interaction>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
                val targetInteraction = interactions.lastOrNull()
                launch {
                    animatable.animateElevation(to = targetInteraction)
                }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is FloatingActionButtonElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        return hoveredElevation == other.hoveredElevation
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        return result
    }
}

private class FloatingActionButtonElevationAnimatable(
    private var defaultElevation: Dp,
    private var pressedElevation: Dp,
    private var hoveredElevation: Dp,
    private var focusedElevation: Dp
) {
    private val animatable = Animatable(defaultElevation, Dp.VectorConverter)

    private var lastTargetInteraction: Interaction? = null
    private var targetInteraction: Interaction? = null

    private fun Interaction?.calculateTarget(): Dp {
        return when (this) {
            is PressInteraction.Press -> pressedElevation
            is HoverInteraction.Enter -> hoveredElevation
            is FocusInteraction.Focus -> focusedElevation
            else -> defaultElevation
        }
    }

    suspend fun updateElevation(
        defaultElevation: Dp,
        pressedElevation: Dp,
        hoveredElevation: Dp,
        focusedElevation: Dp
    ) {
        this.defaultElevation = defaultElevation
        this.pressedElevation = pressedElevation
        this.hoveredElevation = hoveredElevation
        this.focusedElevation = focusedElevation
        snapElevation()
    }

    private suspend fun snapElevation() {
        val target = targetInteraction.calculateTarget()
        if (animatable.targetValue != target) {
            try {
                animatable.snapTo(target)
            } finally {
                lastTargetInteraction = targetInteraction
            }
        }
    }

    suspend fun animateElevation(to: Interaction?) {
        val target = to.calculateTarget()
        // Update the interaction even if the values are the same, for when we change to another
        // interaction later
        targetInteraction = to
        try {
            if (animatable.targetValue != target) {
                animatable.animateElevation(
                    target = target,
                    from = lastTargetInteraction,
                    to = to
                )
            }
        } finally {
            lastTargetInteraction = to
        }
    }

    fun asState(): State<Dp> = animatable.asState()
}

private val ExtendedFabStartIconPadding = 16.dp

private val ExtendedFabEndIconPadding = 12.dp

private val ExtendedFabTextPadding = 20.dp

private val ExtendedFabMinimumWidth = 80.dp

private val ExtendedFabCollapseAnimation = fadeOut(
    animationSpec = tween(
        durationMillis = MotionTokens.DurationShort2.toInt(),
        easing = MotionTokens.EasingLinearCubicBezier,
    )
) + shrinkHorizontally(
    animationSpec = tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingEmphasizedCubicBezier,
    ),
    shrinkTowards = Alignment.Start,
)

private val ExtendedFabExpandAnimation = fadeIn(
    animationSpec = tween(
        durationMillis = MotionTokens.DurationShort4.toInt(),
        delayMillis = MotionTokens.DurationShort2.toInt(),
        easing = MotionTokens.EasingLinearCubicBezier,
    ),
) + expandHorizontally(
    animationSpec = tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingEmphasizedCubicBezier,
    ),
    expandFrom = Alignment.Start,
)
