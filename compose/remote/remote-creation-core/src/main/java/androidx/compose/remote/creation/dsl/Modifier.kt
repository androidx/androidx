/*
 * Copyright 2026 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:JvmName("RcModifiers")

package androidx.compose.remote.creation.dsl

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.ClickActionModifier
import androidx.compose.remote.creation.modifiers.EventHandlerModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.TouchActionModifier

/**
 * An ordered, immutable collection of modifier [Element]s that can be used to augment components.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Modifier {
    /** Concatenates this modifier with another. */
    public fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)

    /**
     * Accumulates a value starting with [initial] and applying [operation] to each [Element] in
     * this modifier from left to right.
     */
    public fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /** A single element within a [Modifier] chain. */
    public interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
    }

    /** The companion object serves as the empty [Modifier]. */
    public companion object : Modifier {
        override fun then(other: Modifier): Modifier = other

        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
    }
}

/** Internal element contract that bridges a DSL [Modifier.Element] to [RecordingModifier]. */
internal interface RecordingModifierElement : Modifier.Element {
    fun applyTo(modifier: RecordingModifier)
}

/** Internal implementation of a combined modifier chain. */
private class CombinedModifier(private val outer: Modifier, private val inner: Modifier) :
    Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun equals(other: Any?): Boolean =
        other is CombinedModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString(): String =
        "[" +
            foldIn("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}

/** Basic padding modifier. */
public fun Modifier.padding(all: Float): Modifier = then(PaddingModifier(all, all, all, all))

public fun Modifier.padding(all: RcDp): Modifier =
    then(PaddingModifier(all.value, all.value, all.value, all.value))

public fun Modifier.padding(all: RcPx): Modifier =
    then(PaddingModifier(all.value, all.value, all.value, all.value))

public fun Modifier.padding(
    start: Float = 0f,
    top: Float = 0f,
    end: Float = 0f,
    bottom: Float = 0f,
): Modifier = then(PaddingModifier(start, top, end, bottom))

/** Basic size modifier. */
public fun Modifier.size(width: Float, height: Float): Modifier = then(SizeModifier(width, height))

public fun Modifier.size(size: Float): Modifier = size(size, size)

public fun Modifier.size(width: RcDp, height: RcDp): Modifier = size(width.value, height.value)

public fun Modifier.size(size: RcDp): Modifier = size(size.value, size.value)

/** Basic width modifier. */
public fun Modifier.width(width: Float): Modifier = then(WidthModifier(width))

public fun Modifier.width(width: RcFloat): Modifier = then(WidthRcFloatModifier(width))

public fun Modifier.width(width: RcDp): Modifier = width(width.value)

/** Basic height modifier. */
public fun Modifier.height(height: Float): Modifier = then(HeightModifier(height))

public fun Modifier.height(height: RcFloat): Modifier = then(HeightRcFloatModifier(height))

public fun Modifier.height(height: RcDp): Modifier = height(height.value)

/** Draws a solid background fill using a packed ARGB color int (`0xAARRGGBB`). */
public fun Modifier.background(@ColorInt color: Int): Modifier = then(BackgroundModifier(color))

/** Draws a solid background fill using a 32-bit packed ARGB `Long` hex literal (`0xAARRGGBB`). */
public fun Modifier.background(color: Long): Modifier = background(color.toInt())

/** Draws a solid background fill using a static [RcColorValue]. */
public fun Modifier.background(color: RcColorValue): Modifier = then(BackgroundColorModifier(color))

/** Draws a solid background fill using a dynamic [RcColor] reference. */
public fun Modifier.background(color: RcColor): Modifier = then(BackgroundColorIdModifier(color))

/** Allocates horizontal space inside a [RcScope.Row] proportionally to [weight]. */
public fun Modifier.horizontalWeight(
    @FloatRange(from = 0.0, fromInclusive = false) weight: Float
): Modifier = then(WeightModifier(weight, vertical = false))

/** Allocates vertical space inside a [RcScope.Column] proportionally to [weight]. */
public fun Modifier.verticalWeight(
    @FloatRange(from = 0.0, fromInclusive = false) weight: Float
): Modifier = then(WeightModifier(weight, vertical = true))

/** Fills [fraction] (`0f..1f`) of the maximum available width. */
public fun Modifier.fillMaxWidth(@FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f): Modifier =
    then(FillMaxWidthModifier(if (fraction == 1f) Float.NaN else fraction))

/** Fills [fraction] (`0f..1f`) of the maximum available height. */
public fun Modifier.fillMaxHeight(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): Modifier = then(FillMaxHeightModifier(if (fraction == 1f) Float.NaN else fraction))

/** Fills [fraction] (`0f..1f`) of the maximum available width and height. */
public fun Modifier.fillMaxSize(@FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f): Modifier =
    then(FillMaxSizeModifier(if (fraction == 1f) Float.NaN else fraction))

/** Basic wrapContentSize modifier. */
public fun Modifier.wrapContentSize(): Modifier = then(WrapContentSizeModifier)

/** Basic wrapContentWidth modifier. */
public fun Modifier.wrapContentWidth(): Modifier = then(WrapContentWidthModifier)

/** Basic wrapContentHeight modifier. */
public fun Modifier.wrapContentHeight(): Modifier = then(WrapContentHeightModifier)

/** Basic offset modifier. */
public fun Modifier.offset(x: Float = 0f, y: Float = 0f): Modifier = then(OffsetModifier(x, y))

public fun Modifier.offset(x: RcDp = 0.rdp, y: RcDp = 0.rdp): Modifier =
    then(OffsetModifier(x.value, y.value))

/** verticalScroll modifier. */
public fun Modifier.verticalScroll(position: Float = 0f): Modifier =
    then(VerticalScrollModifier(position))

public fun Modifier.verticalScroll(position: RcFloat): Modifier =
    then(VerticalScrollRcFloatModifier(position))

/** Clips the component content to [shape]. */
public fun Modifier.clip(shape: RcShape): Modifier = then(ClipModifier(shape))

/** widthIn modifier. */
public fun Modifier.widthIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(WidthInModifier(min, max))

public fun Modifier.widthIn(min: RcDp, max: RcDp): Modifier = widthIn(min.value, max.value)

/** heightIn modifier. */
public fun Modifier.heightIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(HeightInModifier(min, max))

public fun Modifier.heightIn(min: RcDp, max: RcDp): Modifier = heightIn(min.value, max.value)

/** Assigns a typed [RcComponentId] to this component. */
public fun Modifier.componentId(id: RcComponentId): Modifier = then(ComponentIdModifier(id.id))

/** onClick modifier with ActionScope. */
public fun Modifier.onClick(block: RcActionScope.() -> Unit): Modifier =
    then(ClickActionModifierElement(block, MultiClickModifier.CLICK_TYPE_SINGLE))

/** onLongClick modifier with ActionScope. */
public fun Modifier.onLongClick(block: RcActionScope.() -> Unit): Modifier =
    then(ClickActionModifierElement(block, MultiClickModifier.CLICK_TYPE_LONG))

/** onDoubleClick modifier with ActionScope. */
public fun Modifier.onDoubleClick(block: RcActionScope.() -> Unit): Modifier =
    then(ClickActionModifierElement(block, MultiClickModifier.CLICK_TYPE_DOUBLE))

/** onTouchDown modifier with ActionScope. */
public fun Modifier.onTouchDown(block: RcActionScope.() -> Unit): Modifier =
    then(TouchActionModifierElement(block, TouchActionModifier.DOWN))

/** onTouchUp modifier with ActionScope. */
public fun Modifier.onTouchUp(block: RcActionScope.() -> Unit): Modifier =
    then(TouchActionModifierElement(block, TouchActionModifier.UP))

/** onTouchCancel modifier with ActionScope. */
public fun Modifier.onTouchCancel(block: RcActionScope.() -> Unit): Modifier =
    then(TouchActionModifierElement(block, TouchActionModifier.CANCEL))

/**
 * onEvent modifier with ActionScope. Runs [block]'s actions when an event of [type] whose metadata
 * equals [filter] is dispatched to the component.
 *
 * @param type type identifying compatible events
 * @param filter filter metadata required to match the event
 * @param flags routing specific status flags returned on success
 * @param dataIds optional mapping of input payload indices to target float variables
 * @param condition optional RPN condition used to conditionally trigger actions
 * @param block the actions to execute
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Modifier.onEvent(
    type: Int,
    filter: Int,
    flags: Int = 0,
    dataIds: IntArray? = null,
    condition: FloatArray? = null,
    block: RcActionScope.() -> Unit,
): Modifier = then(EventHandlerModifierElement(type, filter, flags, dataIds, condition, block))

/** horizontalScroll modifier. */
public fun Modifier.horizontalScroll(): Modifier = then(HorizontalScrollModifier)

/** Fills [fraction] (`0f..1f`) of the parent's maximum available width. */
public fun Modifier.fillParentMaxWidth(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): Modifier = then(FillParentMaxWidthModifier(fraction))

/** Fills [fraction] (`0f..1f`) of the parent's maximum available height. */
public fun Modifier.fillParentMaxHeight(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): Modifier = then(FillParentMaxHeightModifier(fraction))

/** Fills [fraction] (`0f..1f`) of the parent's maximum available width and height. */
public fun Modifier.fillParentMaxSize(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): Modifier = then(FillParentMaxSizeModifier(fraction))

/** Draws a border with static [RcDp] dimensions and a static [RcColorValue]. */
public fun Modifier.border(
    width: RcDp,
    roundedCorner: RcDp,
    color: RcColorValue,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier = then(BorderModifier(width.value, roundedCorner.value, color.color, shape.value))

/** Draws a border with static [RcDp] dimensions and a 32-bit packed ARGB `Long` hex literal. */
public fun Modifier.border(
    width: RcDp,
    roundedCorner: RcDp,
    color: Long,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier = border(width, roundedCorner, RcColorValue(color), shape)

/** Draws a border with static [RcDp] dimensions and a dynamic [RcColor]. */
public fun Modifier.border(
    width: RcDp,
    roundedCorner: RcDp,
    color: RcColor,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier =
    then(DynamicBorderModifier(width.value, roundedCorner.value, color.id.toShort(), shape.value))

/** Draws a border with static `Float` dimensions and a static [RcColorValue]. */
public fun Modifier.border(
    width: Float,
    roundedCorner: Float,
    color: RcColorValue,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier = then(BorderModifier(width, roundedCorner, color.color, shape.value))

/** Draws a border with static `Float` dimensions and a 32-bit packed ARGB `Long` hex literal. */
public fun Modifier.border(
    width: Float,
    roundedCorner: Float,
    color: Long,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier = border(width, roundedCorner, RcColorValue(color), shape)

/** Draws a border with static `Float` dimensions and a dynamic [RcColor]. */
public fun Modifier.border(
    width: Float,
    roundedCorner: Float,
    color: RcColor,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier = then(DynamicBorderModifier(width, roundedCorner, color.id.toShort(), shape.value))

/** Draws a border with dynamic [RcFloat] dimensions and a static [RcColorValue]. */
public fun Modifier.border(
    width: RcFloat,
    roundedCorner: RcFloat,
    color: RcColorValue,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier =
    then(BorderModifier(width.toFloat(), roundedCorner.toFloat(), color.color, shape.value))

/** Draws a border with dynamic [RcFloat] dimensions and a 32-bit packed ARGB `Long` hex literal. */
public fun Modifier.border(
    width: RcFloat,
    roundedCorner: RcFloat,
    color: Long,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier = border(width, roundedCorner, RcColorValue(color), shape)

/** Draws a border with dynamic [RcFloat] dimensions and a dynamic [RcColor]. */
public fun Modifier.border(
    width: RcFloat,
    roundedCorner: RcFloat,
    color: RcColor,
    shape: RcBorderShape = RcBorderShape.Rectangle,
): Modifier =
    then(
        DynamicBorderModifier(
            width.toFloat(),
            roundedCorner.toFloat(),
            color.id.toShort(),
            shape.value,
        )
    )

/** Controls component visibility (`VISIBLE`, `INVISIBLE`, or `GONE`) from [visible]. */
public fun Modifier.visibility(visible: RcInteger): Modifier = then(VisibilityModifier(visible))

/** Configures layout motion and enter/exit visibility animations on this component. */
public fun Modifier.animationSpec(
    animationId: Int = -1,
    @FloatRange(from = 0.0) motionDuration: Float = 300f,
    motionEasing: RcAnimationCurve = RcAnimationCurve.CubicStandard,
    @FloatRange(from = 0.0) visibilityDuration: Float = 300f,
    visibilityEasing: RcAnimationCurve = RcAnimationCurve.CubicStandard,
    enterAnimation: RcEnterExitAnimation = RcEnterExitAnimation.FadeIn,
    exitAnimation: RcEnterExitAnimation = RcEnterExitAnimation.FadeOut,
    enterAnimationFunction: RcVisibilityAnimation = RcVisibilityAnimation.None,
    exitAnimationFunction: RcVisibilityAnimation = RcVisibilityAnimation.None,
    enterSequence: RcAnimationSequence = RcAnimationSequence.Concurrent,
    exitSequence: RcAnimationSequence = RcAnimationSequence.Concurrent,
): Modifier =
    then(
        AnimationSpecModifier(
            animationId,
            motionDuration,
            motionEasing.value,
            visibilityDuration,
            visibilityEasing.value,
            enterAnimation.toCore(),
            exitAnimation.toCore(),
            enterAnimationFunction.id,
            exitAnimationFunction.id,
            enterSequence.toCore(),
            exitSequence.toCore(),
        )
    )

/** alignByBaseline modifier. */
public fun Modifier.alignByBaseline(): Modifier = then(AlignByBaselineModifier)

/** requiredWidthIn modifier. */
public fun Modifier.requiredWidthIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(RequiredWidthInModifier(min, max))

/** requiredHeightIn modifier. */
public fun Modifier.requiredHeightIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(RequiredHeightInModifier(min, max))

/** Scrolls overflowing horizontal content using a marquee animation. */
public fun Modifier.marquee(
    @IntRange(from = 0) iterations: Int = Int.MAX_VALUE,
    animationMode: RcMarqueeAnimationMode = RcMarqueeAnimationMode.Immediately,
    @FloatRange(from = 0.0) repeatDelayMillis: Float = 0f,
    @FloatRange(from = 0.0) initialDelayMillis: Float = 0f,
    spacing: Float = 0f,
    velocity: Float = 20f,
): Modifier =
    then(
        MarqueeModifier(
            iterations,
            animationMode.value,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    )

/** zIndex modifier. */
public fun Modifier.zIndex(value: Float): Modifier = then(ZIndexModifier(value))

/** Builder scope for [Modifier.graphicsLayer]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcGraphicsLayerScope {
    public var scaleX: Float

    public fun scaleX(value: RcFloat)

    public var scaleY: Float

    public fun scaleY(value: RcFloat)

    public var alpha: Float

    public fun alpha(value: RcFloat)

    public var rotationZ: Float

    public fun rotationZ(value: RcFloat)

    public var translationX: Float

    public fun translationX(value: RcDp)

    public fun translationX(value: RcFloat)

    public var translationY: Float

    public fun translationY(value: RcDp)

    public fun translationY(value: RcFloat)
}

private class RcGraphicsLayerScopeImpl : RcGraphicsLayerScope {
    val attributes: LinkedHashMap<Int, Any> = LinkedHashMap()

    override var scaleX: Float
        get() = (attributes[0] as? Float) ?: 1f
        set(value) {
            attributes[0] = value
        }

    override fun scaleX(value: RcFloat) {
        attributes[0] = value
    }

    override var scaleY: Float
        get() = (attributes[1] as? Float) ?: 1f
        set(value) {
            attributes[1] = value
        }

    override fun scaleY(value: RcFloat) {
        attributes[1] = value
    }

    override var alpha: Float
        get() = (attributes[11] as? Float) ?: 1f
        set(value) {
            attributes[11] = value
        }

    override fun alpha(value: RcFloat) {
        attributes[11] = value
    }

    override var rotationZ: Float
        get() = (attributes[4] as? Float) ?: 0f
        set(value) {
            attributes[4] = value
        }

    override fun rotationZ(value: RcFloat) {
        attributes[4] = value
    }

    override var translationX: Float
        get() = (attributes[7] as? Float) ?: 0f
        set(value) {
            attributes[7] = value
        }

    override fun translationX(value: RcDp) {
        attributes[7] = value.value
    }

    override fun translationX(value: RcFloat) {
        attributes[7] = value
    }

    override var translationY: Float
        get() = (attributes[8] as? Float) ?: 0f
        set(value) {
            attributes[8] = value
        }

    override fun translationY(value: RcDp) {
        attributes[8] = value.value
    }

    override fun translationY(value: RcFloat) {
        attributes[8] = value
    }
}

/** graphicsLayer modifier. */
internal fun Modifier.graphicsLayer(attributes: Map<Int, Any>): Modifier =
    then(GraphicsLayerModifier(attributes))

/** graphicsLayer modifier with type-safe builder lambda. */
public fun Modifier.graphicsLayer(block: RcGraphicsLayerScope.() -> Unit): Modifier =
    graphicsLayer(RcGraphicsLayerScopeImpl().apply(block).attributes)

/** Ripple modifier for standard material design touch feedback. */
public fun Modifier.ripple(): Modifier = then(RippleModifierElement)

/**
 * Allows injecting custom canvas drawing operations before or after the element's children. The
 * block receiver is [RcCanvasScope], providing drawing functions ([RcCanvasScope.drawCircle],
 * [RcCanvasScope.drawRect], etc.) and a way to explicitly render the layout children via
 * [RcScope.drawComponentContent].
 */
public fun Modifier.drawWithContent(block: RcCanvasScope.() -> Unit): Modifier =
    then(DrawWithContentModifierElement(block))

/** Sets the horizontal priority of dynamic layout collapsing (e.g. inside a CollapsibleRow). */
public fun Modifier.horizontalCollapsiblePriority(priority: Float): Modifier =
    then(CollapsiblePriorityModifierElement(orientation = 0 /* HORIZONTAL */, priority = priority))

/** Sets the vertical priority of dynamic layout collapsing (e.g. inside a CollapsibleColumn). */
public fun Modifier.verticalCollapsiblePriority(priority: Float): Modifier =
    then(CollapsiblePriorityModifierElement(orientation = 1 /* VERTICAL */, priority = priority))

/**
 * Configures accessibility semantics for screen readers and other assistive technologies. Provides
 * access to [RcSemanticsScope] to declare descriptions, roles, state, and capabilities.
 */
public fun Modifier.semantics(block: RcSemanticsScope.() -> Unit): Modifier =
    then(SemanticsModifierElement(block))

/**
 * Allows defining dynamic custom measurement calculations on layout bounds (e.g., forcing aspect
 * ratios). Provides access to the layout constraints scope [RcLayoutScope].
 */
public fun Modifier.computeMeasure(block: RcLayoutScope.() -> Unit): Modifier =
    then(ComponentLayoutComputeModifierElement(0 /* TYPE_MEASURE */, block))

/**
 * Allows defining dynamic custom positioning calculations on layout bounds (e.g., centering items).
 * Provides access to the layout constraints scope [RcLayoutScope].
 */
public fun Modifier.computePosition(block: RcLayoutScope.() -> Unit): Modifier =
    then(ComponentLayoutComputeModifierElement(1 /* TYPE_POSITION */, block))

internal data class PaddingModifier(
    val start: Float,
    val top: Float,
    val end: Float,
    val bottom: Float,
) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.padding(start, top, end, bottom)
    }
}

internal data class SizeModifier(val width: Float, val height: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.width(width).height(height)
    }
}

internal data class WidthModifier(val width: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.width(width)
    }
}

internal data class HeightModifier(val height: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.height(height)
    }
}

internal data class WidthRcFloatModifier(val width: RcFloat) :
    RecordingModifierElement, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addWidthModifierOperation(0 /* EXACT */, width.withWriter(writer).id)
    }
}

internal data class HeightRcFloatModifier(val height: RcFloat) :
    RecordingModifierElement, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addHeightModifierOperation(0 /* EXACT */, height.withWriter(writer).id)
    }
}

internal data class BackgroundModifier(val color: Int) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.background(color)
    }
}

