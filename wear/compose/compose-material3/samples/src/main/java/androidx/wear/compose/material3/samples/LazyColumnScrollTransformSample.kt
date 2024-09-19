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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.LazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.scrollTransform
import androidx.wear.compose.material3.lazy.targetMorphingHeight

@Sampled
@Preview
@Composable
fun LazyColumnScalingMorphingEffectSample() {
    val allIngredients = listOf("2 eggs", "tomato", "cheese", "bread")
    val state = rememberLazyColumnState()
    AppScaffold {
        ScreenScaffold(state, bottomButton = { EdgeButton(onClick = {}) { Text("Okay") } }) {
            LazyColumn(
                state = state,
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 10.dp)
            ) {
                item(contentType = "header") {
                    // No modifier is applied - no Material 3 Motion.
                    ListHeader { Text("Ingredients") }
                }

                items(allIngredients, key = { it }) { ingredient ->
                    Text(
                        ingredient,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier.fillMaxWidth()
                                // Apply Material 3 Motion transformations.
                                .scrollTransform(
                                    this,
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(10.dp)
                    )
                }
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun LazyColumnTargetMorphingHeightSample() {
    data class MenuItem(val title: String, val price: Float)

    val drinks =
        listOf(
            MenuItem("Cappuccino", 2.5f),
            MenuItem("Late", 3f),
            MenuItem("Flat White", 3.2f),
            MenuItem("Americano", 1.5f),
            MenuItem("Black tea", 2f),
            MenuItem("London fog", 2.6f),
        )
    val state = rememberLazyColumnState()
    AppScaffold {
        ScreenScaffold(state, bottomButton = { EdgeButton(onClick = {}) { Text("Okay") } }) {
            LazyColumn(
                state = state,
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 10.dp),
            ) {
                item(contentType = "header") {
                    // No modifier is applied - no Material 3 Motion transformations.
                    ListHeader { Text("Drinks", style = MaterialTheme.typography.labelLarge) }
                }
                items(drinks, key = { it.title }) { notification ->
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                // Apply Material 3 Motion effect.
                                .scrollTransform(
                                    this@items,
                                    backgroundColor = Color.DarkGray,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            notification.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            // Morphing is focusing on the title.
                            modifier = Modifier.targetMorphingHeight(this@items)
                        )
                        // Price is revealed after the morph.
                        Text("$${notification.price}")
                    }
                }
            }
        }
    }
}
