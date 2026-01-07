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

package com.android.compose.animation.scene.demo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ValueKey
import com.android.compose.grid.VerticalGrid
import com.android.mechanics.compose.modifier.verticalFadeContentReveal
import com.android.mechanics.compose.modifier.verticalTactileSurfaceReveal
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class QuickSettingsTileIdentity

@Stable
interface QuickSettingsTileViewModel {
    val key: ElementKey
    val isActive: Boolean

    val icon: ImageVector
    val title: String
    val description: String?

    val showChevron: Boolean
    val onClick: () -> Unit

    val textMeasurer: TextMeasurer
}

object QuickSettingsGrid {
    object Elements {
        val Tiles = ElementKey.withIdentity { it is QuickSettingsTileIdentity }
        val GridAnchor = ElementKey("QuickSettingsGridAnchor")
        val GridContainer = ElementKey("QuickSettingsGridAnchor")
    }

    object Values {
        val Expansion = ValueKey("QuickSettingsGridExpansion")
    }

    object Dimensions {
        val TileExpandedHeight = 84.dp
        val TileCollapsedHeight = 60.dp
        val Spacing = 8.dp

        const val RowCountWhenCollapsed = 2
    }

    object Colors {
        val ActiveTileBackground: Color
            @Composable
            @ReadOnlyComposable
            get() = colorResource(android.R.color.system_accent1_100)

        val InactiveTileBackground: Color
            @Composable
            @ReadOnlyComposable
            get() = colorResource(android.R.color.system_neutral1_800)

        val ActiveTileText: Color
            @Composable
            @ReadOnlyComposable
            get() = colorResource(android.R.color.system_accent1_900)

        val InactiveTileText: Color
            @Composable
            @ReadOnlyComposable
            get() = colorResource(android.R.color.system_neutral1_100)
    }

    object Shapes {
        val Tile = RoundedCornerShape(Dimensions.TileCollapsedHeight / 2)
    }
}

class RevealEffectConfig(val deltaY: Float)

/**
 * Display all [tiles] as a grid with [nColumns] columns. The tiles will be expanded if [isExpanded]
 * is true.
 */
@Composable
fun ContentScope.QuickSettingsGrid(
    tiles: List<QuickSettingsTileViewModel>,
    nColumns: Int,
    isExpanded: Boolean,
    revealEffect: Boolean,
    expansionProgress: () -> Float,
    modifier: Modifier = Modifier,
    nRowsTarget: Int? = null,
) {
    val tileHeight = tileHeight(isExpanded)
    val density = LocalDensity.current
    val revealEffectConfig =
        remember(revealEffect, density) {
            if (!revealEffect) return@remember null
            RevealEffectConfig(
                deltaY = with(density) { QuickSettingsGrid.Dimensions.Spacing.toPx() }
            )
        }

    Column(modifier) {
        VerticalGrid(
            columns = nColumns,
            horizontalSpacing = QuickSettingsGrid.Dimensions.Spacing,
            verticalSpacing = QuickSettingsGrid.Dimensions.Spacing,
        ) {
            tiles.forEachIndexed { i, tile ->
                key(tile.key) {
                    Tile(
                        viewModel = tile,
                        height = tileHeight,
                        expansionProgress = expansionProgress,
                        revealEffectConfig = revealEffectConfig,
                        tileId = i,
                    )
                }
            }
        }

        // Fill the space that would be taking by the remaining number of rows in case there are
        // not enough tiles to fill the grid.
        if (nRowsTarget != null) {
            val nRows = ceil(tiles.size.toFloat() / nColumns).roundToInt()
            val nRowsToFill = abs(nRowsTarget - nRows)
            if (nRowsToFill > 0) {
                Spacer(
                    Modifier.height(
                        (tileHeight + QuickSettingsGrid.Dimensions.Spacing) * nRowsToFill
                    )
                )
            }
        }
    }
}

fun tileHeight(isExpanded: Boolean) =
    if (isExpanded) QuickSettingsGrid.Dimensions.TileExpandedHeight
    else QuickSettingsGrid.Dimensions.TileCollapsedHeight

