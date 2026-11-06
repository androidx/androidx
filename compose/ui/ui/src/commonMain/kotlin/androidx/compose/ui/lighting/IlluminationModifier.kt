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

package androidx.compose.ui.lighting

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * Draws a lighting pass over the content, shading it as though it were a physical surface.
 *
 * The content is drawn first. On top of it this modifier draws the light that a surface shaped like
 * [surfaceGeometrySpec], made of [surfaceMaterial], would reflect from [lights].
 *
 * Every light is evaluated independently and their contributions are summed, then clamped. The part
 * of the surface where the normals face away from the light, will not be lit by that particular
 * light.
 *
 * Check compatibility with [IlluminationScope.isSupported]. Unsupported platforms will draw the
 * content unchanged.
 *
 * @param lights the lights illuminating the surface. At most [IlluminationScope.MaxLightsAllowed]
 *   lights are supported. Lights positioned with [LightPosition.Anchored] contribute nothing until
 *   their anchor has been placed.
 * @param surfaceMaterial how the surface reflects light. [SurfaceMaterial.None] reflects nothing,
 *   so at least one component must be set for this modifier to draw anything.
 * @param surfaceGeometrySpec the virtual three-dimensional shape the surface is treated as having.
 * @param blendMode how the lighting pass is composited over the content. The default is
 *   [BlendMode.SrcOver]. Note that you might need a different blendMode to maintain legibility
 *   since [BlendMode.SrcOver] can occlude the underlying content.
 * @throws IllegalArgumentException if more than [IlluminationScope.MaxLightsAllowed] lights are
 *   supplied, or if the shape backing [surfaceGeometrySpec] is not one that lighting supports.
 * @sample androidx.compose.ui.samples.IlluminateSample
 * @sample androidx.compose.ui.samples.IlluminateWithSpotLightSample
 * @sample androidx.compose.ui.samples.IlluminateWithDirectionalLightSample
 * @see lightAnchor
 * @see SurfaceGeometrySpec
 * @see SurfaceMaterial
 */
public fun Modifier.illuminate(
    lights: List<Light>,
    surfaceMaterial: SurfaceMaterial,
    surfaceGeometrySpec: SurfaceGeometrySpec,
    blendMode: BlendMode = BlendMode.SrcOver,
): Modifier =
    this then IlluminationModifierElement(lights, surfaceMaterial, surfaceGeometrySpec, blendMode)

/**
 * The receiver of the block passed to [illuminate], used to configure the lighting drawn over the
 * content. Each run of the block starts with every property at its default.
 *
 * Lighting is computed with local illumination, which shades each point of the surface using only
 * the light that reaches it directly from [lights], the direction the surface faces at that point
 * according to [surfaceGeometrySpec], and how [surfaceMaterial] reflects light. Nothing else on
 * screen is taken into account, so other composables neither cast shadows onto the surface nor
 * reflect light onto it.
 */
public class IlluminationScope internal constructor() {
    /**
     * The lights illuminating the surface, empty to draw only the content. A light positioned with
     * [LightPosition.Anchored] contributes nothing until its anchor has been placed.
     */
    public var lights: List<Light> = emptyList()

    /**
     * How the surface reflects [lights]. The default [SurfaceMaterial.None] reflects nothing, so no
     * light is drawn until at least one of its components is set.
     */
    public var surfaceMaterial: SurfaceMaterial = SurfaceMaterial.None

    /**
     * The virtual three-dimensional shape the surface is treated as having. It decides which way
     * each point of the surface faces, and so which parts catch [lights]. The default is a flat
     * rectangle with no thickness.
     *
     * Shapes that lighting does not support throw an [IllegalArgumentException] when the surface is
     * drawn. See [SurfaceGeometrySpec] for the supported shapes.
     */
    public var surfaceGeometrySpec: SurfaceGeometrySpec = DefaultSurfaceGeometrySpec

    /** Restores every property to its default before the block runs again. */
    internal fun reset() {
        lights = emptyList()
        surfaceMaterial = SurfaceMaterial.None
        surfaceGeometrySpec = DefaultSurfaceGeometrySpec
    }

