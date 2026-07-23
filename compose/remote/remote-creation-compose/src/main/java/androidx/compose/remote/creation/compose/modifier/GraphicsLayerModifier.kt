/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.CircleShape
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RectShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Auto
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.ModulateAlpha
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Offscreen

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GraphicsLayerModifier(
    public val scaleX: RemoteFloat,
    public val scaleY: RemoteFloat,
    public val rotationX: RemoteFloat,
    public val rotationY: RemoteFloat,
    public val rotationZ: RemoteFloat,
    public val shadowElevation: RemoteFloat,
    public val transformOriginX: RemoteFloat,
    public val transformOriginY: RemoteFloat,
    public val translationX: RemoteFloat,
    public val translationY: RemoteFloat,
    public val shape: Shape,
    public val compositingStrategy: Int,
    public val alpha: RemoteFloat,
    public val cameraDistance: RemoteFloat,
    public val renderEffect: RenderEffect?,
) : RemoteModifier.Element {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        val layer = androidx.compose.remote.creation.modifiers.GraphicsLayerModifier()
        if (scaleX.floatId != 1f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.SCALE_X, scaleX.floatId)
        }
        if (scaleY.floatId != 1f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.SCALE_Y, scaleY.floatId)
        }
        if (rotationX.floatId != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ROTATION_X, rotationX.floatId)
        }
        if (rotationY.floatId != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ROTATION_Y, rotationY.floatId)
        }
        if (rotationZ.floatId != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ROTATION_Z, rotationZ.floatId)
        }
        if (shadowElevation.floatId != 0f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.SHADOW_ELEVATION,
                shadowElevation.floatId,
            )
        }
        if (transformOriginX.floatId != 0.5f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X,
                transformOriginX.floatId,
            )
        }
        if (transformOriginY.floatId != 0.5f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y,
                transformOriginY.floatId,
            )
        }
        if (translationX.floatId != 0f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSLATION_X,
                translationX.floatId,
            )
        }
        if (translationY.floatId != 0f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSLATION_Y,
                translationY.floatId,
            )
        }
        if (alpha.floatId != 1f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ALPHA, alpha.floatId)
        }
        if (cameraDistance.floatId != 8f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.CAMERA_DISTANCE,
                cameraDistance.floatId,
            )
        }
        if (compositingStrategy != 0) {
            layer.setIntAttribute(
                GraphicsLayerModifierOperation.COMPOSITING_STRATEGY,
                compositingStrategy,
            )
        }
        if (renderEffect is BlurEffect) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.BLUR_RADIUS_X,
                renderEffect.radiusX,
            )
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.BLUR_RADIUS_Y,
                renderEffect.radiusY,
            )
            val tileMode =
                when (renderEffect.edgeTreatment) {
                    TileMode.Clamp -> GraphicsLayerModifierOperation.TILE_MODE_CLAMP
                    TileMode.Repeated -> GraphicsLayerModifierOperation.TILE_MODE_REPEATED
                    TileMode.Mirror -> GraphicsLayerModifierOperation.TILE_MODE_MIRROR
                    TileMode.Decal -> GraphicsLayerModifierOperation.TILE_MODE_DECAL
                    else -> GraphicsLayerModifierOperation.TILE_MODE_CLAMP
                }
            layer.setIntAttribute(GraphicsLayerModifierOperation.BLUR_TILE_MODE, tileMode)
        }
        if (shape is RectShape) {
            layer.setIntAttribute(
                GraphicsLayerModifierOperation.SHAPE,
                GraphicsLayerModifierOperation.SHAPE_RECT,
            )
        } else if (shape is RoundedCornerShape) {
            layer.setIntAttribute(
                GraphicsLayerModifierOperation.SHAPE,
                GraphicsLayerModifierOperation.SHAPE_ROUND_RECT,
            )
            layer.setFloatAttribute(GraphicsLayerModifierOperation.SHAPE_RADIUS, 40f)
        } else if (shape is CircleShape) {
            layer.setIntAttribute(
                GraphicsLayerModifierOperation.SHAPE,
                GraphicsLayerModifierOperation.SHAPE_CIRCLE,
            )
        }
        return layer
    }
}

/**
 * Applies a graphics layer modifier to the [RemoteModifier].
 *
 * A graphics layer modifier can be used to apply effects such as scaling, rotation, translation,
 * shadow, clipping, alpha, and render effects (like blur) to the drawing content.
 *
 * This overload accepts [RemoteFloat] values, which allow binding properties to dynamic expressions
 * or state.
 *
 * @param scaleX The horizontal scale factor.
 * @param scaleY The vertical scale factor.
 * @param rotationX The rotation of the layer around the X axis, in degrees.
 * @param rotationY The rotation of the layer around the Y axis, in degrees.
 * @param rotationZ The rotation of the layer around the Z axis, in degrees.
 * @param shadowElevation The shadow elevation of the layer.
 * @param transformOriginX The horizontal center of the transform, as a fraction of the layer width.
 * @param transformOriginY The vertical center of the transform, as a fraction of the layer height.
 * @param translationX The horizontal translation of the layer.
 * @param translationY The vertical translation of the layer.
 * @param alpha The alpha (opacity) of the layer.
 * @param shape The shape of the layer.
 * @param compositingStrategy The compositing strategy to use for the layer.
 * @param cameraDistance The camera distance for 3D transforms.
 * @param renderEffect The [RenderEffect] to apply, or null.
 */
