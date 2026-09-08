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

package androidx.compose.material3.integration.a2ui.ui.samples

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.List.Align
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.List.Direction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ListSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var direction by rememberSaveable { mutableStateOf(Direction.Vertical) }
    var align by rememberSaveable { mutableStateOf(Align.Stretch) }
    var itemCount by rememberSaveable { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        updatePayload(
            direction = direction,
            align = align,
            itemCount = itemCount,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Layout Direction", subtitle = "Scroll direction of the list") {
            ChoiceChips(
                options = Direction.entries,
                selectedOption = direction,
                onOptionSelected = {
                    direction = it
                    updatePayload(
                        direction = it,
                        align = align,
                        itemCount = itemCount,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Cross-Axis Alignment",
            subtitle = "Alignment of children along cross axis",
        ) {
            ChoiceChips(
                options = Align.entries,
                selectedOption = align,
                onOptionSelected = {
                    align = it
                    updatePayload(
                        direction = direction,
                        align = it,
                        itemCount = itemCount,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Item Count", subtitle = "Number of child card elements in the list") {
            ChoiceChips(
                options = listOf(3, 5, 8),
                selectedOption = itemCount,
                onOptionSelected = {
                    itemCount = it
                    updatePayload(
                        direction = direction,
                        align = align,
                        itemCount = it,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { "$it items" },
            )
        }
    }
}

private fun updatePayload(
    direction: Direction,
    align: Align,
    itemCount: Int,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val childIds = (1..itemCount).map { "item$it" }
    val components = buildList {
        add(
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf(
                        "children" to childIds,
                        "direction" to direction.value,
                        "align" to align.value,
                    ),
            )
        )

        childIds.forEachIndexed { index, id ->
            val textId = "${id}_text"
            add(
                A2uiComponentPayload(
                    id = id,
                    type = "Card",
                    properties = mapOf("child" to textId),
                )
            )
            add(
                A2uiComponentPayload(
                    id = textId,
                    type = "Text",
                    properties = mapOf("text" to "List Item #${index + 1}", "variant" to "body"),
                )
            )
        }
    }

    onPayloadUpdated(components, emptyMap())
}
