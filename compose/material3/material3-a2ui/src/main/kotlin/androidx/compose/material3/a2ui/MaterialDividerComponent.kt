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
import androidx.a2ui.compose.runtime.StaticA2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Divider"` component schema.
 *
 * This component displays a visual dividing line to separate content, supporting both horizontal
 * and vertical orientation variants.
 *
 * **Schema Properties:**
 * * `axis` (String Enum): The orientation of the divider. Supported values are `"horizontal"` and
 *   `"vertical"`. If omitted, `"horizontal"` is used by default.
 */
public object MaterialDividerComponent : A2uiComponent {

    private val axisProp =
        A2uiProperty.stringEnum("axis", enumValues = listOf("horizontal", "vertical"))

    override val name: String = "Divider"
    override val description: String = "A visual separator line"
    override val properties: List<StaticA2uiProperty<*>> = listOf(axisProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        if (properties[axisProp] == "vertical") {
            VerticalDivider(modifier = modifier)
        } else {
            HorizontalDivider(modifier = modifier)
        }
    }
}
