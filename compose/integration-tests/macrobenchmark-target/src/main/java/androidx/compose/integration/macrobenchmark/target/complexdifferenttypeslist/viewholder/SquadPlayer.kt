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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.viewholder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common.resolveTableRowColor
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadPlayerUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.theme.SquadTheme
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@Composable
fun SquadPlayer(uiModel: SquadPlayerUiModel, onPlayerClicked: () -> Unit) {
    Surface(
        color = resolveTableRowColor(uiModel = uiModel.colorDefinition),
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onPlayerClicked, enabled = uiModel.isClickable)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            uiModel.playerShirtNumber?.let {
                PlayerShirtNumber(shirtNumber = uiModel.playerShirtNumber)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Flag()
            Spacer(modifier = Modifier.width(12.dp))
            PlayerName(name = uiModel.playerName, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            PlayerYears(years = uiModel.playerYears)
        }
    }
}

@Composable
private fun PlayerShirtNumber(shirtNumber: String) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(width = 1.dp, color = SquadTheme.colors.shirtNumberBorder),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp),
        ) {
            Text(
                text = shirtNumber,
                style = SquadTheme.typography.medium.s11,
                color = SquadTheme.colors.shirtNumber,
            )
        }
    }
}

@Composable
private fun Flag() {
    Icon(
        imageVector = ImageVector.vectorResource(id = SquadTheme.drawables.emptyFlag),
        contentDescription = "Empty flag",
        modifier = Modifier.size(width = 12.dp, height = 12.dp),
    )
}

@Composable
private fun PlayerName(name: String, modifier: Modifier) {
    Text(
        text = name,
        color = SquadTheme.colors.primaryText,
        style = SquadTheme.typography.regular.s13,
        modifier = modifier,
    )
}

@Composable
private fun PlayerYears(years: String?) {
    years?.let {
        Text(
            text = it,
            color = SquadTheme.colors.secondaryText,
            style = SquadTheme.typography.regular.s11,
        )
    }
}
