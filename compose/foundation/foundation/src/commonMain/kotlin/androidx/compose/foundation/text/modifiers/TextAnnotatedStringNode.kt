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

import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Node that implements Text for [AnnotatedString] or [onTextLayout] parameters.
 */
internal class TextAnnotatedStringNode(
    private var text: AnnotatedString,
    private var style: TextStyle,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
    private var onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
    private var selectionController: SelectionController? = null,
    private var overrideColor: ColorProducer? = null
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, SemanticsModifierNode {
    private var baselineCache: Map<AlignmentLine, Int>? = null

    private var _layoutCache: MultiParagraphLayoutCache? = null
    private val layoutCache: MultiParagraphLayoutCache
        get() {
            if (_layoutCache == null) {
                _layoutCache = MultiParagraphLayoutCache(
                    text,
                    style,
                    fontFamilyResolver,
                    overflow,
                    softWrap,
                    maxLines,
                    minLines,
                    placeholders
                )
            }
            return _layoutCache!!
        }

    private fun getLayoutCache(density: Density): MultiParagraphLayoutCache {
        return layoutCache.also { it.density = density }
    }

    /**
     * Element has draw parameters to update
     */
    fun updateDraw(color: ColorProducer?, style: TextStyle): Boolean {
        var changed = false
        if (color != this.overrideColor) {
            changed = true
        }
        overrideColor = color
        changed = changed || !style.hasSameDrawAffectingAttributes(this.style)
        return changed
    }

    /**
     * Element has text parameters to update
     */
    fun updateText(text: AnnotatedString): Boolean {
        if (this.text == text) return false
        this.text = text
        return true
    }

    /**
     * Element has layout parameters to update
     */
    fun updateLayoutRelatedArgs(
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>?,
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow
    ): Boolean {
        var changed: Boolean

        changed = !this.style.hasSameLayoutAffectingAttributes(style)
        this.style = style

        if (this.placeholders != placeholders) {
            this.placeholders = placeholders
            changed = true
        }

        if (this.minLines != minLines) {
            this.minLines = minLines
            changed = true
        }

        if (this.maxLines != maxLines) {
            this.maxLines = maxLines
            changed = true
        }

        if (this.softWrap != softWrap) {
            this.softWrap = softWrap
            changed = true
        }

        if (this.fontFamilyResolver != fontFamilyResolver) {
            this.fontFamilyResolver = fontFamilyResolver
            changed = true
        }

        if (this.overflow != overflow) {
            this.overflow = overflow
            changed = true
        }

        return changed
    }

    /**
     * Element has callback parameters to update
     */
    fun updateCallbacks(
        onTextLayout: ((TextLayoutResult) -> Unit)?,
        onPlaceholderLayout: ((List<Rect?>) -> Unit)?,
        selectionController: SelectionController?
    ): Boolean {
        var changed = false

        if (this.onTextLayout != onTextLayout) {
            this.onTextLayout = onTextLayout
            changed = true
        }

        if (this.onPlaceholderLayout != onPlaceholderLayout) {
            this.onPlaceholderLayout = onPlaceholderLayout
            changed = true
        }

        if (this.selectionController != selectionController) {
            this.selectionController = selectionController
            changed = true
        }
        return changed
    }

    /**
     * Do appropriate invalidate calls based on the results of update above.
     */
    fun doInvalidations(
        drawChanged: Boolean,
        textChanged: Boolean,
        layoutChanged: Boolean,
        callbacksChanged: Boolean
    ) {
        if (textChanged) {
            invalidateSemantics()
        }

        if (textChanged || layoutChanged || callbacksChanged) {
            layoutCache.update(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                placeholders = placeholders
            )
            invalidateMeasurement()
            invalidateDraw()
        }
        if (drawChanged) {
            invalidateDraw()
        }
    }

    private var semanticsTextLayoutResult: ((MutableList<TextLayoutResult>) -> Boolean)? = null

    override fun SemanticsPropertyReceiver.applySemantics() {
        var localSemanticsTextLayoutResult = semanticsTextLayoutResult
        if (localSemanticsTextLayoutResult == null) {
            localSemanticsTextLayoutResult = { textLayoutResult ->
                val layout = layoutCache.layoutOrNull?.also {
                    textLayoutResult.add(it)
                }
                layout != null
            }
            semanticsTextLayoutResult = localSemanticsTextLayoutResult
        }
        text = this@TextAnnotatedStringNode.text
        getTextLayoutResult(action = localSemanticsTextLayoutResult)
    }

    fun measureNonExtension(
        measureScope: MeasureScope,
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return measureScope.measure(measurable, constraints)
    }

    /**
     * Text layout is performed here.
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val layoutCache = getLayoutCache(this)

        val didChangeLayout = layoutCache.layoutWithConstraints(constraints, layoutDirection)
        val textLayoutResult = layoutCache.textLayoutResult

        // ensure measure restarts when hasStaleResolvedFonts by reading in measure
        textLayoutResult.multiParagraph.intrinsics.hasStaleResolvedFonts

        if (didChangeLayout) {
            invalidateLayer()
            onTextLayout?.invoke(textLayoutResult)
            selectionController?.updateTextLayout(textLayoutResult)
            baselineCache = mapOf(
                FirstBaseline to textLayoutResult.firstBaseline.roundToInt(),
                LastBaseline to textLayoutResult.lastBaseline.roundToInt()
            )
        }

        // first share the placeholders
        onPlaceholderLayout?.invoke(textLayoutResult.placeholderRects)

        // then allow children to measure _inside_ our final box, with the above placeholders
        val placeable = measurable.measure(
            Constraints.fixed(
                textLayoutResult.size.width,
                textLayoutResult.size.height
            )
        )

        return layout(
            textLayoutResult.size.width,
            textLayoutResult.size.height,
            baselineCache!!
        ) {
            // this is basically a graphicsLayer
            placeable.place(0, 0)
        }
    }

    fun minIntrinsicWidthNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return intrinsicMeasureScope.minIntrinsicWidth(measurable, height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return getLayoutCache(this).minIntrinsicWidth(layoutDirection)
    }

    fun minIntrinsicHeightNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return intrinsicMeasureScope.minIntrinsicHeight(measurable, width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = getLayoutCache(this).intrinsicHeight(width, layoutDirection)

    fun maxIntrinsicWidthNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = intrinsicMeasureScope.maxIntrinsicWidth(measurable, height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = getLayoutCache(this).maxIntrinsicWidth(layoutDirection)

    fun maxIntrinsicHeightNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = intrinsicMeasureScope.maxIntrinsicHeight(measurable, width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = getLayoutCache(this).intrinsicHeight(width, layoutDirection)

    fun drawNonExtension(
        contentDrawScope: ContentDrawScope
    ) {
        return contentDrawScope.draw()
    }
    override fun ContentDrawScope.draw() {
        selectionController?.draw(this)
        drawIntoCanvas { canvas ->
            val textLayoutResult = layoutCache.textLayoutResult
            val localParagraph = textLayoutResult.multiParagraph
            val willClip = textLayoutResult.hasVisualOverflow && overflow != TextOverflow.Visible
            if (willClip) {
                val width = textLayoutResult.size.width.toFloat()
                val height = textLayoutResult.size.height.toFloat()
                val bounds = Rect(Offset.Zero, Size(width, height))
                canvas.save()
                canvas.clipRect(bounds)
            }
            try {
                val textDecoration = style.textDecoration ?: TextDecoration.None
                val shadow = style.shadow ?: Shadow.None
                val drawStyle = style.drawStyle ?: Fill
                val brush = style.brush
                if (brush != null) {
                    val alpha = style.alpha
                    localParagraph.paint(
                        canvas = canvas,
                        brush = brush,
                        alpha = alpha,
                        shadow = shadow,
                        drawStyle = drawStyle,
                        decoration = textDecoration
                    )
                } else {
                    val overrideColorVal = overrideColor?.invoke() ?: Color.Unspecified
                    val color = if (overrideColorVal.isSpecified) {
                        overrideColorVal
                    } else if (style.color.isSpecified) {
                        style.color
                    } else {
                        Color.Black
                    }
                    localParagraph.paint(
                        canvas = canvas,
                        color = color,
                        shadow = shadow,
                        drawStyle = drawStyle,
                        decoration = textDecoration
                    )
                }
            } finally {
                if (willClip) {
                    canvas.restore()
                }
            }
        }
        if (!placeholders.isNullOrEmpty()) {
            drawContent()
        }
    }
}