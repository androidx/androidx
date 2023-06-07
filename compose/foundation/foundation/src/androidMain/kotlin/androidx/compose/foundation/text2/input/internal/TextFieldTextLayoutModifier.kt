/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.CodepointTransformation
import androidx.compose.foundation.text2.input.SingleLineCodepointTransformation
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.toVisualText
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * This ModifierNodeElement is only responsible for laying out text and reporting its global
 * position coordinates. Text layout is kept in a separate node than the rest of the Core modifiers
 * because it will also go through scroll and minSize constraints. We need to know exact size and
 * coordinates of [TextLayoutResult] to make it relatively easier to calculate the offset between
 * exact touch coordinates and where they map on the [TextLayoutResult].
 */
@OptIn(ExperimentalFoundationApi::class)
internal data class TextFieldTextLayoutModifier(
    private val textLayoutState: TextLayoutState,
    private val textFieldState: TextFieldState,
    private val codepointTransformation: CodepointTransformation?,
    private val textStyle: TextStyle,
    private val singleLine: Boolean,
    private val onTextLayout: Density.(TextLayoutResult) -> Unit
) : ModifierNodeElement<TextFieldTextLayoutModifierNode>() {
    override fun create(): TextFieldTextLayoutModifierNode = TextFieldTextLayoutModifierNode(
        textLayoutState = textLayoutState,
        textFieldState = textFieldState,
        codepointTransformation = codepointTransformation,
        textStyle = textStyle,
        singleLine = singleLine,
        onTextLayout = onTextLayout
    )

    override fun update(node: TextFieldTextLayoutModifierNode) {
        node.updateNode(
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
            codepointTransformation = codepointTransformation,
            textStyle = textStyle,
            singleLine = singleLine,
            onTextLayout = onTextLayout
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector info
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldTextLayoutModifierNode(
    private var textLayoutState: TextLayoutState,
    private var textFieldState: TextFieldState,
    private var codepointTransformation: CodepointTransformation?,
    private var textStyle: TextStyle,
    private var singleLine: Boolean,
    private var onTextLayout: Density.(TextLayoutResult) -> Unit
) : Modifier.Node(),
    LayoutModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    /**
     * Updates all the related properties and invalidates internal state based on the changes.
     */
    fun updateNode(
        textLayoutState: TextLayoutState,
        textFieldState: TextFieldState,
        codepointTransformation: CodepointTransformation?,
        textStyle: TextStyle,
        singleLine: Boolean,
        onTextLayout: Density.(TextLayoutResult) -> Unit
    ) {
        this.textLayoutState = textLayoutState
        this.textFieldState = textFieldState
        this.codepointTransformation = codepointTransformation
        this.textStyle = textStyle
        this.singleLine = singleLine
        this.onTextLayout = onTextLayout
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.textLayoutState.innerTextFieldCoordinates = coordinates
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val result = with(textLayoutState) {
            // First prefer provided codepointTransformation if not null, e.g.
            // BasicSecureTextField would send Password Transformation.
            // Second, apply a SingleLineCodepointTransformation if text field is configured
            // to be single line.
            // Else, don't apply any visual transformation.
            val appliedCodepointTransformation = codepointTransformation
                ?: SingleLineCodepointTransformation.takeIf { singleLine }

            val visualText = textFieldState.text.toVisualText(appliedCodepointTransformation)
            // Composition Local reads are automatically tracked here because we are in layout
            layout(
                text = AnnotatedString(visualText.toString()),
                textStyle = textStyle,
                softWrap = !singleLine,
                density = currentValueOf(LocalDensity),
                fontFamilyResolver = currentValueOf(LocalFontFamilyResolver),
                constraints = constraints,
                onTextLayout = onTextLayout
            )
        }

        val placeable = measurable.measure(
            Constraints.fixed(result.size.width, result.size.height)
        )

        // TODO: min height

        return layout(
            width = result.size.width,
            height = result.size.height,
            alignmentLines = mapOf(
                FirstBaseline to result.firstBaseline.roundToInt(),
                LastBaseline to result.lastBaseline.roundToInt()
            )
        ) {
            placeable.place(0, 0)
        }
    }
}