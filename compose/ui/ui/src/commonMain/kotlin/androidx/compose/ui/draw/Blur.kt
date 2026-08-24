/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Determines the strategy used to render pixels in the blurred result that may extend beyond the
 * bounds of the original input.
 *
 * [BlurredEdgeTreatment] will clip the blur result to the boundaries of the original content and
 * optionally specified [shape].
 *
 * Sampling of pixels outside of content bounds will have the same value as the pixels at the
 * closest edge. This is recommended for blurring content that does not contain transparent pixels
 * and ensuring the blurred result does not extend beyond the original bounds (ex. blurring an
 * image)
 *
 * @see TileMode.Clamp
 *
 * Alternatively using [BlurredEdgeTreatment.Unbounded] will not clip the blur result to the
 * boundaries of the original content. Sampling of pixels outside of the content bounds will sample
 * transparent black instead. This is recommended for blurring content that is intended to render
 * outside of the original bounds and may contain transparent pixels in the original bounds (ex.
 * blurring an arbitrary shape or text)
 *
 * @see TileMode.Decal
 */
@Immutable
@kotlin.jvm.JvmInline
public value class BlurredEdgeTreatment(public val shape: Shape?) {

    public companion object {

        /** Bounded [BlurredEdgeTreatment] that clips content bounds to a rectangular shape */
        public val Rectangle: BlurredEdgeTreatment
            get() = BlurredEdgeTreatment(RectangleShape)

        /**
         * Do not clip the blur result to the boundaries of the original content. Sampling of pixels
         * outside of the content bounds will sample transparent black instead. This is recommended
         * for blurring content that is intended to render outside of the original bounds and may
         * contain transparent pixels in the original bounds (ex. blurring an arbitrary shape or
         * text)
         *
         * @see TileMode.Decal
         */
        public val Unbounded: BlurredEdgeTreatment
            get() = BlurredEdgeTreatment(null)
    }
}

/**
 * Draws content blurred with the specified radii.
 *
 * Note this effect is only supported on Android 12 and above. Attempts to use this Modifier on
 * older Android versions will be ignored.
 *
 * Usage of this API renders the corresponding composable into a separate graphics layer. Because
 * the blurred content renders a larger area by the blur radius, this layer is explicitly clipped to
 * the content bounds. Introduce additional space around the drawn content by the specified blur
 * radius to remain within the content bounds.
 *
 * @param radiusX radius of the blur along the x axis
 * @param radiusY radius of the blur along the y axis
 * @param edgeTreatment strategy used to render pixels outside content bounds
 * @sample androidx.compose.ui.samples.BlurSample
 * @sample androidx.compose.ui.samples.ImageBlurSample
 * @see graphicsLayer
 */
@Stable
public fun Modifier.blur(
    radiusX: Dp,
    radiusY: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle,
): Modifier {
    val clip: Boolean
    val tileMode: TileMode
    if (edgeTreatment.shape != null) {
        clip = true
        tileMode = TileMode.Clamp
    } else {
        clip = false
        tileMode = TileMode.Decal
    }
    return if ((radiusX > 0.dp && radiusY > 0.dp) || clip) {
        graphicsLayer {
            val horizontalBlurPixels = radiusX.toPx()
            val verticalBlurPixels = radiusY.toPx()
            this.renderEffect =
                // Only non-zero blur radii are valid BlurEffect parameters
                if (horizontalBlurPixels > 0f && verticalBlurPixels > 0f) {
                    BlurEffect(horizontalBlurPixels, verticalBlurPixels, tileMode)
                } else {
                    null
                }
            this.shape = edgeTreatment.shape ?: RectangleShape
            this.clip = clip
        }
    } else {
        this
    }
}

/**
 * Draws content blurred with the specified radius.
 *
 * Note this effect is only supported on Android 12 and above. Attempts to use this Modifier on
 * older Android versions will be ignored.
 *
 * Usage of this API renders the corresponding composable into a separate graphics layer. Because
 * the blurred content renders a larger area by the blur radius, this layer is explicitly clipped to
 * the content bounds. Introduce additional space around the drawn content by the specified blur
 * radius to remain within the content bounds.
 *
 * @param radius radius of the blur along both the x and y axis
 * @param edgeTreatment strategy used to render pixels outside content bounds
 * @sample androidx.compose.ui.samples.BlurSample
 * @sample androidx.compose.ui.samples.ImageBlurSample
 * @see graphicsLayer
 */
@Stable
public fun Modifier.blur(
    radius: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle,
): Modifier = blur(radius, radius, edgeTreatment)

/**
 * Draws content with a blur specified by [radius].
 *
 * A uniform radius renders on Android 12 (API 31) and above; spatially-varying radii (gradients and
 * custom shaders) require Android 13 (API 33) and above. Below these versions the modifier is
 * ignored.
 *
 * Usage of this API renders the corresponding composable into a separate graphics layer. Because
 * the blurred content renders a larger area by the blur radius, this layer is explicitly clipped to
 * the content bounds. Introduce additional space around the drawn content by the specified blur
 * radius to remain within content bounds.
 *
 * @param radius varying blur radius configuration across the surface
 * @param edgeTreatment strategy used to render pixels outside content bounds
 * @param alpha opacity of the blurred content in the 0..1 range
 * @sample androidx.compose.ui.samples.ProgressiveBlurSample
 * @see graphicsLayer
 */
