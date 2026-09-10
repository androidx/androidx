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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.integration.a2ui.icons.ChevronForwardIcon
import androidx.compose.material3.integration.a2ui.model.ComponentCategory
import androidx.compose.material3.integration.a2ui.model.UiComponent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

@Composable
fun ComponentListScreen(
    onComponentSelected: (UiComponent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.semantics { heading() },
                title = { Text("A2UI Components") },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier.fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .verticalFadingEdge(
                        lazyListState = lazyListState,
                        fadeHeight = 32.dp,
                        bottomOffset = innerPadding.calculateBottomPadding(),
                    ),
            contentPadding =
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            ComponentCategory.entries.forEach { category ->
                val components = UiComponent.byCategory[category] ?: emptyList()
                item(key = category.name) {
                    ComponentCategorySection(
                        category = category,
                        components = components,
                        onComponentSelected = onComponentSelected,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentCategorySection(
    category: ComponentCategory,
    components: List<UiComponent>,
    onComponentSelected: (UiComponent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardColors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
    val dividerColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
    val dividerModifier = Modifier.padding(horizontal = 16.dp)

    Column(modifier = modifier) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.primary,
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
                    ComponentRow(
                        component = component,
                        onClick = { onComponentSelected(component) },
                    )
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

@Composable
private fun ComponentRow(
    component: UiComponent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "View details", onClick = onClick)
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = component.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!component.isSupported) {
                ComingSoonBadge(modifier = Modifier.padding(end = 8.dp))
            }
            Icon(
                imageVector = ChevronForwardIcon,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComingSoonBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier,
    ) {
        Text(
            text = "Coming soon",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
