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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external"
 * target="_blank">Material Design floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![FAB image](https://developer.android.com/images/reference/androidx/compose/material3/fab.png)
 *
 * FAB typically contains an icon, for a FAB with text and an icon, see
 * [ExtendedFloatingActionButton].
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
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
) =
    FloatingActionButton(
        onClick,
        ExtendedFabPrimaryTokens.LabelTextFont.value,
        FabPrimaryTokens.ContainerWidth,
        FabPrimaryTokens.ContainerHeight,
        modifier,
        shape,
        containerColor,
        contentColor,
        elevation,
        interactionSource,
        content
    )

@Composable
private fun FloatingActionButton(
    onClick: () -> Unit,
    textStyle: TextStyle,
    minWidth: Dp,
    minHeight: Dp,
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
        ProvideContentColorTextStyle(contentColor = contentColor, textStyle = textStyle) {
            Box(
                modifier =
                    Modifier.defaultMinSize(
                        minWidth = minWidth,
                        minHeight = minHeight,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

/**
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external"
 * target="_blank">Material Design small floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![Small FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small-fab.png)
 *
 * @sample androidx.compose.material3.samples.SmallFloatingActionButtonSample
 *
 * FABs can also be shown and hidden with an animation when the main content is scrolled:
 *
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
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
        modifier =
            modifier.sizeIn(
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
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external"
 * target="_blank">Material Design medium floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * @sample androidx.compose.material3.samples.MediumFloatingActionButtonSample
 *
 * FABs can also be shown and hidden with an animation when the main content is scrolled:
 *
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.mediumShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier =
            // TODO: update sizes to use tokens
            modifier.sizeIn(
                minWidth = 80.dp,
                minHeight = 80.dp,
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
 * <a href="https://m3.material.io/components/floating-action-button/overview" class="external"
 * target="_blank">Material Design large floating action button</a>.
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![Large FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/large-fab.png)
 *
 * @sample androidx.compose.material3.samples.LargeFloatingActionButtonSample
 *
 * FABs can also be shown and hidden with an animation when the main content is scrolled:
 *
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
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
        modifier =
            modifier.sizeIn(
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

// TODO link to image
/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design small extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other small extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.SmallExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SmallExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.smallExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = SmallExtendedFabTextStyle.value,
        minWidth = SmallExtendedFabMinimumWidth,
        minHeight = SmallExtendedFabMinimumHeight,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SmallExtendedFabHorizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// TODO link to image
/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design medium extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other medium extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.MediumExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.mediumExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = MediumExtendedFabTextStyle.value,
        minWidth = MediumExtendedFabMinimumWidth,
        minHeight = MediumExtendedFabMinimumHeight,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MediumExtendedFabHorizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// TODO link to image
/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design large extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other large extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.LargeExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LargeExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.largeExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = LargeExtendedFabTextStyle.value,
        minWidth = LargeExtendedFabMinimumWidth,
        minHeight = LargeExtendedFabMinimumHeight,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LargeExtendedFabHorizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * ![Extended FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The other extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
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
            modifier =
                Modifier.sizeIn(minWidth = ExtendedFabMinimumWidth)
                    .padding(horizontal = ExtendedFabTextPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design small extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other small extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.SmallExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.SmallAnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SmallExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.smallExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) =
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        textStyle = SmallExtendedFabTextStyle.value,
        minWidth = SmallExtendedFabMinimumWidth,
        minHeight = SmallExtendedFabMinimumHeight,
        horizontalPadding = SmallExtendedFabHorizontalPadding,
        iconPadding = SmallExtendedFabIconPadding,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design medium extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other medium extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.MediumExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.MediumAnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.mediumExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) =
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        textStyle = MediumExtendedFabTextStyle.value,
        minWidth = MediumExtendedFabMinimumWidth,
        minHeight = MediumExtendedFabMinimumHeight,
        horizontalPadding = MediumExtendedFabHorizontalPadding,
        iconPadding = MediumExtendedFabIconPadding,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design large extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other large extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.LargeExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.LargeAnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LargeExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.largeExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) =
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        textStyle = LargeExtendedFabTextStyle.value,
        minWidth = LargeExtendedFabMinimumWidth,
        minHeight = LargeExtendedFabMinimumHeight,
        horizontalPadding = LargeExtendedFabHorizontalPadding,
        iconPadding = LargeExtendedFabIconPadding,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )

/**
 * <a href="https://m3.material.io/components/extended-fab/overview" class="external"
 * target="_blank">Material Design extended floating action button</a>.
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * ![Extended FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The other extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
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
            modifier =
                Modifier.sizeIn(
                        minWidth =
                            if (expanded) {
                                ExtendedFabMinimumWidth
                            } else {
                                FabPrimaryTokens.ContainerWidth
                            }
                    )
                    .padding(start = startPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            icon()
            AnimatedVisibility(
                visible = expanded,
                enter = extendedFabExpandAnimation(),
                exit = extendedFabCollapseAnimation(),
            ) {
                Row(Modifier.clearAndSetSemantics {}) {
                    Spacer(Modifier.width(ExtendedFabEndIconPadding))
                    text()
                }
            }
        }
    }
}

@Composable
private fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    textStyle: TextStyle,
    minWidth: Dp,
    minHeight: Dp,
    horizontalPadding: Dp,
    iconPadding: Dp,
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
        textStyle = textStyle,
        minWidth = Dp.Unspecified,
        minHeight = Dp.Unspecified,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        val expandTransition = updateTransition(if (expanded) 1f else 0f, label = "expanded state")
        // TODO Load the motionScheme tokens from the component tokens file
        val sizeAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
        val opacityAnimationSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
        val expandedWidthProgress =
            expandTransition.animateFloat(transitionSpec = { sizeAnimationSpec }) { it }
        val expandedAlphaProgress =
            expandTransition.animateFloat(transitionSpec = { opacityAnimationSpec }) { it }
        Row(
            modifier =
                Modifier.layout { measurable, constraints ->
                        val expandedWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
                        val width =
                            lerp(minWidth.roundToPx(), expandedWidth, expandedWidthProgress.value)
                        val placeable = measurable.measure(constraints)
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    }
                    .sizeIn(minWidth = minWidth, minHeight = minHeight)
                    .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            val fullyCollapsed =
                remember(expandTransition) {
                    derivedStateOf {
                        expandTransition.currentState == 0f && !expandTransition.isRunning
                    }
                }
            if (!fullyCollapsed.value) {
                Row(
                    Modifier.clearAndSetSemantics {}
                        .graphicsLayer { alpha = expandedAlphaProgress.value }
                ) {
                    Spacer(Modifier.width(iconPadding))
                    text()
                }
            }
        }
    }
}

/** Contains the default values used by [FloatingActionButton] */
object FloatingActionButtonDefaults {
    internal val ShowHideTargetScale = 0.2f

    /** The recommended size of the icon inside a [MediumFloatingActionButton]. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    val MediumIconSize = 28.dp // TODO: update to use token

    /** The recommended size of the icon inside a [LargeFloatingActionButton]. */
    val LargeIconSize = FabPrimaryLargeTokens.IconSize

    /** Default shape for a floating action button. */
    val shape: Shape
        @Composable get() = FabPrimaryTokens.ContainerShape.value

    /** Default shape for a small floating action button. */
    val smallShape: Shape
        @Composable get() = FabPrimarySmallTokens.ContainerShape.value

    /** Default shape for a medium floating action button. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    val mediumShape: Shape
        @Composable get() = ShapeDefaults.LargeIncreased // TODO: update to use token

    /** Default shape for a large floating action button. */
    val largeShape: Shape
        @Composable get() = FabPrimaryLargeTokens.ContainerShape.value

    /** Default shape for an extended floating action button. */
    val extendedFabShape: Shape
        @Composable get() = ExtendedFabPrimaryTokens.ContainerShape.value

    /** Default shape for a small extended floating action button. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    val smallExtendedFabShape: Shape
        @Composable get() = ShapeDefaults.Large // TODO: update to use token

    /** Default shape for a medium extended floating action button. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    val mediumExtendedFabShape: Shape
        @Composable get() = ShapeDefaults.LargeIncreased // TODO: update to use token

    /** Default shape for a large extended floating action button. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    val largeExtendedFabShape: Shape
        @Composable get() = ShapeDefaults.ExtraLarge // TODO: update to use token

    /** Default container color for a floating action button. */
    val containerColor: Color
        @Composable get() = FabPrimaryTokens.ContainerColor.value

    /**
     * Creates a [FloatingActionButtonElevation] that represents the elevation of a
     * [FloatingActionButton] in different states. For use cases in which a less prominent
     * [FloatingActionButton] is possible consider the [loweredElevation].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     *   [Interaction]s.
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
    ): FloatingActionButtonElevation =
        FloatingActionButtonElevation(
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
     *   [Interaction]s.
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
    ): FloatingActionButtonElevation =
        FloatingActionButtonElevation(
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
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    fun bottomAppBarFabElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp
    ): FloatingActionButtonElevation =
        FloatingActionButtonElevation(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation
        )
}

/**
 * Apply this modifier to a [FloatingActionButton] to show or hide it with an animation, typically
 * based on the app's main content scrolling.
 *
 * @param visible whether the FAB should be shown or hidden with an animation
 * @param alignment the direction towards which the FAB should be scaled to and from
 * @param targetScale the initial scale value when showing the FAB and the final scale value when
 *   hiding the FAB
 * @param scaleAnimationSpec the [AnimationSpec] to use for the scale part of the animation, if null
 *   the Fast Spatial spring spec from the [MotionScheme] will be used
 * @param alphaAnimationSpec the [AnimationSpec] to use for the alpha part of the animation, if null
 *   the Fast Effects spring spec from the [MotionScheme] will be used
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 */
@ExperimentalMaterial3ExpressiveApi
fun Modifier.animateFloatingActionButton(
    visible: Boolean,
    alignment: Alignment,
    targetScale: Float = FloatingActionButtonDefaults.ShowHideTargetScale,
    scaleAnimationSpec: AnimationSpec<Float>? = null,
    alphaAnimationSpec: AnimationSpec<Float>? = null
): Modifier {
    return this.then(
        FabVisibleModifier(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec
        )
    )
}

internal data class FabVisibleModifier(
    private val visible: Boolean,
    private val alignment: Alignment,
    private val targetScale: Float,
    private val scaleAnimationSpec: AnimationSpec<Float>? = null,
    private val alphaAnimationSpec: AnimationSpec<Float>? = null
) : ModifierNodeElement<FabVisibleNode>() {

    override fun create(): FabVisibleNode =
        FabVisibleNode(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )

    override fun update(node: FabVisibleNode) {
        node.updateNode(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class FabVisibleNode(
    visible: Boolean,
    private var alignment: Alignment,
    private var targetScale: Float,
    private var scaleAnimationSpec: AnimationSpec<Float>? = null,
    private var alphaAnimationSpec: AnimationSpec<Float>? = null,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {

    private val scaleAnimatable = Animatable(if (visible) 1f else 0f)
    private val alphaAnimatable = Animatable(if (visible) 1f else 0f)

    val delegate =
        delegate(
            CacheDrawModifierNode {
                val layer = obtainGraphicsLayer()
                // Use a larger layer size to make sure the elevation shadow doesn't get clipped
                // and offset via layer.topLeft and DrawScope.inset to preserve the visual
                // position of the FAB.
                val layerInsetSize = 16.dp.toPx()
                val layerSize =
                    Size(size.width + layerInsetSize * 2f, size.height + layerInsetSize * 2f)
                        .toIntSize()
                val nodeSize = size.toIntSize()

                layer.apply {
                    topLeft = IntOffset(-layerInsetSize.roundToInt(), -layerInsetSize.roundToInt())

                    alpha = alphaAnimatable.value

                    // Scale towards the direction of the provided alignment
                    val alignOffset = alignment.align(IntSize(1, 1), nodeSize, layoutDirection)
                    pivotOffset = alignOffset.toOffset() + Offset(layerInsetSize, layerInsetSize)
                    scaleX = lerp(targetScale, 1f, scaleAnimatable.value)
                    scaleY = lerp(targetScale, 1f, scaleAnimatable.value)

                    record(size = layerSize) {
                        inset(layerInsetSize, layerInsetSize) { this@record.drawContent() }
                    }
                }

                onDrawWithContent { drawLayer(layer) }
            }
        )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun updateNode(
        visible: Boolean,
        alignment: Alignment,
        targetScale: Float,
        scaleAnimationSpec: AnimationSpec<Float>?,
        alphaAnimationSpec: AnimationSpec<Float>?
    ) {
        this.alignment = alignment
        this.targetScale = targetScale
        this.scaleAnimationSpec = scaleAnimationSpec
        this.alphaAnimationSpec = alphaAnimationSpec

        coroutineScope.launch {
            // TODO Load the motionScheme tokens from the component tokens file
            scaleAnimatable.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec =
                    scaleAnimationSpec ?: currentValueOf(LocalMotionScheme).fastSpatialSpec()
            )
        }

        coroutineScope.launch {
            // TODO Load the motionScheme tokens from the component tokens file
            alphaAnimatable.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec =
                    alphaAnimationSpec ?: currentValueOf(LocalMotionScheme).fastEffectsSpec()
            )
        }
    }
}

/**
 * Represents the tonal and shadow elevation for a floating action button in different states.
 *
 * See [FloatingActionButtonDefaults.elevation] for the default elevation used in a
 * [FloatingActionButton] and [ExtendedFloatingActionButton].
 */
@Stable
open class FloatingActionButtonElevation
internal constructor(
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
        val animatable =
            remember(interactionSource) {
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
                launch { animatable.animateElevation(to = targetInteraction) }
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
                animatable.animateElevation(target = target, from = lastTargetInteraction, to = to)
            }
        } finally {
            lastTargetInteraction = to
        }
    }

    fun asState(): State<Dp> = animatable.asState()
}

private val SmallExtendedFabMinimumWidth = 56.dp
private val SmallExtendedFabMinimumHeight = 56.dp
private val SmallExtendedFabHorizontalPadding = 16.dp
private val SmallExtendedFabIconPadding = 8.dp
private val SmallExtendedFabTextStyle = TypographyKeyTokens.TitleMedium

private val MediumExtendedFabMinimumWidth = 80.dp
private val MediumExtendedFabMinimumHeight = 80.dp
private val MediumExtendedFabHorizontalPadding = 26.dp
private val MediumExtendedFabIconPadding = 16.dp
private val MediumExtendedFabTextStyle = TypographyKeyTokens.TitleLarge

private val LargeExtendedFabMinimumWidth = 96.dp
private val LargeExtendedFabMinimumHeight = 96.dp
private val LargeExtendedFabHorizontalPadding = 28.dp
private val LargeExtendedFabIconPadding = 20.dp
private val LargeExtendedFabTextStyle = TypographyKeyTokens.HeadlineSmall

private val ExtendedFabStartIconPadding = 16.dp

private val ExtendedFabEndIconPadding = 12.dp

private val ExtendedFabTextPadding = 20.dp

private val ExtendedFabMinimumWidth = 80.dp

@Composable
private fun extendedFabCollapseAnimation() =
    fadeOut(
        // TODO Load the motionScheme tokens from the component tokens file
        animationSpec = MotionSchemeKeyTokens.FastEffects.value()
    ) +
        shrinkHorizontally(
            animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value(),
            shrinkTowards = Alignment.Start,
        )

@Composable
private fun extendedFabExpandAnimation() =
    fadeIn(
        // TODO Load the motionScheme tokens from the component tokens file
        animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
    ) +
        expandHorizontally(
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            expandFrom = Alignment.Start,
        )
