/*
 * Copyright (C) 2023 The Android Open Source Project
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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.animateContentFloatAsState
import com.android.compose.animation.scene.content.state.TransitionState
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ContentScope.QuickSettingsPager(
    pagerState: PagerState,
    tiles: List<QuickSettingsTileViewModel>,
    nRows: Int,
    nColumns: Int,
    modifier: Modifier = Modifier,
    revealEffect: Boolean = false,
) {
    val nTiles = tiles.size
    val nTilesPerPage = nRows * nColumns

    // Each page must have exactly nRows rows, unless there is a single page.
    val nRowsTarget =
        if (nTiles < nTilesPerPage) ceil(nTiles.toFloat() / nColumns).roundToInt() else nRows

    PagerStateResetter(pagerState)

    Column(modifier.noResizeDuringTransitions()) {
        Box {
            GridAnchor(isExpanded = true)

            // The grid expansion progress. To make the Tile() implementation more self-contained we
            // could have an animateSharedFloatAsState, but this will create a new shared value for
            // each tile that will all have the same value so we instead create a single shared
            // value here used by all Tiles.
            val expansionProgress by
                animateContentFloatAsState(
                    1f,
                    QuickSettingsGrid.Values.Expansion,
                    canOverflow = false,
                )

            HorizontalPager(pagerState, Modifier.fillMaxWidth()) { page ->
                val firstTileIndex = page * nTilesPerPage
                QuickSettingsGrid(
                    tiles.subList(
                        firstTileIndex,
                        minOf(tiles.size, firstTileIndex + nTilesPerPage),
                    ),
                    nColumns,
                    isExpanded = true,
                    revealEffect = revealEffect,
                    expansionProgress = { expansionProgress },
                    modifier = QuickSettings.Modifiers.HorizontalPadding,
                    nRowsTarget = nRowsTarget,
                )
            }
        }

        val activeColor = QuickSettingsGrid.Colors.ActiveTileBackground
        val inactiveColor = QuickSettingsGrid.Colors.InactiveTileBackground
        PagerIndicators(
            pagerState,
            activeColor,
            inactiveColor,
            Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

fun nQuickSettingsPages(nTiles: Int, nRows: Int, nColumns: Int): Int {
    val nTilesPerPage = nRows * nColumns
    return ceil(nTiles.toFloat() / nTilesPerPage).roundToInt()
}

/**
 * The invisible anchor used to anchor the movement of the brightness slider and height of the tiles
 * that are not shared and appearing.
 */
@Composable
fun ContentScope.GridAnchor(isExpanded: Boolean, modifier: Modifier = Modifier) {
    // The width of this anchor does not matter, but the height is used to anchor the size of the
    // (dis)appearing tiles.
    Spacer(modifier.element(QuickSettingsGrid.Elements.GridAnchor).height(tileHeight(isExpanded)))
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ContentScope.PagerIndicators(
    pagerState: PagerState,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.element(QuickSettings.Elements.PagerIndicators).height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pagerState.pageCount) { i ->
            Canvas(modifier = Modifier.size(6.dp)) {
                val color = if (pagerState.currentPage == i) activeColor else inactiveColor
                drawCircle(color)
            }
        }
    }
}

/**
 * Snaps [pagerState] to the first page if we are transitioning from QS => Shade and the transition
 * goes past [QuickSettings.TransitionToShadeCommittedProgress].
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ContentScope.PagerStateResetter(pagerState: PagerState) {
    if (
        pagerState.currentPage != 0 &&
            layoutState.isTransitioning(from = Scenes.QuickSettings, to = Scenes.Shade)
    ) {
        val transition = layoutState.transitionState as TransitionState.Transition
        val scrollScope = rememberCoroutineScope()
        LaunchedEffect(transition, pagerState) {
            // Wait for the progress to reach TransitionToShadeCommittedProgress.
            snapshotFlow { transition.progress }
                .first { it >= QuickSettings.TransitionToShadeCommittedProgress }

            // Scroll to the first page.
            scrollScope.launch { pagerState.scrollToPage(0) }
        }
    }
}
