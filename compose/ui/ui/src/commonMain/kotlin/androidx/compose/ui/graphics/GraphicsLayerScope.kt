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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.layout.PlacementScopeMarker
import androidx.compose.ui.unit.Density

/**
 * Default camera distance for all layers
 */
const val DefaultCameraDistance = 8.0f

/**
 * Default ambient shadow color for all layers.
 */
val DefaultShadowColor = Color.Black

/**
 * A scope which can be used to define the effects to apply for the content, such as scaling
 * ([scaleX], [scaleY]), rotation ([rotationX], [rotationY], [rotationZ]), opacity ([alpha]), shadow
 * ([shadowElevation], [shape]), and clipping ([clip], [shape]).
 */
@JvmDefaultWithCompatibility
@PlacementScopeMarker
interface GraphicsLayerScope : Density {
    /**
     * The horizontal scale of the drawn area. Default value is `1`.
     */
    var scaleX: Float

    /**
     * The vertical scale of the drawn area. Default value is `1`.
     */
    var scaleY: Float

    /**
     * The alpha of the drawn area. Setting this to something other than `1`
     * will cause the drawn contents to be translucent and setting it to `0` will
     * cause it to be fully invisible. Default value is `1` and the range is between
     * `0` and `1`.
     */
    /*@setparam:FloatRange(from = 0.0, to = 1.0)*/
    var alpha: Float

    /**
     * Horizontal pixel offset of the layer relative to its left bound. Default value is `0`.
     */
    var translationX: Float

    /**
     * Vertical pixel offset of the layer relative to its top bound. Default value is `0`
     */
    var translationY: Float

    /**
     * Sets the elevation for the shadow in pixels. With the [shadowElevation] > 0f and
     * [shape] set, a shadow is produced. Default value is `0` and the value must not be
     * negative.
     *
     * Note that if you provide a non-zero [shadowElevation] and if the passed [shape] is concave the
     * shadow will not be drawn on Android versions less than 10.
     */
    /*@setparam:FloatRange(from = 0.0)*/
    var shadowElevation: Float

    /**
     * Sets the color of the ambient shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final ambient shadow is a function of the shadow caster height, the
     * alpha channel of the [ambientShadowColor] (typically opaque), and the
     * [android.R.attr.ambientShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    // Add default getter/setter implementation to avoid breaking api changes due to abstract
    // method additions. ReusableGraphicsLayer is the only implementation anyway.
    var ambientShadowColor: Color
        get() = DefaultShadowColor
        // Keep the parameter name so current.txt maintains it for named parameter usage
        @Suppress("UNUSED_PARAMETER")
        set(ambientShadowColor) {}

    /**
     * Sets the color of the spot shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final spot shadow is a function of the shadow caster height, the
     * alpha channel of the [spotShadowColor] (typically opaque), and the
     * [android.R.attr.spotShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    // Add default getter/setter implementation to avoid breaking api changes due to abstract
    // method additions. ReusableGraphicsLayer is the only implementation anyway.
    var spotShadowColor: Color
        get() = DefaultShadowColor
        // Keep the parameter name so current.txt maintains it for named parameter usage
        @Suppress("UNUSED_PARAMETER")
        set(spotShadowColor) {}

    /**
     * The rotation, in degrees, of the contents around the horizontal axis in degrees. Default
     * value is `0`.
     */
    var rotationX: Float

    /**
     * The rotation, in degrees, of the contents around the vertical axis in degrees. Default
     * value is `0`.
     */
    var rotationY: Float

    /**
     * The rotation, in degrees, of the contents around the Z axis in degrees. Default value is
     * `0`.
     */
    var rotationZ: Float

    /**
     * Sets the distance along the Z axis (orthogonal to the X/Y plane on which
     * layers are drawn) from the camera to this layer. The camera's distance
     * affects 3D transformations, for instance rotations around the X and Y
     * axis. If the rotationX or rotationY properties are changed and this view is
     * large (more than half the size of the screen), it is recommended to always
     * use a camera distance that's greater than the height (X axis rotation) or
     * the width (Y axis rotation) of this view.
     *
     * The distance of the camera from the drawing plane can have an affect on the
     * perspective distortion of the layer when it is rotated around the x or y axis.
     * For example, a large distance will result in a large viewing angle, and there
     * will not be much perspective distortion of the view as it rotates. A short
     * distance may cause much more perspective distortion upon rotation, and can
     * also result in some drawing artifacts if the rotated view ends up partially
     * behind the camera (which is why the recommendation is to use a distance at
     * least as far as the size of the view, if the view is to be rotated.)
     *
     * The distance is expressed in pixels and must always be positive.
     * Default value is [DefaultCameraDistance]
     */
    /*@setparam:FloatRange(from = 0.0)*/
    var cameraDistance: Float

    /**
     * Offset percentage along the x and y axis for which contents are rotated and scaled.
     * The default value of 0.5f, 0.5f indicates the pivot point will be at the midpoint of the
     * left and right as well as the top and bottom bounds of the layer.
     * Default value is [TransformOrigin.Center]
     */
    var transformOrigin: TransformOrigin

    /**
     * The [Shape] of the layer. When [shadowElevation] is non-zero a shadow is produced using
     * this [shape]. When [clip] is `true` contents will be clipped to this [shape].
     * When clipping, the content will be redrawn when the [shape] changes.
     * Default value is [RectangleShape]
     */
    var shape: Shape

