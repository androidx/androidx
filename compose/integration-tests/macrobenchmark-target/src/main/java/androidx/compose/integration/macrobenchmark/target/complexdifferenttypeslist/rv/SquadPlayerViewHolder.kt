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
import androidx.compose.integration.macrobenchmark.target.R
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.BaseViewBindingHolder
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.inflateBinding
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadPlayerUiModel
import androidx.compose.integration.macrobenchmark.target.databinding.ItemSquadPlayerBinding
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

class SquadPlayerViewHolder(parent: ViewGroup) :
    BaseViewBindingHolder<ItemSquadPlayerBinding, SquadPlayerUiModel>(
        parent.inflateBinding(ItemSquadPlayerBinding::inflate)
    ) {
    override fun ItemSquadPlayerBinding.bind(viewModel: SquadPlayerUiModel) =
        with(viewModel) {
            if (colorDefinition.hasDarkerBackground) {
                container.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.darkerTableRow)
                )
            } else {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            }

            playerShirtNumberContainer.isVisible = playerShirtNumber != null

            playerShirtNumberTextView.text = playerShirtNumber
            playerNameTextView.text = playerName
            playerAgeTextView.text = playerYears
        }
}
