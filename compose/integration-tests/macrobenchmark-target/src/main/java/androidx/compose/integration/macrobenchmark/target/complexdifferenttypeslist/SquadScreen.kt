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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.AdapterItemWrapper
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.populate
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SectionHeaderUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadPlayerUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadViewTopPlayersUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder.SquadPlayer
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder.SquadSectionHeader
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder.SquadViewTopPlayers
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder.SquadViewType
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun SquadScreen(items: List<AdapterItemWrapper>) {
    Surface {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().semantics { contentDescription = "IamLazy" },
        ) {
            populate<SquadViewType>(
                listData = items,
                factory = { viewType, data, _ ->
                    when (viewType) {
                        SquadViewType.VIEW_TOP_PLAYERS ->
                            SquadViewTopPlayers(
                                viewTopPlayersUiModel = data as SquadViewTopPlayersUiModel,
                                onClicked = { /*No-op.*/ },
                            )
                        SquadViewType.POSITION_SECTION_HEADER ->
                            SquadSectionHeader(uiModel = data as SectionHeaderUiModel)
                        SquadViewType.COACH ->
                            SquadPlayer(
                                uiModel = data as SquadPlayerUiModel,
                                onPlayerClicked = { /* No-op. */ },
                            )
                        SquadViewType.PLAYER_ROW ->
                            SquadPlayer(
                                uiModel = data as SquadPlayerUiModel,
                                onPlayerClicked = { /*No-op.*/ },
                            )
                    }
                },
            )
        }
    }
}
