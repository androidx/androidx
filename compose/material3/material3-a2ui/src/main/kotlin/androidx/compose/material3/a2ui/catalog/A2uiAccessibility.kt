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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

/**
 * Applies [A2uiBasicCatalogV1.AccessibilityAttributes] as [semantics] properties.
 *
 * Note: Do not apply this modifier to text components (e.g. [MaterialA2uiBasicCatalogV1Text]) or
 * text input fields (e.g. [MaterialA2uiBasicCatalogV1TextField]). Setting a [contentDescription] on
 * a text component may cause screen readers (like TalkBack) to read only the content description,
 * masking the actual text content. Text components implement custom accessibility to include their
 * visual text in the announced content description, while text fields map accessibility attributes
 * to [androidx.compose.ui.semantics.hintText].
 *
 * @param attributes The A2UI accessibility attributes to apply.
 * @param isClickable Pass true if the component (like a Button) has a click/toggle action.
 */
internal fun Modifier.a2uiAccessibility(
    attributes: A2uiBasicCatalogV1.AccessibilityAttributes?,
    isClickable: Boolean = false,
): Modifier =
    this.then(
        if (attributes == null) Modifier
        else
            Modifier.semantics {
                val contentDescription = attributes.toContentDescription(isClickable)
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }

                if (isClickable && !attributes.description.isNullOrBlank()) {
                    // Provides the TalkBack hint: "Double tap to [description]".
                    // Passing action = null lets Compose merge this label with the component's
                    // native clickable/toggleable action.
                    onClick(label = attributes.description, action = null)
                }
            }
    )

/**
 * Converts [A2uiBasicCatalogV1.AccessibilityAttributes] to a single content description string.
 *
 * For non-clickable components ([isClickable] = `false`), returns `"$label - $description"` if both
 * are non-blank, or the non-blank property if only one is non-blank, or `null` if both are null or
 * blank.
 *
 * For clickable components ([isClickable] = `true`), returns only [label] if non-blank, as
 * [description] is reserved for the action hint (e.g. TalkBack "Double tap to [description]").
 *
 * @param isClickable Whether the component has a clickable/toggleable action.
 */
internal fun A2uiBasicCatalogV1.AccessibilityAttributes.toContentDescription(
    isClickable: Boolean = false
): String? {
    return if (isClickable) {
        label?.takeUnless { it.isBlank() }
    } else {
        buildContentDescription(label = label, description = description)
    }
}

/**
 * Combines [label] and [description] into a content description string in `"$label - $description"`
 * format, ignoring blank values.
 *
 * Returns `null` if both [label] and [description] are null or blank.
 */
internal fun buildContentDescription(label: String?, description: String?): String? {
    val cleanLabel = label?.takeUnless { it.isBlank() }
    val cleanDescription = description?.takeUnless { it.isBlank() }
    return when {
        cleanLabel != null && cleanDescription != null -> "$cleanLabel - $cleanDescription"
        cleanLabel != null -> cleanLabel
        cleanDescription != null -> cleanDescription
        else -> null
    }
}