internal data class BackgroundColorIdModifier(val color: RcColor) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.backgroundId(color.id)
    }
}

internal data class BackgroundColorModifier(val color: RcColorValue) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.background(color.color)
    }
}

internal data class WeightModifier(val weight: Float, val vertical: Boolean) :
    RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        if (vertical) {
            modifier.verticalWeight(weight)
        } else {
            modifier.horizontalWeight(weight)
        }
    }
}

internal data class FillMaxWidthModifier(val fraction: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillMaxWidth(fraction)
    }
}

internal data class FillMaxHeightModifier(val fraction: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillMaxHeight(fraction)
    }
}

internal data class FillMaxSizeModifier(val fraction: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillMaxSize(fraction)
    }
}

internal data object WrapContentSizeModifier : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.wrapContentSize()
    }
}

internal data object WrapContentWidthModifier : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.wrapContentWidth()
    }
}

internal data object WrapContentHeightModifier : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.wrapContentHeight()
    }
}

internal data class OffsetModifier(val x: Float, val y: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.offset(x, y)
    }
}

internal data class VerticalScrollModifier(val position: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.verticalScroll(position)
    }
}

internal data class VerticalScrollRcFloatModifier(val position: RcFloat) :
    RecordingModifierElement, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addClipRectModifier()
        writer.addModifierScroll(0 /* VERTICAL */, position.withWriter(writer).id)
    }
}

internal data class ClipModifier(val shape: RcShape) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.clip(shape.legacyShape)
    }
}

internal data class WidthInModifier(val min: Float, val max: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.widthIn(min, max)
    }
}

internal data class HeightInModifier(val min: Float, val max: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.heightIn(min, max)
    }
}

internal data class ComponentIdModifier(val id: Int) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.componentId(id)
    }
}

internal class ClickActionModifierElement(val block: RcActionScope.() -> Unit, val clickType: Int) :
    RecordingModifierElement, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val scope = RcActionScopeImpl()
        scope.block()
        val actions = scope.build(writer)
        val legacyModifier = ClickActionModifier(actions, clickType)
        legacyModifier.write(writer)
    }
}

internal class TouchActionModifierElement(val block: RcActionScope.() -> Unit, val touchType: Int) :
    RecordingModifierElement, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val scope = RcActionScopeImpl()
        scope.block()
        val actions = scope.build(writer)
        val legacyModifier = TouchActionModifier(touchType, actions)
        legacyModifier.write(writer)
    }
}

internal class EventHandlerModifierElement(
    val type: Int,
    val filter: Int,
    val flags: Int,
    val dataIds: IntArray?,
    val condition: FloatArray?,
    val block: RcActionScope.() -> Unit,
) : RecordingModifierElement, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val scope = RcActionScopeImpl()
        scope.block()
        val actions = scope.build(writer)
        EventHandlerModifier(type, filter, flags, dataIds, condition, actions).write(writer)
    }
}

internal data object HorizontalScrollModifier : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.horizontalScroll()
    }
}

internal data class FillParentMaxWidthModifier(val fraction: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillParentMaxWidth(fraction)
    }
}

internal data class FillParentMaxHeightModifier(val fraction: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillParentMaxHeight(fraction)
    }
}

internal data class FillParentMaxSizeModifier(val fraction: Float) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillParentMaxSize(fraction)
    }
}

internal data class BorderModifier(
    val width: Float,
    val roundedCorner: Float,
    val color: Int,
    val shape: Int,
) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.border(width, roundedCorner, color, shape)
    }
}

internal data class DynamicBorderModifier(
    val width: Float,
    val roundedCorner: Float,
    val color: Short,
    val shape: Int,
) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.dynamicBorder(width, roundedCorner, color, shape)
    }
}

internal data class VisibilityModifier(val visible: RcInteger) : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.visibility(visible.id.toInt())
    }
}

internal data class AnimationSpecModifier(
    val animationId: Int = -1,
    val motionDuration: Float = 300f,
    val motionEasingType: Int =
        androidx.compose.remote.core.operations.utilities.easing.GeneralEasing.CUBIC_STANDARD,
    val visibilityDuration: Float = 300f,
    val visibilityEasingType: Int =
        androidx.compose.remote.core.operations.utilities.easing.GeneralEasing.CUBIC_STANDARD,
    val enterAnimation: AnimationSpec.ANIMATION = AnimationSpec.ANIMATION.FADE_IN,
    val exitAnimation: AnimationSpec.ANIMATION = AnimationSpec.ANIMATION.FADE_OUT,
    val enterFunctionId: Int = -1,
    val exitFunctionId: Int = -1,
    val enterSequence: AnimationSpec.SEQUENCE = AnimationSpec.SEQUENCE.CONCURRENT,
    val exitSequence: AnimationSpec.SEQUENCE = AnimationSpec.SEQUENCE.CONCURRENT,
) : RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.animationSpec(
            animationId,
            motionDuration,
            motionEasingType,
            visibilityDuration,
            visibilityEasingType,
            enterAnimation,
            exitAnimation,
            enterFunctionId,
            exitFunctionId,
            enterSequence,
            exitSequence,
        )
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addAnimationSpecModifier(
            animationId,
            motionDuration,
            motionEasingType,
            visibilityDuration,
            visibilityEasingType,
            AnimationSpec.packAnimation(enterAnimation, enterSequence),
            AnimationSpec.packAnimation(exitAnimation, exitSequence),
            enterFunctionId,
            exitFunctionId,
        )
    }
}

