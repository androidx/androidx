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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Image.Fit
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Image.Variant
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

private enum class ImagePreset(val label: String, val url: String) {
    Landscape("Landscape", "https://picsum.photos/id/10/800/600"),
    Puppy("Puppy", "https://picsum.photos/id/237/400/400"),
    Coffee("Coffee", "https://picsum.photos/id/1060/600/400"),
}

@Composable
internal fun ImageSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var variant by rememberSaveable { mutableStateOf(Variant.MediumFeature) }
    var fit by rememberSaveable { mutableStateOf(Fit.Cover) }
    var url by rememberSaveable { mutableStateOf(ImagePreset.Landscape.url) }

    LaunchedEffect(Unit) {
        updatePayload(variant = variant, fit = fit, url = url, onPayloadUpdated = onPayloadUpdated)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Image Size Variant", subtitle = "Sizing and clipping preset") {
            ChoiceChips(
                options = Variant.entries,
                selectedOption = variant,
                onOptionSelected = {
                    variant = it
                    updatePayload(
                        variant = it,
                        fit = fit,
                        url = url,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Content Scale (Fit)",
            subtitle = "How the image fills its bounding container",
        ) {
            ChoiceChips(
                options = Fit.entries,
                selectedOption = fit,
                onOptionSelected = {
                    fit = it
                    updatePayload(
                        variant = variant,
                        fit = it,
                        url = url,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Sample Image Presets", subtitle = "Quick-select sample images") {
            ChoiceChips(
                options = ImagePreset.entries,
                selectedOption = ImagePreset.entries.find { it.url == url },
                onOptionSelected = {
                    url = it.url
                    updatePayload(
                        variant = variant,
                        fit = fit,
                        url = it.url,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.label },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Image URL", subtitle = "Resource locator for image loading") {
            TextInputControl(
                value = url,
                onValueChange = {
                    url = it
                    updatePayload(
                        variant = variant,
                        fit = fit,
                        url = it,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "URL",
            )
        }
    }
}

private fun updatePayload(
    variant: Variant,
    fit: Fit,
    url: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "Image",
            properties =
                mapOf(
                    "url" to url,
                    "variant" to variant.value,
                    "fit" to fit.value,
                    "description" to "Sample image preview",
                ),
        )
    onPayloadUpdated(listOf(component), emptyMap())
}
