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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// TODO(b/544548540): Add Markdown support as per the basic catalog specification.
internal object MaterialA2uiBasicCatalogV1Text : A2uiBasicCatalogV1.Text {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val spec =
            when (variant) {
                A2uiBasicCatalogV1.Text.Variant.H1 -> H1Spec
                A2uiBasicCatalogV1.Text.Variant.H2 -> H2Spec
                A2uiBasicCatalogV1.Text.Variant.H3 -> H3Spec
                A2uiBasicCatalogV1.Text.Variant.H4 -> H4Spec
                A2uiBasicCatalogV1.Text.Variant.H5 -> H5Spec
                A2uiBasicCatalogV1.Text.Variant.Caption -> CaptionSpec
                A2uiBasicCatalogV1.Text.Variant.Body -> BodySpec
            }

        var textModifier = modifier.textAccessibility(accessibility, text)
        if (spec.topPadding > 0.dp || spec.bottomPadding > 0.dp) {
            textModifier = textModifier.padding(top = spec.topPadding, bottom = spec.bottomPadding)
        }
        if (spec.isHeading) {
            textModifier = textModifier.semantics { heading() }
        }

        Text(
            text = text,
            style = spec.getTextStyle(MaterialTheme.typography),
            modifier = textModifier,
        )
    }

    private class TypographySpec(
        val topPadding: Dp,
        val bottomPadding: Dp,
        val isHeading: Boolean,
        val getTextStyle: (Typography) -> TextStyle,
    )

    private val H1Spec = TypographySpec(16.dp, 8.dp, isHeading = true) { it.headlineLarge }
    private val H2Spec = TypographySpec(12.dp, 6.dp, isHeading = true) { it.headlineMedium }
    private val H3Spec = TypographySpec(8.dp, 4.dp, isHeading = true) { it.headlineSmall }
    private val H4Spec = TypographySpec(4.dp, 2.dp, isHeading = true) { it.titleLarge }
    private val H5Spec = TypographySpec(4.dp, 2.dp, isHeading = true) { it.titleMedium }
    private val CaptionSpec = TypographySpec(0.dp, 0.dp, isHeading = false) { it.labelMedium }
    private val BodySpec = TypographySpec(0.dp, 0.dp, isHeading = false) { it.bodyLarge }
}

/**
 * Applies text-specific accessibility semantics using [attributes] and visual [text].
 *
 * Sets [contentDescription] to a combined string containing [text] when [attributes] has a
 * non-empty label or description. When [attributes] is null or empty, no semantics are applied,
 * preserving default Compose [Text] behavior.
 *
 * @param attributes The accessibility attributes to apply.
 * @param text The visual text content of the component.
 */
private fun Modifier.textAccessibility(
    attributes: A2uiBasicCatalogV1.AccessibilityAttributes?,
    text: String,
): Modifier {
    if (attributes == null) return this
    val contentDescription = attributes.toTextContentDescription(text) ?: return this
    return semantics { this.contentDescription = contentDescription }
}

/**
 * Converts [A2uiBasicCatalogV1.AccessibilityAttributes] and visual [text] into a content
 * description.
 *
 * Combines present properties in `[label] - [text] - [description]` order while deduplicating
 * matching values. Returns `null` if both [label] and [description] are null or blank, ensuring
 * screen readers fall back to native text traversal.
 *
 * @param text The visual text content of the component.
 * @return The combined content description string, or null if no attributes are set.
 */
private fun A2uiBasicCatalogV1.AccessibilityAttributes.toTextContentDescription(
    text: String
): String? {
    if (label.isNullOrBlank() && description.isNullOrBlank()) {
        return null
    }
    val parts = buildList {
        if (!label.isNullOrBlank()) {
            add(label)
        }
        if (text.isNotBlank() && !text.equals(label, ignoreCase = true)) {
            add(text)
        }
        if (
            !description.isNullOrBlank() &&
                !description.equals(text, ignoreCase = true) &&
                !description.equals(label, ignoreCase = true)
        ) {
            add(description)
        }
    }
    if (parts.isEmpty()) return null
    return buildString {
        for (i in parts.indices) {
            if (i > 0) append(" - ")
            append(parts[i])
        }
    }
}
