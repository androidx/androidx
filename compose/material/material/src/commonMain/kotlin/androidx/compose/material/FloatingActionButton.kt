/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * <a href="https://material.io/components/buttons-floating-action-button" class="external" target="_blank">Material Design floating action button</a>.
 *
 * A floating action button (FAB) represents the primary action of a screen.
 *
 * ![Floating action button image](https://developer.android.com/images/reference/androidx/compose/material/floating-action-button.png)
 *
 * This FAB is typically used with an [Icon]:
 *
 * @sample androidx.compose.material.samples.SimpleFab
 *
 * See [ExtendedFloatingActionButton] for an extended FAB that contains text and an optional icon.
 *
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB.
 * @param content the content of this FAB - this is typically an [Icon].
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation.elevation(interactionSource).value,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(MaterialTheme.typography.button) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = FabSize, minHeight = FabSize),
                    contentAlignment = Alignment.Center
                ) { content() }
            }
        }
    }
}

/**
 * <a href="https://material.io/components/buttons-floating-action-button#extended-fab" class="external" target="_blank">Material Design extended floating action button</a>.
 *
 * The extended FAB is wider than a regular FAB, and it includes a text label.
 *
 * ![Extended floating action button image](https://developer.android.com/images/reference/androidx/compose/material/extended-floating-action-button.png)
 *
 * This extended FAB contains text and an optional icon that will be placed at the start. See
 * [FloatingActionButton] for a FAB that just contains some content, typically an icon.
 *
 * @sample androidx.compose.material.samples.SimpleExtendedFabWithIcon
 *
 * If you want FAB’s container to have a fluid width (to be defined by its relationship to something
 * else on screen, such as screen width or the layout grid) just apply an appropriate modifier.
 * For example to fill the whole available width you can do:
 *
 * @sample androidx.compose.material.samples.FluidExtendedFab
 *
 * @param text Text label displayed inside this FAB
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB
 * @param icon Optional icon for this FAB, typically this will be a
 * [Icon].
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation()
) {
    FloatingActionButton(
        modifier = modifier.sizeIn(
            minWidth = ExtendedFabSize,
            minHeight = ExtendedFabSize
        ),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        val startPadding = if (icon == null) ExtendedFabTextPadding else ExtendedFabIconPadding
        Row(
            modifier = Modifier.padding(
                start = startPadding,
                end = ExtendedFabTextPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(ExtendedFabIconPadding))
            }
            text()
        }
    }
}

/**
 * Represents the elevation for a floating action button in different states.
 *
 * See [FloatingActionButtonDefaults.elevation] for the default elevation used in a
 * [FloatingActionButton] and [ExtendedFloatingActionButton].
 */
@Stable
interface FloatingActionButtonElevation {
    /**
     * Represents the elevation used in a floating action button, depending on
     * [interactionSource].
     *
     * @param interactionSource the [InteractionSource] for this floating action button
     */
    @Composable
    fun elevation(interactionSource: InteractionSource): State<Dp>
}

/**
 * Contains the default values used by [FloatingActionButton]
 */
object FloatingActionButtonDefaults {
    /**
     * Creates a [FloatingActionButtonElevation] that will animate between the provided values
     * according to the Material specification.
     *
     * @param defaultElevation the elevation to use when the [FloatingActionButton] has no
     * [Interaction]s
     * @param pressedElevation the elevation to use when the [FloatingActionButton] is
     * pressed.
     */
    @Deprecated("Use another overload of elevation", level = DeprecationLevel.HIDDEN)
    @Composable
    fun elevation(
        defaultElevation: Dp = 6.dp,
        pressedElevation: Dp = 12.dp,
    ): FloatingActionButtonElevation = elevation(
        defaultElevation,
        pressedElevation,
        hoveredElevation = 8.dp,
        focusedElevation = 8.dp,
    )

    /**
     * Creates a [FloatingActionButtonElevation] that will animate between the provided values
     * according to the Material specification.
     *
     * @param defaultElevation the elevation to use when the [FloatingActionButton] has no
     * [Interaction]s
     * @param pressedElevation the elevation to use when the [FloatingActionButton] is
     * pressed.
     * @param hoveredElevation the elevation to use when the [FloatingActionButton] is
     * hovered.
     * @param focusedElevation the elevation to use when the [FloatingActionButton] is
     * focused.
     */
    @Composable
    fun elevation(
        defaultElevation: Dp = 6.dp,
        pressedElevation: Dp = 12.dp,
        hoveredElevation: Dp = 8.dp,
        focusedElevation: Dp = 8.dp,
    ): FloatingActionButtonElevation {
        return remember(defaultElevation, pressedElevation, hoveredElevation, focusedElevation) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation
            )
        }
    }
}

/**
 * Default [FloatingActionButtonElevation] implementation.
 */
@Stable
private class DefaultFloatingActionButtonElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val hoveredElevation: Dp,
    private val focusedElevation: Dp
) : FloatingActionButtonElevation {
    @Composable
    override fun elevation(interactionSource: InteractionSource): State<Dp> {
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
        if (other !is DefaultFloatingActionButtonElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        return focusedElevation == other.focusedElevation
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
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

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp
