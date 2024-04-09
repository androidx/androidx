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

package androidx.tv.integration.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed

const val rowsCount = 20
const val columnsCount = 100

@Composable
fun LazyRowsAndColumns() {
    var pivotOffset by remember { mutableStateOf(PivotOffsets()) }
    TvLazyColumn(
        pivotOffsets = pivotOffset,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(rowsCount) { rowIndex ->
            SampleLazyRow(Modifier.onFocusChanged {
                if (it.hasFocus) {
                    pivotOffset = if (rowIndex == 2) PivotOffsets(0f) else PivotOffsets()
                }
            })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SampleLazyRow(modifier: Modifier = Modifier) {
    val colors = listOf(Color.Red, Color.Magenta, Color.Green, Color.Yellow, Color.Blue, Color.Cyan)
    val backgroundColors = List(columnsCount) { colors.random() }
    val focusRequester = remember { FocusRequester() }

    TvLazyRow(
        modifier = modifier
            .lazyListSemantics(1, columnsCount)
            .focusRestorer { focusRequester },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(backgroundColors) { index, item ->
            Card(
                modifier = Modifier
                    .ifElse(index == 0, Modifier.focusRequester(focusRequester))
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(0, 1, index, 1)
                    },
                backgroundColor = item
            )
        }
    }
}

@Composable
fun Modifier.lazyListSemantics(rowCount: Int = -1, columnCount: Int = -1): Modifier {
    return this.then(
        remember(rowCount, columnCount) {
            Modifier.semantics {
                collectionInfo = CollectionInfo(rowCount, columnCount)
            }
        }
    )
}
