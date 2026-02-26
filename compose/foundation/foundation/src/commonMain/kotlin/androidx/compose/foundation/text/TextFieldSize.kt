/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.ComposeFoundationFlags.isBasicTextFieldMinSizeOptimizationEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.textFieldMinSize(style: TextStyle) =
    if (isBasicTextFieldMinSizeOptimizationEnabled) {
        this then TextFieldSizeElement(style)
    } else legacyTextFieldMinSize(style)

private class TextFieldSizeElement(private val style: TextStyle) :
    ModifierNodeElement<TextFieldSizeNode>() {
    override fun create() = TextFieldSizeNode(style)

    override fun update(node: TextFieldSizeNode) {
        node.update(style)
    }

    override fun hashCode() = style.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldSizeElement) return false
        return style == other.style
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "textFieldMinSize"
        properties["style"] = style
    }
}

private class TextFieldSizeNode(private val style: TextStyle) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode,
    ObserverModifierNode {

    private var resolvedStyle: TextStyle? = null
    private var fontResolutionState: State<Any>? = null
    private var minSizeState: TextFieldSize? = null

    private fun requireResolvedStyle() =
        requirePreconditionNotNull(resolvedStyle) { "Resolved style is not set." }

    private fun requireFontResolutionState() =
        requirePreconditionNotNull(fontResolutionState) { "Font resolution state is not set." }

    private fun getOrCreateMinSizeState(): TextFieldSize {
        if (minSizeState == null) {
            minSizeState =
                TextFieldSize(
                    requireLayoutDirection(),
                    requireDensity(),
                    currentValueOf(LocalFontFamilyResolver),
                    requireResolvedStyle(),
                    requireFontResolutionState().value,
                )
        }
        return minSizeState!!
    }

    override val shouldAutoInvalidate = false

    override fun onAttach() {
        // We already create the value holder here to start font resolution before layout as it
        // might take a longer time.
        resolvedStyle = resolveDefaults(style, requireLayoutDirection())
        // TODO: Remove when b/487589072 is fixed
        @Suppress("SuspiciousCompositionLocalModifierRead")
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        fontResolutionState =
            fontFamilyResolver.resolve(
                requireResolvedStyle().fontFamily,
                requireResolvedStyle().fontWeight ?: FontWeight.Normal,
                requireResolvedStyle().fontStyle ?: FontStyle.Normal,
                requireResolvedStyle().fontSynthesis ?: FontSynthesis.All,
            )
        observeReads { requireFontResolutionState().value }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val minSize =
            getOrCreateMinSizeState()
                .cachedMinSizeOrComputeMinSize(currentValueOf(LocalFontFamilyResolver))

        val childConstraints =
            constraints.copy(
                minWidth = minSize.width.coerceIn(constraints.minWidth, constraints.maxWidth),
                minHeight = minSize.height.coerceIn(constraints.minHeight, constraints.maxHeight),
            )
        val measured = measurable.measure(childConstraints)
        return layout(measured.width, measured.height) { measured.placeRelative(0, 0) }
    }

    override fun onObservedReadsChanged() {
        onFontResolutionStateChanged()
    }

    override fun onLayoutDirectionChange() {
        minSizeState?.update(layoutDirection = requireLayoutDirection())
        invalidateMeasurement()
    }

    override fun onDensityChange() {
        minSizeState?.update(density = requireDensity())
        invalidateMeasurement()
    }

    override fun onDetach() {
        resolvedStyle = null
        fontResolutionState = null
        minSizeState = null
    }

    private fun onFontResolutionStateChanged() {
        if (fontResolutionState != null) {
            observeReads { requireFontResolutionState().value }
        }
        minSizeState?.update(typeface = requireFontResolutionState().value)
        invalidateMeasurement()
    }

    fun update(style: TextStyle) {
        val resolvedStyle = resolveDefaults(style, requireLayoutDirection())
        getOrCreateMinSizeState().update(resolvedStyle = resolvedStyle)
        invalidateMeasurement()
    }
}

internal fun Modifier.legacyTextFieldMinSize(style: TextStyle) = composed {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val layoutDirection = LocalLayoutDirection.current

    val resolvedStyle = remember(style, layoutDirection) { resolveDefaults(style, layoutDirection) }
    val typeface by
        remember(fontFamilyResolver, resolvedStyle) {
            fontFamilyResolver.resolve(
                resolvedStyle.fontFamily,
                resolvedStyle.fontWeight ?: FontWeight.Normal,
                resolvedStyle.fontStyle ?: FontStyle.Normal,
                resolvedStyle.fontSynthesis ?: FontSynthesis.All,
            )
        }

    val minSizeState = remember {
        TextFieldSize(layoutDirection, density, fontFamilyResolver, style, typeface)
    }

    minSizeState.update(layoutDirection, density, fontFamilyResolver, resolvedStyle, typeface)

    Modifier.layout { measurable, constraints ->
        val minSize = minSizeState.minSize

        val childConstraints =
            constraints.copy(
                minWidth = minSize.width.coerceIn(constraints.minWidth, constraints.maxWidth),
                minHeight = minSize.height.coerceIn(constraints.minHeight, constraints.maxHeight),
            )
        val measured = measurable.measure(childConstraints)
        layout(measured.width, measured.height) { measured.placeRelative(0, 0) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class TextFieldSize(
    var layoutDirection: LayoutDirection,
    var density: Density,
    var fontFamilyResolver: FontFamily.Resolver,
    var resolvedStyle: TextStyle,
    var typeface: Any,
) {
    private var dirty = true
    var minSize: IntSize =
        if (isBasicTextFieldMinSizeOptimizationEnabled) IntSize.Zero
        else computeMinSize(fontFamilyResolver)
        private set

    fun cachedMinSizeOrComputeMinSize(fontFamilyResolver: FontFamily.Resolver): IntSize {
        if (dirty) {
            minSize = computeMinSize(fontFamilyResolver)
            dirty = false
        }
        return minSize
    }

    fun update(
        layoutDirection: LayoutDirection = this.layoutDirection,
        density: Density = this.density,
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        resolvedStyle: TextStyle = this.resolvedStyle,
        typeface: Any = this.typeface,
    ) {
        if (
            layoutDirection != this.layoutDirection ||
                density != this.density ||
                fontFamilyResolver != this.fontFamilyResolver ||
                resolvedStyle != this.resolvedStyle ||
                typeface != this.typeface
        ) {
            this.layoutDirection = layoutDirection
            this.density = density
            this.fontFamilyResolver = fontFamilyResolver
            this.resolvedStyle = resolvedStyle
            this.typeface = typeface
            if (isBasicTextFieldMinSizeOptimizationEnabled) {
                dirty = true
            } else {
                minSize = computeMinSize(fontFamilyResolver)
            }
        }
    }

    fun computeMinSize(fontFamilyResolver: FontFamily.Resolver): IntSize =
        computeSizeForDefaultText(
            style = resolvedStyle,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
        )
}
