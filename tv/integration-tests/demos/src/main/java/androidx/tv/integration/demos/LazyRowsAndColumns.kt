/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.integration.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow

const val rowsCount = 20
const val columnsCount = 100

@Composable
fun LazyRowsAndColumns() {
    TvLazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(rowsCount) { SampleLazyRow() }
    }
}

@Composable
fun SampleLazyRow() {
    val colors = listOf(Color.Red, Color.Magenta, Color.Green, Color.Yellow, Color.Blue, Color.Cyan)
    val backgroundColors = List(columnsCount) { colors.random() }

    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        backgroundColors.forEach { backgroundColor ->
            item {
                Box(
                    modifier = Modifier
                        .background(backgroundColor.copy(alpha = 0.3f))
                        .width(200.dp)
                        .height(150.dp)
                        .drawBorderOnFocus()
                        .focusable()
                )
            }
        }
    }
}