    public companion object {
        /**
         * The most lights [illuminate] can apply to a surface, with either overload. Drawing a
         * surface lit by more lights throws an [IllegalArgumentException].
         */
        public val MaxLightsAllowed: Int
            get() = LightShader.MaxLightsAllowed

        /**
         * Capability query to determine if [illuminate] can draw lighting on the current platform.
         * When this returns `false`, only the content is drawn. On Android, lighting requires
         * API 33.
         */
        public fun isSupported(): Boolean = LightShader.isSupported()
    }
}

private val DefaultSurfaceGeometrySpec = SurfaceGeometrySpec.flat(RectangleShape, thickness = 0.dp)

/**
 * Draws a lighting pass over the content, shading it as though it were a physical surface, with the
 * lighting configured in [block].
 *
 * The content is drawn first. On top of it this modifier draws the light that a surface shaped like
 * [IlluminationScope.surfaceGeometrySpec], made of [IlluminationScope.surfaceMaterial], would
 * reflect from [IlluminationScope.lights].
 *
 * Check compatibility with [IlluminationScope.isSupported]. Unsupported platforms will draw the
 * content unchanged.
 *
 * Prefer this overload when the lighting is backed by a [androidx.compose.runtime.State] or an
 * animated value, such as a moving light, as reading a state inside [block] only redraws the
 * surface without triggering recomposition and relayout.
 *
 * [block] runs every time the surface is drawn, starting from a [IlluminationScope] with every
 * property at its default. It should therefore set every property it relies on, and do as little
 * other work as possible.
 *
 * @param blendMode how the lighting pass is composited over the content. The default is
 *   [BlendMode.SrcOver]. Note that you might need a different blendMode to maintain legibility
 *   since [BlendMode.SrcOver] can occlude the underlying content.
 * @param block configures the light, material and geometry of the surface.
 * @throws IllegalArgumentException if more than [IlluminationScope.MaxLightsAllowed] lights are
 *   supplied, or if the shape backing [IlluminationScope.surfaceGeometrySpec] is not one that
 *   lighting supports.
 * @see IlluminationScope
 * @see lightAnchor
 */
public fun Modifier.illuminate(
    blendMode: BlendMode = BlendMode.SrcOver,
    block: IlluminationScope.() -> Unit,
): Modifier = this then BlockIlluminationModifierElement(blendMode, block)

private class IlluminationModifierElement(
    private val lights: List<Light>,
    private val surfaceMaterial: SurfaceMaterial,
    private val surfaceGeometrySpec: SurfaceGeometrySpec,
    private val blendMode: BlendMode,
) : ModifierNodeElement<IlluminationModifierNode>() {
    override fun create(): IlluminationModifierNode =
        IlluminationModifierNode(lights, surfaceMaterial, surfaceGeometrySpec, blendMode)

    override fun update(node: IlluminationModifierNode) {
        var needsRedraw = false
        if (node.lights != lights) {
            node.lights = lights
            needsRedraw = true
        }
        if (node.blendMode != blendMode) {
            node.blendMode = blendMode
            needsRedraw = true
        }
        if (node.surfaceMaterial != surfaceMaterial) {
            node.surfaceMaterial = surfaceMaterial
            needsRedraw = true
        }
        if (node.surfaceGeometrySpec != surfaceGeometrySpec) {
            node.surfaceGeometrySpec = surfaceGeometrySpec
            needsRedraw = true
        }
        if (needsRedraw) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "illuminate"
        properties["lights"] = lights
        properties["surfaceMaterial"] = surfaceMaterial
        properties["surfaceGeometrySpec"] = surfaceGeometrySpec
        properties["blendMode"] = blendMode
    }

    override fun hashCode(): Int {
        var result = lights.hashCode()
        result = 31 * result + surfaceMaterial.hashCode()
        result = 31 * result + surfaceGeometrySpec.hashCode()
        result = 31 * result + blendMode.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IlluminationModifierElement) return false

        if (lights != other.lights) return false
        if (surfaceMaterial != other.surfaceMaterial) return false
        if (surfaceGeometrySpec != other.surfaceGeometrySpec) return false
        if (blendMode != other.blendMode) return false

        return true
    }
}

