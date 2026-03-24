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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
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
import androidx.compose.ui.unit.constrain

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
    Modifier.Node(), CompositionLocalConsumerModifierNode, LayoutModifierNode {
    private var fontResolutionState: State<Any>? = null
    private var minSizeState: TextFieldSize? = null

    private fun requireFontResolutionState() =
        requirePreconditionNotNull(fontResolutionState) { "Font resolution state is not set." }

    private fun requireMinSizeState() =
        requirePreconditionNotNull(minSizeState) { "Min size state is not set." }

    override val shouldAutoInvalidate = false

    override fun onAttach() {
        val resolvedStyle = resolveDefaults(style, requireLayoutDirection())
        // TODO: Remove when b/487589072 is fixed
        @Suppress("SuspiciousCompositionLocalModifierRead")
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        updateFontResolutionState(resolvedStyle, fontFamilyResolver)
        minSizeState =
            TextFieldSize(
                requireLayoutDirection(),
                requireDensity(),
                fontFamilyResolver,
                resolvedStyle,
                requireFontResolutionState().value,
            )
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Observe font resolution state so that we invalidate measure when it changes
        val minSize =
            requireMinSizeState().cachedMinSizeOrComputeMinSize(requireFontResolutionState().value)

        val childConstraints = Constraints(minWidth = minSize.width, minHeight = minSize.height)
        val measured = measurable.measure(constraints.constrain(childConstraints))
        return layout(measured.width, measured.height) { measured.placeRelative(0, 0) }
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
        fontResolutionState = null
        minSizeState = null
    }

    fun update(style: TextStyle) {
        val resolvedStyle = resolveDefaults(style, requireLayoutDirection())
        updateFontResolutionState(resolvedStyle, currentValueOf(LocalFontFamilyResolver))
        requireMinSizeState().update(resolvedStyle = resolvedStyle)
        invalidateMeasurement()
    }

    private fun updateFontResolutionState(
        resolvedStyle: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
    ) {
        fontResolutionState =
            fontFamilyResolver.resolve(
                resolvedStyle.fontFamily,
                resolvedStyle.fontWeight ?: FontWeight.Normal,
                resolvedStyle.fontStyle ?: FontStyle.Normal,
                resolvedStyle.fontSynthesis ?: FontSynthesis.All,
            )
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
        LegacyTextFieldSize(layoutDirection, density, fontFamilyResolver, style, typeface)
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
    private var dirty by mutableStateOf(true)

    private var minSize: IntSize = computeMinSize(fontFamilyResolver)

    fun cachedMinSizeOrComputeMinSize(typeface: Any): IntSize {
        updateTypeface(typeface)
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
                resolvedStyle != this.resolvedStyle
        ) {
            this.layoutDirection = layoutDirection
            this.density = density
            this.fontFamilyResolver = fontFamilyResolver
            this.resolvedStyle = resolvedStyle
            dirty = true
            return
        }
        updateTypeface(typeface)
    }

    fun updateTypeface(typeface: Any) {
        if (typeface != this.typeface) {
            this.typeface = typeface
            dirty = true
        }
    }

    fun computeMinSize(fontFamilyResolver: FontFamily.Resolver): IntSize =
        computeSizeForDefaultText(
            style = resolvedStyle,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
        )
}

private class LegacyTextFieldSize(
    var layoutDirection: LayoutDirection,
    var density: Density,
    var fontFamilyResolver: FontFamily.Resolver,
    var resolvedStyle: TextStyle,
    var typeface: Any,
) {
    var minSize = computeMinSize()
        private set

    fun update(
        layoutDirection: LayoutDirection,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        resolvedStyle: TextStyle,
        typeface: Any,
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
            minSize = computeMinSize()
        }
    }

    private fun computeMinSize(): IntSize {
        return computeSizeForDefaultText(
            style = resolvedStyle,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
        )
    }
}
