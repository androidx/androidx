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

@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.isSpecified
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.trace
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope

/**
 * This class is currently the actual object that [Style] lambdas get executed with, so this is the
 * primary implementation of [StyleScope] that exists. This is currently implemented as a genuine
 * class with properties/fields for all the possible values that can be set via [StyleScope].
 *
 * Since [StyleScope] is a large API, that currently means there are ~50 individual fields on this
 * object, which means it is a fairly expensive object to allocate, and takes up a decent amount of
 * memory. We might consider alternative implementations that would hold values in an array or
 * something which might allow for the memory impact of a ResolvedStyle to be less, but the runtime
 * impact might be worse. This is all implementation detail however, and could be experimented with
 * over time.
 */
internal class ResolvedStyle internal constructor() : StyleScope, InspectableValue {
    // TODO: there's currently ~50 individual properties on this class. We could use a single bit
    //  for every single property if flags became a Long, which might allow for some functions to
    //  be written more efficiently?

    /**
     * This field is effectively a bitset indicating which "categories" of properties have been set
     * on this object. There are a lot of different properties, and we can make certain optimal
     * decisions if we know that a certain category of properties have never been changed from their
     * default value. For instance, we have some properties which affect "draw", but not "layout",
     * so we can avoid invalidating layout in those cases.
     *
     * For the flag values, see constants defined like [DrawFlag].
     */
    private var _density: Float = 1f
    private var _fontScale: Float = 1f
    private var node: StyleOuterNode? = null
    private var properties: StyleProperties? = null
    private var baseProperties: StyleProperties? = null
    private var animationProperties: StyleProperties? = null
    private var targetProperties: StyleProperties? = null
    private var toSpecs: MutableIntObjectMap<AnimationSpec<Float>>? = null
    private var fromSpecs: MutableIntObjectMap<AnimationSpec<Float>>? = null
    private var defaultToSpec: AnimationSpec<Float>? = null
    private var defaultFromSpec: AnimationSpec<Float>? = null
    private var animations: StyleAnimations? = null

    internal fun build(style: Style, node: StyleOuterNode, density: Density) {
        trace("Compose:Styles:build") {
            startBuild(node, density)
            with(style) { applyStyle() }
            doneBuild()
        }
    }

    internal fun buildForTesting(
        style: Style,
        density: Density,
        state: StyleState? = null,
        coroutineScope: CoroutineScope? = null,
    ) {
        startBuild(StyleOuterNode(state, style, coroutineScope), density)
        with(style) { applyStyle() }
        doneBuild()
    }

    internal fun closeForTesting() {
        animations?.close()
    }

    val animatingFlags: Int
        get() = animations?.phaseFlags() ?: 0

    fun resolveInto(flags: Int, properties: StyleProperties) {
        val baseProperties =
            baseProperties
                ?: run {
                    EmptyStyleProperties.copyInto(properties)
                    return
                }
        baseProperties.copyInto(properties)
        val animations = animations ?: return
        if (animations.isEmpty()) return
        val targetProperties = this@ResolvedStyle.targetProperties ?: return
        val primitivesSet = primitivesSetForFlags(flags)
        val objectsSet = objectsSetForFlags(flags)
        lerp(baseProperties, targetProperties, animations, primitivesSet, objectsSet, properties)
    }

    override val inspectableElements: Sequence<ValueElement>
        @Suppress("ListIterator") // Only allocates in the inspector API
        get() = properties?.valueElements()?.asSequence() ?: emptySequence()

    override val density: Float
        get() = _density

    override val fontScale: Float
        get() = _fontScale

    override val state: StyleState
        get() = node!!.state

    // contentPadding
    override fun contentPaddingStart(value: Dp) {
        recordWrite(ContentPaddingStartId)
        properties?.contentPaddingStart(value.roundToPx().toFloat())
    }

    override fun contentPaddingEnd(value: Dp) {
        recordWrite(ContentPaddingEndId)
        properties?.contentPaddingEnd(value.roundToPx().toFloat())
    }

    override fun contentPaddingTop(value: Dp) {
        recordWrite(ContentPaddingTopId)
        properties?.contentPaddingTop(value.roundToPx().toFloat())
    }

    override fun contentPaddingBottom(value: Dp) {
        recordWrite(ContentPaddingBottomId)
        properties?.contentPaddingBottom(value.roundToPx().toFloat())
    }

    // externalPadding
    override fun externalPaddingStart(value: Dp) {
        recordWrite(ExternalPaddingStartId)
        properties?.externalPaddingStart(value.roundToPx().toFloat())
    }

    override fun externalPaddingEnd(value: Dp) {
        recordWrite(ExternalPaddingEndId)
        properties?.externalPaddingEnd(value.roundToPx().toFloat())
    }

    override fun externalPaddingTop(value: Dp) {
        recordWrite(ExternalPaddingTopId)
        properties?.externalPaddingTop(value.roundToPx().toFloat())
    }

    override fun externalPaddingBottom(value: Dp) {
        recordWrite(ExternalPaddingBottomId)
        properties?.externalPaddingBottom(value.roundToPx().toFloat())
    }

    // border
    override fun borderWidth(value: Dp) {
        val width =
            when (value) {
                Dp.Unspecified -> 0.0f
                Dp.Hairline -> 1.0f
                else -> ceil(value.value * _density)
            }
        recordWrite(BorderWidthId)
        properties?.borderWidth(width)
    }

    override fun borderColor(value: Color) {
        recordWrite(BorderColorId)
        recordWrite(BorderBrushId)
        properties?.borderColor(value)
    }

    override fun borderBrush(value: Brush) {
        recordWrite(BorderColorId)
        recordWrite(BorderBrushId)
        properties?.borderBrush(value)
    }

    override fun width(value: Dp) {
        recordWrite(WidthId)
        properties?.width(value.value * _density)
    }

    override fun height(value: Dp) {
        recordWrite(HeightId)
        properties?.height(value.value * _density)
    }

    override fun width(fraction: Float) {
        recordWrite(WidthFractionId)
        properties?.widthFraction(fraction)
    }

    override fun height(fraction: Float) {
        recordWrite(HeightFractionId)
        properties?.heightFraction(fraction)
    }

    override fun left(value: Dp) {
        recordWrite(LeftId)
        properties?.left(value.value * _density)
    }

    override fun top(value: Dp) {
        recordWrite(TopId)
        properties?.top(value.value * _density)
    }

    override fun right(value: Dp) {
        recordWrite(RightId)
        properties?.right(value.value * _density)
    }

    override fun bottom(value: Dp) {
        recordWrite(BottomId)
        properties?.bottom(value.value * _density)
    }

    override fun minWidth(value: Dp) {
        recordWrite(MinWidthId)
        properties?.minWidth(value.value * _density)
    }

    override fun minHeight(value: Dp) {
        recordWrite(MinHeightId)
        properties?.minHeight(value.value * _density)
    }

    override fun maxWidth(value: Dp) {
        recordWrite(MaxWidthId)
        properties?.maxWidth(value.value * _density)
    }

    override fun maxHeight(value: Dp) {
        recordWrite(MaxHeightId)
        properties?.maxHeight(value.value * _density)
    }

    // layer properties
    override fun alpha(value: Float) {
        recordWrite(AlphaId)
        properties?.alpha(value)
    }

    override fun scaleX(value: Float) {
        recordWrite(ScaleXId)
        properties?.scaleX(value)
    }

    override fun scaleY(value: Float) {
        recordWrite(ScaleYId)
        properties?.scaleY(value)
    }

    override fun translationX(value: Float) {
        recordWrite(TranslationXId)
        properties?.translationX(value)
    }

    override fun translationY(value: Float) {
        recordWrite(TranslationYId)
        properties?.translationY(value)
    }

    override fun rotationX(value: Float) {
        recordWrite(RotationXId)
        properties?.rotationX(value)
    }

    override fun rotationY(value: Float) {
        recordWrite(RotationYId)
        properties?.rotationY(value)
    }

    override fun rotationZ(value: Float) {
        recordWrite(RotationZId)
        properties?.rotationZ(value)
    }

    override fun transformOriginX(value: Float) {
        recordWrite(TransformOriginXId)
        properties?.transformOriginX(value)
    }

    override fun transformOriginY(value: Float) {
        recordWrite(TransformOriginYId)
        properties?.transformOriginY(value)
    }

    override fun colorFilter(value: ColorFilter?) {
        recordWrite(ColorFilterId)
        properties?.colorFilter(value)
    }

    override fun clip(value: Boolean) {
        recordWrite(ClipId)
        properties?.clip(value)
    }

    override fun zIndex(value: Float) {
        recordWrite(ZIndexId)
        properties?.zIndex(value)
    }

