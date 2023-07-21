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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ImmersiveList

@Composable
fun ImmersiveListContent() {
    TvLazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(3) { SampleLazyRow() }
        item { SampleImmersiveList() }
        items(3) { SampleLazyRow() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)
@Composable
private fun SampleImmersiveList() {
    val immersiveListHeight = 300.dp
    val cardSpacing = 10.dp
    val cardHeight = 150.dp
    val backgrounds = listOf(
        Color.Red,
        Color.Blue,
        Color.Magenta,
    )

    FocusGroup {
        ImmersiveList(
            modifier = Modifier
                .height(immersiveListHeight + cardHeight / 2)
                .fillMaxWidth(),
            background = { index, _ ->
                Box(
                    modifier = Modifier
                        .background(backgrounds[index].copy(alpha = 0.3f))
                        .height(immersiveListHeight)
                        .fillMaxWidth()
                )
            }
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                modifier = Modifier.lazyListSemantics(1, backgrounds.count())
            ) {
                itemsIndexed(backgrounds) { index, backgroundColor ->
                    val cardModifier =
                        if (index == 0)
                            Modifier.initiallyFocused()
                        else
                            Modifier.restorableFocus()

                    Card(
                        modifier = cardModifier
                            .semantics {
                                collectionItemInfo = CollectionItemInfo(0, 1, index, 1)
                            }
                            .immersiveListItem(index),
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
    }
}