@Stable
public fun Modifier.blur(
    radius: BlurRadiusSpec,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle,
    alpha: Float = 1f,
): Modifier = blur {
    this.radius = radius
    this.edgeTreatment = edgeTreatment
    this.alpha = alpha
}

/**
 * Draws content with a blur whose radius can vary across the surface.
 *
 * When [block] assigns [BlurScope.radius] multiple times, the last assignment wins.
 *
 * A uniform radius renders on Android 12 (API 31) and above; spatially-varying radii (gradients and
 * custom shaders) require Android 13 (API 33) and above. Below these versions the modifier is
 * ignored.
 *
 * Usage of this API renders the corresponding composable into a separate graphics layer. Because
 * the blurred content renders a larger area by the blur radius, this layer is explicitly clipped to
 * the content bounds. Introduce additional space around the drawn content by the specified blur
 * radius to remain within the content bounds.
 *
 * @param block configuration block run against [BlurScope]
 * @sample androidx.compose.ui.samples.ProgressiveBlurSample
 * @sample androidx.compose.ui.samples.AnimatedProgressiveBlurSample
 * @sample androidx.compose.ui.samples.MultiStopProgressiveBlurSample
 * @sample androidx.compose.ui.samples.AngledProgressiveBlurSample
 * @sample androidx.compose.ui.samples.RadialProgressiveBlurSample
 * @sample androidx.compose.ui.samples.ShaderProgressiveBlurSample
 * @see graphicsLayer
 */
@Stable
public fun Modifier.blur(block: BlurScope.() -> Unit): Modifier =
    this then ProgressiveBlurElement(block)

/**
 * Configures progressive [blur] layer properties.
 *
 * Runs when the blur layer is configured. Inherits [Density] to resolve [Dp] values directly.
 */
public sealed interface BlurScope : Density {
    /** Size of the blurred layer in [Dp], the coordinate space of gradient geometry. */
    public val size: DpSize

    /**
     * Varying blur radius configuration across the surface.
     *
     * Defaults to `BlurRadiusSpec.uniform(0.dp)` (no blur).
     */
    public var radius: BlurRadiusSpec

    /** Strategy used to sample pixels outside content bounds. */
    public var edgeTreatment: BlurredEdgeTreatment

    /** Opacity of the blurred content in the 0..1 range. */
    public var alpha: Float
}

private val UniformZero: BlurRadiusSpec = BlurRadiusSpec.uniform(0.dp)

internal class BlurScopeImpl : BlurScope {
    override var size: DpSize = DpSize.Zero
    override var radius: BlurRadiusSpec = UniformZero
    override var edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle
    override var alpha: Float = 1f

    internal var currentDensity: Density = Density(1f)
    override val density: Float
        get() = currentDensity.density

    override val fontScale: Float
        get() = currentDensity.fontScale

    fun reset() {
        radius = UniformZero // shared instance, no per-frame alloc
        edgeTreatment = BlurredEdgeTreatment.Rectangle
        alpha = 1f
    }
}

internal class BlurNode(var block: BlurScope.() -> Unit) : Modifier.Node(), LayoutModifierNode {

    // The blur is expressed as graphics-layer properties (renderEffect/alpha/clip/shape), applied
    // via placeWithLayer rather than by recording content in a draw node. This mirrors how
    // Modifier.graphicsLayer animates properties: changing only the radius re-runs the layer block
    // (observed by the layer system) to push a new renderEffect onto the existing RenderNode,
    // without re-recording the content display list. The content is
    // re-recorded only when the content itself is invalidated.
    override val shouldAutoInvalidate: Boolean
        get() = false

    private val scope = BlurScopeImpl()

    private var previousEffect: RenderEffect? = null

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        scope.size = size.toDpSize()
        scope.currentDensity = this
        scope.reset()
        scope.block()

        val maskShape = scope.edgeTreatment.shape
        val tileMode = if (maskShape == null) TileMode.Decal else TileMode.Clamp
        renderEffect =
            if (scope.radius == UniformZero) {
                // The block left the radius at (or equal to) the shared no-blur default: clear the
                // layer's effect instead of constructing one. This is the common no-blur case and
                // keeps a radius animated down to uniform zero from pinning a stale effect.
                null
            } else {
                val effect = scope.radius.createRenderEffect(size, this, tileMode)
                val previous = previousEffect
                if (effect == previous) previous else effect.also { previousEffect = it }
            }
        alpha = scope.alpha
        clip = maskShape != null
        shape = maskShape ?: RectangleShape
    }

    override fun onDetach() {
        previousEffect = null
    }

    /** Re-applies the layer block (e.g. after [block] is updated) without re-measuring. */
    fun invalidateBlock() = updateLayerBlock(layerBlock)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}

internal class ProgressiveBlurElement(val block: BlurScope.() -> Unit) :
    ModifierNodeElement<BlurNode>() {
    override fun create(): BlurNode = BlurNode(block)

    override fun update(node: BlurNode) {
        node.block = block
        node.invalidateBlock()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "blur"
        properties["block"] = block
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressiveBlurElement) return false
        return block === other.block
    }

    override fun hashCode(): Int = block.hashCode()
}
