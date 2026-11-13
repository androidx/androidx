/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.tooling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.PlaceholderState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScreenScaffoldDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.placeholderShimmer
import androidx.wear.compose.material3.rememberPlaceholderState
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp
import kotlin.math.ceil

/**
 * Value of Wear's [LocalReduceMotion] observed by the last composition of
 * [WearReduceMotionPreview].
 */
internal var observedWearReduceMotion: Boolean? = null

@Preview
@Composable
fun WearReduceMotionPreview() {
    observedWearReduceMotion = LocalReduceMotion.current
}

/** Value of Wear's [LocalReduceMotion] observed by the last [WearReduceMotionObservingWrapper]. */
internal var observedWrapperWearReduceMotion: Boolean? = null

/** A [PreviewWrapperProvider] recording the value of Wear's [LocalReduceMotion] it sees. */
class WearReduceMotionObservingWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        observedWrapperWearReduceMotion = LocalReduceMotion.current
        content()
    }
}

@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandScalingLazyColumnPreview() {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), autoCentering = null) {
        items(EXPAND_LONG_LIST_ITEM_COUNT) {
            Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
        }
    }
}

/** State of the list of the last composed [ExpandAutoCenteringScalingLazyColumnPreview]. */
internal var expandAutoCenteringScalingLazyListState: ScalingLazyListState? = null

/**
 * Default auto centering: the list inserts top and bottom spacers that grow with the viewport
 * unless collapsed when expanding vertically.
 */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandAutoCenteringScalingLazyColumnPreview() {
    val state = rememberScalingLazyListState()
    expandAutoCenteringScalingLazyListState = state
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        items(EXPAND_LONG_LIST_ITEM_COUNT) {
            Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
        }
    }
}

/**
 * A 2-item auto-centering `ScalingLazyColumn`. At `maxHeight`, `topSpacer + items + bottomSpacer`
 * equals the viewport height, so `maxScrollValue == 0` even before collapsing spacers; expanding
 * vertically must still collapse the spacers so the first item starts at the top.
 */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandShortAutoCenteringScalingLazyColumnPreview() {
    val state = rememberScalingLazyListState()
    expandAutoCenteringScalingLazyListState = state
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        items(2) { Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT)) }
    }
}

internal val EXPAND_LIST_TOP_PADDING = 20.dp

/** Bottom content padding, like the space `ScreenScaffold` reserves for an `EdgeButton`. */
internal val EXPAND_LIST_BOTTOM_PADDING = 80.dp

@Composable
private fun TransformingLazyColumnWithPadding(modifier: Modifier) {
    TransformingLazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(top = EXPAND_LIST_TOP_PADDING, bottom = EXPAND_LIST_BOTTOM_PADDING),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(EXPAND_LONG_LIST_ITEM_COUNT) {
            Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
        }
    }
}

/** A TransformingLazyColumn whose content padding must be entirely visible once expanded. */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandTransformingLazyColumnWithPaddingPreview() {
    TransformingLazyColumnWithPadding(Modifier.fillMaxSize())
}

/**
 * A short auto-centering ScalingLazyColumn next to a taller TransformingLazyColumn with content
 * padding.
 */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandAutoCenteringAndTransformingLazyColumnsPreview() {
    Row(Modifier.fillMaxSize()) {
        ScalingLazyColumn(modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(EXPAND_SHORT_LIST_ITEM_COUNT) {
                Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
            }
        }
        TransformingLazyColumnWithPadding(Modifier.weight(1f).fillMaxHeight())
    }
}

/**
 * Screen dimensions (`screenWidthDp()` and `screenHeightDp()`) observed by the last composition of
 * [ExpandWearPlaceholderAndScreenDimensionsPreview].
 */
internal var observedWearScreenSizeDp: IntSize? = null

/**
 * Default [ScreenScaffoldDefaults.contentPadding] (`PaddingDefaults.horizontalContentPadding()` and
 * `PaddingDefaults.verticalContentPadding()`) observed by the last composition of
 * [ExpandWearPlaceholderAndScreenDimensionsPreview].
 */
internal var observedWearScaffoldContentPadding: PaddingValues? = null

/**
 * [PlaceholderState] created by `rememberPlaceholderState` in the last composition of
 * [ExpandWearPlaceholderAndScreenDimensionsPreview].
 */
internal var observedWearPlaceholderState: PlaceholderState? = null

/**
 * A Wear Material3 `ScreenScaffold` + `TransformingLazyColumn` preview that uses
 * `rememberPlaceholderState` (`Modifier.placeholder` and `Modifier.placeholderShimmer`),
 * `ScreenScaffoldDefaults.contentPadding`, `screenWidthDp()`, and `screenHeightDp()`.
 */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandWearPlaceholderAndScreenDimensionsPreview() {
    observedWearScreenSizeDp = IntSize(screenWidthDp(), screenHeightDp())
    val contentPadding = ScreenScaffoldDefaults.contentPadding
    observedWearScaffoldContentPadding = contentPadding
    val listState = rememberTransformingLazyColumnState()
    val placeholderState = rememberPlaceholderState(isVisible = true)
    observedWearPlaceholderState = placeholderState

    AppScaffold {
        ScreenScaffold(scrollState = listState, contentPadding = contentPadding) { padding ->
            TransformingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = padding,
            ) {
                items(5) { index ->
                    TitleCard(
                        onClick = {},
                        title = {
                            Text(
                                text = "Item $index",
                                modifier = Modifier.placeholder(placeholderState),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().placeholderShimmer(placeholderState),
                    ) {
                        Text(
                            text = "Subtitle $index",
                            modifier = Modifier.placeholder(placeholderState),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Height-based minimum vertical content paddings (13% and 23% of `screenHeightDp()`) observed by
 * the last composition of [ExpandWearHeightBasedVerticalPaddingPreview].
 */
internal var observedWearMinimumTopContentPadding: Dp? = null
internal var observedWearMinimumBottomContentPadding: Dp? = null

/**
 * A `TransformingLazyColumn` whose top and bottom minimum vertical content paddings and item
 * vertical padding are derived from `screenHeightDp()`.
 */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandWearHeightBasedVerticalPaddingPreview() {
    val screenHeight = screenHeightDp()
    val topPadding = ceil(screenHeight * 0.13f).dp
    val bottomPadding = ceil(screenHeight * 0.23f).dp
    observedWearMinimumTopContentPadding = topPadding
    observedWearMinimumBottomContentPadding = bottomPadding
    val itemVerticalPadding = (screenHeight * 0.10f).dp

    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(EXPAND_SHORT_LIST_ITEM_COUNT) { index ->
            val minPaddingModifier =
                when (index) {
                    0 -> Modifier.minimumVerticalContentPadding(top = topPadding, bottom = 0.dp)
                    EXPAND_SHORT_LIST_ITEM_COUNT - 1 ->
                        Modifier.minimumVerticalContentPadding(top = 0.dp, bottom = bottomPadding)
                    else -> Modifier
                }
            Box(
                modifier =
                    minPaddingModifier
                        .fillMaxWidth()
                        .padding(vertical = itemVerticalPadding)
                        .height(EXPAND_ITEM_HEIGHT)
            )
        }
    }
}
