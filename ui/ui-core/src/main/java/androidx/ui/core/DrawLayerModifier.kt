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

package androidx.ui.core

import androidx.annotation.FloatRange
import androidx.ui.graphics.Shape

/**
 * A set of properties that can be modified in a [DrawLayerModifier].
 *
 * @sample androidx.ui.core.samples.ChangeOpacity
 */
interface DrawLayerProperties {
    /**
     * The horizontal scale of the drawn area. This would typically default to `1`.
     */
    val scaleX: Float get() = 1f

    /**
     * The vertical scale of the drawn area. This would typically default to `1`.
     */
    val scaleY: Float get() = 1f

    /**
     * The alpha of the drawn area. Setting this to something other than `1`
     * will cause the drawn contents to be translucent and setting it to `0` will
     * cause it to be fully invisible.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val alpha: Float get() = 1f

    /**
     * Sets the Z coordinate of the layer in pixels. With [outlineShape] set, this will cause
     * a shadow. Varying the [elevation] can also change the order in which layers are drawn.
     */
    @get:FloatRange(from = 0.0)
    val elevation: Float get() = 0f

    /**
     * The rotation of the contents around the horizontal axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationX: Float get() = 0f

    /**
     * The rotation of the contents around the vertical axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationY: Float get() = 0f

    /**
     * The rotation of the contents around the Z axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationZ: Float get() = 0f

    /**
     * The [Shape] of the layer. When [elevation] is non-zero and [outlineShape] is non-null,
     * a shadow is produced. When [clipToOutline] is `true` and [outlineShape] is non-null, the
     * contents will be clipped to the outline.
     */
    val outlineShape: Shape? get() = null

    /**
     * Set to `true` to clip the content to the size of the layer or `false` to allow
     * drawing outside of the layer's bounds. This a convenient way to clip to the bounding
     * rectangle. When [clipToOutline] is `true` the contents are clipped by both the
     * bounding rectangle and the [outlineShape].
     *
     * @see clipToOutline
     */
    val clipToBounds: Boolean get() = true

    /**
     * Clips the content to the [outlineShape]. If [outlineShape] is null, no clipping will occur.
     * When both [clipToBounds] and [clipToOutline] are `true`, the content will be clipped by
     * both the bounding rectangle and the [outlineShape].
     */
    val clipToOutline: Boolean get() = true
}

/**
 * A [Modifier.Element] that makes content draw into a layer, allowing easily changing
 * [DrawLayerProperties] for the drawn contents.
 *
 * @sample androidx.ui.core.samples.AnimateFadeIn
 */
interface DrawLayerModifier : Modifier.Element {
    val properties: DrawLayerProperties
}

private data class SimpleDrawLayerModifier(
    override val properties: DrawLayerProperties
) : DrawLayerModifier

/**
 * Create a [DrawLayerModifier] with a given [DrawLayerProperties].
 */
fun drawLayer(drawLayerProperties: DrawLayerProperties): Modifier =
    SimpleDrawLayerModifier(drawLayerProperties)

/**
 * Create a [DrawLayerModifier] with fixed properties.
 *
 * @sample androidx.ui.core.samples.ChangeOpacity
 *
 * @param scaleX [DrawLayerProperties.scaleX]
 * @param scaleY [DrawLayerProperties.scaleY]
 * @param alpha [DrawLayerProperties.alpha]
 * @param elevation [DrawLayerProperties.elevation]
 * @param rotationX [DrawLayerProperties.rotationX]
 * @param rotationY [DrawLayerProperties.rotationY]
 * @param rotationZ [DrawLayerProperties.rotationZ]
 * @param outlineShape [DrawLayerProperties.outlineShape]
 * @param clipToBounds [DrawLayerProperties.clipToBounds]
 * @param clipToOutline [DrawLayerProperties.clipToOutline]
 */
fun drawLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    elevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    outlineShape: Shape? = null,
    clipToBounds: Boolean = true,
    clipToOutline: Boolean = true
): Modifier = SimpleDrawLayerModifier(
    SimpleDrawLayerProperties(
        scaleX,
        scaleY,
        alpha,
        elevation,
        rotationX,
        rotationY,
        rotationZ,
        outlineShape,
        clipToBounds,
        clipToOutline
    )
)

private data class SimpleDrawLayerProperties(
    override val scaleX: Float,
    override val scaleY: Float,
    override val alpha: Float,
    override val elevation: Float,
    override val rotationX: Float,
    override val rotationY: Float,
    override val rotationZ: Float,
    override val outlineShape: Shape?,
    override val clipToBounds: Boolean,
    override val clipToOutline: Boolean
) : DrawLayerProperties
