/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.impl.impress

import android.view.Surface
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.ar.imp.view.View
import java.nio.FloatBuffer
import java.nio.IntBuffer

/** Interface for the JNI API for communicating with the Impress Split Engine instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ImpressApi {

    /**
     * Specifies how the Surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * Values here match values from androidx.media3.common.C.StereoMode
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        StereoMode.MONO,
        StereoMode.TOP_BOTTOM,
        StereoMode.SIDE_BY_SIDE,
        StereoMode.MULTIVIEW_LEFT_PRIMARY,
        StereoMode.MULTIVIEW_RIGHT_PRIMARY,
    )
    public annotation class StereoMode {
        public companion object {
            // Each eye will see the entire surface (no separation)
            public const val MONO: Int = 0
            // The [top, bottom] halves of the surface will map to [left, right] eyes
            public const val TOP_BOTTOM: Int = 1
            // The [left, right] halves of the surface will map to [left, right] eyes
            public const val SIDE_BY_SIDE: Int = 2
            // Multiview video, [primary, auxiliary] views will map to [left, right] eyes
            public const val MULTIVIEW_LEFT_PRIMARY: Int = 4
            // Multiview video, [primary, auxiliary] views will map to [right, left] eyes
            public const val MULTIVIEW_RIGHT_PRIMARY: Int = 5
        }
    }

    /**
     * Specifies the draw mode of the surface.
     *
     * Values here match values from imp::PrimitiveType
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(DrawMode.TRIANGLES, DrawMode.TRIANGLE_STRIP, DrawMode.TRIANGLE_FAN)
    public annotation class DrawMode {
        public companion object {
            public const val TRIANGLES: Int = 0
            public const val TRIANGLE_STRIP: Int = 1
            public const val TRIANGLE_FAN: Int = 2
        }
    }

    /**
     * Specifies the content security level of the surface.
     *
     * Values here match values from imp::ContentSecurityLevel
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ContentSecurityLevel.NONE, ContentSecurityLevel.PROTECTED)
    public annotation class ContentSecurityLevel {
        public companion object {
            // No secure content will be rendered on the surface.
            public const val NONE: Int = 0
            // Surface will be used to render secure content.
            public const val PROTECTED: Int = 1
        }
    }

    /**
     * Specifies the blending mode of the content.
     *
     * Values here match values from imp::MediaBlendingMode.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(MediaBlendingMode.OPAQUE, MediaBlendingMode.TRANSPARENT)
    public annotation class MediaBlendingMode {
        public companion object {
            // Content is alpha-blended with the background.
            public const val TRANSPARENT: Int = 0
            // Content is opaque and does not blend with the background.
            public const val OPAQUE: Int = 1
        }
    }

    /**
     * Specifies the color standard of the content.
     *
     * Values here match values from androidx.media3.common.C.ColorSpace. For the enum values,
     * please see:
     * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/media/java/android/media/MediaFormat.java
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ColorSpace.BT709,
        ColorSpace.BT601_PAL,
        ColorSpace.BT2020,
        ColorSpace.BT601_525,
        ColorSpace.DISPLAY_P3,
        ColorSpace.DCI_P3,
        ColorSpace.ADOBE_RGB,
    )
    public annotation class ColorSpace {
        public companion object {
            public const val BT709: Int = 1
            public const val BT601_PAL: Int = 2
            public const val BT2020: Int = 6
            // Additional standard values not supported by Exoplayer.
            // The enum values must match the values from
            // third_party/impress/core/media/media_color_space.h
            public const val BT601_525: Int = 0xf0
            public const val DISPLAY_P3: Int = 0xf1
            public const val DCI_P3: Int = 0xf2
            public const val ADOBE_RGB: Int = 0xf3
        }
    }

    /**
     * Specifies the transfer function of the content.
     *
     * Values here match values from androidx.media3.common.C.ColorTransfer. For the enum values
     * (except sRGB and Gamma 2.2), please see:
     * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/media/java/android/media/MediaFormat.java
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ColorTransfer.LINEAR,
        ColorTransfer.SRGB,
        ColorTransfer.SDR,
        ColorTransfer.GAMMA_2_2,
        ColorTransfer.ST2084,
        ColorTransfer.HLG,
    )
    public annotation class ColorTransfer {
        public companion object {
            public const val LINEAR: Int = 1
            public const val SRGB: Int = 2
            public const val SDR: Int = 3 // SMPTE170M
            public const val GAMMA_2_2: Int = 10
            public const val ST2084: Int = 6
            public const val HLG: Int = 7
        }
    }

    /**
     * Specifies the color range of the content.
     *
     * Values here match values from androidx.media3.common.C.ColorRange. For the enum values,
     * please see:
     * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/media/java/android/media/MediaFormat.java
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ColorRange.FULL, ColorRange.LIMITED)
    public annotation class ColorRange {
        public companion object {
            public const val FULL: Int = 1
            public const val LIMITED: Int = 2
        }
    }

    public companion object {
        /** Constant that represents an unknown/unspecified maximum content light level. */
        public const val MAX_CONTENT_LIGHT_LEVEL_UNKNOWN: Int = 0
    }

    /** This method initializes the Impress Split Engine instance. */
    public fun setup(view: View?)

    /** This method initializes the Impress Split Engine instance for test purposes. */
    @VisibleForTesting public fun setup(nativeTestViewHandle: Long)

    /** Called when the activity or fragment is resumed. */
    public fun onResume()

    /** Called when the activity or fragment is paused. */
    public fun onPause()

    /**
     * Returns the resource manager
     *
     * @throws IllegalStateException if the BindingsResourceManager is not initialized.
     */
    public fun getBindingsResourceManager(): BindingsResourceManager

    /** This method releases the asset pointer of a previously loaded image based lighting asset. */
    public fun releaseImageBasedLightingAsset(iblToken: Long)

    /**
     * This method loads an image based lighting asset from the assets folder and returns a token
     * that can be used to reference the asset in other JNI calls.
     */
    public suspend fun loadImageBasedLightingAsset(path: String): ExrImage

    /**
     * This method loads an image based lighting asset from a byte array and returns a token that
     * can be used to reference the asset in other JNI calls.
     */
    public suspend fun loadImageBasedLightingAsset(data: ByteArray, key: String): ExrImage

    /**
     * This method loads a glTF model from the local assets folder or a remote URL, and returns a
     * model token that can be used to reference the model in other JNI calls.
     */
    public suspend fun loadGltfAsset(path: String): GltfModel

    /**
     * This method loads a glTF model from a byte array and returns the model token that can be used
     * to reference the model in other JNI calls.
     */
    public suspend fun loadGltfAsset(data: ByteArray, key: String): GltfModel

    /** This method releases the asset pointer of a previously loaded glTF model. */
    // TODO(b/374216912) - Add support for cancellation of loading operations (GLTF, EXR, etc.)
    public fun releaseGltfAsset(gltfToken: Long)

    /**
     * This method instantiates a glTF model from a previously loaded model and returns an entity ID
     * corresponding to the Impress node associated with the model. Using this method will enable
     * the collider for the model.
     */
    public fun instanceGltfModel(gltfToken: Long): ImpressNode

    /**
     * This method instantiates a glTF model from a previously loaded model and returns an entity ID
     * corresponding to the Impress node associated with the model. It gives the ability to disable
     * the collider for the model.
     */
    public fun instanceGltfModel(gltfToken: Long, enableCollider: Boolean): ImpressNode

    /**
     * Toggle the collider of a glTF model.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param enableCollider If the glTF model should have a collider or not.
     */
    public fun setGltfModelColliderEnabled(impressNode: ImpressNode, enableCollider: Boolean)

    /**
     * Enable reform affordance for a glTF model.
     *
     * @param impressNode The object of Impress node for the instance of the glTF model.
     * @param enabled If the reform affordance should be added or removed.
     * @param systemMovable If the system should handle move input events.
     */
    public fun setGltfReformAffordanceEnabled(
        impressNode: ImpressNode,
        enabled: Boolean,
        systemMovable: Boolean,
    )

    /**
     * Starts an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param animationName A nullable String which contains a requested animation to play. If null
     *   is provided, this will attempt to play the first animation it finds
     * @param looping True if the animation should loop. Note that if the animation is looped, the
     *   returned Coroutine will never fire successfully.
     * @param speed The speed of the animation where 1.0 is the normal speed and negative values
     *   will play the animation in reverse.
     * @param startTime The start time of the animation in seconds.
     * @param channel The channel of the animation.
     * @return a Coroutine which fires when the animation stops. It will return an exception if the
     *   animation can't play.
     */
    public suspend fun animateGltfModelNew(
        impressNode: ImpressNode,
        animationName: String?,
        looping: Boolean,
        speed: Float,
        startTime: Float,
        channel: Int,
    ): Void?

    /**
     * Starts an animation on an instanced glTF model.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param animationName A nullable String which contains a requested animation to play. If null
     *   is provided, this will attempt to play the first animation it finds
     * @param looping True if the animation should loop. Note that if the animation is looped, the
     *   returned Coroutine will never fire successfully.
     * @return a Coroutine which fires when the animation stops. It will return an exception if the
     *   animation can't play.
     */
    // TODO: b/465818627 - Remove old animation APIs once all clients are migrated
    // to new animation system.
    public suspend fun animateGltfModel(
        impressNode: ImpressNode,
        animationName: String?,
        looping: Boolean,
    ): Void?

    /**
     * Stops an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param channel The channel of the animation.
     */
    public fun stopGltfModelAnimationNew(impressNode: ImpressNode, channel: Int)

    /**
     * Stops an animation on an instanced glTF model.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     */
    // TODO: b/465818627 - Remove old animation APIs once all clients are migrated
    // to new animation system.
    public fun stopGltfModelAnimation(impressNode: ImpressNode)

    /**
     * Toggles the playback of a glTF model's animation to pause or resume on a specific channel.
     *
     * @param impressNode The object of the Impress node for the instance of the GLTF
     * @param playing `true` to resume the animation, `false` to pause it.
     * @param channel The channel of the animation.
     */
    public fun toggleGltfModelAnimationNew(impressNode: ImpressNode, playing: Boolean, channel: Int)

    /**
     * Toggles the playback of a glTF model's animation to pause or resume.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param playing `true` to resume the animation, `false` to pause it.
     */
    // TODO: b/465818627 - Remove old animation APIs once all clients are migrated
    // to new animation system.
    public fun toggleGltfModelAnimation(impressNode: ImpressNode, playing: Boolean)

    /**
     * Sets the playback time of a glTF model's animation on a specific channel.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param playbackTime The playback time of the animation in seconds.
     * @param channel The channel of the animation.
     */
    public fun setGltfModelAnimationPlaybackTime(
        impressNode: ImpressNode,
        playbackTime: Float,
        channel: Int,
    )

    /**
     * Sets the speed of a glTF model's animation on a specific channel.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param speed The speed of the animation where 1.0 is the normal speed and negative values
     *   will play the animation in reverse.
     * @param channel The channel of the animation.
     */
    public fun setGltfModelAnimationSpeed(impressNode: ImpressNode, speed: Float, channel: Int)

    /**
     * Returns the number of animations on an instanced glTF model.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @return The number of animations on the model.
     */
    public fun getGltfModelAnimationCount(impressNode: ImpressNode): Int

    /**
     * Returns the name of the animation on an instanced glTF model if it exists.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param index The index of the animation as defined in the glTF file.
     * @return The name of the animation.
     */
    public fun getGltfModelAnimationName(impressNode: ImpressNode, index: Int): String?

    /**
     * Returns the duration of the animation on an instanced glTF model.
     *
     * @param impressNode The object of the Impress node for the instance of the glTF model.
     * @param index The index of the animation as defined in the glTF file.
     * @return The duration of the animation in seconds.
     */
    public fun getGltfModelAnimationDurationSeconds(impressNode: ImpressNode, index: Int): Float

    /** This method creates an Impress node and returns its impress node object. */
    public fun createImpressNode(): ImpressNode

    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model.
     *
     * The bounding box is defined in the model's local coordinate space, before any transformations
     * (like scaling) from the entity are applied. This default implementation returns a unit box
     * centered at the origin. The concrete implementation should query the underlying rendering
     * engine for the actual bounds.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the glTF model.
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    public fun getGltfModelBoundingBox(impressNode: ImpressNode): BoundingBox =
        BoundingBox.fromCenterAndHalfExtents(
            center = Vector3(0.5f, 0.5f, 0.5f),
            halfExtents = FloatSize3d(0.5f, 0.5f, 0.5f),
        )

    /** This method destroys an Impress node using its node object. */
    public fun destroyImpressNode(impressNode: ImpressNode)

    /** This method parents an Impress node to another using their respective node objects. */
    public fun setImpressNodeParent(impressNodeChild: ImpressNode, impressNodeParent: ImpressNode)

    /**
     * Returns the parent node of the given Impress node.
     *
     * @param impressNode The node for which we want to get the parent.
     * @return An ImpressNode representing the parent of the given node.
     */
    public fun getImpressNodeParent(impressNode: ImpressNode): ImpressNode

    /**
     * This method returns the number of child node of a given Impress node.
     *
     * @param impressNode The node for which we want to query the number of child nodes.
     * @return An Int for the amount of child nodes for the given Impress node.
     */
    public fun getImpressNodeChildCount(impressNode: ImpressNode): Int

    /**
     * This method returns the child node of an Impress node at a specific index.
     *
     * @param impressNode The parent Impress node.
     * @param childIndex The index (unique ID) of the child Impress node to get.
     * @return An ImpressNode for the child Impress node at that index.
     */
    public fun getImpressNodeChildAt(impressNode: ImpressNode, childIndex: Int): ImpressNode

    /**
     * This method returns the name of the Impress node. An empty string will be returned if the
     * node does not have a name.
     *
     * @param impressNode The node for which we want to get the name.
     * @return A String for the name of the node.
     */
    public fun getImpressNodeName(impressNode: ImpressNode): String

    /**
     * Sets the local transform (TRS) of an Impress node relative to its direct parent.
     *
     * @param impressNode The node for which we want to set the local transform.
     * @param transform The [Matrix4] representing the new local transform.
     */
    public fun setImpressNodeLocalTransform(impressNode: ImpressNode, transform: Matrix4)

    /**
     * Retrieves the local transform (TRS) of an Impress node relative to its direct parent.
     *
     * @param impressNode The node for which we want to get the local transform.
     * @return A [Matrix4] representing the local transform.
     */
    public fun getImpressNodeLocalTransform(impressNode: ImpressNode): Matrix4

    /**
     * Sets the transform (TRS) of an Impress node relative to another Impress node.
     *
     * @param impressNode The node for which we want to set the transform.
     * @param relativeNode The relative node to act as the coordinate space origin.
     * @param transform The [Matrix4] representing the new relative transform.
     */
    public fun setImpressNodeRelativeTransform(
        impressNode: ImpressNode,
        relativeNode: ImpressNode,
        transform: Matrix4,
    )

    /**
     * Retrieves the transform (TRS) of an Impress node relative to another Impress node.
     *
     * @param impressNode The node for which we want to get the relative transform.
     * @param relativeNode The relative node to act as the coordinate space origin.
     * @return A [Matrix4] representing the relative transform.
     */
    public fun getImpressNodeRelativeTransform(
        impressNode: ImpressNode,
        relativeNode: ImpressNode,
    ): Matrix4

    /**
     * This method creates an Impress node with a stereo panel and returns the node object. Note
     * that the StereoSurfaceEntity will not render anything until the canvas shape is set.
     * Furthermore, the surface cannot be used to render secure content.
     *
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode.
     * @return An int impress node ID which can be used for updating the surface later
     * @throws InvalidArgumentException if stereoMode is invalid.
     */
    // TODO - b/411225487: Remove this method.
    public fun createStereoSurface(@StereoMode stereoMode: Int): ImpressNode

    /**
     * This method creates an Impress node with a stereo panel and returns the node object. Note
     * that the StereoSurfaceEntity will not render anything until the canvas shape is set.
     *
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode.
     * @param contentSecurityLevel The [Int] contentSecurityLevel to apply. Must be a member of
     *   ContentSecurityLevel.
     * @return An int impress node ID which can be used for updating the surface later
     * @throws InvalidArgumentException if stereoMode or contentSecurityLevel are invalid.
     */
    public fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
    ): ImpressNode

    /**
     * This method creates an Impress node with a stereo panel and returns the node object. Note
     * that the StereoSurfaceEntity will not render anything until the canvas shape is set.
     *
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode.
     * @param contentSecurityLevel The [Int] contentSecurityLevel to apply. Must be a member of
     *   ContentSecurityLevel.
     * @param useSuperSampling This [Boolean] specifies if the super sampling filter is enabled when
     *   rendering the surface.
     * @return An int impress node ID which can be used for updating the surface later
     * @throws InvalidArgumentException if stereoMode or contentSecurityLevel are invalid.
     */
    public fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
        useSuperSampling: Boolean,
    ): ImpressNode

    /**
     * This method creates an Impress node with a stereo panel and returns the node object. Note
     * that the StereoSurfaceEntity will not render anything until the canvas shape is set.
     *
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode.
     * @param mediaBlendingMode The [Int] mediaBlendingMode to apply. Must be a member of
     *   MediaBlendingMode.
     * @param contentSecurityLevel The [Int] contentSecurityLevel to apply. Must be a member of
     *   ContentSecurityLevel.
     * @param useSuperSampling This [Boolean] specifies if the super sampling filter is enabled when
     *   rendering the surface.
     * @return An int impress node ID which can be used for updating the surface later
     * @throws InvalidArgumentException if stereoMode, mediaBlendingMode or contentSecurityLevel are
     *   invalid.
     */
    public fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @MediaBlendingMode mediaBlendingMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
        useSuperSampling: Boolean,
    ): ImpressNode

    /**
     * This method sets the Surface pixel dimensions for a StereoSurfaceEntity.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param width The width in pixels to set the buffer size for the Surface.
     * @param height The height in pixels to set the buffer size for the Surface.
     * @throws IllegalArgumentException if the width or height are not positive, or if the impress
     *   node does not host a StereoSurfaceEntity.
     */
    public fun setStereoSurfaceEntitySurfaceSize(impressNode: ImpressNode, width: Int, height: Int)

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress node object.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param width The width in local spatial units to set the quad to.
     * @param height The height in local spatial units to set the quad to.
     */
    public fun setStereoSurfaceEntityCanvasShapeQuad(
        impressNode: ImpressNode,
        width: Float,
        height: Float,
        cornerRadius: Float,
    )

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress node object.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param radius The radius in local spatial units to set the sphere to.
     */
    public fun setStereoSurfaceEntityCanvasShapeSphere(impressNode: ImpressNode, radius: Float)

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress node object.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param radius The radius in local spatial units of the hemisphere.
     */
    public fun setStereoSurfaceEntityCanvasShapeHemisphere(impressNode: ImpressNode, radius: Float)

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress node object.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param leftPositions The positions of the left eye mesh.
     * @param leftTexCoords The texture coordinates of the left eye mesh.
     * @param leftIndices The indices of the left eye mesh.
     * @param rightPositions The positions of the right eye mesh.
     * @param rightTexCoords The texture coordinates of the right eye mesh.
     * @param rightIndices The indices of the right eye mesh.
     * @param drawMode The draw mode of the mesh.
     * @throws IllegalArgumentException if the number of positions and texcoords do not correspond
     *   to the same number of vertices for either eye (i.e. `positions.capacity() / 3 !=
     *   texCoords.capacity() / 2`), or if values in the indices are out of bounds (greater than or
     *   equal to the number of vertices).
     */
    public fun setStereoSurfaceEntityCanvasShapeCustomMesh(
        impressNode: ImpressNode,
        leftPositions: FloatBuffer,
        leftTexCoords: FloatBuffer,
        leftIndices: IntBuffer?,
        rightPositions: FloatBuffer?,
        rightTexCoords: FloatBuffer?,
        rightIndices: IntBuffer?,
        @DrawMode drawMode: Int,
    )

    /**
     * Dynamically enables or disables the collider for the StereoSurfaceEntity.
     *
     * The shape of the collider is determined by the canvas shape:
     * - Quad -> BoxCollider
     * - Sphere -> SphereCollider
     * - Hemisphere -> MeshCollider
     *
     * Enabling the collider will cause the cursor to be visible when the User hovers over the
     * Entity and will allow for the Entity to receive input events. It will also block input for
     * objects behind it.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param enableCollider True to enable the collider, false to disable it.
     */
    public fun setStereoSurfaceEntityColliderEnabled(
        impressNode: ImpressNode,
        enableCollider: Boolean,
    )

    /**
     * Updates the StereoMode for an impress node hosting a StereoSurface.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode
     * @throws InvalidArgumentException if stereoMode is invalid.
     */
    public fun setStereoModeForStereoSurface(
        panelImpressNode: ImpressNode,
        @StereoMode stereoMode: Int,
    )

    /**
     * Updates the blending mode for an impress node hosting a StereoSurface.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param blendingMode The [Int] blending mode to apply. Must be a member of MediaBlendingMode.
     * @throws IllegalArgumentException if blendingMode is invalid.
     */
    public fun setBlendingModeForStereoSurfaceEntity(
        panelImpressNode: ImpressNode,
        @MediaBlendingMode blendingMode: Int,
    )

    /**
     * Updates the color information for an impress node hosting a StereoSurface.
     *
     * @param stereoSurfaceNode The Impress node which hosts the StereoSurface to be updated.
     * @param colorSpace The [Int] color standard to apply. Must be a member of ColorSpace.
     * @param colorTransfer The [Int] color transfer function to apply. Must be a member of
     *   ColorTransfer.
     * @param colorRange The [Int] color range to apply. Must be a member of ColorRange.
     * @param maxLuminance The maximum luminance (Max Content Light Level - maxCLL) of the content
     *   in nits. This value should be within the range [1, 65535]. Values outside this range are
     *   considered invalid; smaller values will be ignored, and larger values will be clipped to
     *   65535 in the backend. Use [MAX_CONTENT_LIGHT_LEVEL_UNKNOWN] if this information is not
     *   available.
     */
    public fun setContentColorMetadataForStereoSurface(
        stereoSurfaceNode: ImpressNode,
        @ColorSpace colorSpace: Int,
        @ColorTransfer colorTransfer: Int,
        @ColorRange colorRange: Int,
        maxLuminance: Int,
    )

    /**
     * Resets the color information for an impress node hosting a StereoSurface. This will cause the
     * system to perform best-effort color transformations.
     *
     * @param stereoSurfaceNode The Impress node which hosts the StereoSurface to be updated.
     */
    public fun resetContentColorMetadataForStereoSurface(stereoSurfaceNode: ImpressNode)

    /**
     * Updates the radius of the (alpha) feathered edges for an Impress node hosting a
     * StereoSurface.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param radiusX The radius of the left/right feathering.
     * @param radiusY The radius of the top/bottom feathering.
     */
    public fun setFeatherRadiusForStereoSurface(
        panelImpressNode: ImpressNode,
        radiusX: Float,
        radiusY: Float,
    )

    /**
     * Sets the primary alpha mask for a stereo surface. The alpha mask will be composited into the
     * alpha channel of the surface. If null or empty, the alpha mask will be disabled.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param alphaMask The primary alpha mask texture
     */
    public fun setPrimaryAlphaMaskForStereoSurface(panelImpressNode: ImpressNode, alphaMask: Long)

    /**
     * Sets the auxiliary alpha mask for a stereo surface. The alpha mask will be composited into
     * the alpha channel of the surface if an interleaved video is being rendered.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param alphaMask The auxiliary alpha mask texture. Only used with interleaved video formats.
     *   If null or empty, the alpha mask will be disabled.
     */
    public fun setAuxiliaryAlphaMaskForStereoSurface(panelImpressNode: ImpressNode, alphaMask: Long)

    /**
     * Retrieve the android surface for this stereo panel
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @return A Surface backed by an imp::AndroidExternalTextureSurface
     */
    public fun getSurfaceFromStereoSurface(panelImpressNode: ImpressNode): Surface

    /**
     * This method loads a local texture from the assets folder or a remote texture from a URL and
     * returns the texture token that can be used to reference the texture in other JNI calls.
     *
     * @param path The name of the texture file to load or the URL of the remote texture.
     * @return The loaded texture.
     */
    public suspend fun loadTexture(path: String): Texture

    /**
     * This method borrows the reflection texture from the currently set environment IBL.
     *
     * @return A texture that can be used to reference the texture in other JNI calls.
     */
    public fun borrowReflectionTexture(): Texture

    /**
     * This method borrows the reflection texture from the given IBL.
     *
     * @return A texture that can be used to reference the texture in other JNI calls.
     */
    public fun getReflectionTextureFromIbl(iblToken: Long): Texture

    /**
     * This method creates a water material and returns the material native handle that can be used
     * to reference the water material in other JNI calls.
     *
     * @param isAlphaMapVersion True if the water material should be the alpha map version.
     * @return A WaterMaterial backed by an imp::WaterMaterial. The WaterMaterial can be destroyed
     *   by passing it to destroyNativeObject.
     */
    public suspend fun createWaterMaterial(isAlphaMapVersion: Boolean): WaterMaterial

    /**
     * This method sets the reflection map for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param reflectionMap The native handle of the texture to be used as the reflection map.
     * @param sampler The sampler used for the reflection map.
     */
    public fun setReflectionMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        reflectionMap: Long,
        sampler: TextureSampler,
    )

    /**
     * This method sets the normal map for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param normalMap The native handle of the texture to be used as the normal map.
     * @param sampler The sampler used for the normal map.
     */
    public fun setNormalMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        normalMap: Long,
        sampler: TextureSampler,
    )

    /**
     * This method sets the normal tiling for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param normalTiling The tiling to use for the normal map.
     */
    public fun setNormalTilingOnWaterMaterial(nativeWaterMaterial: Long, normalTiling: Float)

    /**
     * This method sets the normal speed for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param normalSpeed The speed to use for the normal map.
     */
    public fun setNormalSpeedOnWaterMaterial(nativeWaterMaterial: Long, normalSpeed: Float)

    /**
     * This method sets the alpha step multiplier for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param alphaStepMultiplier The alpha step multiplier to use for the water material.
     */
    public fun setAlphaStepMultiplierOnWaterMaterial(
        nativeWaterMaterial: Long,
        alphaStepMultiplier: Float,
    )

    /**
     * This method sets the alpha map for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param alphaMap The native handle of the texture to be used as the alpha map.
     * @param sampler The sampler used for the alpha map.
     */
    public fun setAlphaMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        alphaMap: Long,
        sampler: TextureSampler,
    )

    /**
     * This method sets the normal z for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param normalZ The normal z to use for the water material.
     */
    public fun setNormalZOnWaterMaterial(nativeWaterMaterial: Long, normalZ: Float)

    /**
     * This method sets the normal boundary for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param boundary The normal boundary to use for the water material.
     */
    public fun setNormalBoundaryOnWaterMaterial(nativeWaterMaterial: Long, boundary: Float)

    /**
     * This method sets the alpha step U for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param x The x coordinate of the alpha step U.
     * @param y The y coordinate of the alpha step U.
     * @param z The z coordinate of the alpha step U.
     * @param w The w coordinate of the alpha step U.
     */
    public fun setAlphaStepUOnWaterMaterial(
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    )

    /**
     * This method sets the alpha step V for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param x The x coordinate of the alpha step V.
     * @param y The y coordinate of the alpha step V.
     * @param z The z coordinate of the alpha step V.
     * @param w The w coordinate of the alpha step V.
     */
    public fun setAlphaStepVOnWaterMaterial(
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    )

    /**
     * This method creates a Khronos PBR material and returns a material native handle that can be
     * used to reference the Khronos PBR material in other JNI calls.
     *
     * @param spec The Khronos PBR material spec to use for the material.
     * @return A Khronos PBR material.
     * @throws IllegalArgumentException if the Khronos PBR material spec is invalid.
     */
    public suspend fun createKhronosPbrMaterial(spec: KhronosPbrMaterialSpec): KhronosPbrMaterial

    /**
     * Sets the base color texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param baseColorTexture The native handle of the base color texture.
     * @param sampler The sampler used for the base color texture.
     */
    public fun setBaseColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        baseColorTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the base color texture.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setBaseColorUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    )

    /**
     * Sets the base color factors for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setBaseColorFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    )

    /**
     * Sets the metallic-roughness texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param metallicRoughnessTexture The native handle of the metallic-roughness texture.
     * @param sampler The sampler used for the metallic roughness texture.
     */
    public fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        metallicRoughnessTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the metallic-roughness texture.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    )

    /**
     * Sets the metallic factor for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param factor The metallic factor.
     */
    public fun setMetallicFactorOnKhronosPbrMaterial(nativeKhronosPbrMaterial: Long, factor: Float)

    /**
     * Sets the roughness factor for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param factor The roughness factor.
     */
    public fun setRoughnessFactorOnKhronosPbrMaterial(nativeKhronosPbrMaterial: Long, factor: Float)

    /**
     * Sets the normal map texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param normalTexture The native handle of the normal map texture.
     * @param sampler The sampler used for the normal map texture.
     */
    public fun setNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        normalTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the normal map texture.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setNormalUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    )

    /**
     * Sets the factor of the normal map effect.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param factor The factor of the normal map.
     */
    public fun setNormalFactorOnKhronosPbrMaterial(nativeKhronosPbrMaterial: Long, factor: Float)

    /**
     * Sets the ambient occlusion texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param ambientOcclusionTexture The native handle of the ambient occlusion texture.
     * @param sampler The sampler used for the ambient occlusion texture.
     */
    public fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ambientOcclusionTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the ambient occlusion texture.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    )

    /**
     * Sets the factor of the ambient occlusion effect.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param factor The factor of the ambient occlusion.
     */
    public fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    )

    /**
     * Sets the emissive texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param emissiveTexture The native handle of the emissive texture.
     * @param sampler The sampler used for the emissive texture.
     */
    public fun setEmissiveTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        emissiveTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the emissive texture.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setEmissiveUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    )

    /**
     * Sets the emissive color factors for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setEmissiveFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    )

    /**
     * Sets the clearcoat texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param clearcoatTexture The native handle of the clearcoat texture.
     * @param sampler The sampler used for the clearcoat texture.
     */
    public fun setClearcoatTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the clearcoat normal texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param clearcoatNormalTexture The native handle of the clearcoat normal texture.
     * @param sampler The sampler used for the clearcoat normal texture.
     */
    public fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatNormalTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the clearcoat roughness texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param clearcoatRoughnessTexture The native handle of the clearcoat roughness texture.
     * @param sampler The sampler used for the clearcoat roughness texture.
     */
    public fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatRoughnessTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the clearcoat factor for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param intensity The intensity of the clearcoat.
     * @param roughness The roughness of the clearcoat.
     * @param normal The normal of the clearcoat.
     */
    public fun setClearcoatFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        intensity: Float,
        roughness: Float,
        normal: Float,
    )

    /**
     * Sets the sheen color texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param sheenColorTexture The native handle of the sheen color texture.
     * @param sampler The sampler used for the sheen color texture.
     */
    public fun setSheenColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenColorTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the sheen color factors for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setSheenColorFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    )

    /**
     * Sets the sheen roughness texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param sheenRoughnessTexture The native handle of the sheen roughness texture.
     * @param sampler The sampler used for the sheen roughness texture.
     */
    public fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenRoughnessTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the sheen roughness factor for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param factor The sheen roughness factor.
     */
    public fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    )

    /**
     * Sets the transmission texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param transmissionTexture The native handle of the transmission texture.
     * @param sampler The sampler used for the transmission texture.
     */
    public fun setTransmissionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        transmissionTexture: Long,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the transmission texture.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     */
    public fun setTransmissionUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    )

    /**
     * Sets the transmission factor for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param factor The transmission factor.
     */
    public fun setTransmissionFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    )

    /**
     * Sets the index of refraction for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param indexOfRefraction The index of refraction.
     */
    public fun setIndexOfRefractionOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        indexOfRefraction: Float,
    )

    /**
     * Sets the alpha cutoff for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param alphaCutoff The alpha cutoff value.
     */
    public fun setAlphaCutoffOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        alphaCutoff: Float,
    )

    /**
     * This method destroys a native Impress object using its native handle.
     *
     * @param nativeHandle The native handle of the native Impress object to be destroyed.
     */
    public fun destroyNativeObject(nativeHandle: Long)

    /**
     * Schedules reskinning of a glTF model. This should be called after modifying node transforms
     * that affect skinned meshes.
     *
     * @param impressNode The root node of the glTF model or the specific node to reskin.
     */
    public fun scheduleGltfReskinning(impressNode: ImpressNode)

    /**
     * Sets a material override for a specific primitive of a specific glTF model node.
     *
     * @param impressNode The specific Impress node (retrieved via introspection) to override.
     * @param nativeMaterial The native handle of the material to be used as the override.
     * @param primitiveIndex The zero-based index of the primitive to override within the mesh.
     */
    public fun setGltfModelNodeMaterialOverride(
        impressNode: ImpressNode,
        nativeMaterial: Long,
        primitiveIndex: Int,
    )

    /**
     * Clears a material override for a specific primitive of a specific glTF model node.
     *
     * @param impressNode The specific Impress node (retrieved via introspection) to clear.
     * @param primitiveIndex The zero-based index of the primitive to clear within the mesh.
     */
    public fun clearGltfModelNodeMaterialOverride(impressNode: ImpressNode, primitiveIndex: Int)

    /**
     * This method sets the IBL asset preference of the client to be set by the system.
     *
     * @param iblToken The native handle of the IBL asset to be used by the system.
     * @throws NotFoundException if iblToken is not a previously loaded IBL asset.
     * @throws IllegalStateException if the SplitEngineSerializer is not valid.
     */
    public fun setPreferredEnvironmentLight(iblToken: Long)

    /**
     * This method clears the IBL asset preference of the client to be set by the system.
     *
     * @throws IllegalStateException if the SplitEngineSerializer is not valid.
     */
    public fun clearPreferredEnvironmentIblAsset()

    /**
     * This method disposes all of the resources associated with the Impress Split Engine instance.
     *
     * This should be called when the Impress Split Engine instance is no longer needed.
     */
    public fun disposeAllResources()
}
