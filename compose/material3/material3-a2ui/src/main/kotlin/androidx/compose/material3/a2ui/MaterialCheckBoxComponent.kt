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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"CheckBox"` component schema.
 *
 * Displays a checkable control with an associated label.
 *
 * **Schema Properties:**
 * * `value` (Dynamic Boolean, required): The current state of the checkbox (true for checked, false
 *   for unchecked).
 * * `label` (Dynamic String, required): The text to display next to the checkbox.
 */
public object MaterialCheckBoxComponent : A2uiComponent {

    private val valueProp =
        A2uiProperty.dynamicBoolean(
            key = "value",
            required = true,
            description =
                "The current state of the checkbox (true for checked, false for unchecked).",
        )
    private val labelProp =
        A2uiProperty.dynamicString(
            key = "label",
            required = true,
            description = "The text to display next to the checkbox.",
        )

    override val name: String = "CheckBox"
    override val description: String = "A toggleable control with a label."
    override val properties: List<A2uiProperty<*>> = listOf(valueProp, labelProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(valueProp) != null && properties.bind(labelProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val label =
            checkNotNull(properties.bind(labelProp)) {
                "Required property '${labelProp.key}' is missing."
            }
        val remoteValue =
            checkNotNull(properties.bind(valueProp)) {
                "Required property '${valueProp.key}' is missing."
            }
        val onRemoteValueChange = properties.bindUpdater(valueProp)
        val isToggleable = onRemoteValueChange != null

        val interactionSource = remember { MutableInteractionSource() }

        Row(
            modifier =
                modifier.toggleable(
                    value = remoteValue,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Checkbox,
                    enabled = isToggleable,
                    onValueChange = { newValue -> onRemoteValueChange?.invoke(newValue) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = remoteValue,
                onCheckedChange = null,
                interactionSource = interactionSource,
                enabled = isToggleable,
            )

            Spacer(CheckBoxSpacingModifier)

            Text(text = label)
        }
    }
}

private val CheckBoxSpacingModifier = Modifier.width(8.dp)
