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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Device spec of a phone-sized preview that expands vertically to fit its scrollable content. */
internal const val EXPAND_VERTICAL_DEVICE = "spec:width=411dp,height=891dp,expand=vertical"

/** Device spec of a phone-sized preview that expands horizontally to fit its scrollable content. */
internal const val EXPAND_HORIZONTAL_DEVICE = "spec:width=411dp,height=891dp,expand=horizontal"

internal val EXPAND_ITEM_HEIGHT = 40.dp
internal val EXPAND_BOTTOM_BAR_HEIGHT = 40.dp
internal const val EXPAND_LONG_LIST_ITEM_COUNT = 15
internal const val EXPAND_SHORT_LIST_ITEM_COUNT = 3
internal val EXPAND_ITEM_WIDTH = 40.dp

@Composable
private fun ScaffoldWithLazyColumn(itemCount: Int) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = { Box(Modifier.fillMaxWidth().height(EXPAND_BOTTOM_BAR_HEIGHT)) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            items(itemCount) {
                Text("Item $it", Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
            }
        }
    }
}

@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandLongLazyColumnInScaffoldPreview() {
    ScaffoldWithLazyColumn(EXPAND_LONG_LIST_ITEM_COUNT)
}

@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandShortLazyColumnInScaffoldPreview() {
    ScaffoldWithLazyColumn(EXPAND_SHORT_LIST_ITEM_COUNT)
}

@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandVerticalScrollColumnPreview() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        repeat(EXPAND_LONG_LIST_ITEM_COUNT) {
            Text("Item $it", Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
        }
    }
}

/**
 * A fillMaxSize() preview without scrollable content: when expanded, it is as tall as the viewport,
 * otherwise as tall as its content when measured with an UNSPECIFIED height.
 */
@Preview
@Composable
fun ExpandFixedHeightPreview() {
    Box(Modifier.fillMaxSize()) { Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT)) }
}

/** Window height seen by the media queries of the last composition of [MediaQueryHeightPreview]. */
internal var observedMediaQueryWindowHeight: Dp? = null

@OptIn(ExperimentalMediaQueryApi::class)
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun MediaQueryHeightPreview() {
    observedMediaQueryWindowHeight = mediaQuery { windowHeight }
    ExpandVerticalScrollColumnPreview()
}

@Composable
private fun LazyRowPreview(itemCount: Int) {
    LazyRow(modifier = Modifier.fillMaxSize()) {
        items(itemCount) { Text("Item $it", Modifier.fillMaxHeight().width(EXPAND_ITEM_WIDTH)) }
    }
}

@Preview(device = EXPAND_HORIZONTAL_DEVICE)
@Composable
fun ExpandLongLazyRowPreview() {
    LazyRowPreview(EXPAND_LONG_LIST_ITEM_COUNT)
}

@Preview(device = EXPAND_HORIZONTAL_DEVICE)
@Composable
fun ExpandShortLazyRowPreview() {
    LazyRowPreview(EXPAND_SHORT_LIST_ITEM_COUNT)
}

@Preview(device = EXPAND_HORIZONTAL_DEVICE)
@Composable
fun ExpandHorizontalScrollRowPreview() {
    Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        repeat(EXPAND_LONG_LIST_ITEM_COUNT) {
            Text("Item $it", Modifier.fillMaxHeight().width(EXPAND_ITEM_WIDTH))
        }
    }
}

/** Window width seen by the media queries of the last composition of [MediaQueryWidthPreview]. */
internal var observedMediaQueryWindowWidth: Dp? = null

@OptIn(ExperimentalMediaQueryApi::class)
@Preview(device = EXPAND_HORIZONTAL_DEVICE)
@Composable
fun MediaQueryWidthPreview() {
    observedMediaQueryWindowWidth = mediaQuery { windowWidth }
    ExpandHorizontalScrollRowPreview()
}
