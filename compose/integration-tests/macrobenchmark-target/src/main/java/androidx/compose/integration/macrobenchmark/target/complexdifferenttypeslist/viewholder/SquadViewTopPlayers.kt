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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadViewTopPlayersUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.theme.SquadTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@Composable
fun SquadViewTopPlayers(viewTopPlayersUiModel: SquadViewTopPlayersUiModel, onClicked: () -> Unit) {
    Surface(shape = SquadTheme.shapes.allRounded, modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onClicked)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            CupIcon()
            Title(title = viewTopPlayersUiModel.title)
        }
    }
}

@Composable
private fun CupIcon() {
    Image(
        imageVector = ImageVector.vectorResource(id = SquadTheme.drawables.cupCircle),
        contentDescription = "Cup icon",
    )
}

@Composable
private fun Title(title: String) {
    Text(
        text = title.uppercase(),
        style = SquadTheme.typography.medium.s12,
        color = SquadTheme.colors.secondaryText,
        modifier = Modifier.padding(start = 12.dp),
    )
}