private class BlockIlluminationModifierElement(
    private val blendMode: BlendMode,
    private val block: IlluminationScope.() -> Unit,
) : ModifierNodeElement<BlockIlluminationModifierNode>() {
    override fun create(): BlockIlluminationModifierNode {
        return BlockIlluminationModifierNode(blendMode, block)
    }

    override fun update(node: BlockIlluminationModifierNode) {
        val needsRedraw = node.blendMode != blendMode || node.block !== block
        node.blendMode = blendMode
        node.block = block
        if (needsRedraw) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "illuminate"
        properties["blendMode"] = blendMode
        properties["block"] = block
    }

    override fun hashCode(): Int {
        var result = blendMode.hashCode()
        result = 31 * result + block.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlockIlluminationModifierElement) return false

        if (blendMode != other.blendMode) return false
        if (block !== other.block) return false
        return true
    }
}

private class IlluminationModifierNode(
    var lights: List<Light>,
    var surfaceMaterial: SurfaceMaterial,
    var surfaceGeometrySpec: SurfaceGeometrySpec,
    var blendMode: BlendMode,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private val paint = Paint()
    private var lightShader: LightShader? = null
    private val localLights = mutableListOf<Light>()

    override fun ContentDrawScope.draw() {
        drawContent()

        if (!LightShader.isSupported() || lights.isEmpty() || size.isEmpty()) return

        val coordinates = requireLayoutCoordinates()
        localLights.clear()

        lights.fastForEach { light -> localLights += with(light) { inLocalSpace(coordinates) } }

        var shader = lightShader
        if (shader == null) {
            shader =
                LightShader(
                    lights = localLights,
                    surfaceMaterial = surfaceMaterial,
                    surfaceGeometrySpec = surfaceGeometrySpec,
                    density = this,
                    layoutDirection = layoutDirection,
                    size = size,
                )
            lightShader = shader
        } else {
            shader.update(
                lights = localLights,
                surfaceMaterial = surfaceMaterial,
                surfaceGeometrySpec = surfaceGeometrySpec,
                density = this,
                layoutDirection = layoutDirection,
                size = size,
            )
        }
        paint.blendMode = blendMode
        shader.applyTo(paint)
        this.drawContext.canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (lights.fastAny { it.position !is LightPosition.Local }) {
            invalidateDraw()
        }
    }

    override fun onDetach() {
        lightShader = null
        localLights.clear()
    }
}

private class BlockIlluminationModifierNode(
    var blendMode: BlendMode,
    var block: IlluminationScope.() -> Unit,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private val paint = Paint()
    private var lightShader: LightShader? = null
    private val localLights = mutableListOf<Light>()
    private var localIlluminationScope: IlluminationScope = IlluminationScope()

    override fun ContentDrawScope.draw() {
        localIlluminationScope.reset()
        localIlluminationScope.block()

        drawContent()

        if (!LightShader.isSupported() || localIlluminationScope.lights.isEmpty() || size.isEmpty())
            return

        val coordinates = requireLayoutCoordinates()

        localLights.clear()
        localIlluminationScope.lights.fastForEach { light ->
            localLights += with(light) { inLocalSpace(coordinates) }
        }

        var shader = lightShader
        if (shader == null) {
            shader =
                LightShader(
                    lights = localLights,
                    surfaceMaterial = localIlluminationScope.surfaceMaterial,
                    surfaceGeometrySpec = localIlluminationScope.surfaceGeometrySpec,
                    density = this,
                    layoutDirection = layoutDirection,
                    size = size,
                )
            lightShader = shader
        } else {
            shader.update(
                lights = localLights,
                surfaceMaterial = localIlluminationScope.surfaceMaterial,
                surfaceGeometrySpec = localIlluminationScope.surfaceGeometrySpec,
                density = this,
                layoutDirection = layoutDirection,
                size = size,
            )
        }
        paint.blendMode = blendMode
        shader.applyTo(paint)
        this.drawContext.canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (localIlluminationScope.lights.fastAny { it.position !is LightPosition.Local }) {
            invalidateDraw()
        }
    }

    override fun onDetach() {
        lightShader = null
        localLights.clear()
        localIlluminationScope.reset()
    }
}
