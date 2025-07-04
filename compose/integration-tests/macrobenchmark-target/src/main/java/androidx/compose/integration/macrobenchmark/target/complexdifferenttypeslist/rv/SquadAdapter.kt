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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.rv

import android.view.ViewGroup
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.BaseListAdapter
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.BaseViewHolder
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder.SquadViewType

class SquadAdapter : BaseListAdapter<SquadViewType>(SquadViewType.values()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: SquadViewType): BaseViewHolder<*> =
        when (viewType) {
            SquadViewType.VIEW_TOP_PLAYERS -> SquadViewTopPlayersViewHolder(parent)
            SquadViewType.POSITION_SECTION_HEADER -> SquadSectionHeaderViewHolder(parent)
            SquadViewType.COACH -> SquadPlayerViewHolder(parent)
            SquadViewType.PLAYER_ROW -> SquadPlayerViewHolder(parent)
        }
}
