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

package androidx.compose.ui.draw

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlockGraphicsLayerModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Creates a [graphicsLayer] that draws a shadow. The [elevation] defines the visual depth of the
 * physical object. The physical object has a shape specified by [shape].
 *
 * If the passed [shape] is concave the shadow will not be drawn on Android versions less than 10.
 *
 * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order. Use
 * a [androidx.compose.ui.zIndex] modifier if you want to draw the elements with larger [elevation]
 * after all the elements with a smaller one.
 *
 * Usage of this API renders this composable into a separate graphics layer
 *
 * @param elevation The elevation for the shadow in pixels
 * @param shape Defines a shape of the physical object
 * @param clip When active, the content drawing clips to the shape.
 * @sample androidx.compose.ui.samples.ShadowSample
 * @see graphicsLayer
 *
 * Example usage:
 */
@Deprecated(
    "Replace with shadow which accepts ambientColor and spotColor parameters",
    ReplaceWith(
        "Modifier.shadow(elevation, shape, clip, DefaultShadowColor, DefaultShadowColor)",
        "androidx.compose.ui.draw",
    ),
    DeprecationLevel.HIDDEN,
)
@Stable
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp,
) = shadow(elevation, shape, clip, DefaultShadowColor, DefaultShadowColor)

/**
 * Creates a [graphicsLayer] that draws a shadow. The [elevation] defines the visual depth of the
 * physical object. The physical object has a shape specified by [shape].
 *
 * If the passed [shape] is concave the shadow will not be drawn on Android versions less than 10.
 *
 * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order. Use
 * a [androidx.compose.ui.zIndex] modifier if you want to draw the elements with larger [elevation]
 * after all the elements with a smaller one.
 *
 * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions, this
 * property always returns [Color.Black] and setting new values is ignored.
 *
 * Usage of this API renders this composable into a separate graphics layer
 *
 * @param elevation The elevation for the shadow in pixels
 * @param shape Defines a shape of the physical object
 * @param clip When active, the content drawing clips to the shape.
 * @param ambientColor Color of the ambient shadow drawn when [elevation] > 0f
 * @param spotColor Color of the spot shadow that is drawn when [elevation] > 0f
 * @sample androidx.compose.ui.samples.ShadowSample
 * @see graphicsLayer
 *
 * Example usage:
 */
@Stable
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp,
    ambientColor: Color = DefaultShadowColor,
    spotColor: Color = DefaultShadowColor,
) =
    if (elevation > 0.dp || clip) {
        this then ShadowGraphicsLayerElement(elevation, shape, clip, ambientColor, spotColor)
    } else {
        this
    }

/**
 * Draws a drop shadow behind the rest of the content with the geometry specified by the given shape
 * and the shadow properties defined by the [Shadow]. This is different than [Modifier.shadow] as
 * this does not introduce a graphicsLayer to render elevation based shadows. This shadow is
 * rendered without a single light source and will render consistently regardless of the on screen
 * position of the content.
 *
 * @param shape Geometry of the shadow
 * @param shadow Properties of the shadow like radius, spread, offset, and fill properties like the
 *   color or brush
 * @sample androidx.compose.ui.samples.DropShadowSample
 */
@Stable
fun Modifier.dropShadow(shape: Shape, shadow: Shadow): Modifier =
    this then SimpleDropShadowElement(shape, shadow)

/**
 * Draws a drop shadow behind the rest of the content with the geometry specified by the given shape
 * and the shadow properties defined the [DropShadowScope]. This is different than [Modifier.shadow]
 * as this does not introduce a graphicsLayer to render elevation based shadows. This shadow is
 * rendered without a single light source and will render consistently regardless of the on screen
 * position of the content. This is similar to [Modifier.dropShadow] except that specification of
 * drop shadow parameters is done with the lambda with [DropShadowScope] allows for more efficient
 * transformations for animated use cases without recomposition.
 *
 * @param shape Geometry of the shadow
 * @param block [DropShadowScope] block where shadow properties are defined
 * @sample androidx.compose.ui.samples.DropShadowSample
 */
@Stable
fun Modifier.dropShadow(shape: Shape, block: DropShadowScope.() -> Unit) =
    this then BlockDropShadowElement(shape, block)

/**
 * Draws an inner shadow on top of the rest of the content with the geometry specified by the given
 * shape and the shadow properties defined by the [Shadow]. This is different than [Modifier.shadow]
 * as this does not introduce a graphicsLayer to render elevation based shadows. Additionally this
 * shadow will render only within the geometry and can be used to provide a recessed like visual
 * effect. This shadow is rendered without a single light source and will render consistently
 * regardless of the on screen position of the content.
 *
 * @param shape Geometry of the shadow
 * @param shadow Properties of the shadow like radius, spread, offset, and fill properties like the
 *   color or brush
 * @sample androidx.compose.ui.samples.InnerShadowSample
 */
@Stable
fun Modifier.innerShadow(shape: Shape, shadow: Shadow): Modifier =
    this then SimpleInnerShadowElement(shape, shadow)

/**
 * Draws an inner shadow behind the rest of the content with the geometry specified by the given
 * shape and the shadow properties defined the [InnerShadowScope]. This is different than
 * [Modifier.shadow] as this does not introduce a graphicsLayer to render elevation based shadows.
 * This shadow is rendered without a single light source and will render consistently regardless of
 * the on screen position of the content. This is similar to [Modifier.innerShadow] except that
 * specification of inner shadow parameters is done with the lambda with [InnerShadowScope] allows
 * for more efficient transformations for animated use cases without recomposition.
 *
 * @param shape Geometry of the shadow
 * @param block [InnerShadowScope] block where shadow properties are defined
 * @sample androidx.compose.ui.samples.InnerShadowSample
 */
@Stable
fun Modifier.innerShadow(shape: Shape, block: InnerShadowScope.() -> Unit): Modifier =
    this then BlockInnerShadowElement(shape, block)

// Note because we are merging the offset properties into the scoped interface for configuration
// the DropShadow parameters for optimal animations to that avoid recomposition, the exposed
// receiver scope for InnerShadow and DropShadow are more or less identical.
// InnerShadow exposes offset parameters as changes to the offset itself require regeneration of the
// shadow, however, an offset on a DropShadow just translates the shadow content itself.
// Expose a base [ShadowScope] interface with different interfaces for Drop/Inner shadows for
// to support future changes of parameters that are supported in one or the other but not both

/**
 * Scope that provides the capability to configure the properties of a drop shadow in order to
 * support efficient transformations without recomposition
 */
@JvmDefaultWithCompatibility interface DropShadowScope : ShadowScope

/**
 * Scope that provides the capability to configure the properties of an inner shadow in order to
 * support efficient transformations without recomposition
 */
@JvmDefaultWithCompatibility interface InnerShadowScope : ShadowScope

/**
 * Scope that can be used to define properties to render either a drop shadow or inner shadow. This
 * includes the [radius], [spread], [color], [brush], [alpha], [blendMode], and [offset] parameters.
 */
@JvmDefaultWithCompatibility
interface ShadowScope : Density {

    /** Blur radius of the shadow, in pixels. Defaults to 0. */
    var radius: Float

    /** Spread parameter that adds to the size of the shadow, in pixels. Defaults to 0. */
    var spread: Float

    /**
     * Color of the shadow, Defaults to [Color.Black]. Attempts to provide Color.Unspecified will
     * fallback to rendering with [Color.Black]. This parameter is consumed if [brush] is null.
     */
    var color: Color

    /** The brush to use for the shadow. If null, the color parameter is consumed instead */
    var brush: Brush?

    /** Opacity of the shadow. Defaults to 1f indicating a fully opaque shadow */
    var alpha: Float

    /** Blending algorithm used by the shadow. Defaults to [BlendMode.SrcOver] */
    var blendMode: BlendMode

    /** Offset of the shadow. Defaults to [Offset.Zero]. */
    var offset: Offset
}

internal class BlockDropShadowElement(val shape: Shape, val block: DropShadowScope.() -> Unit) :
    ModifierNodeElement<BlockDropShadowNode>() {

    override fun create(): BlockDropShadowNode = BlockDropShadowNode(shape, block)

    override fun update(node: BlockDropShadowNode) {
        node.update(shape, block)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dropShadow"
        properties["shape"] = shape
        properties["block"] = block
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlockDropShadowElement) return false

        if (shape != other.shape) return false
        if (block !== other.block) return false

        return true
    }

    override fun hashCode() = 31 * shape.hashCode() + block.hashCode()
}

internal data class SimpleDropShadowElement(val shape: Shape, val shadow: Shadow) :
    ModifierNodeElement<SimpleDropShadowNode>() {

    override fun create(): SimpleDropShadowNode = SimpleDropShadowNode(shape, shadow)

    override fun update(node: SimpleDropShadowNode) {
        node.update(shape, shadow)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dropShadow"
        properties["shape"] = shape
        properties["dropShadow"] = shadow
    }
}

/**
 * DropShadow ModifierNode implementation that leverages an immutable DropShadow and offset. This is
 * useful for use cases where the shadow itself is not animated
 */
internal class SimpleDropShadowNode(private var shape: Shape, private var shadow: Shadow) :
    DrawModifierNode, Modifier.Node(), ObserverModifierNode {

    private var shadowPainter: DropShadowPainter? = null

    fun update(shape: Shape, shadow: Shadow) {
        val painterInvalidated = this.shape != shape || this.shadow != shadow
        if (painterInvalidated) {
            shadowPainter = null
        }
        this.shape = shape
        this.shadow = shadow
    }

    private fun obtainPainter(): DropShadowPainter =
        shadowPainter
            ?: requireGraphicsContext().shadowContext.createDropShadowPainter(shape, shadow).also {
                shadowPainter = it
            }

    override fun ContentDrawScope.draw() {
        with(obtainPainter()) { draw(size) }
        drawContent()
    }

    override fun onObservedReadsChanged() {
        shadowPainter = null
        invalidateDraw()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SimpleDropShadowNode) return false

        if (shape != other.shape) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

internal data class SimpleInnerShadowElement(val shape: Shape, val shadow: Shadow) :
    ModifierNodeElement<SimpleInnerShadowNode>() {

    override fun create(): SimpleInnerShadowNode = SimpleInnerShadowNode(shape, shadow)

    override fun update(node: SimpleInnerShadowNode) {
        node.update(shape, shadow)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shape"] = shape
        properties["innerShadow"] = shadow
    }
}

/**
 * InnerShadow ModifierNode implementation that leverages an immutable InnerShadow and offset. This
 * is useful for use cases where the shadow itself is not animated
 */
internal class SimpleInnerShadowNode(private var shape: Shape, private var shadow: Shadow) :
    DrawModifierNode, Modifier.Node(), ObserverModifierNode {

    private var innerShadowPainter: InnerShadowPainter? = null

    fun update(shape: Shape, shadow: Shadow) {
        val painterInvalidated = this.shape != shape || this.shadow != shadow
        if (painterInvalidated) {
            innerShadowPainter = null
        }
        this.shape = shape
        this.shadow = shadow
    }

    private fun obtainPainter(): InnerShadowPainter =
        innerShadowPainter
            ?: requireGraphicsContext().shadowContext.createInnerShadowPainter(shape, shadow).also {
                innerShadowPainter = it
            }

    override fun ContentDrawScope.draw() {
        with(obtainPainter()) { draw(size) }
        drawContent()
    }

    override fun onObservedReadsChanged() {
        innerShadowPainter = null
        invalidateDraw()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SimpleInnerShadowNode

        if (shape != other.shape) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

/** Resets all shadow properties to their default values */
private fun ShadowScope.resetShadow() {
    this.radius = 0f
    this.spread = 0f
    this.offset = Offset.Zero
    this.color = Color.Black
    this.brush = null
    this.alpha = 1f
    this.blendMode = BlendMode.SrcOver
}

internal class BlockInnerShadowElement(val shape: Shape, val block: InnerShadowScope.() -> Unit) :
    ModifierNodeElement<BlockInnerShadowNode>() {

    override fun create(): BlockInnerShadowNode = BlockInnerShadowNode(shape, block)

    override fun update(node: BlockInnerShadowNode) {
        node.update(shape, block)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shape"] = shape
        properties["block"] = block
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlockInnerShadowElement) return false

        if (shape != other.shape) return false
        if (block !== other.block) return false

        return true
    }

    override fun hashCode() = 31 * shape.hashCode() + block.hashCode()
}

/**
 * InnerShadow ModifierNode implementation that leverages a receiver scope for mutable inner shadow
 * properties. This is useful for use cases where the shadow itself is animated and minimizing
 * recompositions is desired.
 */
internal class BlockInnerShadowNode(private var shape: Shape, block: InnerShadowScope.() -> Unit) :
    DrawModifierNode, Modifier.Node(), ObserverModifierNode, InnerShadowScope {

    private var densityObject: Density? = null
    private var targetShadow: Shadow? = null
    private var shadowPainter: InnerShadowPainter? = null
    private var blockRead = false

    private var block: InnerShadowScope.() -> Unit = block
        set(value) {
            if (field !== value) {
                field = value
                blockRead = false
                invalidateDraw()
            }
        }

    override val density: Float
        get() = densityObject?.density ?: 1f

    override val fontScale: Float
        get() = densityObject?.fontScale ?: 1f

    override var radius: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var spread: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var offset: Offset = Offset.Zero
        set(value) {
            if (field != value) {
                field = value
                // Unlike the DropShadow ModifierNode implementation, changes to the offset of an
                // inner shadow require the shadow to be recreated
                invalidateShadow()
            }
        }

    override var color: Color = Color.Black
        set(value) {
            val target =
                if (value.isSpecified) {
                    value
                } else {
                    Color.Black
                }
            if (field != target) {
                field = target
                invalidateShadow()
            }
        }

    override var brush: Brush? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var alpha: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override fun onAttach() {
        super.onAttach()
        updateDensity()
    }

    override fun onDensityChange() {
        if (isAttached) updateDensity()
    }

    private fun updateDensity() {
        val newDensity = requireDensity()
        if (densityObject != newDensity) {
            densityObject = newDensity
            blockRead = false
            invalidateShadow()
        }
    }

    fun update(shape: Shape, block: InnerShadowScope.() -> Unit) {
        this.shape = shape
        this.block = block
    }

    override fun ContentDrawScope.draw() {
        with(obtainPainter()) { draw(size) }
        drawContent()
    }

    private fun obtainPainter(): InnerShadowPainter {
        if (!blockRead) {
            blockRead = true
            resetShadow()
            observeReads { block(this) }
        }
        var shadow = targetShadow
        var painter = shadowPainter
        val tmpBrush = brush
        val radiusDp = radius.toDp()
        val spreadDp = spread.toDp()
        val dpOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
        if (
            painter == null ||
                shadow == null ||
                shadow.radius != radiusDp ||
                shadow.spread != spreadDp ||
                shadow.color != color ||
                shadow.brush != tmpBrush ||
                shadow.alpha != alpha ||
                shadow.blendMode != blendMode ||
                shadow.offset != dpOffset
        ) {
            shadow =
                if (tmpBrush != null) {
                        Shadow(radiusDp, tmpBrush, spreadDp, dpOffset, alpha, blendMode)
                    } else {
                        Shadow(radiusDp, color, spreadDp, dpOffset, alpha, blendMode)
                    }
                    .also { targetShadow = it }
            painter =
                requireGraphicsContext()
                    .shadowContext
                    .createInnerShadowPainter(shape, shadow)
                    .also { shadowPainter = it }
        }
        return painter
    }

    override fun onObservedReadsChanged() {
        blockRead = false
        invalidateShadow()
    }

    private fun invalidateShadow() {
        targetShadow = null
        shadowPainter = null
        invalidateDraw()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is BlockInnerShadowNode) return false

        if (alpha != other.alpha) return false
        if (shape != other.shape) return false
        if (block !== other.block) return false
        if (radius != other.radius) return false
        if (spread != other.spread) return false
        if (offset != other.offset) return false
        if (color != other.color) return false
        if (brush != other.brush) return false
        if (blendMode != other.blendMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alpha.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + block.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + spread.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + blendMode.hashCode()
        return result
    }
}

/**
 * DropShadow ModifierNode implementation that leverages a receiver scope for mutable drop shadow
 * properties and translation offset. This is useful for use cases where the shadow itself is
 * animated and minimizing recompositions is desired.
 */
internal class BlockDropShadowNode(private var shape: Shape, block: DropShadowScope.() -> Unit) :
    DrawModifierNode, Modifier.Node(), ObserverModifierNode, DropShadowScope {

    private var densityObject: Density? = null
    private var targetShadow: Shadow? = null
    private var shadowPainter: DropShadowPainter? = null
    private var blockRead = false
    private var block: DropShadowScope.() -> Unit = block
        set(value) {
            if (field !== value) {
                field = value
                blockRead = false
                invalidateDraw()
            }
        }

    override val density: Float
        get() = densityObject?.density ?: 1f

    override val fontScale: Float
        get() = densityObject?.fontScale ?: 1f

    override var radius: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var spread: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var offset: Offset = Offset.Zero
        set(value) {
            if (field != value) {
                field = value
                // Note changes to offset do not require the shadow to be recreated, however, the
                // shadow needs to be redrawn with the updated offset
                invalidateDraw()
            }
        }

    override var color: Color = Color.Black
        set(value) {
            val target =
                if (value.isSpecified) {
                    value
                } else {
                    Color.Black
                }
            if (field != target) {
                field = target
                invalidateShadow()
            }
        }

    override var brush: Brush? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var alpha: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            if (field != value) {
                field = value
                invalidateShadow()
            }
        }

    override fun onAttach() {
        super.onAttach()
        updateDensity()
    }

    override fun onDensityChange() {
        if (isAttached) updateDensity()
    }

    private fun updateDensity() {
        val newDensity = requireDensity()
        if (densityObject != newDensity) {
            densityObject = newDensity
            blockRead = false
            invalidateShadow()
        }
    }

    fun update(shape: Shape, block: DropShadowScope.() -> Unit) {
        this.shape = shape
        this.block = block
    }

    override fun ContentDrawScope.draw() {
        with(obtainPainter()) { draw(size) }
        drawContent()
    }

    private fun obtainPainter(): DropShadowPainter {
        if (!blockRead) {
            blockRead = true
            resetShadow()
            observeReads { block() }
        }
        var shadow = targetShadow
        var painter = shadowPainter
        val tmpBrush = brush
        val radiusDp = radius.toDp()
        val spreadDp = spread.toDp()
        val dpOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
        if (
            painter == null ||
                shadow == null ||
                shadow.radius != radiusDp ||
                shadow.spread != spreadDp ||
                shadow.color != color ||
                shadow.brush != tmpBrush ||
                shadow.alpha != alpha ||
                shadow.blendMode != blendMode ||
                shadow.offset != dpOffset
        ) {
            shadow =
                if (tmpBrush != null) {
                        Shadow(radiusDp, tmpBrush, spreadDp, dpOffset, alpha, blendMode)
                    } else {
                        Shadow(radiusDp, color, spreadDp, dpOffset, alpha, blendMode)
                    }
                    .also { targetShadow = it }
            painter =
                requireGraphicsContext().shadowContext.createDropShadowPainter(shape, shadow).also {
                    shadowPainter = it
                }
        }
        return painter
    }

    override fun onObservedReadsChanged() {
        blockRead = false
        invalidateShadow()
    }

    private fun invalidateShadow() {
        targetShadow = null
        shadowPainter = null
        invalidateDraw()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is BlockDropShadowNode) return false

        if (alpha != other.alpha) return false
        if (shape != other.shape) return false
        if (block !== other.block) return false
        if (radius != other.radius) return false
        if (spread != other.spread) return false
        if (offset != other.offset) return false
        if (color != other.color) return false
        if (brush != other.brush) return false
        if (blendMode != other.blendMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alpha.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + block.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + spread.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + blendMode.hashCode()
        return result
    }
}

internal data class ShadowGraphicsLayerElement(
    val elevation: Dp,
    val shape: Shape,
    val clip: Boolean,
    val ambientColor: Color,
    val spotColor: Color,
) : ModifierNodeElement<BlockGraphicsLayerModifier>() {

    private fun createBlock(): GraphicsLayerScope.() -> Unit = {
        this.shadowElevation = this@ShadowGraphicsLayerElement.elevation.toPx()
        this.shape = this@ShadowGraphicsLayerElement.shape
        this.clip = this@ShadowGraphicsLayerElement.clip
        this.ambientShadowColor = this@ShadowGraphicsLayerElement.ambientColor
        this.spotShadowColor = this@ShadowGraphicsLayerElement.spotColor
    }

    override fun create() = BlockGraphicsLayerModifier(createBlock())

    override fun update(node: BlockGraphicsLayerModifier) {
        node.layerBlock = createBlock()
        node.invalidateLayerBlock()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shadow"
        properties["elevation"] = elevation
        properties["shape"] = shape
        properties["clip"] = clip
        properties["ambientColor"] = ambientColor
        properties["spotColor"] = spotColor
    }
}
