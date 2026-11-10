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

@file:JvmName("GraphicsLayerModifierKt")

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.remoteComponent
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.toDimension
import androidx.compose.remote.creation.compose.state.RemoteCompositingStrategy
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.GraphicsLayerModifier as CreationGraphicsLayerModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier

internal class GraphicsLayerModifier(
    val scaleX: RemoteFloat,
    val scaleY: RemoteFloat,
    val rotationX: RemoteFloat,
    val rotationY: RemoteFloat,
    val rotationZ: RemoteFloat,
    val shadowElevation: RemoteFloat,
    val transformOriginX: RemoteFloat,
    val transformOriginY: RemoteFloat,
    val translationX: RemoteFloat,
    val translationY: RemoteFloat,
    val shape: RemoteShape = RemoteRectangleShape,
    val compositingStrategy: RemoteCompositingStrategy = RemoteCompositingStrategy.Auto,
    val alpha: RemoteFloat,
    val cameraDistance: RemoteFloat,
    val renderEffect: RemoteRenderEffect?,
) : RemoteModifier.Element {

    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        val layer = CreationGraphicsLayerModifier()
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
        if (transformOriginX.floatId != 0f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X,
                transformOriginX.floatId,
            )
        }
        if (transformOriginY.floatId != 0f) {
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
        if (compositingStrategy.value != 0) {
            layer.setIntAttribute(
                GraphicsLayerModifierOperation.COMPOSITING_STRATEGY,
                compositingStrategy.value,
            )
        }
        if (renderEffect is RemoteBlurEffect) {
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
                    RemoteTileMode.Clamp -> GraphicsLayerModifierOperation.TILE_MODE_CLAMP
                    RemoteTileMode.Repeated -> GraphicsLayerModifierOperation.TILE_MODE_REPEATED
                    RemoteTileMode.Mirror -> GraphicsLayerModifierOperation.TILE_MODE_MIRROR
                    RemoteTileMode.Decal -> GraphicsLayerModifierOperation.TILE_MODE_DECAL
                    else -> GraphicsLayerModifierOperation.TILE_MODE_CLAMP
                }
            layer.setIntAttribute(GraphicsLayerModifierOperation.BLUR_TILE_MODE, tileMode)
        }
        when (shape) {
            RemoteCircleShape -> {
                layer.setIntAttribute(
                    GraphicsLayerModifierOperation.SHAPE,
                    GraphicsLayerModifierOperation.SHAPE_CIRCLE,
                )
            }
            RemoteRectangleShape -> {
                layer.setIntAttribute(
                    GraphicsLayerModifierOperation.SHAPE,
                    GraphicsLayerModifierOperation.SHAPE_RECT,
                )
            }
            is RemoteRoundedCornerShape -> {
                val component = remoteComponent()
                val remoteSize = RemoteSize(component.width, component.height)
                layer.setIntAttribute(
                    GraphicsLayerModifierOperation.SHAPE,
                    GraphicsLayerModifierOperation.SHAPE_ROUND_RECT,
                )
                layer.setFloatAttribute(
                    GraphicsLayerModifierOperation.SHAPE_RADIUS,
                    shape.topStart.toDimension(remoteSize, remoteDensity, densityBehavior).floatId,
                )
            }
        }
        return layer
    }
}

/**
 * Applies visual effects such as scaling, rotation, translation, shadow, alpha, or clipping to
 * content.
 *
 * This overload accepts [RemoteFloat] values, which allow binding properties to dynamic expressions
 * or state.
 *
 * Note: Graphics layer clipping only supports a uniform corner radius. If an asymmetric
 * [RemoteRoundedCornerShape] is provided, [RemoteRoundedCornerShape.topStart] is used for all
 * corners.
 *
 * @param scaleX horizontal scale factor
 * @param scaleY vertical scale factor
 * @param rotationX rotation of the layer around the X axis in degrees
 * @param rotationY rotation of the layer around the Y axis in degrees
 * @param rotationZ rotation of the layer around the Z axis in degrees
 * @param shadowElevation shadow elevation of the layer
 * @param transformOriginX horizontal center of the transform, as a fraction of the layer width
 * @param transformOriginY vertical center of the transform, as a fraction of the layer height
 * @param translationX horizontal translation of the layer
 * @param translationY vertical translation of the layer
 * @param alpha alpha (opacity) of the layer
 * @param shape [RemoteShape] of the layer
 * @param compositingStrategy [RemoteCompositingStrategy] to use for the layer
 * @param cameraDistance camera distance for 3D transforms
 * @param renderEffect [RemoteRenderEffect] to apply, or null
 * @return a [RemoteModifier] that applies the graphics layer effects
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
    shape: RemoteShape = RemoteRectangleShape,
    compositingStrategy: RemoteCompositingStrategy = RemoteCompositingStrategy.Auto,
    cameraDistance: RemoteFloat = 8f.rf, // Default Value for Camera Distance
    renderEffect: RemoteRenderEffect? = null,
): RemoteModifier =
    then(
        GraphicsLayerModifier(
            scaleX = scaleX,
            scaleY = scaleY,
            rotationX = rotationX,
            rotationY = rotationY,
            rotationZ = rotationZ,
            shadowElevation = shadowElevation,
            transformOriginX = transformOriginX,
            transformOriginY = transformOriginY,
            translationX = translationX,
            translationY = translationY,
            shape = shape,
            compositingStrategy = compositingStrategy,
            alpha = alpha,
            cameraDistance = cameraDistance,
            renderEffect = renderEffect,
        )
    )

/** Scope for configuring graphics layer properties in a type-safe builder lambda. */
public interface RemoteGraphicsLayerScope {
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
    public var shape: RemoteShape

    /** The compositing strategy to use for the layer. */
    public var compositingStrategy: RemoteCompositingStrategy

    /** The [RemoteRenderEffect] to apply to this layer, or null. */
    public var renderEffect: RemoteRenderEffect?
}

internal class GraphicsLayerScopeImpl : RemoteGraphicsLayerScope {
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
    override var shape: RemoteShape = RemoteRectangleShape
    override var compositingStrategy: RemoteCompositingStrategy = RemoteCompositingStrategy.Auto
    override var renderEffect: RemoteRenderEffect? = null
}

/**
 * Applies a graphics layer modifier to the [RemoteModifier] using a type-safe builder lambda.
 *
 * This overload allows configuring layer properties within a [RemoteGraphicsLayerScope] block,
 * which is similar to standard Compose's `graphicsLayer { ... }`.
 *
 * Example usage:
 * ```
 * modifier.graphicsLayer {
 *     alpha = 0.5f.rf
 *     rotationZ = 45f.rf
 * }
 * ```
 *
 * @param block The lambda block to configure the [RemoteGraphicsLayerScope].
 * @return a [RemoteModifier] that applies the graphics layer configured by [block]
 */
public fun RemoteModifier.graphicsLayer(
    block: RemoteGraphicsLayerScope.() -> Unit
): RemoteModifier {
    val scope = GraphicsLayerScopeImpl().apply(block)
    return graphicsLayer(
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
        alpha = scope.alpha,
        shape = scope.shape,
        compositingStrategy = scope.compositingStrategy,
        cameraDistance = scope.cameraDistance,
        renderEffect = scope.renderEffect,
    )
}