    /**
     * Set to `true` to clip the content to the [shape].
     * Default value is `false`
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    var clip: Boolean

    /**
     * Configure the [RenderEffect] to apply to this [GraphicsLayerScope].
     * This will apply a visual effect to the results of the [GraphicsLayerScope] before it is
     * drawn. For example if [BlurEffect] is provided, the contents will be drawn in a separate
     * layer, then this layer will be blurred when this [GraphicsLayerScope] is drawn.
     *
     * Note this parameter is only supported on Android 12
     * and above. Attempts to use this Modifier on older Android versions will be ignored.
     */
    var renderEffect: RenderEffect?
        get() = null
        set(_) {}

    /**
     * Determines the [CompositingStrategy] used to render the contents of this graphicsLayer
     * into an offscreen buffer first before rendering to the destination
     */
    var compositingStrategy: CompositingStrategy
        get() = CompositingStrategy.Auto
        // Keep the parameter name so current.txt maintains it for named parameter usage
        @Suppress("UNUSED_PARAMETER")
        set(compositingStrategy) {}

    /**
     * [Size] of the graphicsLayer represented in pixels. Drawing commands can extend beyond
     * the size specified, however, if the graphicsLayer is promoted to an offscreen rasterization
     * layer, any content rendered outside of the specified size will be clipped.
     */
    val size: Size
        get() = Size.Unspecified
}

/**
 * Creates simple [GraphicsLayerScope].
 */
fun GraphicsLayerScope(): GraphicsLayerScope = ReusableGraphicsLayerScope()

internal object Fields {
    const val ScaleX: Int = 0b1 shl 0
    const val ScaleY: Int = 0b1 shl 1
    const val Alpha: Int = 0b1 shl 2
    const val TranslationX: Int = 0b1 shl 3
    const val TranslationY: Int = 0b1 shl 4
    const val ShadowElevation: Int = 0b1 shl 5
    const val AmbientShadowColor: Int = 0b1 shl 6
    const val SpotShadowColor: Int = 0b1 shl 7
    const val RotationX: Int = 0b1 shl 8
    const val RotationY: Int = 0b1 shl 9
    const val RotationZ: Int = 0b1 shl 10
    const val CameraDistance: Int = 0b1 shl 11
    const val TransformOrigin: Int = 0b1 shl 12
    const val Shape: Int = 0b1 shl 13
    const val Clip: Int = 0b1 shl 14
    const val CompositingStrategy: Int = 0b1 shl 15
    const val RenderEffect: Int = 0b1 shl 17

    const val MatrixAffectingFields = ScaleX or
        ScaleY or
        TranslationX or
        TranslationY or
        TransformOrigin or
        RotationX or
        RotationY or
        RotationZ or
        CameraDistance
}

internal class ReusableGraphicsLayerScope : GraphicsLayerScope {
    internal var mutatedFields: Int = 0

    override var scaleX: Float = 1f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.ScaleX
                field = value
            }
        }
    override var scaleY: Float = 1f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.ScaleY
                field = value
            }
        }
    override var alpha: Float = 1f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.Alpha
                field = value
            }
        }
    override var translationX: Float = 0f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.TranslationX
                field = value
            }
        }
    override var translationY: Float = 0f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.TranslationY
                field = value
            }
        }
    override var shadowElevation: Float = 0f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.ShadowElevation
                field = value
            }
        }
    override var ambientShadowColor: Color = DefaultShadowColor
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.AmbientShadowColor
                field = value
            }
        }
    override var spotShadowColor: Color = DefaultShadowColor
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.SpotShadowColor
                field = value
            }
        }
    override var rotationX: Float = 0f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.RotationX
                field = value
            }
        }
    override var rotationY: Float = 0f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.RotationY
                field = value
            }
        }
    override var rotationZ: Float = 0f
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.RotationZ
                field = value
            }
        }
    override var cameraDistance: Float = DefaultCameraDistance
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.CameraDistance
                field = value
            }
        }
    override var transformOrigin: TransformOrigin = TransformOrigin.Center
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.TransformOrigin
                field = value
            }
        }
    override var shape: Shape = RectangleShape
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.Shape
                field = value
            }
        }
    override var clip: Boolean = false
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.Clip
                field = value
            }
        }
    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.CompositingStrategy
                field = value
            }
        }
    override var size: Size = Size.Unspecified

    internal var graphicsDensity: Density = Density(1.0f)

    override val density: Float
        get() = graphicsDensity.density

    override val fontScale: Float
        get() = graphicsDensity.fontScale

    override var renderEffect: RenderEffect? = null
        set(value) {
            if (field != value) {
                mutatedFields = mutatedFields or Fields.RenderEffect
                field = value
            }
        }

    fun reset() {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        translationX = 0f
        translationY = 0f
        shadowElevation = 0f
        ambientShadowColor = DefaultShadowColor
        spotShadowColor = DefaultShadowColor
        rotationX = 0f
        rotationY = 0f
        rotationZ = 0f
        cameraDistance = DefaultCameraDistance
        transformOrigin = TransformOrigin.Center
        shape = RectangleShape
        clip = false
        renderEffect = null
        compositingStrategy = CompositingStrategy.Auto
        size = Size.Unspecified
        // mutatedFields should be reset last as all the setters above modify it.
        mutatedFields = 0
    }
}
