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

@file:Suppress("Deprecation")

package androidx.ui.material

import androidx.compose.Composable
import androidx.ui.core.Constraints
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureScope
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.enforce
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.shape.corner.CornerSize
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSizeIn
import androidx.ui.layout.preferredWidth
import androidx.ui.material.ripple.ripple
import androidx.ui.semantics.Semantics
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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
 * @param icon the content of this FAB
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 6.dp,
    icon: @Composable() () -> Unit
) {
    // Since we're adding layouts in between the clickable layer and the content, we need to
    // merge all descendants, or we'll get multiple nodes
    Semantics(container = true, mergeAllDescendants = true) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            contentColor = contentColor,
            elevation = elevation
        ) {
            Clickable(onClick, modifier = Modifier.ripple()) {
                ProvideTextStyle(MaterialTheme.typography.button) {
                    Box(
                        modifier = MinimumFabSizeModifier,
                        gravity = ContentGravity.Center,
                        children = icon
                    )
                }
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
    modifier: Modifier = Modifier,
    icon: @Composable() (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 6.dp
) {
    FloatingActionButton(
        modifier = modifier.preferredSizeIn(
            minWidth = ExtendedFabSize,
            minHeight = ExtendedFabSize
        ),
        onClick = onClick,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Box(
            modifier = Modifier.padding(
                start = ExtendedFabTextPadding,
                end = ExtendedFabTextPadding
            ),
            gravity = ContentGravity.Center
        ) {
            if (icon == null) {
                text()
            } else {
                Row(verticalGravity = ContentGravity.CenterVertically) {
                    icon()
                    Spacer(Modifier.preferredWidth(ExtendedFabIconPadding))
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
 * minimum just by settings a normal [preferredSizeIn] modifier.
 *
 * TODO: b/150460257 remove after support for this is added as a SizeModifier / similar
 */
private object MinimumFabSizeModifier : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val minWidth = constraints.minWidth.takeIf { it != IntPx.Zero }
        val minHeight = constraints.minHeight.takeIf { it != IntPx.Zero }
        val wrappedConstraints = when {
            minWidth != null && minHeight != null -> constraints
            else -> {
                Constraints(
                    minWidth = minWidth ?: FabSize.toIntPx(),
                    minHeight = minHeight ?: FabSize.toIntPx()
                ).enforce(constraints)
            }
        }
        val placeable = measurable.measure(wrappedConstraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0.ipx, 0.ipx)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.minIntrinsicWidth(height), FabSize.toIntPx())

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.minIntrinsicHeight(width), FabSize.toIntPx())

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.maxIntrinsicWidth(height), FabSize.toIntPx())

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = max(measurable.maxIntrinsicHeight(width), FabSize.toIntPx())
}

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp
