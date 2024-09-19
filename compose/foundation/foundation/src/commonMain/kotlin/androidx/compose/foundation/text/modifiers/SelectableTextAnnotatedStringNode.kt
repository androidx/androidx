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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.text.AutoSize
import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/**
 * Node for any text that is in a selection container.
 *
 * This adds [GlobalPositionAwareModifierNode].
 */
internal class SelectableTextAnnotatedStringNode(
    text: AnnotatedString,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = DefaultMinLines,
    placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
    onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
    private var selectionController: SelectionController? = null,
    overrideColor: ColorProducer? = null,
    autoSize: AutoSize? = null,
    private var onShowTranslation: ((TextAnnotatedStringNode.TextSubstitutionValue) -> Unit)? = null
) : DelegatingNode(), LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode {

    private val delegate =
        delegate(
            TextAnnotatedStringNode(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                placeholders = placeholders,
                onPlaceholderLayout = onPlaceholderLayout,
                selectionController = selectionController,
                overrideColor = overrideColor,
                autoSize = autoSize,
                onShowTranslation = onShowTranslation
            )
        )

    init {
        requirePreconditionNotNull(selectionController) {
            "Do not use SelectionCapableStaticTextModifier unless selectionController != null"
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        selectionController?.updateGlobalPosition(coordinates)
    }

    override fun ContentDrawScope.draw() = delegate.drawNonExtension(this)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = delegate.measureNonExtension(this, measurable, constraints)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = delegate.minIntrinsicWidthNonExtension(this, measurable, height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = delegate.minIntrinsicHeightNonExtension(this, measurable, width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = delegate.maxIntrinsicWidthNonExtension(this, measurable, height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = delegate.maxIntrinsicHeightNonExtension(this, measurable, width)

    fun update(
        text: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>?,
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow,
        onTextLayout: ((TextLayoutResult) -> Unit)?,
        onPlaceholderLayout: ((List<Rect?>) -> Unit)?,
        selectionController: SelectionController?,
        color: ColorProducer?,
        autoSize: AutoSize?
    ) {
        delegate.doInvalidations(
            drawChanged = delegate.updateDraw(color, style),
            textChanged = delegate.updateText(text = text),
            layoutChanged =
                delegate.updateLayoutRelatedArgs(
                    style = style,
                    placeholders = placeholders,
                    minLines = minLines,
                    maxLines = maxLines,
                    softWrap = softWrap,
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = overflow,
                    autoSize = autoSize
                ),
            callbacksChanged =
                delegate.updateCallbacks(
                    onTextLayout = onTextLayout,
                    onPlaceholderLayout = onPlaceholderLayout,
                    selectionController = selectionController,
                    onShowTranslation = onShowTranslation
                ),
        )
        this.selectionController = selectionController
        // we always relayout when we're selectable
        invalidateMeasurement()
    }
}
