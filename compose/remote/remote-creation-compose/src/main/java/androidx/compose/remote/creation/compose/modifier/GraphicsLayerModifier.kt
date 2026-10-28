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
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.layout.RemoteFloatContext
import androidx.compose.remote.creation.compose.layout.RemoteSize
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
                val context = RemoteFloatContext(this)
                val remoteSize = RemoteSize(context.componentWidth(), context.componentHeight())
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
 * Applies a graphics layer modifier to the [RemoteModifier].
 *
 * A graphics layer modifier can be used to apply effects such as scaling, rotation, translation,
 * shadow, clipping, alpha, and render effects (like blur) to the drawing content.
 *
 * This overload accepts [RemoteFloat] values, which allow binding properties to dynamic expressions
 * or state.
 *
 * Note: Graphics layer clipping only supports a uniform corner radius. If an asymmetric
 * [RemoteRoundedCornerShape] is provided, [RemoteRoundedCornerShape.topStart] is used for all
 * corners.
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
 * @param renderEffect The [RemoteRenderEffect] to apply, or null.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    shape: RemoteShape,
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
