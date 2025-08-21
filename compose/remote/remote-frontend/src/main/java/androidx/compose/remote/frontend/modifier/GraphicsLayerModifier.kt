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
package androidx.compose.remote.frontend.modifier

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.modifiers.CircleShape
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RectShape
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Auto
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.ModulateAlpha
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Offscreen

class GraphicsLayerModifier(
    val scaleX: Float,
    val scaleY: Float,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val shadowElevation: Float,
    val transformOriginX: Float,
    val transformOriginY: Float,
    val translationX: Float,
    val translationY: Float,
    val shape: Shape,
    val compositingStrategy: Int,
    val alpha: Float,
    val cameraDistance: Float,
    val renderEffect: RenderEffect?,
) : RemoteLayoutModifier {

    override fun toRemoteComposeElement(): RecordingModifier.Element {
        val layer = androidx.compose.remote.creation.modifiers.GraphicsLayerModifier()
        if (scaleX != 1f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.SCALE_X, scaleX)
        }
        if (scaleY != 1f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.SCALE_Y, scaleY)
        }
        if (rotationX != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ROTATION_X, rotationX)
        }
        if (rotationY != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ROTATION_Y, rotationY)
        }
        if (rotationZ != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ROTATION_Z, rotationZ)
        }
        if (shadowElevation != 0f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.SHADOW_ELEVATION,
                shadowElevation,
            )
        }
        if (transformOriginX != 0.5f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X,
                transformOriginX,
            )
        }
        if (transformOriginY != 0.5f) {
            layer.setFloatAttribute(
                GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y,
                transformOriginY,
            )
        }
        if (translationX != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.TRANSLATION_X, translationX)
        }
        if (translationY != 0f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.TRANSLATION_Y, translationY)
        }
        if (alpha != 1f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.ALPHA, alpha)
        }
        if (cameraDistance != 8f) {
            layer.setFloatAttribute(GraphicsLayerModifierOperation.CAMERA_DISTANCE, cameraDistance)
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

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return graphicsLayer(
            scaleX = scaleX,
            scaleY = scaleY,
            rotationX = rotationX,
            rotationY = rotationY,
            rotationZ = rotationZ,
            shadowElevation = shadowElevation,
            transformOrigin = TransformOrigin(transformOriginX, transformOriginY),
            alpha = alpha,
            shape = shape,
            cameraDistance = cameraDistance,
            renderEffect = renderEffect?.toComposeRenderEffect(),
        )
    }
}

fun RemoteModifier.graphicsLayer(
    scaleX: Number = 1f,
    scaleY: Number = 1f,
    rotationX: Number = 0f,
    rotationY: Number = 0f,
    rotationZ: Number = 0f,
    shadowElevation: Number = 0f,
    transformOriginX: Number = 0.5f,
    transformOriginY: Number = 0.5f,
    translationX: Number = 0f,
    translationY: Number = 0f,
    alpha: Number = 1f,
    shape: Shape = RectangleShape,
    compositingStrategy: CompositingStrategy = Auto,
    cameraDistance: Number = 8f, // Default Value for Camera Distance
    renderEffect: RenderEffect? = null,
): RemoteModifier {

    val sX =
        if (scaleX is RemoteFloat) {
            scaleX.internalAsFloat()
        } else scaleX.toFloat()
    val sY =
        if (scaleY is RemoteFloat) {
            scaleY.internalAsFloat()
        } else scaleY.toFloat()
    val rX =
        if (rotationX is RemoteFloat) {
            rotationX.internalAsFloat()
        } else rotationX.toFloat()
    val rY =
        if (rotationY is RemoteFloat) {
            rotationY.internalAsFloat()
        } else rotationY.toFloat()
    val rZ =
        if (rotationZ is RemoteFloat) {
            rotationZ.internalAsFloat()
        } else rotationZ.toFloat()
    val sE =
        if (shadowElevation is RemoteFloat) {
            shadowElevation.internalAsFloat()
        } else shadowElevation.toFloat()
    val tOX =
        if (transformOriginX is RemoteFloat) {
            transformOriginX.internalAsFloat()
        } else transformOriginX.toFloat()
    val tOY =
        if (transformOriginY is RemoteFloat) {
            transformOriginY.internalAsFloat()
        } else transformOriginY.toFloat()
    val tX =
        if (translationX is RemoteFloat) {
            translationX.internalAsFloat()
        } else translationX.toFloat()
    val tY =
        if (translationY is RemoteFloat) {
            translationY.internalAsFloat()
        } else translationY.toFloat()
    val tA =
        if (alpha is RemoteFloat) {
            alpha.internalAsFloat()
        } else alpha.toFloat()
    val tCD =
        if (cameraDistance is RemoteFloat) {
            cameraDistance.internalAsFloat()
        } else cameraDistance.toFloat()

    val cS =
        when (compositingStrategy) {
            Auto -> 0
            Offscreen -> 1
            ModulateAlpha -> 2
            else -> 0
        }
    return GraphicsLayerModifier(
        sX,
        sY,
        rX,
        rY,
        rZ,
        sE,
        tOX,
        tOY,
        tX,
        tY,
        shape,
        cS,
        tA,
        tCD,
        renderEffect,
    )
}
