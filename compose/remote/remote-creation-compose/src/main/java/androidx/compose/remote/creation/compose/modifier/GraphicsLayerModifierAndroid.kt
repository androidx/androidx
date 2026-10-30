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

@file:JvmName("GraphicsLayerModifierKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.modifier

import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteCompositingStrategy
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Auto
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.ModulateAlpha
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Offscreen

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
 * @param renderEffect The [RemoteRenderEffect] to apply, or null.
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
    renderEffect: RemoteRenderEffect? = null,
): RemoteModifier =
    graphicsLayer(
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
        alpha = alpha,
        shape = shape.toRemoteShape(),
        compositingStrategy = compositingStrategy.toRemoteCompositingStrategy(),
        cameraDistance = cameraDistance,
        renderEffect = renderEffect,
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
    public var shape: Shape

    /** The compositing strategy to use for the layer. */
    public var compositingStrategy: CompositingStrategy

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
    override var shape: Shape = RectangleShape
    override var compositingStrategy: CompositingStrategy = Auto
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
        shape = scope.shape.toRemoteShape(),
        compositingStrategy = scope.compositingStrategy.toRemoteCompositingStrategy(),
        cameraDistance = scope.cameraDistance,
        renderEffect = scope.renderEffect,
    )
}

internal fun Shape.toRemoteShape(): RemoteShape =
    when (this) {
        is RoundedCornerShape -> RemoteRoundedCornerShape(40f.rf)
        RectangleShape -> RemoteRectangleShape
        else -> {
            Log.w("GraphicsLayerModifier", "Unsupported Shape in graphicsLayer: $this")
            RemoteRectangleShape
        }
    }

private fun CompositingStrategy.toRemoteCompositingStrategy(): RemoteCompositingStrategy =
    when (this) {
        Auto -> RemoteCompositingStrategy.Auto
        Offscreen -> RemoteCompositingStrategy.Offscreen
        ModulateAlpha -> RemoteCompositingStrategy.ModulateAlpha
        else -> RemoteCompositingStrategy.Auto
    }
