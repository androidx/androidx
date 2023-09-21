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

package androidx.compose.mpp.demo.components.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarExample() {
    Column {
        Box(Modifier.fillMaxWidth().padding(5.dp)) {
            var text by rememberSaveable { mutableStateOf("") }
            var active by rememberSaveable { mutableStateOf(false) }
            SearchBar(
                modifier = Modifier.align(Alignment.TopCenter),
                query = text,
                onQueryChange = { text = it },
                onSearch = { active = false },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("SearchBar") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
            ) {
                SearchBarSampleContent {
                    text = it
                    active = false
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(5.dp)) {
            var text by rememberSaveable { mutableStateOf("") }
            var active by rememberSaveable { mutableStateOf(false) }
            DockedSearchBar(
                modifier = Modifier.align(Alignment.TopCenter),
                query = text,
                onQueryChange = { text = it },
                onSearch = { active = false },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("DockedSearchBar") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
            ) {
                SearchBarSampleContent {
                    text = it
                    active = false
                }
            }
        }
    }
}

@Composable
private fun SearchBarSampleContent(onClick: (String) -> Unit) {
    repeat(4) { index ->
        val resultText = "Suggestion $index"
        ListItem(
            headlineContent = { Text(resultText) },
            supportingContent = { Text("Additional info") },
            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onClick(resultText) }
        )
    }
}
