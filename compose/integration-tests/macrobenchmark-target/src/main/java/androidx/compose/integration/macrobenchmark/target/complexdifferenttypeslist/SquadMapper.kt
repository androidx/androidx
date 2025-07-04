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

import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.AdapterItemWrapper
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.CommonAdapterItemType
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.and
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.sticky
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.with
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.withId
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SectionHeaderUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadPlayerUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadViewTopPlayersUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.TableRowColorUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.wrapper.SquadSectionDataWrapper
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder.SquadViewType.*

class SquadMapper {

    fun map() =
        arrayListOf<AdapterItemWrapper>().apply {
            repeat(100) {
                add(CommonAdapterItemType.SPACE_12.withId("top_spacing"))
                add(VIEW_TOP_PLAYERS with viewTopPlayers and "view_top_players")
                add(CommonAdapterItemType.SPACE_24.withId("manager_section_top_spacing"))
                add(
                    POSITION_SECTION_HEADER with
                        coachSection.sectionHeader and
                        "manager_section_header" sticky
                        (true)
                )
                coachSection.playersList.forEach { player ->
                    add(COACH with player and "coach_${player.playerId}")
                }
                add(CommonAdapterItemType.SPACE_24.withId("attackers_section_top_spacing"))
                add(
                    POSITION_SECTION_HEADER with
                        attackersSection.sectionHeader and
                        "attack_section_header" sticky
                        (true)
                )
                attackersSection.playersList.forEachIndexed { index, player ->
                    add(PLAYER_ROW with player and "attacker_${player.playerId}_$index")
                }
                add(CommonAdapterItemType.SPACE_24.withId("midfielders_section_top_spacing"))
                add(
                    POSITION_SECTION_HEADER with
                        midfieldersSection.sectionHeader and
                        "midfielders_section_header" sticky
                        (true)
                )
                midfieldersSection.playersList.forEachIndexed { index, player ->
                    add(PLAYER_ROW with player and "midfielder_${player.playerId}_$index")
                }
                add(CommonAdapterItemType.SPACE_24.withId("defenders_section_top_spacing"))
                add(
                    POSITION_SECTION_HEADER with
                        defendersSection.sectionHeader and
                        "defence_section_header" sticky
                        (true)
                )
                defendersSection.playersList.forEachIndexed { index, player ->
                    add(PLAYER_ROW with player and "defender_${player.playerId}_$index")
                }
                add(CommonAdapterItemType.SPACE_24.withId("goalkeepers_section_top_spacing"))
                add(
                    POSITION_SECTION_HEADER with
                        goalkeepersSection.sectionHeader and
                        "goalkeepers_section_header" sticky
                        (true)
                )
                goalkeepersSection.playersList.forEachIndexed { index, player ->
                    add(PLAYER_ROW with player and "goalkeeper_${player.playerId}_$index")
                }
                add(CommonAdapterItemType.SPACE_30.withId("bottomSpacing"))
            }
        }

    private val viewTopPlayers = SquadViewTopPlayersUiModel(title = "VIEW TOP PLAYERS")

    private val coachSection =
        SquadSectionDataWrapper(
            sectionHeader = SectionHeaderUiModel(title = "Coach"),
            playersList =
                listOf(
                    SquadPlayerUiModel(
                        playerId = "coach",
                        playerName = "John Doe",
                        playerShirtNumber = null,
                        playerYears = null,
                        isFirstInTable = true,
                        isLastInTable = true,
                        colorDefinition = TableRowColorUiModel(),
                        isClickable = false,
                    )
                ),
        )

    private val attackersSection =
        SquadSectionDataWrapper(
            sectionHeader = SectionHeaderUiModel(title = "Attackers"),
            playersList =
                List(10) { index ->
                    SquadPlayerUiModel(
                        playerId = "attacker_${index}",
                        playerName = "John Doe $index",
                        playerYears = (20 + index).toString(),
                        playerShirtNumber = "$index",
                        isFirstInTable = index == 0,
                        isLastInTable = index == 9,
                        colorDefinition =
                            TableRowColorUiModel(hasDarkerBackground = index % 2 != 0),
                        isClickable = true,
                    )
                },
        )

    private val midfieldersSection =
        SquadSectionDataWrapper(
            sectionHeader = SectionHeaderUiModel(title = "Midfielders"),
            playersList =
                List(10) { index ->
                    SquadPlayerUiModel(
                        playerId = "midfielder${index}",
                        playerName = "John Doe $index",
                        playerYears = (20 + index).toString(),
                        playerShirtNumber = "$index",
                        isFirstInTable = index == 0,
                        isLastInTable = index == 9,
                        colorDefinition =
                            TableRowColorUiModel(hasDarkerBackground = index % 2 != 0),
                        isClickable = true,
                    )
                },
        )

    private val defendersSection =
        SquadSectionDataWrapper(
            sectionHeader = SectionHeaderUiModel(title = "Defenders"),
            playersList =
                List(10) { index ->
                    SquadPlayerUiModel(
                        playerId = "defender${index}",
                        playerName = "John Doe $index",
                        playerYears = (20 + index).toString(),
                        playerShirtNumber = "$index",
                        isFirstInTable = index == 0,
                        isLastInTable = index == 9,
                        colorDefinition =
                            TableRowColorUiModel(hasDarkerBackground = index % 2 != 0),
                        isClickable = true,
                    )
                },
        )

    private val goalkeepersSection =
        SquadSectionDataWrapper(
            sectionHeader = SectionHeaderUiModel(title = "Goalkeepers"),
            playersList =
                List(3) { index ->
                    SquadPlayerUiModel(
                        playerId = "goalkeepers${index}",
                        playerName = "John Doe $index",
                        playerYears = (20 + index).toString(),
                        playerShirtNumber = "$index",
                        isFirstInTable = index == 0,
                        isLastInTable = index == 9,
                        colorDefinition =
                            TableRowColorUiModel(hasDarkerBackground = index % 2 != 0),
                        isClickable = true,
                    )
                },
        )
}
