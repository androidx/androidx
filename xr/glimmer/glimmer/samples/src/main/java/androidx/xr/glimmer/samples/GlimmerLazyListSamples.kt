/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.GlimmerLazyColumn
import androidx.xr.glimmer.list.items

@Sampled
@Composable
fun GlimmerLazyColumnSample() {
    GlimmerLazyColumn {
        item { ListItem { Text("Header") } }
        items(count = 10) { index -> ListItem { Text("Item-$index") } }
        item { ListItem { Text("Footer") } }
    }
}

@Sampled
@Composable
fun GlimmerLazyColumnWithTitleChipSample() {
    val ingredientItems =
        listOf("Milk", "Flour", "Egg", "Salt", "Apples", "Butter", "Vanilla", "Sugar", "Cinnamon")
    GlimmerLazyColumn(title = { TitleChip { Text("Ingredients") } }) {
        items(ingredientItems) { text -> ListItem { Text(text) } }
    }
}

@Preview
@Composable
private fun GlimmerLazyColumnPreview() {
    GlimmerTheme { GlimmerLazyColumnSample() }
}

@Preview
@Composable
private fun GlimmerLazyColumnWithTitleChipPreview() {
    GlimmerTheme { GlimmerLazyColumnWithTitleChipSample() }
}
