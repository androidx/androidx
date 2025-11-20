/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo

/**
 * Provides [contentColor] for text and iconography to consume. Content color is provided
 * automatically by [surface] - contentColorProvider can be used for cases where some text or icons
 * inside a surface require a different color for emphasis.
 *
 * @param contentColor the content color to provide for descendants
 * @see surface
 * @see calculateContentColor
 */
public fun Modifier.contentColorProvider(contentColor: Color): Modifier =
    this.then(ContentColorProviderElement(contentColor))

/**
 * Calculates the preferred content color for [backgroundColor]. This will return either
 * [Color.White] or [Color.Black], depending on the luminance of the background color.
 *
 * @see surface
 * @see contentColorProvider
 */
public fun calculateContentColor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() < LuminanceContrastRatioBreakpoint) Color.White else Color.Black

/**
 * Retrieves the preferred content color for text and iconography. Most surfaces should be
 * [Color.Black], so content color is typically [Color.White]. In a few cases where surfaces are
 * filled with a different color, the content color may be [Color.Black] to improve contrast. For
 * cases where higher emphasis is required, content color may be a different color from the theme,
 * such as [Colors.primary], and provided separately using [contentColorProvider].
 *
 * Content color is automatically provided by [surface], and calculated from the provided background
 * color by default. It can also be provided with [contentColorProvider]. To manually calculate the
 * default content color for a provided background color, use [calculateContentColor].
 */
internal fun DelegatableNode.currentContentColor(): Color {
    var contentColor = Color.White
    traverseAncestors(ContentColorTraverseKey) {
        if (it is ContentColorProviderNode) {
            contentColor = it.contentColor
            // Stop at the nearest descendant node
            false
        } else {
            // Theoretically someone else could define the same traverse key, so continue just to be
            // safe
            true
        }
    }
    return contentColor
}

private class ContentColorProviderElement(private val contentColor: Color) :
    ModifierNodeElement<ContentColorProviderNode>() {
    override fun create(): ContentColorProviderNode = ContentColorProviderNode(contentColor)

    override fun update(node: ContentColorProviderNode) = node.update(contentColor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContentColorProviderElement) return false

        return (contentColor == other.contentColor)
    }

    override fun hashCode(): Int {
        return contentColor.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "contentColorProvider"
        properties["contentColor"] = contentColor
    }
}

private class ContentColorProviderNode(contentColor: Color) : TraversableNode, Modifier.Node() {

    override val shouldAutoInvalidate = false

    var contentColor by mutableStateOf(contentColor)
        private set

    fun update(contentColor: Color) {
        this.contentColor = contentColor
    }

    override val traverseKey: String = ContentColorTraverseKey
}

private const val ContentColorTraverseKey = "androidx.xr.glimmer.ContentColor"

/**
 * Contrast ratio is defined as (L1 + 0.05) / (L2 + 0.05) where L1 is the relative luminance of the
 * lighter color and L2 is the relative luminance of the darker color. ([WCAG
 * 2.0](https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html#contrast-ratiodef))
 *
 * The luminance of white is 1, and the luminance of black is 0 - so essentially we need to compare
 * whether (1.05) / (L + 0.05) > (L + 0.05) / (0.05) - this can be simplified down to L < 0.179129.
 */
private const val LuminanceContrastRatioBreakpoint = 0.179129f
