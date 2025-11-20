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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SectionHeaderUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.theme.SquadTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(modifier: Modifier = Modifier, uiModel: SectionHeaderUiModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 16.dp, end = 12.dp),
    ) {
        Title(
            title = uiModel.title,
            modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
        )
    }
}

@Composable
private fun Title(title: String, modifier: Modifier) {
    Text(
        text = title,
        style = SquadTheme.typography.medium.s14,
        color = SquadTheme.colors.sectionHeaderTitle,
        modifier = modifier,
    )
}
