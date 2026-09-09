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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.material3.integration.a2ui.ui.SwitchControl
import androidx.compose.material3.integration.a2ui.ui.TextInputControl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun IconSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var iconName by rememberSaveable { mutableStateOf("favorite") }
    var useCustomSvg by rememberSaveable { mutableStateOf(false) }
    var svgPath by rememberSaveable { mutableStateOf(SvgPresets.first().path) }

    LaunchedEffect(Unit) {
        updatePayload(
            iconName = iconName,
            useCustomSvg = useCustomSvg,
            svgPath = svgPath,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(
            title = "Built-in Icon Token",
            subtitle = "Enter an icon token name or select a suggestion",
        ) {
            TextInputControl(
                value = iconName,
                onValueChange = {
                    iconName = it
                    useCustomSvg = false
                    updatePayload(
                        iconName = it,
                        useCustomSvg = false,
                        svgPath = svgPath,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "Icon Token Name",
                placeholder = "e.g. favorite, settings, person",
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Popular Suggestions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ChoiceChips(
                options = PopularIconTokens,
                selectedOption =
                    if (!useCustomSvg && iconName in PopularIconTokens) iconName else null,
                onOptionSelected = {
                    iconName = it
                    useCustomSvg = false
                    updatePayload(
                        iconName = it,
                        useCustomSvg = false,
                        svgPath = svgPath,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Custom SVG Path",
            subtitle = "Render custom client-side vector path data",
        ) {
            SwitchControl(
                title = "Use Custom SVG Path",
                subtitle = "Enable bespoke SVG path data rendering",
                checked = useCustomSvg,
                onCheckedChange = {
                    useCustomSvg = it
                    updatePayload(
                        iconName = iconName,
                        useCustomSvg = it,
                        svgPath = svgPath,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
            )

            if (useCustomSvg) {
                Spacer(modifier = Modifier.height(12.dp))

                TextInputControl(
                    value = svgPath,
                    onValueChange = {
                        svgPath = it
                        updatePayload(
                            iconName = iconName,
                            useCustomSvg = useCustomSvg,
                            svgPath = it,
                            onPayloadUpdated = onPayloadUpdated,
                        )
                    },
                    label = "SVG Path Data",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    placeholder = "Enter SVG path data (d attribute)...",
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Preset Shapes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ChoiceChips(
                    options = SvgPresets,
                    selectedOption = SvgPresets.firstOrNull { it.path == svgPath },
                    onOptionSelected = {
                        svgPath = it.path
                        updatePayload(
                            iconName = iconName,
                            useCustomSvg = useCustomSvg,
                            svgPath = it.path,
                            onPayloadUpdated = onPayloadUpdated,
                        )
                    },
                    label = { it.name },
                )
            }
        }
    }
}

private fun updatePayload(
    iconName: String,
    useCustomSvg: Boolean,
    svgPath: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val nameProperty: Any =
        if (useCustomSvg) {
            mapOf("svgPath" to svgPath)
        } else {
            iconName.trim()
        }

    val component =
        A2uiComponentPayload(
            id = "root",
            type = "Icon",
            properties = mapOf("name" to nameProperty),
        )
    onPayloadUpdated(listOf(component), emptyMap())
}

private data class SvgPreset(val name: String, val path: String)

private val SvgPresets =
    listOf(
        SvgPreset(
            name = "Heart",
            path =
                "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z",
        ),
        SvgPreset(
            name = "Star",
            path =
                "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z",
        ),
        SvgPreset(name = "Check", path = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"),
        SvgPreset(
            name = "Bookmark",
            path = "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z",
        ),
        SvgPreset(
            name = "Android",
            path =
                "M6 18c0 .55.45 1 1 1h1v3.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5V19h2v3.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5V19h1c.55 0 1-.45 1-1V8H6v10zM3.5 8C2.67 8 2 8.67 2 9.5v7c0 .83.67 1.5 1.5 1.5S5 17.33 5 16.5v-7C5 8.67 4.33 8 3.5 8zm17 0c-.83 0-1.5.67-1.5 1.5v7c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5v-7c0-.83-.67-1.5-1.5-1.5zm-4.97-4.84l1.3-1.3c.2-.2.2-.51 0-.71-.2-.2-.51-.2-.71 0l-1.48 1.48C13.85 2.23 12.95 2 12 2c-.96 0-1.86.23-2.66.63L7.85 1.15c-.2-.2-.51-.2-.71 0-.2.2-.2.51 0 .71l1.31 1.31C6.97 4.26 6 6.01 6 8h12c0-1.99-.97-3.75-2.47-4.84zM10 5H9V4h1v1zm5 0h-1V4h1v1z",
        ),
    )

private val PopularIconTokens =
    listOf(
        "favorite",
        "star",
        "home",
        "search",
        "settings",
        "check",
        "notifications",
        "person",
        "edit",
        "close",
        "add",
        "delete",
        "share",
        "camera",
        "call",
        "mail",
    )