    // draw properties
    override fun background(color: Color) {
        recordWrite(BackgroundColorId)
        recordWrite(BackgroundBrushId)
        properties?.backgroundColor(color)
    }

    override fun background(value: Brush) {
        recordWrite(BackgroundColorId)
        recordWrite(BackgroundBrushId)
        properties?.backgroundBrush(value)
    }

    override fun foreground(value: Color) {
        recordWrite(ForegroundColorId)
        recordWrite(ForegroundBrushId)
        properties?.foregroundColor(value)
    }

    override fun foreground(value: Brush) {
        recordWrite(ForegroundColorId)
        recordWrite(ForegroundBrushId)
        properties?.foregroundBrush(value)
    }

    // TODO: consider borderRadius?

    override fun shape(value: Shape) {
        recordWrite(ShapeId)
        properties?.shape(value)
    }

    override fun animate(
        toSpec: AnimationSpec<Float>,
        fromSpec: AnimationSpec<Float>,
        block: () -> Unit,
    ) {
        val previous = properties
        val previousToSpec = defaultToSpec
        val previousFromSpec = defaultFromSpec
        try {
            defaultToSpec = toSpec
            defaultFromSpec = fromSpec
            properties = animationProperties ?: StyleProperties().also { animationProperties = it }
            block()
        } finally {
            properties = previous
            defaultToSpec = previousToSpec
            defaultFromSpec = previousFromSpec
        }
    }

    override val <T> CompositionLocal<T>.currentValue: T
        get() = node!!.currentValueOf(this)

    override fun dropShadow(value: Shadow) {
        recordWrite(DropShadowId)
        properties?.dropShadow(value)
    }

    override fun dropShadow(vararg value: Shadow) {
        recordWrite(DropShadowId)
        properties?.dropShadow(value)
    }

    override fun innerShadow(value: Shadow) {
        recordWrite(InnerShadowId)
        properties?.innerShadow(value)
    }

    override fun innerShadow(vararg value: Shadow) {
        recordWrite(InnerShadowId)
        properties?.innerShadow(value)
    }

    override fun textStyle(value: TextStyle) {
        // TODO: optimize further
        val span = value.toSpanStyle()
        if (span.color.isSpecified) contentColor(span.color)
        if (span.fontSize.isSpecified) fontSize(span.fontSize)
        if (span.letterSpacing.isSpecified) letterSpacing(span.letterSpacing)
        span.brush?.let { contentBrush(it) }
        span.fontStyle?.let { fontStyle(it) }
        span.baselineShift?.let { if (it.isSpecified) baselineShift(it) }
        span.fontWeight?.let { fontWeight(it) }
        span.textDecoration?.let { textDecoration(it) }
        span.fontSynthesis?.let { fontSynthesis(it) }
        span.fontFamily?.let { fontFamily(it) }

        val p = value.toParagraphStyle()
        p.textIndent?.let { textIndent(it) }
        if (p.lineHeight.isSpecified) lineHeight(p.lineHeight)
        if (p.lineBreak.isSpecified) lineBreak(p.lineBreak)
        if (p.hyphens.isSpecified) hyphens(p.hyphens)
        if (p.textDirection.isSpecified) textDirection(p.textDirection)
        if (p.textAlign.isSpecified) textAlign(p.textAlign)
        p.textMotion?.let { textMotion(it) }
    }

    internal val fontStyle: FontStyle
        get() {
            val properties = properties
            if (properties != null) return properties.fontStyle
            return FontStyle.Normal
        }

    internal val textAlign: TextAlign
        get() {
            val properties = properties
            if (properties != null) {
                return properties.textAlign
            }
            return TextAlign.Unspecified
        }

    internal val textDirection: TextDirection
        get() {
            val properties = properties
            if (properties != null) {
                return properties.textDirection
            }
            return TextDirection.Unspecified
        }

    internal val hyphens: Hyphens
        get() {
            val properties = properties
            if (properties != null) {
                return properties.hyphens
            }
            return Hyphens.Unspecified
        }

    internal val fontWeight: FontWeight
        get() {
            val properties = properties
            if (properties != null) {
                return properties.fontWeight
            }
            return FontWeight.Normal
        }

    internal val fontSynthesis: FontSynthesis
        get() {
            val properties = properties
            if (properties != null) {
                return properties.fontSynthesis
            }
            return FontSynthesis.None
        }

    internal val textDecoration: TextDecoration
        get() {
            val properties = properties
            if (properties != null) {
                return properties.textDecoration
            }
            return TextDecoration.None
        }

    override fun contentColor(value: Color) {
        recordWrite(ContentColorId)
        recordWrite(ContentBrushId)
        properties?.contentColor(value)
    }

    override fun contentBrush(value: Brush) {
        recordWrite(ContentColorId)
        recordWrite(ContentBrushId)
        properties?.contentBrush(value)
    }

    override fun textDecoration(value: TextDecoration) {
        recordWrite(TextDecorationId)
        properties?.textDecoration(value)
    }

    override fun fontFamily(value: FontFamily) {
        recordWrite(FontFamilyId)
        properties?.fontFamily(value)
    }

    override fun textMotion(value: TextMotion) {
        recordWrite(TextMotionId)
        properties?.textMotion(value)
    }

    override fun textIndent(value: TextIndent) {
        recordWrite(TextIndentId)
        properties?.textIndent(value)
    }

    override fun fontSize(value: TextUnit) {
        recordWrite(FontSizeId)
        properties?.fontSize(value)
    }

    override fun lineHeight(value: TextUnit) {
        recordWrite(LineHeightId)
        properties?.lineHeight(value)
    }

    override fun letterSpacing(value: TextUnit) {
        recordWrite(LetterSpacingId)
        properties?.letterSpacing(value)
    }

    override fun baselineShift(value: BaselineShift) {
        recordWrite(BaselineShiftId)
        properties?.baselineShift(value)
    }

    override fun lineBreak(value: LineBreak) {
        recordWrite(LineBreakId)
        properties?.lineBreak(value)
    }

    override fun fontStyle(value: FontStyle) {
        recordWrite(FontStyleId)
        properties?.fontStyle(value)
    }

    override fun textAlign(value: TextAlign) {
        recordWrite(TextAlignId)
        properties?.textAlign(value)
    }

    override fun textDirection(value: TextDirection) {
        recordWrite(TextDirectionId)
        properties?.textDirection(value)
    }

    override fun hyphens(value: Hyphens) {
        recordWrite(HyphensId)
        properties?.hyphens(value)
    }

    override fun fontWeight(value: FontWeight) {
        recordWrite(FontWeightId)
        properties?.fontWeight(value)
    }

    override fun fontSynthesis(value: FontSynthesis) {
        recordWrite(FontSynthesisId)
        properties?.fontSynthesis(value)
    }

    override fun <T> state(
        key: StyleStateKey<T>,
        block: () -> Unit,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    ) {
        if (active(key, state)) block()
    }

    internal fun startBuild(node: StyleOuterNode, density: Density) {
        this.node = node
        this._density = density.density
        val properties = baseProperties ?: StyleProperties().also { baseProperties = it }
        properties.clear()
        animationProperties?.clear()
        this.properties = properties
    }

    internal fun doneBuild() {
        val node = node!!
        this.node = null
        val animationProperties = animationProperties ?: return
        val animations =
            animations
                ?: run {
                    val newAnimations = StyleAnimations()
                    animations = newAnimations
                    newAnimations
                }
        animations.recordAnimations(baseProperties, animationProperties, fromSpecs, toSpecs, node)
        val targetProperties =
            this.targetProperties
                ?: run {
                    val newProperties = StyleProperties()
                    this.targetProperties = newProperties
                    newProperties
                }
        animationProperties.copyInto(targetProperties, PhaseFlagMask)
        if (animations.isEmpty()) {
            this.animations = null
            this.targetProperties = null
        }
    }

    private fun recordWrite(id: Byte) {
        if (properties == baseProperties) {
            toSpecs?.remove(id.toInt())
            fromSpecs?.remove(id.toInt())
            return
        }
        val toSpec = defaultToSpec
        val fromSpec = defaultFromSpec
        if (toSpec != null)
            (toSpecs
                ?: run {
                    val newSpecs = mutableIntObjectMapOf<AnimationSpec<Float>>()
                    toSpecs = newSpecs
                    newSpecs
                })[id.toInt()] = toSpec
        if (fromSpec != null)
            (fromSpecs
                ?: run {
                    val newSpecs = mutableIntObjectMapOf<AnimationSpec<Float>>()
                    fromSpecs = newSpecs
                    newSpecs
                })[id.toInt()] = fromSpec
    }

    private inline fun recordWrite(id: Int) {
        recordWrite(id.toByte())
    }
}

internal val DefaultSpringSpec = spring<Float>()
