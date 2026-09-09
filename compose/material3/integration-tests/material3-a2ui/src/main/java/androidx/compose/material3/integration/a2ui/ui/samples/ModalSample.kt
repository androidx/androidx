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
internal fun ModalSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var preset by rememberSaveable { mutableStateOf(ModalPreset.CONFIRMATION) }

    LaunchedEffect(Unit) { onPayloadUpdated(preset.components, preset.dataModel) }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(
            title = "Modal Dialog Preset",
            subtitle = "Tap the trigger button in the preview above to open the modal dialog",
        ) {
            ChoiceChips(
                options = ModalPreset.entries,
                selectedOption = preset,
                onOptionSelected = {
                    preset = it
                    onPayloadUpdated(it.components, it.dataModel)
                },
                label = { it.label },
            )
        }
    }
}

private enum class ModalPreset(
    val label: String,
    val components: List<A2uiComponentPayload>,
    val dataModel: Map<String, Any?> = emptyMap(),
) {
    CONFIRMATION("Confirmation", ConfirmationModalComponents),
    INFO("Info Dialog", InfoModalComponents),
    FORM(
        "Feedback Form",
        FormModalComponents,
        dataModel = mapOf("feedbackText" to "Love the Material 3 integration!"),
    ),
}

private val ConfirmationModalComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Modal",
            properties = mapOf("trigger" to "open_btn", "content" to "dialog_col"),
        ),
        A2uiComponentPayload(
            id = "open_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "open_btn_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "open_modal")),
                ),
        ),
        A2uiComponentPayload(
            id = "open_btn_text",
            type = "Text",
            properties = mapOf("text" to "Delete Project..."),
        ),
        A2uiComponentPayload(
            id = "dialog_col",
            type = "Column",
            properties =
                mapOf(
                    "children" to listOf("dialog_title", "dialog_body", "dialog_actions"),
                    "justify" to "start",
                    "align" to "stretch",
                ),
        ),
        A2uiComponentPayload(
            id = "dialog_title",
            type = "Text",
            properties = mapOf("text" to "Delete Project?", "variant" to "h3"),
        ),
        A2uiComponentPayload(
            id = "dialog_body",
            type = "Text",
            properties =
                mapOf(
                    "text" to
                        "This action cannot be undone. All associated surfaces and " +
                            "agent configurations will be permanently removed.",
                    "variant" to "body",
                ),
        ),
        A2uiComponentPayload(
            id = "dialog_actions",
            type = "Row",
            properties =
                mapOf(
                    "children" to listOf("cancel_btn", "confirm_btn"),
                    "justify" to "end",
                    "align" to "center",
                ),
        ),
        A2uiComponentPayload(
            id = "cancel_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "cancel_btn_text",
                    "variant" to "borderless",
                    "action" to mapOf("event" to mapOf("name" to "modal_cancel")),
                ),
        ),
        A2uiComponentPayload(
            id = "cancel_btn_text",
            type = "Text",
            properties = mapOf("text" to "Cancel"),
        ),
        A2uiComponentPayload(
            id = "confirm_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "confirm_btn_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "modal_confirm_delete")),
                ),
        ),
        A2uiComponentPayload(
            id = "confirm_btn_text",
            type = "Text",
            properties = mapOf("text" to "Delete"),
        ),
    )

private val InfoModalComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Modal",
            properties = mapOf("trigger" to "info_btn", "content" to "info_col"),
        ),
        A2uiComponentPayload(
            id = "info_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "info_btn_text",
                    "variant" to "default",
                    "action" to mapOf("event" to mapOf("name" to "open_info_modal")),
                ),
        ),
        A2uiComponentPayload(
            id = "info_btn_text",
            type = "Text",
            properties = mapOf("text" to "About A2UI Protocol"),
        ),
        A2uiComponentPayload(
            id = "info_col",
            type = "Column",
            properties =
                mapOf(
                    "children" to listOf("info_icon", "info_title", "info_desc", "got_it_btn"),
                    "justify" to "start",
                    "align" to "center",
                ),
        ),
        A2uiComponentPayload(
            id = "info_icon",
            type = "Icon",
            properties = mapOf("name" to "star"),
        ),
        A2uiComponentPayload(
            id = "info_title",
            type = "Text",
            properties = mapOf("text" to "Agent-to-UI (A2UI)", "variant" to "h3"),
        ),
        A2uiComponentPayload(
            id = "info_desc",
            type = "Text",
            properties =
                mapOf(
                    "text" to
                        "A2UI enables autonomous agents to stream declarative, interactive " +
                            "interfaces rendered natively with Material 3 components.",
                    "variant" to "body",
                ),
        ),
        A2uiComponentPayload(
            id = "got_it_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "got_it_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "info_acknowledged")),
                ),
        ),
        A2uiComponentPayload(
            id = "got_it_text",
            type = "Text",
            properties = mapOf("text" to "Got It"),
        ),
    )

private val FormModalComponents =
    listOf(
        A2uiComponentPayload(
            id = "root",
            type = "Modal",
            properties = mapOf("trigger" to "form_btn", "content" to "form_col"),
        ),
        A2uiComponentPayload(
            id = "form_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "form_btn_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "open_form_modal")),
                ),
        ),
        A2uiComponentPayload(
            id = "form_btn_text",
            type = "Text",
            properties = mapOf("text" to "Send Feedback"),
        ),
        A2uiComponentPayload(
            id = "form_col",
            type = "Column",
            properties =
                mapOf(
                    "children" to listOf("form_title", "form_input", "form_submit_btn"),
                    "justify" to "start",
                    "align" to "stretch",
                ),
        ),
        A2uiComponentPayload(
            id = "form_title",
            type = "Text",
            properties = mapOf("text" to "Submit Feedback", "variant" to "h3"),
        ),
        A2uiComponentPayload(
            id = "form_input",
            type = "TextField",
            properties =
                mapOf(
                    "label" to "Your feedback",
                    "value" to mapOf("path" to "/feedbackText"),
                    "variant" to "longText",
                ),
        ),
        A2uiComponentPayload(
            id = "form_submit_btn",
            type = "Button",
            properties =
                mapOf(
                    "child" to "form_submit_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "submit_feedback")),
                ),
        ),
        A2uiComponentPayload(
            id = "form_submit_text",
            type = "Text",
            properties = mapOf("text" to "Submit"),
        ),
    )
