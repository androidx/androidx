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
import androidx.xr.runtime.internal.KhronosPbrMaterialSpec
import androidx.xr.runtime.internal.TextureSampler
import com.google.ar.imp.view.View
import com.google.common.util.concurrent.ListenableFuture

/** Interface for the JNI API for communicating with the Impress Split Engine instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
     * Specifies the color standard of the content.
     *
     * Values here match values from androidx.media3.common.C.ColorSpace For the enum values, please
     * see:
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
     * Values here match values from androidx.media3.common.C.ColorTransfer For the enum values
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
     * Values here match values from androidx.media3.common.C.ColorRange For the enum values, please
     * see:
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
    public fun setup(view: View)

    /** Called when the activity or fragment is resumed. */
    public fun onResume()

    /** Called when the activity or fragment is paused. */
    public fun onPause()

    /** This method releases the asset pointer of a previously loaded image based lighting asset. */
    public fun releaseImageBasedLightingAsset(iblToken: Long)

    /**
     * This method loads an image based lighting asset from the assets folder and returns a future
     * with a token that can be used to reference the asset in other JNI calls.
     */
    public fun loadImageBasedLightingAsset(path: String): ListenableFuture<Long>

    /**
     * This method loads an image based lighting asset from a byte array and returns a future with a
     * token that can be used to reference the asset in other JNI calls.
     */
    public fun loadImageBasedLightingAsset(data: ByteArray, key: String): ListenableFuture<Long>

    /**
     * This method loads a glTF model from the local assets folder or a remote URL, and returns a
     * future with the model token that can be used to reference the model in other JNI calls.
     */
    public fun loadGltfAsset(path: String): ListenableFuture<Long>

    /**
     * This method loads a glTF model from a byte array and returns a future with the model token
     * that can be used to reference the model in other JNI calls.
     */
    // TODO(b/397500220): Add an accessor which gets the model token from a name.
    public fun loadGltfAsset(data: ByteArray, key: String): ListenableFuture<Long>

    /** This method releases the asset pointer of a previously loaded glTF model. */
    // TODO(b/374216912) - Add support for cancellation of loading operations (GLTF, EXR, etc.)
    public fun releaseGltfAsset(gltfToken: Long)

    /**
     * This method instantiates a glTF model from a previously loaded model and returns an entity ID
     * corresponding to the Impress node associated with the model. Using this method will enable
     * the collider for the model.
     */
    public fun instanceGltfModel(gltfToken: Long): Int

    /**
     * This method instantiates a glTF model from a previously loaded model and returns an entity ID
     * corresponding to the Impress node associated with the model. It gives the ability to disable
     * the collider for the model.
     */
    public fun instanceGltfModel(gltfToken: Long, enableCollider: Boolean): Int

    /**
     * Toggle the collider of a glTF model.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the glTF model.
     * @param enableCollider If the glTF model should have a collider or not.
     */
    public fun setGltfModelColliderEnabled(impressNode: Int, enableCollider: Boolean)

    /**
     * Starts an animation on an instanced GLTFModel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param animationName A nullable String which contains a requested animation to play. If null
     *   is provided, this will attempt to play the first animation it finds
     * @param looping True if the animation should loop. Note that if the animation is looped, the
     *   returned Future will never fire successfully.
     * @return a ListenableFuture which fires when the animation stops. It will return an exception
     *   if the animation can't play.
     */
    // TODO: b/362829319 - Remove CompletableFuture from SE integration.
    public fun animateGltfModel(
        impressNode: Int,
        animationName: String?,
        looping: Boolean,
    ): ListenableFuture<Void?>

    /**
     * Stops an animation on an instanced GLTFModel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     */
    public fun stopGltfModelAnimation(impressNode: Int)

    /** This method creates an Impress node and returns its entity ID. */
    public fun createImpressNode(): Int

    /** This method destroys an Impress node using its entity ID. */
    public fun destroyImpressNode(impressNode: Int)

    /** This method parents an Impress node to another using their respective entity IDs. */
    public fun setImpressNodeParent(impressNodeChild: Int, impressNodeParent: Int)

    /**
     * This method creates an Impress node with a stereo panel and returns the entity ID. Note that
     * the StereoSurfaceEntity will not be render anything until the canvas shape is set.
     * Furthermore, the surface cannot be used to render secure content.
     *
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode.
     * @return An int impress node ID which can be used for updating the surface later
     * @throws InvalidArgumentException if stereoMode is invalid.
     */
    // TODO - b/411225487: Remove this method.
    public fun createStereoSurface(@StereoMode stereoMode: Int): Int

    /**
     * This method creates an Impress node with a stereo panel and returns the entity ID. Note that
     * the StereoSurfaceEntity will not be render anything until the canvas shape is set.
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
    ): Int

    /**
     * This method creates an Impress node with a stereo panel and returns the entity ID. Note that
     * the StereoSurfaceEntity will not be render anything until the canvas shape is set.
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
    ): Int

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress ID.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param width The width in local spatial units to set the quad to.
     * @param height The height in local spatial units to set the quad to.
     */
    public fun setStereoSurfaceEntityCanvasShapeQuad(impressNode: Int, width: Float, height: Float)

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress ID.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param radius The radius in local spatial units to set the sphere to.
     */
    public fun setStereoSurfaceEntityCanvasShapeSphere(impressNode: Int, radius: Float)

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress ID.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param radius The radius in local spatial units of the hemisphere.
     */
    public fun setStereoSurfaceEntityCanvasShapeHemisphere(impressNode: Int, radius: Float)

    /**
     * Updates the StereoMode for an impress node hosting a StereoSurface.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param stereoMode The [Int] stereoMode to apply. Must be a member of StereoMode
     * @throws InvalidArgumentException if stereoMode is invalid.
     */
    public fun setStereoModeForStereoSurface(panelImpressNode: Int, @StereoMode stereoMode: Int)

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
        stereoSurfaceNode: Int,
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
    public fun resetContentColorMetadataForStereoSurface(stereoSurfaceNode: Int)

    /**
     * Updates the radius of the (alpha) feathered edges for an Impress node hosting a
     * StereoSurface.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param radiusX The radius of the left/right feathering.
     * @param radiusY The radius of the top/bottom feathering.
     */
    public fun setFeatherRadiusForStereoSurface(
        panelImpressNode: Int,
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
    public fun setPrimaryAlphaMaskForStereoSurface(panelImpressNode: Int, alphaMask: Long)

    /**
     * Sets the auxiliary alpha mask for a stereo surface. The alpha mask will be composited into
     * the alpha channel of the surface if an interleaved video is being rendered.
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @param alphaMask The auxiliary alpha mask texture. Only used with interleaved video formats.
     *   If null or empty, the alpha mask will be disabled.
     */
    public fun setAuxiliaryAlphaMaskForStereoSurface(panelImpressNode: Int, alphaMask: Long)

    /**
     * Retrieve the android surface for this stereo panel
     *
     * @param panelImpressNode The Impress node which hosts the panel to be updated.
     * @return A Surface backed by an imp::AndroidExternalTextureSurface
     */
    public fun getSurfaceFromStereoSurface(panelImpressNode: Int): Surface

    /**
     * This method loads a local texture from the assets folder or a remote texture from a URL and
     * returns a future with the texture token that can be used to reference the texture in other
     * JNI calls.
     *
     * @param path The name of the texture file to load or the URL of the remote texture.
     * @param sampler The sampler to use when loading the texture.
     * @return A future that resolves to the texture when it is loaded.
     */
    public fun loadTexture(path: String, sampler: TextureSampler): ListenableFuture<Texture>

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
     * This method creates a water material and returns a future with the material native handle
     * that can be used to reference the water material in other JNI calls.
     *
     * @param isAlphaMapVersion True if the water material should be the alpha map version.
     * @return A WaterMaterial backed by an imp::WaterMaterial. The WaterMaterial can be destroyed
     *   by passing it to destroyNativeObject.
     */
    public fun createWaterMaterial(isAlphaMapVersion: Boolean): ListenableFuture<WaterMaterial>

    /**
     * This method sets the reflection map for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param reflectionMap The native handle of the texture to be used as the reflection map.
     */
    public fun setReflectionMapOnWaterMaterial(nativeWaterMaterial: Long, reflectionMap: Long)

    /**
     * This method sets the normal map for the water material.
     *
     * @param nativeWaterMaterial The native handle of the water material to be updated.
     * @param normalMap The native handle of the texture to be used as the normal map.
     */
    public fun setNormalMapOnWaterMaterial(nativeWaterMaterial: Long, normalMap: Long)

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
     */
    public fun setAlphaMapOnWaterMaterial(nativeWaterMaterial: Long, alphaMap: Long)

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
     * This method creates a Khronos PBR material and returns a future with the material native
     * handle that can be used to reference the Khronos PBR material in other JNI calls.
     *
     * @param spec The Khronos PBR material spec to use for the material.
     * @return A future that resolves to the Khronos PBR material when it is created.
     * @throws IllegalArgumentException if the Khronos PBR material spec is invalid.
     */
    public fun createKhronosPbrMaterial(
        spec: KhronosPbrMaterialSpec
    ): ListenableFuture<KhronosPbrMaterial>

    /**
     * Sets the base color texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param baseColorTexture The native handle of the base color texture.
     */
    public fun setBaseColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        baseColorTexture: Long,
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
     */
    public fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        metallicRoughnessTexture: Long,
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
     */
    public fun setNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        normalTexture: Long,
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
     */
    public fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ambientOcclusionTexture: Long,
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
     */
    public fun setEmissiveTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        emissiveTexture: Long,
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
     */
    public fun setClearcoatTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatTexture: Long,
    )

    /**
     * Sets the clearcoat normal texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param clearcoatNormalTexture The native handle of the clearcoat normal texture.
     */
    public fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatNormalTexture: Long,
    )

    /**
     * Sets the clearcoat roughness texture for the Khronos PBR material.
     *
     * @param nativeKhronosPbrMaterial The native handle of the Khronos PBR material.
     * @param clearcoatRoughnessTexture The native handle of the clearcoat roughness texture.
     */
    public fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatRoughnessTexture: Long,
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
     */
    public fun setSheenColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenColorTexture: Long,
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
     */
    public fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenRoughnessTexture: Long,
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
     */
    public fun setTransmissionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        transmissionTexture: Long,
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
     * This method sets the material override for the mesh of a glTF model.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the glTF model.
     * @param nativeMaterial The native handle of the material to be used as the override.
     * @param meshName The name of the mesh to be overridden.
     */
    public fun setMaterialOverride(impressNode: Int, nativeMaterial: Long, meshName: String)

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
