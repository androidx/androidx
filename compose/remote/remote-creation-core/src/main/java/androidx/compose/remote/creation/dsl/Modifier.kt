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

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.ClickActionModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.Shape
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
        /** Applies this modifier element to the legacy [RecordingModifier]. */
        public fun applyTo(modifier: RecordingModifier)

        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
    }

    /** The companion object serves as the empty [Modifier]. */
    public companion object : Modifier {
        override fun then(other: Modifier): Modifier = other

        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
    }
}

/** Internal implementation of a combined modifier chain. */
private class CombinedModifier(private val outer: Modifier, private val inner: Modifier) :
    Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

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

public fun Modifier.width(width: RcDp): Modifier = width(width.value)

/** Basic height modifier. */
public fun Modifier.height(height: Float): Modifier = then(HeightModifier(height))

public fun Modifier.height(height: RcDp): Modifier = height(height.value)

/** Basic background modifier. */
public fun Modifier.background(color: Int): Modifier = then(BackgroundModifier(color))

public fun Modifier.background(color: Long): Modifier = background(color.toInt())

public fun Modifier.background(color: RcColorValue): Modifier = then(BackgroundColorModifier(color))

public fun Modifier.background(color: RcColor): Modifier = then(BackgroundColorIdModifier(color))

/** Basic fillMaxWidth modifier. */
public fun Modifier.fillMaxWidth(fraction: Float = Float.NaN): Modifier =
    then(FillMaxWidthModifier(fraction))

/** Basic fillMaxHeight modifier. */
public fun Modifier.fillMaxHeight(fraction: Float = Float.NaN): Modifier =
    then(FillMaxHeightModifier(fraction))

/** Basic fillMaxSize modifier. */
public fun Modifier.fillMaxSize(fraction: Float = Float.NaN): Modifier =
    then(FillMaxSizeModifier(fraction))

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

/** clip modifier. */
public fun Modifier.clip(shape: Shape): Modifier = then(ClipModifier(shape))

/** widthIn modifier. */
public fun Modifier.widthIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(WidthInModifier(min, max))

public fun Modifier.widthIn(min: RcDp, max: RcDp): Modifier = widthIn(min.value, max.value)

/** heightIn modifier. */
public fun Modifier.heightIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(HeightInModifier(min, max))

public fun Modifier.heightIn(min: RcDp, max: RcDp): Modifier = heightIn(min.value, max.value)

/** componentId modifier. */
public fun Modifier.componentId(id: Int): Modifier = then(ComponentIdModifier(id))

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

/** horizontalScroll modifier. */
public fun Modifier.horizontalScroll(): Modifier = then(HorizontalScrollModifier)

/** fillParentMaxWidth modifier. */
public fun Modifier.fillParentMaxWidth(fraction: Float = 1f): Modifier =
    then(FillParentMaxWidthModifier(fraction))

/** fillParentMaxHeight modifier. */
public fun Modifier.fillParentMaxHeight(fraction: Float = 1f): Modifier =
    then(FillParentMaxHeightModifier(fraction))

/** fillParentMaxSize modifier. */
public fun Modifier.fillParentMaxSize(fraction: Float = 1f): Modifier =
    then(FillParentMaxSizeModifier(fraction))

/** border modifier. */
public fun Modifier.border(width: Float, roundedCorner: Float, color: Int, shape: Int): Modifier =
    then(BorderModifier(width, roundedCorner, color, shape))

/** dynamicBorder modifier. */
public fun Modifier.dynamicBorder(
    width: Float,
    roundedCorner: Float,
    color: Short,
    shape: Int,
): Modifier = then(DynamicBorderModifier(width, roundedCorner, color, shape))

/** visibility modifier. */
public fun Modifier.visibility(visible: RcInteger): Modifier = then(VisibilityModifier(visible))

/** animationSpec modifier. */
public fun Modifier.animationSpec(animationId: Int): Modifier =
    then(AnimationSpecModifier(animationId))

/** alignByBaseline modifier. */
public fun Modifier.alignByBaseline(): Modifier = then(AlignByBaselineModifier)

/** requiredWidthIn modifier. */
public fun Modifier.requiredWidthIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(RequiredWidthInModifier(min, max))

/** requiredHeightIn modifier. */
public fun Modifier.requiredHeightIn(min: Float = 0f, max: Float = Float.MAX_VALUE): Modifier =
    then(RequiredHeightInModifier(min, max))

/** marquee modifier. */
public fun Modifier.marquee(
    iterations: Int,
    animationMode: Int,
    repeatDelayMillis: Float,
    initialDelayMillis: Float,
    spacing: Float,
    velocity: Float,
): Modifier =
    then(
        MarqueeModifier(
            iterations,
            animationMode,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    )

/** zIndex modifier. */
public fun Modifier.zIndex(value: Float): Modifier = then(ZIndexModifier(value))

/** graphicsLayer modifier. */
public fun Modifier.graphicsLayer(attributes: Map<Int, Any>): Modifier =
    then(GraphicsLayerModifier(attributes))

// =====================================================================================
//
// Each delegates to the matching raw-Int border / componentId modifier above. The
// raw-Int variants stay for backward compatibility; new code should prefer these.
// @TODO remove non RcBorderShape
// =====================================================================================

/** Border with a typed [RcBorderShape]. */
public fun Modifier.border(
    width: Float,
    roundedCorner: Float,
    color: RcColorValue,
    shape: RcBorderShape,
): Modifier = border(width, roundedCorner, color.id, shape.value)

/** Dynamic border with a typed [RcBorderShape]. */
public fun Modifier.border(
    width: Float,
    roundedCorner: Float,
    color: RcColor,
    shape: RcBorderShape,
): Modifier = dynamicBorder(width, roundedCorner, color.id.toShort(), shape.value)

/** Border with a typed [RcBorderShape]. */
public fun Modifier.border(
    width: RcFloat,
    roundedCorner: RcFloat,
    color: RcColorValue,
    shape: RcBorderShape,
): Modifier = border(width.id, roundedCorner.id, color.id, shape.value)

/** Dynamic border with a typed [RcBorderShape]. */
public fun Modifier.border(
    width: RcFloat,
    roundedCorner: RcFloat,
    color: RcColor,
    shape: RcBorderShape,
): Modifier = dynamicBorder(width.id, roundedCorner.id, color.id.toShort(), shape.value)

/** Component ID using a typed [RcComponentId]. */
public fun Modifier.componentId(id: RcComponentId): Modifier = componentId(id.id)

internal class PaddingModifier(
    val start: Float,
    val top: Float,
    val end: Float,
    val bottom: Float,
) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.padding(start, top, end, bottom)
    }
}

internal class SizeModifier(val width: Float, val height: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.width(width).height(height)
    }
}

internal class WidthModifier(val width: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.width(width)
    }
}

internal class HeightModifier(val height: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.height(height)
    }
}

internal class BackgroundModifier(val color: Int) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.background(color)
    }
}

internal class BackgroundColorIdModifier(val color: RcColor) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.backgroundId(color.id)
    }
}

internal class BackgroundColorModifier(val color: RcColorValue) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.background(color.id)
    }
}

internal class WeightModifier(val weight: Float, val vertical: Boolean) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        if (vertical) {
            modifier.verticalWeight(weight)
        } else {
            modifier.horizontalWeight(weight)
        }
    }
}

internal class FillMaxWidthModifier(val fraction: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillMaxWidth(fraction)
    }
}

internal class FillMaxHeightModifier(val fraction: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillMaxHeight(fraction)
    }
}

internal class FillMaxSizeModifier(val fraction: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillMaxSize(fraction)
    }
}

internal object WrapContentSizeModifier : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.wrapContentSize()
    }
}

internal object WrapContentWidthModifier : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.wrapContentWidth()
    }
}

internal object WrapContentHeightModifier : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.wrapContentHeight()
    }
}

internal class OffsetModifier(val x: Float, val y: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.offset(x, y)
    }
}

internal class VerticalScrollModifier(val position: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.verticalScroll(position)
    }
}

internal class VerticalScrollRcFloatModifier(val position: RcFloat) :
    Modifier.Element, RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addModifierScroll(0 /* VERTICAL */, position.withWriter(writer).toFloat())
    }
}

internal class ClipModifier(val shape: Shape) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.clip(shape)
    }
}

internal class WidthInModifier(val min: Float, val max: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.widthIn(min, max)
    }
}

internal class HeightInModifier(val min: Float, val max: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.heightIn(min, max)
    }
}

internal class ComponentIdModifier(val id: Int) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.componentId(id)
    }
}

internal class ClickActionModifierElement(val block: RcActionScope.() -> Unit, val clickType: Int) :
    Modifier.Element, RecordingModifier.Element {
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
    Modifier.Element, RecordingModifier.Element {
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

internal object HorizontalScrollModifier : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.horizontalScroll()
    }
}

internal class FillParentMaxWidthModifier(val fraction: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillParentMaxWidth(fraction)
    }
}

internal class FillParentMaxHeightModifier(val fraction: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillParentMaxHeight(fraction)
    }
}

internal class FillParentMaxSizeModifier(val fraction: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.fillParentMaxSize(fraction)
    }
}

internal class BorderModifier(
    val width: Float,
    val roundedCorner: Float,
    val color: Int,
    val shape: Int,
) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.border(width, roundedCorner, color, shape)
    }
}

internal class DynamicBorderModifier(
    val width: Float,
    val roundedCorner: Float,
    val color: Short,
    val shape: Int,
) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.dynamicBorder(width, roundedCorner, color, shape)
    }
}

internal class VisibilityModifier(val visible: RcInteger) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.visibility(visible.id.toInt())
    }
}

internal class AnimationSpecModifier(val animationId: Int) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.animationSpec(animationId)
    }
}

internal object AlignByBaselineModifier : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.alignByBaseline()
    }
}

internal class RequiredWidthInModifier(val min: Float, val max: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.requiredWidthIn(min, max)
    }
}

internal class RequiredHeightInModifier(val min: Float, val max: Float) : Modifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.requiredHeightIn(min, max)
    }
}

internal class MarqueeModifier(
    val iterations: Int,
    val animationMode: Int,
    val repeatDelayMillis: Float,
    val initialDelayMillis: Float,
    val spacing: Float,
    val velocity: Float,
) : Modifier.Element, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
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

internal class ZIndexModifier(val value: Float) :
    Modifier.Element, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        writer.addModifierZIndex(value)
    }
}

internal class GraphicsLayerModifier(val attributes: Map<Int, Any>) :
    Modifier.Element, androidx.compose.remote.creation.modifiers.RecordingModifier.Element {
    override fun applyTo(modifier: RecordingModifier) {
        modifier.then(this)
    }

    override fun write(writer: RemoteComposeWriter) {
        val hashMap = HashMap<Int, Any>().apply { putAll(attributes) }
        @Suppress("UNCHECKED_CAST") writer.addModifierGraphicsLayer(hashMap as HashMap<Int, Any>)
    }
}
