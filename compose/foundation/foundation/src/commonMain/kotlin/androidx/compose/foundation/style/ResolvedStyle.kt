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
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
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
import kotlin.Byte
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
     * decisions if we know that a certain category of properties is never changed from their
     * default value. For instance, we have some properties which affect "draw", but not "layout",
     * so we can avoid invalidating layout in those cases.
     *
     * For the flag values, see constants defined like [DrawFlag].
     */
    private var _density: Float = 1f
    private var _fontScale: Float = 1f
    private var node: StyleOuterNode? = null
    private var properties: StyleProperties? = null
    private var previous: StyleProperties? = null
    private var inFlightAnimationProperties: StyleProperties? = null
    private var fromProperties: StyleProperties? = null
    private var toProperties: StyleProperties? = null
    private var toSpecs: MutableIntObjectMap<AnimationSpec<Float>>? = null
    private var fromSpecs: MutableIntObjectMap<AnimationSpec<Float>>? = null
    private var previousFromSpecs: MutableIntObjectMap<AnimationSpec<Float>>? = null
    private var animatedPrimitives: Long = 0
    private var animatedObjects: Int = 0
    private var defaultToSpec: AnimationSpec<Float>? = UnspecifiedSpec
    private var defaultFromSpec: AnimationSpec<Float>? = UnspecifiedSpec
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

    fun lerpInto(primitivesSet: Long, objectsSet: Int, target: StyleProperties) {
        val animations = animations ?: return
        val fromProperties = fromProperties ?: previous ?: return
        val toProperties = toProperties ?: properties ?: return
        lerp(fromProperties, toProperties, animations, primitivesSet, objectsSet, target)
    }

    fun resolveInto(flags: Int, target: StyleProperties) {
        val properties = properties ?: EmptyStyleProperties
        properties.copyInto(target)
        val animations = animations ?: return
        val fromProperties = fromProperties ?: previous ?: return
        val toProperties = toProperties ?: properties
        if (animations.isEmpty()) {
            this.fromProperties = null
            this.toProperties = null
            this.animations = null
            return
        }
        val primitivesFilter = primitivesSetForFlags(flags)
        val objectsFilter = objectsSetForFlags(flags)
        val inFlight = animations.inFlight()
        val inFlightPrimitives = inFlight.toPrimitivesSet()
        val inFlightObjects = inFlight.toObjectsSet()
        val primitivesToAnimate =
            widenPrimitivesSet(inFlightPrimitives, inFlightObjects) and primitivesFilter
        val objectsToAnimate =
            widenObjectsSet(primitivesToAnimate, inFlightObjects) and objectsFilter
        if (primitivesToAnimate == 0L && objectsToAnimate == 0) return

        lerp(
            fromProperties,
            toProperties,
            animations,
            primitivesToAnimate,
            objectsToAnimate,
            target,
        )
    }

    override val inspectableElements: Sequence<ValueElement>
        @Suppress("ListIterator") // Only allocates in the inspector API
        get() = properties?.valueElements(-1L, -1)?.asSequence() ?: emptySequence()

    override val density: Float
        get() = _density

    override val fontScale: Float
        get() = _fontScale

    override val state: StyleState
        get() = node!!.state

    // contentPadding
    override fun contentPaddingStart(value: Dp) {
        recordWrite(ContentPaddingStartId, defaultToSpec, defaultFromSpec)
        properties?.contentPaddingStart(value.roundToPx().toFloat())
    }

    override fun contentPaddingEnd(value: Dp) {
        recordWrite(ContentPaddingEndId, defaultToSpec, defaultFromSpec)
        properties?.contentPaddingEnd(value.roundToPx().toFloat())
    }

    override fun contentPaddingTop(value: Dp) {
        recordWrite(ContentPaddingTopId, defaultToSpec, defaultFromSpec)
        properties?.contentPaddingTop(value.roundToPx().toFloat())
    }

    override fun contentPaddingBottom(value: Dp) {
        recordWrite(ContentPaddingBottomId, defaultToSpec, defaultFromSpec)
        properties?.contentPaddingBottom(value.roundToPx().toFloat())
    }

    // externalPadding
    override fun externalPaddingStart(value: Dp) {
        recordWrite(ExternalPaddingStartId, defaultToSpec, defaultFromSpec)
        properties?.externalPaddingStart(value.roundToPx().toFloat())
    }

    override fun externalPaddingEnd(value: Dp) {
        recordWrite(ExternalPaddingEndId, defaultToSpec, defaultFromSpec)
        properties?.externalPaddingEnd(value.roundToPx().toFloat())
    }

    override fun externalPaddingTop(value: Dp) {
        recordWrite(ExternalPaddingTopId, defaultToSpec, defaultFromSpec)
        properties?.externalPaddingTop(value.roundToPx().toFloat())
    }

    override fun externalPaddingBottom(value: Dp) {
        recordWrite(ExternalPaddingBottomId, defaultToSpec, defaultFromSpec)
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
        recordWrite(BorderWidthId, defaultToSpec, defaultFromSpec)
        properties?.borderWidth(width)
    }

    override fun borderColor(value: Color) {
        recordWrite(BorderColorId, defaultToSpec, defaultFromSpec)
        recordWrite(BorderBrushId, defaultToSpec, defaultFromSpec)
        properties?.borderColor(value)
    }

    override fun borderBrush(value: Brush) {
        recordWrite(BorderColorId, defaultToSpec, defaultFromSpec)
        recordWrite(BorderBrushId, defaultToSpec, defaultFromSpec)
        properties?.borderBrush(value)
    }

    override fun width(value: Dp) {
        recordWrite(WidthId, defaultToSpec, defaultFromSpec)
        properties?.width(value.value * _density)
    }

    override fun height(value: Dp) {
        recordWrite(HeightId, defaultToSpec, defaultFromSpec)
        properties?.height(value.value * _density)
    }

    override fun width(fraction: Float) {
        recordWrite(WidthFractionId, defaultToSpec, defaultFromSpec)
        properties?.widthFraction(fraction)
    }

    override fun height(fraction: Float) {
        recordWrite(HeightFractionId, defaultToSpec, defaultFromSpec)
        properties?.heightFraction(fraction)
    }

    override fun left(value: Dp) {
        recordWrite(LeftId, defaultToSpec, defaultFromSpec)
        properties?.left(value.value * _density)
    }

    override fun top(value: Dp) {
        recordWrite(TopId, defaultToSpec, defaultFromSpec)
        properties?.top(value.value * _density)
    }

    override fun right(value: Dp) {
        recordWrite(RightId, defaultToSpec, defaultFromSpec)
        properties?.right(value.value * _density)
    }

    override fun bottom(value: Dp) {
        recordWrite(BottomId, defaultToSpec, defaultFromSpec)
        properties?.bottom(value.value * _density)
    }

    override fun minWidth(value: Dp) {
        recordWrite(MinWidthId, defaultToSpec, defaultFromSpec)
        properties?.minWidth(value.value * _density)
    }

    override fun minHeight(value: Dp) {
        recordWrite(MinHeightId, defaultToSpec, defaultFromSpec)
        properties?.minHeight(value.value * _density)
    }

    override fun maxWidth(value: Dp) {
        recordWrite(MaxWidthId, defaultToSpec, defaultFromSpec)
        properties?.maxWidth(value.value * _density)
    }

    override fun maxHeight(value: Dp) {
        recordWrite(MaxHeightId, defaultToSpec, defaultFromSpec)
        properties?.maxHeight(value.value * _density)
    }

    // layer properties
    override fun alpha(value: Float) {
        recordWrite(AlphaId, defaultToSpec, defaultFromSpec)
        properties?.alpha(value)
    }

    override fun scaleX(value: Float) {
        recordWrite(ScaleXId, defaultToSpec, defaultFromSpec)
        properties?.scaleX(value)
    }

    override fun scaleY(value: Float) {
        recordWrite(ScaleYId, defaultToSpec, defaultFromSpec)
        properties?.scaleY(value)
    }

    override fun translationX(value: Float) {
        recordWrite(TranslationXId, defaultToSpec, defaultFromSpec)
        properties?.translationX(value)
    }

    override fun translationY(value: Float) {
        recordWrite(TranslationYId, defaultToSpec, defaultFromSpec)
        properties?.translationY(value)
    }

    override fun rotationX(value: Float) {
        recordWrite(RotationXId, defaultToSpec, defaultFromSpec)
        properties?.rotationX(value)
    }

    override fun rotationY(value: Float) {
        recordWrite(RotationYId, defaultToSpec, defaultFromSpec)
        properties?.rotationY(value)
    }

    override fun rotationZ(value: Float) {
        recordWrite(RotationZId, defaultToSpec, defaultFromSpec)
        properties?.rotationZ(value)
    }

    override fun transformOriginX(value: Float) {
        recordWrite(TransformOriginXId, defaultToSpec, defaultFromSpec)
        properties?.transformOriginX(value)
    }

    override fun transformOriginY(value: Float) {
        recordWrite(TransformOriginYId, defaultToSpec, defaultFromSpec)
        properties?.transformOriginY(value)
    }

    override fun colorFilter(value: ColorFilter?) {
        recordWrite(ColorFilterId, defaultToSpec, defaultFromSpec)
        properties?.colorFilter(value)
    }

    override fun clip(value: Boolean) {
        recordWrite(ClipId, defaultToSpec, defaultFromSpec)
        properties?.clip(value)
    }

    override fun zIndex(value: Float) {
        recordWrite(ZIndexId, defaultToSpec, defaultFromSpec)
        properties?.zIndex(value)
    }

    // draw properties
    override fun background(color: Color) {
        recordWrite(BackgroundColorId, defaultToSpec, defaultFromSpec)
        recordWrite(BackgroundBrushId, defaultToSpec, defaultFromSpec)
        properties?.backgroundColor(color)
    }

    override fun background(value: Brush) {
        recordWrite(BackgroundColorId, defaultToSpec, defaultFromSpec)
        recordWrite(BackgroundBrushId, defaultToSpec, defaultFromSpec)
        properties?.backgroundBrush(value)
    }

    override fun foreground(value: Color) {
        recordWrite(ForegroundColorId, defaultToSpec, defaultFromSpec)
        recordWrite(ForegroundBrushId, defaultToSpec, defaultFromSpec)
        properties?.foregroundColor(value)
    }

    override fun foreground(value: Brush) {
        recordWrite(ForegroundColorId, defaultToSpec, defaultFromSpec)
        recordWrite(ForegroundBrushId, defaultToSpec, defaultFromSpec)
        properties?.foregroundBrush(value)
    }

    // TODO: consider borderRadius?

    override fun shape(value: Shape) {
        recordWrite(ShapeId, defaultToSpec, defaultFromSpec)
        properties?.shape(value)
    }

    override fun animate(
        toSpec: AnimationSpec<Float>,
        fromSpec: AnimationSpec<Float>,
        block: () -> Unit,
    ) {
        val previousToSpec = defaultToSpec
        val previousFromSpec = defaultFromSpec
        try {
            defaultToSpec = toSpec
            defaultFromSpec = fromSpec

            block()
        } finally {
            defaultToSpec = previousToSpec
            defaultFromSpec = previousFromSpec
        }
    }

    override val <T> CompositionLocal<T>.currentValue: T
        get() = node!!.currentValueOf(this)

    override fun dropShadow(value: Shadow) {
        recordWrite(DropShadowId, defaultToSpec, defaultFromSpec)
        properties?.dropShadow(value)
    }

    override fun dropShadow(vararg value: Shadow) {
        recordWrite(DropShadowId, defaultToSpec, defaultFromSpec)
        properties?.dropShadow(value)
    }

    override fun innerShadow(value: Shadow) {
        recordWrite(InnerShadowId, defaultToSpec, defaultFromSpec)
        properties?.innerShadow(value)
    }

    override fun innerShadow(vararg value: Shadow) {
        recordWrite(InnerShadowId, defaultToSpec, defaultFromSpec)
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
        recordWrite(ContentColorId, defaultToSpec, defaultFromSpec)
        recordWrite(ContentBrushId, defaultToSpec, defaultFromSpec)
        properties?.contentColor(value)
    }

    override fun contentBrush(value: Brush) {
        recordWrite(ContentColorId, defaultToSpec, defaultFromSpec)
        recordWrite(ContentBrushId, defaultToSpec, defaultFromSpec)
        properties?.contentBrush(value)
    }

    override fun textDecoration(value: TextDecoration) {
        recordWrite(TextDecorationId, defaultToSpec, defaultFromSpec)
        properties?.textDecoration(value)
    }

    override fun fontFamily(value: FontFamily) {
        recordWrite(FontFamilyId, defaultToSpec, defaultFromSpec)
        properties?.fontFamily(value)
    }

    override fun textMotion(value: TextMotion) {
        recordWrite(TextMotionId, defaultToSpec, defaultFromSpec)
        properties?.textMotion(value)
    }

    override fun textIndent(value: TextIndent) {
        recordWrite(TextIndentId, defaultToSpec, defaultFromSpec)
        properties?.textIndent(value)
    }

    override fun fontSize(value: TextUnit) {
        recordWrite(FontSizeId, defaultToSpec, defaultFromSpec)
        properties?.fontSize(value)
    }

    override fun lineHeight(value: TextUnit) {
        recordWrite(LineHeightId, defaultToSpec, defaultFromSpec)
        properties?.lineHeight(value)
    }

    override fun letterSpacing(value: TextUnit) {
        recordWrite(LetterSpacingId, defaultToSpec, defaultFromSpec)
        properties?.letterSpacing(value)
    }

    override fun baselineShift(value: BaselineShift) {
        recordWrite(BaselineShiftId, defaultToSpec, defaultFromSpec)
        properties?.baselineShift(value)
    }

    override fun lineBreak(value: LineBreak) {
        recordWrite(LineBreakId, defaultToSpec, defaultFromSpec)
        properties?.lineBreak(value)
    }

    override fun fontStyle(value: FontStyle) {
        recordWrite(FontStyleId, defaultToSpec, defaultFromSpec)
        properties?.fontStyle(value)
    }

    override fun textAlign(value: TextAlign) {
        recordWrite(TextAlignId, defaultToSpec, defaultFromSpec)
        properties?.textAlign(value)
    }

    override fun textDirection(value: TextDirection) {
        recordWrite(TextDirectionId, defaultToSpec, defaultFromSpec)
        properties?.textDirection(value)
    }

    override fun hyphens(value: Hyphens) {
        recordWrite(HyphensId, defaultToSpec, defaultFromSpec)
        properties?.hyphens(value)
    }

    override fun fontWeight(value: FontWeight) {
        recordWrite(FontWeightId, defaultToSpec, defaultFromSpec)
        properties?.fontWeight(value)
    }

    override fun fontSynthesis(value: FontSynthesis) {
        recordWrite(FontSynthesisId, defaultToSpec, defaultFromSpec)
        properties?.fontSynthesis(value)
    }

    override fun <T> state(
        key: StyleStateKey<T>,
        block: () -> Unit,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    ) {
        if (active(key, state)) block()
    }

    internal fun prepareBuild() {
        // If there are any in-flight animations we need to capture their values before we build
        // so we can ensure we have the correct from values to animate from.
        // It is important that this is done outside a state observation scope as it will read the
        // animations which will be frequently changed. The animations are observed by
        // resolveInto so they don't need to be read during build.
        val animations = animations
        val properties = properties
        if (animations != null && properties != null) {
            val inFlight = animations.inFlight()
            if (inFlight != 0L) {
                val inFlightPrimitives = inFlight.toPrimitivesSet()
                val inFlightObjects = inFlight.toObjectsSet()
                val inFlightAnimationProperties = StyleProperties()

                // Collect the current value of in-flight animated properties
                lerpInto(inFlightPrimitives, inFlightObjects, inFlightAnimationProperties)
                this.inFlightAnimationProperties = inFlightAnimationProperties
            } else {
                this.inFlightAnimationProperties = null
            }
        } else {
            this.inFlightAnimationProperties = null
        }
    }

    internal fun startBuild(node: StyleOuterNode, density: Density) {
        this.node = node
        this._density = density.density
        val properties = properties
        val newProperties = previous?.also { it.clear() } ?: StyleProperties()
        this.properties = newProperties
        previous = properties
        previousFromSpecs = null
    }

    internal fun doneBuild() {
        val node = node!!
        this.node = null

        // If no properties where set to animate and no animations are running, there is no need to
        // compute animations
        if (animations.isNullOrEmpty() && animatedPrimitives == 0L && animatedObjects == 0) {
            return
        }

        // If this is the very first time resolving this style there are no changes to animate to.
        // Even if a property is marked as "animate", we ignore the animation because we only
        // animate when the value changes.
        val properties = properties ?: return
        val previous = previous ?: return

        // Check both brushes and colors together
        val effectiveAnimatedObjects = widenObjectsSet(animatedPrimitives, animatedObjects)
        val effectiveAnimatedPrimitives = widenPrimitivesSet(animatedPrimitives, animatedObjects)

        // Determine which properties that have been marked animated have changed.
        val objectsChanged = previous.diffObjects(properties, effectiveAnimatedObjects)
        val primitivesChanged = previous.diffPrimitives(properties, effectiveAnimatedPrimitives)

        // If no animated properties have changed there is no need to start any new animations.
        // This leaves existing animations running.
        if (primitivesChanged == 0L && objectsChanged == 0) {
            return
        }

        val animations =
            animations
                ?: run {
                    val newAnimations = StyleAnimations()
                    animations = newAnimations
                    newAnimations
                }

        // Animations only thinks it is animating brushes. If only colors are animating then
        // lerp will lerp the colors. This avoids trying to start an animation for colors and
        // brushes at the same time.
        val effectivePrimitivesChanged = filterPrimitiveColors(primitivesChanged)
        val effectiveObjectsChanged = widenObjectsSet(primitivesChanged, objectsChanged)

        // Convert the sets to the property set used by style animations
        val changedProperties =
            propertySetsToAnimationSet(effectivePrimitivesChanged, effectiveObjectsChanged)

        val animatedProperties =
            propertySetsToAnimationSet(effectiveAnimatedPrimitives, effectiveAnimatedObjects)

        val started =
            animations.recordAnimations(
                animatedProperties,
                changedProperties,
                toSpecs,
                fromSpecs,
                previousFromSpecs,
                node,
            )

        // Record the "from" values for any properties that have started an animation.
        if (started != 0L) {
            val fromProperties =
                fromProperties
                    ?: run {
                        val fromProperties = StyleProperties()
                        this.fromProperties = fromProperties
                        fromProperties
                    }

            val startedPrimitives = started.toPrimitivesSet()
            val startedObjects = started.toObjectsSet()

            // Copy both colors and brushes
            val widenedPrimitivesStarted = widenPrimitivesSet(startedPrimitives, startedObjects)
            val widenedObjectsStarted = widenObjectsSet(startedPrimitives, startedObjects)

            // Copy the previous values for animations that have been started
            previous.copyInto(fromProperties, widenedPrimitivesStarted, widenedObjectsStarted)

            // Copy any of the current values of any animations that are in-flight.
            inFlightAnimationProperties?.copyInto(
                fromProperties,
                widenedPrimitivesStarted,
                widenedObjectsStarted,
            )
            inFlightAnimationProperties = null
        }

        if (animations.isEmpty()) {
            this.animations = null
        }
    }

    private fun addToSpec(id: Int, to: AnimationSpec<Float>) {
        (toSpecs
            ?: run {
                val newSpecs = mutableIntObjectMapOf<AnimationSpec<Float>>()
                toSpecs = newSpecs
                newSpecs
            })[id] = to
    }

    private fun removeToSpec(id: Int) {
        toSpecs?.remove(id)
    }

    private fun addFromSpec(id: Int, from: AnimationSpec<Float>) {
        (fromSpecs
            ?: run {
                val newSpecs = mutableIntObjectMapOf<AnimationSpec<Float>>()
                fromSpecs = newSpecs
                newSpecs
            })[id] = from
    }

    private fun removeFromSpec(id: Int) {
        fromSpecs?.let { fromSpecs ->
            val previous = fromSpecs[id]
            fromSpecs.remove(id)
            if (previous != null) {
                (previousFromSpecs
                    ?: run {
                        val newSpecs = mutableIntObjectMapOf<AnimationSpec<Float>>()
                        previousFromSpecs = newSpecs
                        newSpecs
                    })[id] = previous
            }
        }
    }

    private fun recordWriteCommon(id: Int, to: AnimationSpec<Float>?, from: AnimationSpec<Float>?) {
        if (to != null && to != DefaultSpringSpec) {
            addToSpec(id, to)
        } else removeToSpec(id)
        if (from != null && from != DefaultSpringSpec) {
            addFromSpec(id, from)
        } else removeFromSpec(id)
    }

    private fun recordWrite(id: Byte, to: AnimationSpec<Float>?, from: AnimationSpec<Float>?) {
        // Only update the value of the specification if it was specified, otherwise leave it the
        // same as it was set previously.
        val effectiveTo =
            if (to === UnspecifiedSpec) {
                if (animatedPrimitives.hasId(id)) toSpecs?.get(id.toInt()) ?: DefaultSpringSpec
                else null
            } else to
        val effectiveFrom =
            if (from === UnspecifiedSpec) {
                if (animatedPrimitives.hasId(id)) fromSpecs?.get(id.toInt()) ?: DefaultSpringSpec
                else null
            } else from
        val animated = effectiveTo != null && effectiveFrom != null
        animatedPrimitives =
            if (animated) animatedPrimitives.withId(id) else animatedPrimitives.withoutId(id)
        recordWriteCommon(id.toInt(), effectiveTo, effectiveFrom)
    }

    private inline fun recordWrite(
        id: Int,
        to: AnimationSpec<Float>?,
        from: AnimationSpec<Float>?,
    ) {
        // Only update the value of the specification if it was specified, otherwise leave it the
        // same as it was set previously.
        val effectiveTo =
            if (to === UnspecifiedSpec) {
                if (animatedObjects.hasId(id)) toSpecs?.get(id) ?: DefaultSpringSpec else null
            } else to
        val effectiveFrom =
            if (from === UnspecifiedSpec) {
                if (animatedObjects.hasId(id)) fromSpecs?.get(id) ?: DefaultSpringSpec else null
            } else from
        val animated = effectiveTo != null && effectiveFrom != null
        animatedObjects =
            if (animated) animatedObjects.withId(id) else animatedObjects.withoutId(id)
        recordWriteCommon(id, effectiveTo, effectiveFrom)
    }
}

internal val DefaultSpringSpec = spring<Float>()

// Used as a sentinel value to track when the animation was not set. This value is never used as
// a specification, it just needs to compare not-equal to any other specification.
internal object UnspecifiedSpec : FiniteAnimationSpec<Float> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Float, V>
    ): VectorizedFiniteAnimationSpec<V> = error("UnspecifiedSpec should never be used as a spec")
}
