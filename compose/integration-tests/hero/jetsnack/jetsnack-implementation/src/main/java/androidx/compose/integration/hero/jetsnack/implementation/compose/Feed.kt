/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.jetsnack.implementation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.integration.hero.jetsnack.implementation.SnackCollection
import androidx.compose.integration.hero.jetsnack.implementation.SnackRepo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace

@Composable
fun Feed(onSnackClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val snackCollections = remember { SnackRepo.getSnacks() }
    Feed(snackCollections, onSnackClick, modifier.semantics { testTagsAsResourceId = true })
}

@Composable
private fun Feed(
    snackCollections: List<SnackCollection>,
    onSnackClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) =
    trace("Feed") {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box {
                SnackCollectionList(snackCollections, onSnackClick)
                DestinationBar()
            }
        }
    }

@Composable
private fun SnackCollectionList(
    snackCollections: List<SnackCollection>,
    onSnackClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) =
    trace("SnackCollectionList") {
        Box(modifier) {
            LazyColumn(modifier = Modifier.testTag("snack_list")) {
                item {
                    Spacer(
                        Modifier.windowInsetsTopHeight(
                            WindowInsets.statusBars.add(WindowInsets(top = 56.dp))
                        )
                    )
                }
                itemsIndexed(snackCollections) { index, snackCollection ->
                    if (index > 0) {
                        JetsnackDivider(thickness = 2.dp)
                    }

                    SnackCollection(
                        snackCollection = snackCollection,
                        onSnackClick = onSnackClick,
                        index = index,
                        modifier = Modifier.testTag("snack_collection")
                    )
                }
            }
        }
    }
