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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Column.Align
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Column.Justify
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
internal fun ColumnSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var justify by rememberSaveable { mutableStateOf(Justify.Start) }
    var align by rememberSaveable { mutableStateOf(Align.Center) }
    var weightPreset by rememberSaveable { mutableStateOf(ChildWeightPreset.NONE) }
    var itemCount by rememberSaveable { mutableIntStateOf(2) }

    LaunchedEffect(Unit) {
        updatePayload(
            justify = justify,
            align = align,
            weightPreset = weightPreset,
            itemCount = itemCount,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(
            title = "Vertical Arrangement (justify)",
            subtitle = "Arrangement of children along the vertical main axis",
        ) {
            ChoiceChips(
                options = Justify.entries,
                selectedOption = justify,
                onOptionSelected = {
                    justify = it
                    updatePayload(
                        justify = it,
                        align = align,
                        weightPreset = weightPreset,
                        itemCount = itemCount,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Horizontal Alignment (align)",
            subtitle = "Alignment of children along the horizontal cross axis",
        ) {
            ChoiceChips(
                options = Align.entries,
                selectedOption = align,
                onOptionSelected = {
                    align = it
                    updatePayload(
                        justify = justify,
                        align = it,
                        weightPreset = weightPreset,
                        itemCount = itemCount,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Child Weights",
            subtitle = "Relative vertical proportional weight applied to child items",
        ) {
            ChoiceChips(
                options = ChildWeightPreset.entries,
                selectedOption = weightPreset,
                onOptionSelected = {
                    weightPreset = it
                    updatePayload(
                        justify = justify,
                        align = align,
                        weightPreset = it,
                        itemCount = itemCount,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.label },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Number of Items", subtitle = "Number of child buttons in the column") {
            ChoiceChips(
                options = listOf(2, 3),
                selectedOption = itemCount,
                onOptionSelected = {
                    itemCount = it
                    updatePayload(
                        justify = justify,
                        align = align,
                        weightPreset = weightPreset,
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
    justify: Justify,
    align: Align,
    weightPreset: ChildWeightPreset,
    itemCount: Int,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val childIds = (1..itemCount).map { "item$it" }
    val components = buildList {
        add(
            A2uiComponentPayload(
                id = "root",
                type = "Column",
                properties =
                    mapOf(
                        "children" to childIds,
                        "justify" to justify.value,
                        "align" to align.value,
                    ),
            )
        )

        childIds.forEachIndexed { index, id ->
            val textId = "${id}_text"
            val weight =
                when (weightPreset) {
                    ChildWeightPreset.NONE -> null
                    ChildWeightPreset.EQUAL -> 1
                    ChildWeightPreset.WEIGHTED -> if (index == 0) 2 else 1
                }
            val variant =
                when (index) {
                    0 -> "primary"
                    1 -> "borderless"
                    else -> "default"
                }
            val buttonProps =
                buildMap<String, Any?> {
                    put("child", textId)
                    put("variant", variant)
                    put("action", mapOf("event" to mapOf("name" to "${id}_click")))
                    if (weight != null) {
                        put("weight", weight)
                    }
                }

            add(A2uiComponentPayload(id = id, type = "Button", properties = buttonProps))
            add(
                A2uiComponentPayload(
                    id = textId,
                    type = "Text",
                    properties = mapOf("text" to "Action Item ${index + 1}"),
                )
            )
        }
    }

    onPayloadUpdated(components, emptyMap())
}
