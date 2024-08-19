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

package androidx.tv.integration.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShowsGrid(modifier: Modifier = Modifier) {
    var keyword by remember { mutableStateOf("") }
    val movies by
        remember(keyword) {
            mutableStateOf(allMovies.filter { movie -> movie.name.contains(keyword) })
        }
    Column(
        modifier = Modifier.height(520.dp).padding(top = 70.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 58.dp).fillMaxWidth()) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text(text = "Search", color = Color.White) },
                modifier = modifier.fillMaxWidth().align(Alignment.Center),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                    )
            )
        }

        AppSpacer(height = 20.dp)

        if (movies.isEmpty()) {
            AppSpacer(height = 20.dp)
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No movies matched",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
        }

        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 58.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().bringIntoViewIfChildrenAreFocused(),
        ) {
            items(movies.size) {
                val movie = movies[it]

                Box(modifier = Modifier.padding(end = 30.dp)) {
                    ImageCard(
                        movie,
                        customCardWidth = 150.dp,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
