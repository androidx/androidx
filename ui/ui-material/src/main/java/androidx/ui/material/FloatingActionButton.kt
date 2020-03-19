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

package androidx.ui.material

import androidx.compose.Composable
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.Modifier
import androidx.ui.core.enforce
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.material.ripple.ripple
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.max

/**
 * A floating action button (FAB) is a button that represents the primary action of a screen.
 *
 * This FAB is typically used with an [androidx.ui.foundation.Icon]:
 *
 * @sample androidx.ui.material.samples.SimpleFab
 *
 * See [ExtendedFloatingActionButton] for an extended FAB that contains text and an optional icon.
 *
 * @param modifier [Modifier] to be applied to this FAB.
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 *  when it is null.
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation The z-coordinate at which to place this FAB. This controls the size
 * of the shadow below the button.
 * @param children the content of this FAB
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.None,
    shape: Shape = CircleShape,
    backgroundColor: Color = MaterialTheme.colors().primary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 6.dp,
    children: @Composable() () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Clickable(onClick, modifier = ripple()) {
            ProvideTextStyle(MaterialTheme.typography().button) {
                Box(
                    modifier = MinimumFabSizeModifier,
                    gravity = ContentGravity.Center,
                    children = children
                )
            }
        }
    }
}

/**
 * A floating action button (FAB) is a button that represents the primary action of a screen.
 *
 * This extended FAB contains text and an optional icon that will be placed at the start. See
 * [FloatingActionButton] for a FAB that just contains some content, typically an icon.
 *
 * @sample androidx.ui.material.samples.SimpleExtendedFabWithIcon
 *
 * @param text Text label displayed inside this FAB
 * @param icon Optional icon for this FAB, typically this will be a [androidx.ui.foundation.Icon]
 * @param modifier [Modifier] to be applied to this FAB
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 * when it is null.
 * @param shape The [Shape] of this FAB
 * @param onClick will be called when user clicked on the button. The button will be disabled
 * when it is null.
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable() () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.None,
    icon: @Composable() (() -> Unit)? = null,
    shape: Shape = CircleShape,
    backgroundColor: Color = MaterialTheme.colors().primary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 6.dp
) {
    FloatingActionButton(
        modifier = modifier + LayoutSize.Min(ExtendedFabSize),
        onClick = onClick,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Box(
            modifier = LayoutPadding(
                start = ExtendedFabTextPadding,
                end = ExtendedFabTextPadding
            ),
            gravity = ContentGravity.Center
        ) {
            if (icon == null) {
                text()
            } else {
                Row {
                    icon()
                    Spacer(LayoutWidth(ExtendedFabIconPadding))
                    text()
                }
            }
        }
    }
}

/**
 * [LayoutModifier] that will set minimum constraints in each dimension to be [FabSize] unless
 * there is an incoming, non-zero minimum already. This allows us to define a default minimum in
 * [FloatingActionButton], but let [ExtendedFloatingActionButton] override it with a smaller
 * minimum just by settings a normal [LayoutHeight.Min] modifier.
 *
 * TODO: b/150460257 remove after support for this is added as a SizeModifier / similar
 */
private object MinimumFabSizeModifier : LayoutModifier {
    override fun Density.modifyConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Constraints {
        val minWidth = constraints.minWidth.takeIf { it != IntPx.Zero }
        val minHeight = constraints.minHeight.takeIf { it != IntPx.Zero }
        return when {
            minWidth != null && minHeight != null -> constraints
            else -> {
                Constraints(
                    minWidth = minWidth ?: FabSize.toIntPx(),
                    minHeight = minHeight ?: FabSize.toIntPx()
                ).enforce(constraints)
            }
        }
    }

    override fun Density.minIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.minIntrinsicWidth(height), FabSize.toIntPx())

    override fun Density.minIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.minIntrinsicHeight(width), FabSize.toIntPx())

    override fun Density.maxIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.maxIntrinsicWidth(height), FabSize.toIntPx())

    override fun Density.maxIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.maxIntrinsicHeight(width), FabSize.toIntPx())
}

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp
