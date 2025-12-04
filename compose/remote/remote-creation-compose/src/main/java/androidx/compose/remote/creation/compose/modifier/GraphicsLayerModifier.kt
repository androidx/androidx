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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.state.RemoteFloat
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
    public val scaleX: Float,
    public val scaleY: Float,
    public val rotationX: Float,
    public val rotationY: Float,
    public val rotationZ: Float,
    public val shadowElevation: Float,
    public val transformOriginX: Float,
    public val transformOriginY: Float,
    public val translationX: Float,
    public val translationY: Float,
    public val shape: Shape,
    public val compositingStrategy: Int,
    public val alpha: Float,
    public val cameraDistance: Float,
    public val renderEffect: RenderEffect?,
) : RemoteModifier.Element {

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
}

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
            scaleX.id,
            scaleY.id,
            rotationX.id,
            rotationY.id,
            rotationZ.id,
            shadowElevation.id,
            transformOriginX.id,
            transformOriginY.id,
            translationX.id,
            translationY.id,
            shape,
            cS,
            alpha.id,
            cameraDistance.id,
            renderEffect,
        )
    )
}