internal data object AlignByBaselineModifier : RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.alignByBaseline()
    }
}

internal data class RequiredWidthInModifier(val min: Float, val max: Float) :
    RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.requiredWidthIn(min, max)
    }
}

internal data class RequiredHeightInModifier(val min: Float, val max: Float) :
    RecordingModifierElement {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.requiredHeightIn(min, max)
    }
}

internal data class MarqueeModifier(
    val iterations: Int,
    val animationMode: Int,
    val repeatDelayMillis: Float,
    val initialDelayMillis: Float,
    val spacing: Float,
    val velocity: Float,
) : RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addModifierMarquee(
            iterations,
            animationMode,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    }
}

internal data class ZIndexModifier(val value: Float) :
    RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addModifierZIndex(value)
    }
}

internal data class GraphicsLayerModifier(val attributes: Map<Int, Any>) :
    RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val hashMap = HashMap<Int, Any>(attributes.size)
        for ((key, value) in attributes) {
            hashMap[key] = if (value is RcFloat) value.withWriter(writer).toFloat() else value
        }
        writer.addModifierGraphicsLayer(hashMap)
    }
}

internal data object RippleModifierElement :
    RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addModifierRipple()
    }
}

internal class DrawWithContentModifierElement(val block: RcCanvasScope.() -> Unit) :
    RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val scope = RcCanvasScopeImpl(writer)
        writer.startCanvasOperations()
        scope.block()
        writer.endCanvasOperations()
    }
}

internal data class CollapsiblePriorityModifierElement(val orientation: Int, val priority: Float) :
    RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addCollapsiblePriorityModifier(orientation, priority)
    }
}

internal class SemanticsModifierElement(val block: RcSemanticsScope.() -> Unit) :
    RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val scope = RcSemanticsScopeImpl(writer)
        scope.block()
        val coreSemantics = scope.build()
        val legacyModifier =
            androidx.compose.remote.creation.modifiers.SemanticsModifier(coreSemantics)
        legacyModifier.write(writer)
    }
}

internal class ComponentLayoutComputeModifierElement(
    val type: Int,
    val block: RcLayoutScope.() -> Unit,
) : RecordingModifierElement, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addLayoutCompute(type) { changes ->
            val scope = RcLayoutScopeImpl(changes, writer)
            scope.block()
        }
    }
}
