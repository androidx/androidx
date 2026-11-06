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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.integration.a2ui.icons.ChevronForwardIcon
import androidx.compose.material3.integration.a2ui.model.CanonicalExample
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GalleryScreen(
    examples: List<CanonicalExample>,
    onExampleSelected: (CanonicalExample) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bottomFadeOffset: Dp = 0.dp,
) {
    LazyColumn(
        state = state,
        modifier =
            modifier
                .fillMaxSize()
                .verticalFadingEdge(
                    lazyListState = state,
                    fadeHeight = 32.dp,
                    bottomOffset = bottomFadeOffset,
                ),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(examples, key = { _, example -> example.fileName }) { index, example ->
            ExampleCardItem(
                example = example,
                isFirst = index == 0,
                isLast = index == examples.lastIndex,
                onClick = { onExampleSelected(example) },
            )
        }
    }
}

/**
 * Shows [example] in its own [Card], so that the examples can be composed lazily. The cards have no
 * spacing between them, and only the top corners of the first card and the bottom corners of the
 * last card are rounded, so together they look like a single card listing all examples.
 */
@Composable
private fun ExampleCardItem(
    example: CanonicalExample,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val topCornerRadius = if (isFirst) CardCornerRadius else 0.dp
    val bottomCornerRadius = if (isLast) CardCornerRadius else 0.dp

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = if (isFirst) 16.dp else 0.dp,
                    end = 16.dp,
                    bottom = if (isLast) 16.dp else 0.dp,
                ),
        shape =
            RoundedCornerShape(
                topStart = topCornerRadius,
                topEnd = topCornerRadius,
                bottomEnd = bottomCornerRadius,
                bottomStart = bottomCornerRadius,
            ),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
    ) {
        ExampleRow(example = example, onClick = onClick)
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun ExampleRow(
    example: CanonicalExample,
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
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = example.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
            if (example.description.isNotEmpty()) {
                Text(
                    text = example.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            imageVector = ChevronForwardIcon,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
        )
    }
}

private val CardCornerRadius = 16.dp
