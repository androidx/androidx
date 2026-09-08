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
internal fun CardSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var preset by rememberSaveable { mutableStateOf(CardContentPreset.PROFILE) }

    LaunchedEffect(Unit) { onPayloadUpdated(preset.components, emptyMap()) }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(
            title = "Card Content Preset",
            subtitle = "Template and hierarchy rendered inside the Card",
        ) {
            ChoiceChips(
                options = CardContentPreset.entries,
                selectedOption = preset,
                onOptionSelected = {
                    preset = it
                    onPayloadUpdated(it.components, emptyMap())
                },
                label = { it.label },
            )
        }
    }
}

private enum class CardContentPreset(
    val label: String,
    val components: List<A2uiComponentPayload>,
) {
    PROFILE("Profile Card", ProfileCardComponents),
    INFO_BANNER("Info Banner", InfoBannerComponents),
    SIMPLE_NOTE("Simple Note", SimpleNoteComponents),
}

private val ProfileCardComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Card",
            properties = mapOf("child" to "card_column"),
        ),
        A2uiComponentPayload(
            id = "card_column",
            type = "Column",
            properties =
                mapOf(
                    "children" to listOf("card_icon", "card_title", "card_desc", "card_btn"),
                    "justify" to "center",
                    "align" to "center",
                ),
        ),
        A2uiComponentPayload(
            id = "card_icon",
            type = "Icon",
            properties = mapOf("name" to "accountCircle"),
        ),
        A2uiComponentPayload(
            id = "card_title",
            type = "Text",
            properties = mapOf("text" to "Alex Rivera", "variant" to "h4"),
        ),
        A2uiComponentPayload(
            id = "card_desc",
            type = "Text",
            properties = mapOf("text" to "Software Engineer • AndroidX", "variant" to "body"),
        ),
        A2uiComponentPayload(
            id = "card_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "btn_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "follow_click")),
                ),
        ),
        A2uiComponentPayload(
            id = "btn_text",
            type = "Text",
            properties = mapOf("text" to "View Profile"),
        ),
    )

private val InfoBannerComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Card",
            properties = mapOf("child" to "card_row"),
        ),
        A2uiComponentPayload(
            id = "card_row",
            type = "Row",
            properties =
                mapOf(
                    "children" to listOf("info_icon", "info_text"),
                    "justify" to "start",
                    "align" to "center",
                ),
        ),
        A2uiComponentPayload(
            id = "info_icon",
            type = "Icon",
            properties = mapOf("name" to "info"),
        ),
        A2uiComponentPayload(
            id = "info_text",
            type = "Text",
            properties =
                mapOf(
                    "text" to "New updates are available for your A2UI surfaces.",
                    "variant" to "body",
                ),
        ),
    )

private val SimpleNoteComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Card",
            properties = mapOf("child" to "note_text"),
        ),
        A2uiComponentPayload(
            id = "note_text",
            type = "Text",
            properties =
                mapOf(
                    "text" to
                        "Card is a container component designed to present " +
                            "grouped, cohesive information.",
                    "variant" to "body",
                ),
        ),
    )
