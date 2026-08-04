/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.integration.a2ui.icons.ChevronForwardIcon
import androidx.compose.material3.integration.a2ui.model.ComponentCategory
import androidx.compose.material3.integration.a2ui.model.UiComponent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentListScreen(onComponentSelected: (UiComponent) -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val categoryTitleColor = colorScheme.primary
    val cardColors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
    val componentTextColor = colorScheme.onSurface
    val iconTintColor = colorScheme.onSurfaceVariant
    val dividerColor = colorScheme.outlineVariant.copy(alpha = 0.5f)

    val dividerModifier = Modifier.padding(horizontal = 16.dp)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("A2UI Components") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            ComponentCategory.entries.forEach { category ->
                val components = UiComponent.byCategory[category] ?: emptyList()
                item(key = category.name) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryTitleColor,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = cardColors,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                components.fastForEachIndexed { index, component ->
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .clickable(onClickLabel = "View details") {
                                                    onComponentSelected(component)
                                                }
                                                .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = component.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = componentTextColor,
                                        )
                                        Icon(
                                            imageVector = ChevronForwardIcon,
                                            contentDescription = null,
                                            tint = iconTintColor,
                                        )
                                    }
                                    if (index < components.lastIndex) {
                                        HorizontalDivider(
                                            modifier = dividerModifier,
                                            color = dividerColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
