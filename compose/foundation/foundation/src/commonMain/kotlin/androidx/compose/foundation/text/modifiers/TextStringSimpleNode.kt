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
import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.semantics.clearTextSubstitution
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.isShowingTextSubstitution
import androidx.compose.ui.semantics.setTextSubstitution
import androidx.compose.ui.semantics.showTextSubstitution
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSubstitution
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Constraints.Companion.fitPrioritizingWidth
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastRoundToInt

/**
 * Node that implements Text for [String].
 *
 * It has reduced functionality, and as a result gains in performance.
 *
 * Note that this Node never calculates [TextLayoutResult] unless needed by semantics.
 */
internal class TextStringSimpleNode(
    private var text: String,
    private var style: TextStyle,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var overrideColor: ColorProducer? = null,
    private var autoSize: TextAutoSize? = null
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, SemanticsModifierNode {
    @Suppress("PrimitiveInCollection") // Map required for use in public API.
    // Usages of this collection are so few that the gains of using
    // MutableObjectIntMap<AlignmentLine> and then converting to a Map<AlignmentLine, Int>
    // as needed for the public API is not worth the performance benefit.
    private var baselineCache: MutableMap<AlignmentLine, Int>? = null

    private var _layoutCache: ParagraphLayoutCache? = null
    private val layoutCache: ParagraphLayoutCache
        get() {
            if (_layoutCache == null) {
                _layoutCache =
                    ParagraphLayoutCache(
                        text,
                        style,
                        fontFamilyResolver,
                        overflow,
                        softWrap,
                        maxLines,
                        minLines,
                        autoSize
                    )
            }
            return _layoutCache!!
        }

    /**
     * Get the layout cache for the current state of the node.
     *
     * If text substitution is active, this will return the layout cache for the substitution.
     * Otherwise, it will return the layout cache for the original text.
     */
    private fun getLayoutCache(density: Density): ParagraphLayoutCache {
        textSubstitution?.let { textSubstitutionValue ->
            if (textSubstitutionValue.isShowingSubstitution) {
                textSubstitutionValue.layoutCache?.let { cache ->
                    return cache.also { it.density = density }
                }
            }
        }
        return layoutCache.also { it.density = density }
    }

    fun updateDraw(color: ColorProducer?, style: TextStyle): Boolean {
        var changed = false
        if (color != this.overrideColor) {
            changed = true
        }
        overrideColor = color
        changed = changed || !style.hasSameDrawAffectingAttributes(this.style)
        return changed
    }

    /** Element has text params to update */
    fun updateText(text: String): Boolean {
        if (this.text == text) return false
        this.text = text
        clearSubstitution()
        return true
    }

    /** Element has layout related params to update */
    fun updateLayoutRelatedArgs(
        style: TextStyle,
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow,
        autoSize: TextAutoSize?
    ): Boolean {
        var changed: Boolean

        changed = !this.style.hasSameLayoutAffectingAttributes(style)
        this.style = style

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

        if (this.autoSize != autoSize) {
            this.autoSize = autoSize
            changed = true
        }

        return changed
    }

    /** request invalidate based on the results of [updateText] and [updateLayoutRelatedArgs] */
    fun doInvalidations(drawChanged: Boolean, textChanged: Boolean, layoutChanged: Boolean) {
        // bring caches up to date even if the node is detached in case it is used again later
        if (textChanged || layoutChanged) {
            layoutCache.update(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                autoSize = autoSize
            )
        }

        if (!isAttached) {
            // no-up for !isAttached. The node will invalidate when attaching again.
            return
        }
        if (textChanged || (drawChanged && semanticsTextLayoutResult != null)) {
            invalidateSemantics()
        }

        if (textChanged || layoutChanged) {
            invalidateMeasurement()
            invalidateDraw()
        }
        if (drawChanged) {
            invalidateDraw()
        }
    }

    private var semanticsTextLayoutResult: ((MutableList<TextLayoutResult>) -> Boolean)? = null

    data class TextSubstitutionValue(
        val original: String,
        var substitution: String,
        var isShowingSubstitution: Boolean = false,
        var layoutCache: ParagraphLayoutCache? = null,
        // TODO(b/283944749): add animation

    ) {
        // don't emit any user strings in toString
        override fun toString(): String =
            "TextSubstitution(" +
                "layoutCache=$layoutCache, isShowingSubstitution=$isShowingSubstitution" +
                ")"
    }

    private var textSubstitution: TextSubstitutionValue? = null

    private fun setSubstitution(updatedText: String): Boolean {
        val currentTextSubstitution = textSubstitution
        if (currentTextSubstitution != null) {
            if (updatedText == currentTextSubstitution.substitution) {
                return false
            }
            currentTextSubstitution.substitution = updatedText
            currentTextSubstitution.layoutCache?.update(
                updatedText,
                style,
                fontFamilyResolver,
                overflow,
                softWrap,
                maxLines,
                minLines,
                autoSize
            ) ?: return false
        } else {
            val newTextSubstitution = TextSubstitutionValue(text, updatedText)
            val substitutionLayoutCache =
                ParagraphLayoutCache(
                    updatedText,
                    style,
                    fontFamilyResolver,
                    overflow,
                    softWrap,
                    maxLines,
                    minLines,
                    autoSize
                )
            substitutionLayoutCache.density = layoutCache.density
            newTextSubstitution.layoutCache = substitutionLayoutCache
            textSubstitution = newTextSubstitution
        }
        return true
    }

    private fun clearSubstitution() {
        textSubstitution = null
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        var localSemanticsTextLayoutResult = semanticsTextLayoutResult
        if (localSemanticsTextLayoutResult == null) {
            localSemanticsTextLayoutResult = { textLayoutResult ->
                val layout =
                    layoutCache
                        .slowCreateTextLayoutResultOrNull(
                            style =
                                style.merge(color = overrideColor?.invoke() ?: Color.Unspecified)
                        )
                        ?.also { textLayoutResult.add(it) }
                layout != null
            }
            semanticsTextLayoutResult = localSemanticsTextLayoutResult
        }

        text = AnnotatedString(this@TextStringSimpleNode.text)
        val currentTextSubstitution = this@TextStringSimpleNode.textSubstitution
        if (currentTextSubstitution != null) {
            isShowingTextSubstitution = currentTextSubstitution.isShowingSubstitution
            textSubstitution = AnnotatedString(currentTextSubstitution.substitution)
        }

        setTextSubstitution { updatedText ->
            setSubstitution(updatedText.text)

            invalidateForTranslate()

            true
        }
        showTextSubstitution {
            if (this@TextStringSimpleNode.textSubstitution == null) {
                return@showTextSubstitution false
            }

            this@TextStringSimpleNode.textSubstitution?.isShowingSubstitution = it

            invalidateForTranslate()

            true
        }
        clearTextSubstitution {
            clearSubstitution()

            invalidateForTranslate()

            true
        }
        getTextLayoutResult(action = localSemanticsTextLayoutResult)
    }

    /** Call whenever text substitution changes state */
    private fun invalidateForTranslate() {
        invalidateSemantics()
        invalidateMeasurement()
        invalidateDraw()
    }

    /** Text layout happens here */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val layoutCache = getLayoutCache(this)

        val didChangeLayout = layoutCache.layoutWithConstraints(constraints, layoutDirection)
        // ensure measure restarts when hasStaleResolvedFonts by reading in measure
        layoutCache.observeFontChanges
        val paragraph = layoutCache.paragraph!!
        val layoutSize = layoutCache.layoutSize

        if (didChangeLayout) {
            invalidateLayer()
            // Map<AlignmentLine, Int> required for use in public API `layout` below
            @Suppress("PrimitiveInCollection") var cache = baselineCache
            if (cache == null) {
                cache = HashMap(2)
                baselineCache = cache
            }
            cache[FirstBaseline] = paragraph.firstBaseline.fastRoundToInt()
            cache[LastBaseline] = paragraph.lastBaseline.fastRoundToInt()
        }

        // then allow children to measure _inside_ our final box, with the above placeholders
        val placeable =
            measurable.measure(
                fitPrioritizingWidth(
                    minWidth = layoutSize.width,
                    maxWidth = layoutSize.width,
                    minHeight = layoutSize.height,
                    maxHeight = layoutSize.height
                )
            )

        return layout(layoutSize.width, layoutSize.height, baselineCache!!) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return getLayoutCache(this).minIntrinsicWidth(layoutDirection)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = getLayoutCache(this).intrinsicHeight(width, layoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = getLayoutCache(this).maxIntrinsicWidth(layoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = getLayoutCache(this).intrinsicHeight(width, layoutDirection)

    /** Optimized Text draw. */
    override fun ContentDrawScope.draw() {
        if (!isAttached) {
            // no-up for !isAttached. The node will invalidate when attaching again.
            return
        }

        val layoutCache = getLayoutCache(this)
        val localParagraph =
            requirePreconditionNotNull(layoutCache.paragraph) {
                "no paragraph (layoutCache=$_layoutCache, textSubstitution=$textSubstitution)"
            }

        drawIntoCanvas { canvas ->
            val willClip = layoutCache.didOverflow
            if (willClip) {
                val width = layoutCache.layoutSize.width.toFloat()
                val height = layoutCache.layoutSize.height.toFloat()
                canvas.save()
                canvas.clipRect(left = 0f, top = 0f, right = width, bottom = height)
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
                        textDecoration = textDecoration
                    )
                } else {
                    val overrideColorVal = overrideColor?.invoke() ?: Color.Unspecified
                    val color =
                        if (overrideColorVal.isSpecified) {
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
                        textDecoration = textDecoration
                    )
                }
            } finally {
                if (willClip) {
                    canvas.restore()
                }
            }
        }
    }
}