@Composable
private fun ContentScope.Tile(
    viewModel: QuickSettingsTileViewModel,
    height: Dp,
    expansionProgress: () -> Float,
    revealEffectConfig: RevealEffectConfig?,
    tileId: Int,
    modifier: Modifier = Modifier,
) {
    val isActive = viewModel.isActive
    val icon = viewModel.icon
    val title = viewModel.title
    val description = viewModel.description
    val showChevron = viewModel.showChevron

    val targetBackgroundColor: Color =
        if (isActive) QuickSettingsGrid.Colors.ActiveTileBackground
        else QuickSettingsGrid.Colors.InactiveTileBackground
    val targetContentColor: Color =
        if (isActive) QuickSettingsGrid.Colors.ActiveTileText
        else QuickSettingsGrid.Colors.InactiveTileText
    val backgroundColor = animateColorAsState(targetBackgroundColor)
    val contentColor = animateColorAsState(targetContentColor)

    Layout(
        modifier =
            modifier
                .element(viewModel.key)
                .then(
                    if (revealEffectConfig != null) {
                        Modifier.verticalTactileSurfaceReveal(
                            deltaY = revealEffectConfig.deltaY,
                            label = "tile($tileId)",
                        )
                    } else Modifier
                )
                .fillMaxWidth()
                // Note: This height modifier is what is setting the size of this tile when idle,
                // but during transitions the height is actually interpolated and constrained by
                // Modifier.element(key) above.
                .height(height)
                .clip(QuickSettingsGrid.Shapes.Tile)
                .clickable(onClick = viewModel.onClick)
                .drawBehind { drawRect(backgroundColor.value) }
                .then(
                    if (revealEffectConfig != null) {
                        Modifier.verticalFadeContentReveal(
                            deltaY = revealEffectConfig.deltaY,
                            label = "tile($tileId)",
                        )
                    } else Modifier
                ),
        contents =
            remember(icon, title, description, contentColor, showChevron, expansionProgress) {
                tileContents(
                    icon,
                    title,
                    description,
                    showChevron,
                    { contentColor.value },
                    expansionProgress,
                    viewModel.textMeasurer,
                )
            },
    ) { measurables, constraints ->
        val contentsSize = measurables.size
        check(contentsSize >= 2 && contentsSize <= 4)
        repeat(contentsSize) { check(measurables[it].size == 1) }

        // The indices of the chevron and description in the measurables, or -1 if they are not
        // shown.
        val descriptionIndex: Int
        val chevronIndex: Int
        if (description != null) {
            descriptionIndex = 2
            if (showChevron) {
                chevronIndex = 3
            } else {
                chevronIndex = -1
            }
        } else {
            descriptionIndex = -1
            if (showChevron) {
                chevronIndex = 2
            } else {
                chevronIndex = -1
            }
        }

        // The height of this layout should already be fixed by Modifier.element(),
        // Modifier.height() and Modifier.fillMaxWidth().
        check(constraints.minWidth == constraints.maxWidth)
        check(constraints.minHeight == constraints.maxHeight)

        // Icon and chevron are exactly 20dp x 20dp.
        val iconSize = 20.dp.roundToPx()
        val iconConstraints = Constraints.fixed(iconSize, iconSize)
        val iconPlaceable = measurables[0][0].measure(iconConstraints)
        val chevronPlaceable =
            if (chevronIndex != -1) {
                measurables[chevronIndex][0].measure(iconConstraints)
            } else {
                null
            }

        // The tile has a horizontal padding of 16dp.
        val horizontalPadding = 16.dp.roundToPx()

        // There is a 8dp spacing between the icon and the texts.
        val iconSpacing = 8.dp.roundToPx()

        // The remaining width for the texts.
        val textMaxWidthWithoutChevron =
            constraints.maxWidth - iconPlaceable.width - horizontalPadding * 2 - iconSpacing
        val textMaxWidth =
            if (chevronPlaceable != null) {
                textMaxWidthWithoutChevron - chevronPlaceable.width - iconSpacing
            } else {
                textMaxWidthWithoutChevron
            }

        // We don't want to remeasure texts and icons every frame of animations, so we use the same
        // maxHeight independently of the current height.
        val collapsedHeight = QuickSettingsGrid.Dimensions.TileCollapsedHeight.roundToPx()
        val textMaxHeight = collapsedHeight
        val textConstraints = Constraints(maxWidth = textMaxWidth, maxHeight = textMaxHeight)
        val titlePlaceable = measurables[1][0].measure(textConstraints)
        val descriptionPlaceable =
            if (descriptionIndex != -1) measurables[2][0].measure(textConstraints) else null

        val width = constraints.minWidth
        val height = constraints.minHeight
        layout(width, height) {
            // Icon and chevron are centered vertically.
            iconPlaceable.placeRelative(horizontalPadding, (height - iconPlaceable.height) / 2)
            chevronPlaceable?.placeRelative(
                width - horizontalPadding - chevronPlaceable.width,
                (height - chevronPlaceable.height) / 2,
            )

            // If there is no description, the title is always centered vertically.
            val textX = horizontalPadding + iconPlaceable.width + iconSpacing
            if (descriptionPlaceable == null) {
                titlePlaceable.placeRelative(textX, (height - titlePlaceable.height) / 2)
                return@layout
            }

            // When collapsed, the title is centered vertically and the description if faded out.
            val titleCollapsedY = (height - titlePlaceable.height) / 2

            // When expanded, the title + description are centered vertically.
            val titleExpandedY = (height - titlePlaceable.height - descriptionPlaceable.height) / 2

            val titleY =
                (titleCollapsedY + expansionProgress() * (titleExpandedY - titleCollapsedY))
                    .roundToInt()
            titlePlaceable.placeRelative(textX, titleY)
            descriptionPlaceable.placeRelative(textX, titleY + titlePlaceable.height)
        }
    }
}

private fun tileContents(
    icon: ImageVector,
    title: String,
    description: String?,
    showChevron: Boolean,
    contentColor: () -> Color,
    expansionProgress: () -> Float,
    textMeasurer: TextMeasurer,
): List<@Composable () -> Unit> {
    return buildList {
        // Icon.
        add { Icon(icon, null, tint = contentColor()) }

        // Title.
        add {
            CachedText(
                title,
                textMeasurer,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = contentColor,
            )
        }

        // Description.
        if (description != null) {
            add {
                CachedText(
                    description,
                    textMeasurer,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor,
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = expansionProgress()
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        },
                )
            }
        }

        // Chevron.
        if (showChevron) {
            add {
                // TODO(b/231674463): We should not read contentColor() during composition.
                Icon(Icons.Default.ChevronRight, null, tint = contentColor())
            }
        }
    }
}
