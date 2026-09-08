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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Text.Variant
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
import androidx.compose.material3.integration.a2ui.ui.ControlCard
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
internal fun TextSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var variant by rememberSaveable { mutableStateOf(Variant.H3) }
    var text by rememberSaveable {
        mutableStateOf(
            "A2UI brings declarative, cross-platform UI generation to autonomous agents."
        )
    }

    LaunchedEffect(Unit) {
        updatePayload(variant = variant, text = text, onPayloadUpdated = onPayloadUpdated)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Typography Variant", subtitle = "Text style and hierarchical scale") {
            ChoiceChips(
                options = Variant.entries,
                selectedOption = variant,
                onOptionSelected = {
                    variant = it
                    updatePayload(variant = it, text = text, onPayloadUpdated = onPayloadUpdated)
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Text Content", subtitle = "The string literal to display") {
            TextInputControl(
                value = text,
                onValueChange = {
                    text = it
                    updatePayload(variant = variant, text = it, onPayloadUpdated = onPayloadUpdated)
                },
                label = "Content",
                singleLine = false,
            )
        }
    }
}

private fun updatePayload(
    variant: Variant,
    text: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "Text",
            properties = mapOf("text" to text, "variant" to variant.value),
        )
    onPayloadUpdated(listOf(component), emptyMap())
}
