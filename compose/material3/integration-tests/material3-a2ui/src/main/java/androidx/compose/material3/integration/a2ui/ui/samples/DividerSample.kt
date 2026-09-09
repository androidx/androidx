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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Divider.Axis
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun DividerSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var axis by rememberSaveable { mutableStateOf(Axis.Horizontal) }

    LaunchedEffect(Unit) { onPayloadUpdated(componentsForAxis(axis), emptyMap()) }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Divider Axis", subtitle = "Orientation of the dividing line rule") {
            ChoiceChips(
                options = Axis.entries,
                selectedOption = axis,
                onOptionSelected = {
                    axis = it
                    onPayloadUpdated(componentsForAxis(it), emptyMap())
                },
                label = { it.value },
            )
        }
    }
}

private val HorizontalDividerComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Column",
            properties =
                mapOf(
                    "children" to listOf("text_above", "divider_comp", "text_below"),
                    "justify" to "center",
                    "align" to "stretch",
                ),
        ),
        A2uiComponentPayload(
            id = "text_above",
            type = "Text",
            properties = mapOf("text" to "Section Above Divider", "variant" to "body"),
        ),
        A2uiComponentPayload(
            id = "divider_comp",
            type = "Divider",
            properties = mapOf("axis" to "horizontal"),
        ),
        A2uiComponentPayload(
            id = "text_below",
            type = "Text",
            properties = mapOf("text" to "Section Below Divider", "variant" to "body"),
        ),
    )

private val VerticalDividerComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Row",
            properties =
                mapOf(
                    "children" to listOf("text_left", "divider_comp", "text_right"),
                    "justify" to "center",
                    "align" to "stretch",
                ),
        ),
        A2uiComponentPayload(
            id = "text_left",
            type = "Text",
            properties = mapOf("text" to "Left Pane", "variant" to "body"),
        ),
        A2uiComponentPayload(
            id = "divider_comp",
            type = "Divider",
            properties = mapOf("axis" to "vertical"),
        ),
        A2uiComponentPayload(
            id = "text_right",
            type = "Text",
            properties = mapOf("text" to "Right Pane", "variant" to "body"),
        ),
    )

private fun componentsForAxis(axis: Axis): List<A2uiComponentPayload> =
    when (axis) {
        Axis.Horizontal -> HorizontalDividerComponents
        Axis.Vertical -> VerticalDividerComponents
    }