public fun RemoteModifier.graphicsLayer(
    scaleX: RemoteFloat = 1f.rf,
    scaleY: RemoteFloat = 1f.rf,
    rotationX: RemoteFloat = 0f.rf,
    rotationY: RemoteFloat = 0f.rf,
    rotationZ: RemoteFloat = 0f.rf,
    shadowElevation: RemoteFloat = 0f.rf,
    transformOriginX: RemoteFloat = 0.5f.rf,
    transformOriginY: RemoteFloat = 0.5f.rf,
    translationX: RemoteFloat = 0f.rf,
    translationY: RemoteFloat = 0f.rf,
    alpha: RemoteFloat = 1f.rf,
    shape: Shape = RectangleShape,
    compositingStrategy: CompositingStrategy = Auto,
    cameraDistance: RemoteFloat = 8f.rf, // Default Value for Camera Distance
    renderEffect: RenderEffect? = null,
): RemoteModifier {

    val cS =
        when (compositingStrategy) {
            Auto -> 0
            Offscreen -> 1
            ModulateAlpha -> 2
            else -> 0
        }
    return then(
        GraphicsLayerModifier(
            scaleX,
            scaleY,
            rotationX,
            rotationY,
            rotationZ,
            shadowElevation,
            transformOriginX,
            transformOriginY,
            translationX,
            translationY,
            shape,
            cS,
            alpha,
            cameraDistance,
            renderEffect,
        )
    )
}

/** Scope for configuring graphics layer properties in a type-safe builder lambda. */
public interface GraphicsLayerScope {
    /** The horizontal scale factor. */
    public var scaleX: RemoteFloat

    /** The vertical scale factor. */
    public var scaleY: RemoteFloat

    /** The rotation of the layer around the X axis, in degrees. */
    public var rotationX: RemoteFloat

    /** The rotation of the layer around the Y axis, in degrees. */
    public var rotationY: RemoteFloat

    /** The rotation of the layer around the Z axis, in degrees. */
    public var rotationZ: RemoteFloat

    /** The shadow elevation of the layer. */
    public var shadowElevation: RemoteFloat

    /** The horizontal center of the transform, as a fraction of the layer width. */
    public var transformOriginX: RemoteFloat

    /** The vertical center of the transform, as a fraction of the layer height. */
    public var transformOriginY: RemoteFloat

    /** The horizontal translation of the layer. */
    public var translationX: RemoteFloat

    /** The vertical translation of the layer. */
    public var translationY: RemoteFloat

    /** The alpha (opacity) of the layer, from 0f (transparent) to 1f (opaque). */
    public var alpha: RemoteFloat

    /** The camera distance for 3D transforms. */
    public var cameraDistance: RemoteFloat

    /** The shape of the layer. Used for clipping and outline shadows. */
    public var shape: Shape

    /** The compositing strategy to use for the layer. */
    public var compositingStrategy: CompositingStrategy

    /** The [RenderEffect] to apply to this layer, or null. */
    public var renderEffect: RenderEffect?
}

internal class GraphicsLayerScopeImpl : GraphicsLayerScope {
    override var scaleX: RemoteFloat = 1f.rf
    override var scaleY: RemoteFloat = 1f.rf
    override var rotationX: RemoteFloat = 0f.rf
    override var rotationY: RemoteFloat = 0f.rf
    override var rotationZ: RemoteFloat = 0f.rf
    override var shadowElevation: RemoteFloat = 0f.rf
    override var transformOriginX: RemoteFloat = 0.5f.rf
    override var transformOriginY: RemoteFloat = 0.5f.rf
    override var translationX: RemoteFloat = 0f.rf
    override var translationY: RemoteFloat = 0f.rf
    override var alpha: RemoteFloat = 1f.rf
    override var cameraDistance: RemoteFloat = 8f.rf
    override var shape: Shape = RectangleShape
    override var compositingStrategy: CompositingStrategy = Auto
    override var renderEffect: RenderEffect? = null
}

/**
 * Applies a graphics layer modifier to the [RemoteModifier] using a type-safe builder lambda.
 *
 * This overload allows configuring layer properties within a [GraphicsLayerScope] block, which is
 * similar to standard Compose's `graphicsLayer { ... }`.
 *
 * Example usage:
 * ```
 * modifier.graphicsLayer {
 *     alpha = 0.5f.rf
 *     rotationZ = 45f.rf
 * }
 * ```
 *
 * @param block The lambda block to configure the [GraphicsLayerScope].
 */
public fun RemoteModifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): RemoteModifier {
    val scope = GraphicsLayerScopeImpl().apply(block)
    val cS =
        when (scope.compositingStrategy) {
            Auto -> 0
            Offscreen -> 1
            ModulateAlpha -> 2
            else -> 0
        }
    return then(
        GraphicsLayerModifier(
            scaleX = scope.scaleX,
            scaleY = scope.scaleY,
            rotationX = scope.rotationX,
            rotationY = scope.rotationY,
            rotationZ = scope.rotationZ,
            shadowElevation = scope.shadowElevation,
            transformOriginX = scope.transformOriginX,
            transformOriginY = scope.transformOriginY,
            translationX = scope.translationX,
            translationY = scope.translationY,
            shape = scope.shape,
            compositingStrategy = cS,
            alpha = scope.alpha,
            cameraDistance = scope.cameraDistance,
            renderEffect = scope.renderEffect,
        )
    )
}
