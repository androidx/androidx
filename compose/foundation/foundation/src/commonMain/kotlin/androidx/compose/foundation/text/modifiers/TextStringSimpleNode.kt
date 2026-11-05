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
import androidx.compose.foundation.text.toTextStyle
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
import androidx.compose.ui.text.ReusableTextStyleScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextStyleScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Constraints.Companion.fitPrioritizingWidth
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import kotlin.jvm.JvmName

/**
 * Node that implements Text for [String].
 *
 * It has reduced functionality, and as a result gains in performance.
 *
 * Note that this Node never calculates [TextLayoutResult] unless needed by semantics.
 */
internal class TextStringSimpleNode
private constructor(
    private var text: String,
    private var style: TextStyle?,
    private var styleProvider: (TextStyleScope.() -> Unit)?,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var defaultLocaleList: LocaleList,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var overrideColor: ColorProducer? = null,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, SemanticsModifierNode {

    constructor(
        text: String,
        style: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = DefaultMinLines,
        overrideColor: ColorProducer? = null,
    ) : this(
        text = text,
        style = style,
        styleProvider = null,
        fontFamilyResolver = fontFamilyResolver,
        defaultLocaleList = defaultLocaleList,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        overrideColor = overrideColor,
    )

    constructor(
        text: String,
        styleProvider: (TextStyleScope.() -> Unit)?,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = DefaultMinLines,
    ) : this(
        text = text,
        style = null,
        styleProvider = styleProvider,
        fontFamilyResolver = fontFamilyResolver,
        defaultLocaleList = defaultLocaleList,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        overrideColor = null,
    )

    private var cachesInitialized = false

    private var reusableStyleScope: ReusableTextStyleScope? = null
    private var evaluationStyleScope: ReusableTextStyleScope? = null
    private var lastResolvedStyle: TextStyle = TextStyle.Default

    override val shouldAutoInvalidate: Boolean
        get() = false

    @Suppress("PrimitiveInCollection") // Map required for use in public API.
    // Usages of this collection are so few that the gains of using
    // MutableObjectIntMap<AlignmentLine> and then converting to a Map<AlignmentLine, Int>
    // as needed for the public API is not worth the performance benefit.
    private var baselineCache: MutableMap<AlignmentLine, Int>? = null

    private var _layoutCache: ParagraphLayoutCache? = null
    private val layoutCache: ParagraphLayoutCache
        get() {
            initializeCachesIfNeeded()
            return _layoutCache!!
        }

    private fun evaluateStyleProvider(): PhaseInvalidations {
        val currentStyleProvider = styleProvider ?: return PhaseInvalidations.None
        val prevScope = reusableStyleScope
        if (prevScope == null) {
            val newScope = ReusableTextStyleScope()
            reusableStyleScope = newScope
            currentStyleProvider(newScope)
            lastResolvedStyle = newScope.toTextStyle()
            return PhaseInvalidations.Layout or PhaseInvalidations.Draw
        }
        val evalScope =
            evaluationStyleScope?.also { it.reset() }
                ?: ReusableTextStyleScope().also { evaluationStyleScope = it }
        currentStyleProvider(evalScope)
        var result = PhaseInvalidations.None
        if (!evalScope.hasSameLayoutAffectingAttributes(prevScope)) {
            result = result or PhaseInvalidations.Layout
        }
        if (!evalScope.hasSameDrawAffectingAttributes(prevScope)) {
            result = result or PhaseInvalidations.Draw
        }
        if (result != PhaseInvalidations.None) {
            prevScope.copyFrom(evalScope)
            lastResolvedStyle = prevScope.toTextStyle()
        }
        return result
    }

    private fun applyLayoutStyleChange() {
        if (!cachesInitialized) return
        val newStyle = lastResolvedStyle
        layoutCache.update(
            text = text,
            style = newStyle,
            fontFamilyResolver = fontFamilyResolver,
            defaultLocaleList = defaultLocaleList,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
        )
        textSubstitution?.let {
            it.layoutCache?.update(
                text = it.substitution,
                style = newStyle,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = defaultLocaleList,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
            )
        }
    }

    private fun updateDynamicStyle() {
        if (style != null || styleProvider == null) return
        val hadPreviousStyle = reusableStyleScope != null || cachesInitialized
        val invalidations = evaluateStyleProvider()
        if (invalidations.layoutChanged) {
            applyLayoutStyleChange()
        }
        if (hadPreviousStyle) {
            if (invalidations.drawChanged && semanticsTextLayoutResult != null) {
                invalidateSemantics()
            }
            if (invalidations.drawChanged || invalidations.layoutChanged) {
                invalidateDraw()
            }
        }
    }

    private fun initializeCachesIfNeeded() {
        if (!isAttached) return
        if (!cachesInitialized) {
            val initialStyle = style ?: lastResolvedStyle
            val cache = _layoutCache
            if (cache != null) {
                cache.update(
                    text = text,
                    style = initialStyle,
                    fontFamilyResolver = fontFamilyResolver,
                    defaultLocaleList = defaultLocaleList,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                )
            } else {
                _layoutCache =
                    ParagraphLayoutCache(
                        text = text,
                        style = initialStyle,
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                        overflow = overflow,
                        softWrap = softWrap,
                        maxLines = maxLines,
                        minLines = minLines,
                    )
            }
            cachesInitialized = true
        }
    }

    /**
     * Get the layout cache for the current state of the node during layout.
     *
     * If text substitution is active, this will return the layout cache for the substitution.
     * Otherwise, it will return the layout cache for the original text.
     *
     * @receiver Current measure scope that requests the layout cache. This scope is used to update
     *   the density value of the returned cache.
     */
    private fun IntrinsicMeasureScope.getLayoutCacheForMeasure(): ParagraphLayoutCache {
        updateDynamicStyle()
        val activeCache = getLayoutCache()
        activeCache.density = this@getLayoutCacheForMeasure
        return activeCache
    }

    /**
     * Get the layout cache for the current state of the node without updating the density.
     *
     * Warning; DO NOT USE this function from a MeasureScope. Instead please use
     * [getLayoutCacheForMeasure].
     *
     * The reason this function does not update the density value is because the density should not
     * change between layout and draw phases. This is a micro optimization to skip the unnecessary
     * density comparison.
     *
     * If text substitution is active, this will return the layout cache for the substitution.
     * Otherwise, it will return the layout cache for the original text.
     */
    @JvmName("getLayoutCacheOrSubstitute")
    private fun getLayoutCache(): ParagraphLayoutCache {
        initializeCachesIfNeeded()
        return textSubstitution?.takeIf { it.isShowingSubstitution }?.layoutCache ?: _layoutCache!!
    }

    fun updateStyle(styleProvider: (TextStyleScope.() -> Unit)?): Boolean {
        var changed = false
        if (this.style != null) {
            this.style = null
            changed = true
        }
        if (this.styleProvider !== styleProvider) {
            this.styleProvider = styleProvider
            changed = true
        }
        return changed
    }

    fun updateDraw(color: ColorProducer?, style: TextStyle): Boolean {
        var changed = false
        if (color != this.overrideColor) {
            changed = true
        }
        overrideColor = color
        val currStyle = this.style
        changed = changed || currStyle == null || !style.hasSameDrawAffectingAttributes(currStyle)
        return changed
    }

    fun updateDraw(color: ColorProducer?): Boolean {
        var changed = false
        if (color != this.overrideColor) {
            this.overrideColor = color
            changed = true
        }
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
        defaultLocaleList: LocaleList,
        overflow: TextOverflow,
    ): Boolean {
        val currStyle = this.style
        val changed =
            currStyle == null ||
                !currStyle.hasSameLayoutAffectingAttributes(style) ||
                this.styleProvider != null
        this.style = style
        this.styleProvider = null
        this.reusableStyleScope = null

        return updateLayoutRelatedArgs(
            minLines = minLines,
            maxLines = maxLines,
            softWrap = softWrap,
            fontFamilyResolver = fontFamilyResolver,
            defaultLocaleList = defaultLocaleList,
            overflow = overflow,
        ) || changed
    }

    /** Element has layout related params to update */
    fun updateLayoutRelatedArgs(
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        overflow: TextOverflow,
    ): Boolean {
        var changed = false

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

        if (this.defaultLocaleList != defaultLocaleList) {
            this.defaultLocaleList = defaultLocaleList
            changed = true
        }

        if (this.overflow != overflow) {
            this.overflow = overflow
            changed = true
        }

        return changed
    }

    /** request invalidate based on the results of [updateText] and [updateLayoutRelatedArgs] */
    fun doInvalidations(
        drawChanged: Boolean,
        textChanged: Boolean,
        layoutChanged: Boolean,
        styleChanged: Boolean,
    ) {
        // bring caches up to date even if the node is detached in case it is used again later
        if (cachesInitialized && (textChanged || layoutChanged)) {
            val currentStyle = style ?: lastResolvedStyle
            layoutCache.update(
                text = text,
                style = currentStyle,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = defaultLocaleList,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
            )
            textSubstitution?.let {
                it.layoutCache?.update(
                    text = it.substitution,
                    style = currentStyle,
                    fontFamilyResolver = fontFamilyResolver,
                    defaultLocaleList = defaultLocaleList,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                )
            }
        }

        if (!isAttached) {
            // no-up for !isAttached. The node will invalidate when attaching again.
            return
        }
        if (textChanged || (drawChanged && semanticsTextLayoutResult != null)) {
            invalidateSemantics()
        }

        if (textChanged || layoutChanged || styleChanged) {
            invalidateMeasurement()
            if (textChanged || layoutChanged) {
                invalidateDraw()
            }
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
        val currentStyle = style ?: lastResolvedStyle
        if (currentTextSubstitution != null) {
            if (updatedText == currentTextSubstitution.substitution) {
                return false
            }
            currentTextSubstitution.substitution = updatedText
            currentTextSubstitution.layoutCache?.update(
                updatedText,
                currentStyle,
                fontFamilyResolver,
                defaultLocaleList,
                overflow,
                softWrap,
                maxLines,
                minLines,
            ) ?: return false
        } else {
            val newTextSubstitution = TextSubstitutionValue(text, updatedText)
            val substitutionLayoutCache =
                ParagraphLayoutCache(
                    updatedText,
                    currentStyle,
                    fontFamilyResolver,
                    defaultLocaleList,
                    overflow,
                    softWrap,
                    maxLines,
                    minLines,
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
                val staticStyle = style
                val layout =
                    getLayoutCache()
                        .slowCreateTextLayoutResultOrNull(
                            style =
                                staticStyle?.merge(
                                    color = overrideColor?.invoke() ?: Color.Unspecified
                                ) ?: lastResolvedStyle
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
        constraints: Constraints,
    ): MeasureResult {
        trace("TextStringSimpleNode::measure") {
            val layoutCache = getLayoutCacheForMeasure()

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
                        maxHeight = layoutSize.height,
                    )
                )

            return layout(layoutSize.width, layoutSize.height, baselineCache!!) {
                placeable.place(0, 0)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        return getLayoutCacheForMeasure().minIntrinsicWidth(layoutDirection)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        return getLayoutCacheForMeasure().intrinsicHeight(width, layoutDirection)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        return getLayoutCacheForMeasure().maxIntrinsicWidth(layoutDirection)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        return getLayoutCacheForMeasure().intrinsicHeight(width, layoutDirection)
    }

    /** Optimized Text draw. */
    override fun ContentDrawScope.draw() {
        if (!isAttached) {
            // no-up for !isAttached. The node will invalidate when attaching again.
            return
        }

        val layoutCache = getLayoutCache()
        val localParagraph =
            requirePreconditionNotNull(layoutCache.paragraph) {
                "Internal Error: ParagraphLayoutCache could not provide a Paragraph during the draw phase. Please report this bug on the official Issue Tracker with the following diagnostic information: (layoutCache=$_layoutCache, textSubstitution=$textSubstitution)"
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
                val staticStyle = style
                val scope = reusableStyleScope
                val textDecoration =
                    scope?.textDecoration ?: staticStyle?.textDecoration ?: TextDecoration.None
                val shadow = scope?.shadow ?: staticStyle?.shadow ?: Shadow.None
                val drawStyle = scope?.drawStyle ?: staticStyle?.drawStyle ?: Fill
                val brush = scope?.brush ?: staticStyle?.brush
                if (brush != null) {
                    val alpha =
                        if (scope != null && !scope.alpha.isNaN()) scope.alpha
                        else staticStyle?.alpha ?: Float.NaN
                    localParagraph.paint(
                        canvas = canvas,
                        brush = brush,
                        alpha = alpha,
                        shadow = shadow,
                        drawStyle = drawStyle,
                        textDecoration = textDecoration,
                    )
                } else {
                    val overrideColorVal = overrideColor?.invoke() ?: Color.Unspecified
                    val color =
                        if (overrideColorVal.isSpecified) {
                            overrideColorVal
                        } else if (scope != null && scope.color.isSpecified) {
                            scope.color
                        } else if (staticStyle != null && staticStyle.color.isSpecified) {
                            staticStyle.color
                        } else {
                            Color.Black
                        }
                    localParagraph.paint(
                        canvas = canvas,
                        color = color,
                        shadow = shadow,
                        drawStyle = drawStyle,
                        textDecoration = textDecoration,
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
