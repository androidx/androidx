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

import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorRange
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorSpace
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorTransfer
import androidx.xr.scenecore.impl.impress.ImpressApi.ContentSecurityLevel
import androidx.xr.scenecore.impl.impress.ImpressApi.DrawMode
import androidx.xr.scenecore.impl.impress.ImpressApi.MediaBlendingMode
import androidx.xr.scenecore.impl.impress.ImpressApi.StereoMode
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.ar.imp.view.View
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Implementation of the JNI API for communicating with the Impress Split Engine instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImpressApiImpl : ImpressApi {
    private var view: View? = null
    private var testViewHandle: Long = 0

    private lateinit var resourceManager: BindingsResourceManager

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.StereoMode is in sync with imp::MediaStereoMode.
     */
    private fun validateStereoMode(@StereoMode stereoMode: Int): Int =
        when (stereoMode) {
            StereoMode.MONO,
            StereoMode.TOP_BOTTOM,
            StereoMode.SIDE_BY_SIDE,
            StereoMode.MULTIVIEW_LEFT_PRIMARY,
            StereoMode.MULTIVIEW_RIGHT_PRIMARY -> stereoMode
            else ->
                throw IllegalArgumentException(
                    "Unsupported value for ImpressApi.StereoMode: $stereoMode"
                )
        }

    private fun validateMediaBlendingMode(@MediaBlendingMode mediaBlendingMode: Int): Int =
        when (mediaBlendingMode) {
            MediaBlendingMode.TRANSPARENT,
            MediaBlendingMode.OPAQUE -> mediaBlendingMode
            else ->
                throw IllegalArgumentException(
                    "Unsupported value for ImpressApi.MediaBlendingMode: $mediaBlendingMode"
                )
        }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ContentSecurityLevel is in sync with imp::ContentSecurityLevel.
     */
    private fun validateContentSecurityLevel(@ContentSecurityLevel contentSecurityLevel: Int): Int =
        when (contentSecurityLevel) {
            ContentSecurityLevel.NONE,
            ContentSecurityLevel.PROTECTED -> contentSecurityLevel
            else ->
                throw IllegalArgumentException(
                    "Unsupported value for ImpressApi.ContentSecurityLevel: " +
                        "$contentSecurityLevel"
                )
        }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ColorSpace is in sync with the native counterpart.
     */
    private fun validateColorSpace(@ColorSpace colorSpace: Int): Int =
        when (colorSpace) {
            ColorSpace.BT709,
            ColorSpace.BT601_PAL,
            ColorSpace.BT2020,
            ColorSpace.BT601_525,
            ColorSpace.DISPLAY_P3,
            ColorSpace.DCI_P3,
            ColorSpace.ADOBE_RGB -> colorSpace
            else ->
                throw IllegalArgumentException(
                    "Unsupported value for ImpressApi.ColorSpace: $colorSpace"
                )
        }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ColorTransfer is in sync with the native counterpart.
     */
    private fun validateColorTransfer(@ColorTransfer colorTransfer: Int): Int =
        when (colorTransfer) {
            ColorTransfer.LINEAR,
            ColorTransfer.SRGB,
            ColorTransfer.SDR,
            ColorTransfer.GAMMA_2_2,
            ColorTransfer.ST2084,
            ColorTransfer.HLG -> colorTransfer
            else ->
                throw IllegalArgumentException(
                    "Unsupported value for ImpressApi.ColorTransfer: $colorTransfer"
                )
        }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ColorRange is in sync with the native counterpart.
     */
    private fun validateColorRange(@ColorRange colorRange: Int): Int =
        when (colorRange) {
            ColorRange.FULL,
            ColorRange.LIMITED -> colorRange
            else ->
                throw IllegalArgumentException(
                    "Unsupported value for ImpressApi.ColorRange: $colorRange"
                )
        }

    private fun validateMaxLuminance(maxLuminance: Int): Int {
        if (maxLuminance !in 0..65535) {
            throw IllegalArgumentException(
                "maxLuminance must be either 0 (unknown) or greater than 0 and smaller than" +
                    " 65536: $maxLuminance"
            )
        }
        return maxLuminance
    }

    override fun setup(view: View?) {
        this.view = view
        if (this.view != null) {
            nSetup(getViewNativeHandle(view))
        }
        resourceManager = BindingsResourceManager(Handler(Looper.getMainLooper()))
    }

    @VisibleForTesting
    override fun setup(nativeTestViewHandle: Long) {
        testViewHandle = nativeTestViewHandle
        nSetup(getViewNativeHandle(view))
        resourceManager = BindingsResourceManager(Handler(Looper.getMainLooper()))
    }

    override fun onResume(): Unit {
        view?.onResume()
    }

    override fun onPause(): Unit {
        view?.onPause()
    }

    override fun getBindingsResourceManager(): BindingsResourceManager = resourceManager

    override fun releaseImageBasedLightingAsset(iblToken: Long): Unit =
        nReleaseImageBasedLightingAsset(getViewNativeHandle(view), iblToken)

    override suspend fun loadImageBasedLightingAsset(path: String): ExrImage =
        suspendCancellableCoroutine { continuation ->
            // TODO: b/374216912 - Add a cancellationListener to the completer here when the
            // loading APIs support cancellation.
            nLoadImageBasedLightingAssetFromPath(
                getViewNativeHandle(view),
                // The underlying C++ code will hold a reference to this (anoynomous)
                // AssetLoader until the load is complete.
                object : AssetLoader {
                    override fun onSuccess(value: Long) {
                        val exrImage: ExrImage =
                            ExrImage.Builder()
                                .setImpressApi(this@ImpressApiImpl)
                                .setNativeExrImage(value)
                                .build()
                        continuation.resume(exrImage)
                    }

                    override fun onFailure(message: String) {
                        // We can safely check for the CANCELLED string here since we
                        // know that the underlying absl Status code is being
                        // translated to a java Exception and the message is being
                        // propagated. Ideally the native code would generate a separate
                        // signal call for this.
                        // TODO: b/374217508 - Publish a more precisely typed Exception
                        // interface for this.
                        if (message.contains("CANCELLED")) {
                            onCancelled(message)
                        } else {
                            continuation.resumeWithException(Exception(message))
                        }
                    }

                    override fun onCancelled(message: String) {
                        continuation.cancel(Exception(message))
                    }
                },
                path,
            )
            "LoadImageBasedLightingAsset Operation"
        }

    override suspend fun loadImageBasedLightingAsset(data: ByteArray, key: String): ExrImage =
        suspendCancellableCoroutine { continuation ->
            nLoadImageBasedLightingAssetFromByteArray(
                getViewNativeHandle(view),
                // The underlying C++ code will hold a reference to this (anoynomous)
                // AssetLoader until the load is complete.
                object : AssetLoader {
                    override fun onSuccess(value: Long) {
                        val exrImage: ExrImage =
                            ExrImage.Builder()
                                .setImpressApi(this@ImpressApiImpl)
                                .setNativeExrImage(value)
                                .build()
                        continuation.resume(exrImage)
                    }

                    override fun onFailure(message: String) {
                        // We can safely check for the CANCELLED string here since we
                        // know that the underlying absl Status code is being
                        // translated to a java Exception and the message is being
                        // propagated. Ideally the native code would generate a separate
                        // signal call for this.
                        // TODO: b/374217508 - Publish a more precisely typed Exception
                        // interface for this.
                        if (message.contains("CANCELLED")) {
                            onCancelled(message)
                        } else {
                            continuation.resumeWithException(Exception(message))
                        }
                    }

                    override fun onCancelled(message: String) {
                        continuation.cancel(Exception(message))
                    }
                },
                data,
                key,
            )
            "LoadImageBasedLightingAsset Operation"
        }

    override suspend fun loadGltfAsset(path: String): GltfModel =
        suspendCancellableCoroutine { continuation ->
            // TODO: b/374216912 - Add a cancellationListener to the completer here when the
            // loading APIs support cancellation.
            nLoadGltfAssetFromPath(
                getViewNativeHandle(view),
                // The underlying C++ code will hold a reference to this (anoynomous)
                // AssetLoader until the load is complete.
                object : AssetLoader {
                    override fun onSuccess(value: Long) {
                        val model: GltfModel =
                            GltfModel.Builder()
                                .setImpressApi(this@ImpressApiImpl)
                                .setNativeGltfModel(value)
                                .build()
                        continuation.resume(model)
                    }

                    override fun onFailure(message: String) {
                        // We can safely check for the CANCELLED string here since we
                        // know that the underlying absl Status code is being
                        // translated to a java Exception and the message is being
                        // propagated. Ideally the native code would generate a separate
                        // signal call for this.
                        // TODO: b/374217508 - Publish a more precisely typed Exception
                        // interface for this.
                        if (message.contains("CANCELLED")) {
                            onCancelled(message)
                        } else {
                            continuation.resumeWithException(Exception(message))
                        }
                    }

                    override fun onCancelled(message: String) {
                        continuation.cancel(Exception(message))
                    }
                },
                path,
            )
            "LoadGltfAsset Operation"
        }

    override suspend fun loadGltfAsset(data: ByteArray, key: String): GltfModel =
        suspendCancellableCoroutine { continuation ->
            // TODO: b/374216912 - Add a cancellationListener to the completer here when the
            // loading APIs support cancellation.
            nLoadGltfAssetFromByteArray(
                getViewNativeHandle(view),
                // The underlying C++ code will hold a reference to this (anoynomous)
                // AssetLoader until the load is complete.
                object : AssetLoader {
                    override fun onSuccess(value: Long) {
                        val model: GltfModel =
                            GltfModel.Builder()
                                .setImpressApi(this@ImpressApiImpl)
                                .setNativeGltfModel(value)
                                .build()
                        continuation.resume(model)
                    }

                    override fun onFailure(message: String) {
                        // We can safely check for the CANCELLED string here since we
                        // know that the underlying absl Status code is being
                        // translated to a java Exception and the message is being
                        // propagated. Ideally the native code would generate a separate
                        // signal call for this.
                        // TODO: b/374217508 - Publish a more precisely typed Exception
                        // interface for this.
                        if (message.contains("CANCELLED")) {
                            onCancelled(message)
                        } else {
                            continuation.resumeWithException(Exception(message))
                        }
                    }

                    override fun onCancelled(message: String) {
                        continuation.cancel(Exception(message))
                    }
                },
                data,
                key,
            )
            "LoadGltfAsset Operation"
        }

    override fun releaseGltfAsset(gltfToken: Long): Unit =
        nReleaseGltfAsset(getViewNativeHandle(view), gltfToken)

    override fun instanceGltfModel(gltfToken: Long): ImpressNode =
        ImpressNode(
            nInstanceGltfModel(getViewNativeHandle(view), gltfToken, /* enableCollider= */ false)
        )

    override fun instanceGltfModel(gltfToken: Long, enableCollider: Boolean): ImpressNode =
        ImpressNode(nInstanceGltfModel(getViewNativeHandle(view), gltfToken, enableCollider))

    override fun setGltfModelColliderEnabled(
        impressNode: ImpressNode,
        enableCollider: Boolean,
    ): Unit =
        nSetGltfModelColliderEnabled(
            getViewNativeHandle(view),
            impressNode.handle.toLong(),
            enableCollider,
        )

    /**
     * Enables reform affordance on an instanced gLTF model.
     *
     * @param impressNode The integer ID of the impress node for the instance of the gLTF
     * @param enabled A boolean indicated whether to add or remove the reform affordance for the
     *   gLTF model.
     * @param systemMovable A boolean indicating whether to handle the move input events or not.
     */
    override fun setGltfReformAffordanceEnabled(
        impressNode: ImpressNode,
        enabled: Boolean,
        systemMovable: Boolean,
    ): Unit =
        nSetGltfReformAffordanceEnabled(
            getViewNativeHandle(view),
            impressNode.handle,
            enabled,
            systemMovable,
        )

    /**
     * Starts an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param animationName A nullable String which contains a requested animation to play. If null
     *   is provided, this will attempt to play the first animation it finds
     * @param looping True if the animation should loop. Note that if the animation is looped, the
     *   returned Future will never fire successfully.
     * @param speed The speed of the animation where 1.0 is the normal speed and negative values
     *   will play the animation in reverse.
     * @param startTime The start time of the animation in seconds.
     * @param channel The channel of the animation.
     * @return a [Void] result when the animation completed.
     */
    override suspend fun animateGltfModel(
        impressNode: ImpressNode,
        animationName: String?,
        looping: Boolean,
        speed: Float,
        startTime: Float,
        channel: Int,
    ): Void? = suspendCancellableCoroutine { continuation ->
        nAnimateGltfModel(
            getViewNativeHandle(view),
            impressNode.handle,
            animationName,
            looping,
            speed,
            startTime,
            channel,
            object : AssetAnimator {
                override fun onComplete() {
                    continuation.resume(null)
                }

                override fun onFailure(message: String) {
                    // We can safely check for the CANCELLED string here since we
                    // know that the underlying absl Status code is being
                    // translated to a java Exception and the message is being
                    // propagated. Ideally the native code would generate a separate
                    // signal call for this.
                    // TODO: b/374217508 - Publish a more precisely typed Exception
                    // interface for this.
                    if (message.contains("CANCELLED")) {
                        onCancelled(message)
                    } else {
                        continuation.resumeWithException(Exception(message))
                    }
                }

                override fun onCancelled(message: String) {
                    continuation.cancel(Exception(message))
                }
            },
        )
        "AnimateGltfModel Operation"
    }

    /**
     * Stops an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param channel The channel of the animation.
     */
    override fun stopGltfModelAnimation(impressNode: ImpressNode, channel: Int): Unit =
        nStopGltfModelAnimation(getViewNativeHandle(view), impressNode.handle, channel)

    /**
     * Toggles an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param playing True if the animation should play, false if it should stop.
     * @param channel The channel of the animation.
     */
    override fun toggleGltfModelAnimation(
        impressNode: ImpressNode,
        playing: Boolean,
        channel: Int,
    ): Unit =
        nToggleGltfModelAnimation(getViewNativeHandle(view), impressNode.handle, playing, channel)

    /**
     * Sets the playback time of an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param playbackTime The playback time of the animation.
     * @param channel The channel of the animation.
     */
    override fun setGltfModelAnimationPlaybackTime(
        impressNode: ImpressNode,
        playbackTime: Float,
        channel: Int,
    ): Unit =
        nSetGltfModelAnimationPlaybackTime(
            getViewNativeHandle(view),
            impressNode.handle,
            playbackTime,
            channel,
        )

    /**
     * Sets the playback speed of an animation on an instanced glTF model on a specific channel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param speed The speed of the animation where 1.0 is the normal speed and negative values
     *   will play the animation in reverse.
     * @param channel The channel of the animation.
     */
    override fun setGltfModelAnimationSpeed(
        impressNode: ImpressNode,
        speed: Float,
        channel: Int,
    ): Unit =
        nSetGltfModelAnimationSpeed(getViewNativeHandle(view), impressNode.handle, speed, channel)

    /**
     * Returns the number of animations on an instanced glTF model.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     */
    override fun getGltfModelAnimationCount(impressNode: ImpressNode): Int =
        nGetGltfModelAnimationCount(getViewNativeHandle(view), impressNode.handle)

    /**
     * Returns the name of the animation on an instanced glTF model.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param index The index of the animation as defined in the glTF file.
     */
    override fun getGltfModelAnimationName(impressNode: ImpressNode, index: Int): String? =
        nGetGltfModelAnimationName(getViewNativeHandle(view), impressNode.handle, index)

    /**
     * Returns the duration in seconds of the animation on an instanced glTF model.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param index The index of the animation as defined in the glTF file.
     */
    override fun getGltfModelAnimationDurationSeconds(impressNode: ImpressNode, index: Int): Float =
        nGetGltfModelAnimationDurationSeconds(getViewNativeHandle(view), impressNode.handle, index)

    override fun createImpressNode(): ImpressNode =
        ImpressNode(nCreateImpressNode(getViewNativeHandle(view)))

    override fun destroyImpressNode(impressNode: ImpressNode): Unit =
        nDestroyImpressNode(getViewNativeHandle(view), impressNode.handle)

    override fun getGltfModelBoundingBox(impressNode: ImpressNode): BoundingBox {
        val centerData = FloatArray(3)
        val halfExtentsData = FloatArray(3)
        nGetGltfModelLocalBounds(
            getViewNativeHandle(view),
            impressNode.handle,
            centerData,
            halfExtentsData,
        )

        // TODO: b/463842626 - A policy to handle the NaN or negative center / halfExtents of a glTF
        // model
        var center = Vector3.Zero
        if (!centerData[0].isNaN() && !centerData[1].isNaN() && !centerData[2].isNaN()) {
            center = Vector3(centerData[0], centerData[1], centerData[2])
        }
        var halfExtents = FloatSize3d(0f, 0f, 0f)
        if (halfExtentsData[0] >= 0 && halfExtentsData[1] >= 0 && halfExtentsData[2] >= 0) {
            halfExtents = FloatSize3d(halfExtentsData[0], halfExtentsData[1], halfExtentsData[2])
        }

        return BoundingBox.fromCenterAndHalfExtents(center, halfExtents)
    }

    override fun setImpressNodeParent(
        impressNodeChild: ImpressNode,
        impressNodeParent: ImpressNode,
    ): Unit =
        nSetImpressNodeParent(
            getViewNativeHandle(view),
            impressNodeChild.handle,
            impressNodeParent.handle,
        )

    override fun getImpressNodeParent(impressNode: ImpressNode): ImpressNode =
        ImpressNode(nGetImpressNodeParent(getViewNativeHandle(view), impressNode.handle))

    override fun getImpressNodeChildCount(impressNode: ImpressNode): Int =
        nGetImpressNodeChildCount(getViewNativeHandle(view), impressNode.handle)

    override fun getImpressNodeChildAt(impressNode: ImpressNode, childIndex: Int): ImpressNode =
        ImpressNode(
            nGetImpressNodeChildAt(getViewNativeHandle(view), impressNode.handle, childIndex)
        )

    override fun getImpressNodeName(impressNode: ImpressNode): String =
        nGetImpressNodeName(getViewNativeHandle(view), impressNode.handle)

    override fun setImpressNodeLocalTransform(impressNode: ImpressNode, transform: Matrix4) {
        val pose = transform.pose
        val scale = transform.scale
        nSetImpressNodeLocalTransform(
            getViewNativeHandle(view),
            impressNode.handle,
            pose.translation.x,
            pose.translation.y,
            pose.translation.z,
            pose.rotation.x,
            pose.rotation.y,
            pose.rotation.z,
            pose.rotation.w,
            scale.x,
            scale.y,
            scale.z,
        )
    }

    override fun getImpressNodeLocalTransform(impressNode: ImpressNode): Matrix4 {
        val buffer = FloatArray(10)
        nGetImpressNodeLocalTransform(getViewNativeHandle(view), impressNode.handle, buffer)

        return Matrix4.fromTrs(
            Vector3(buffer[0], buffer[1], buffer[2]),
            Quaternion(buffer[3], buffer[4], buffer[5], buffer[6]),
            Vector3(buffer[7], buffer[8], buffer[9]),
        )
    }

    override fun setImpressNodeRelativeTransform(
        impressNode: ImpressNode,
        relativeNode: ImpressNode,
        transform: Matrix4,
    ) {
        val pose = transform.pose
        val scale = transform.scale
        nSetImpressNodeRelativeTransform(
            getViewNativeHandle(view),
            impressNode.handle,
            relativeNode.handle,
            pose.translation.x,
            pose.translation.y,
            pose.translation.z,
            pose.rotation.x,
            pose.rotation.y,
            pose.rotation.z,
            pose.rotation.w,
            scale.x,
            scale.y,
            scale.z,
        )
    }

    override fun getImpressNodeRelativeTransform(
        impressNode: ImpressNode,
        relativeNode: ImpressNode,
    ): Matrix4 {
        val buffer = FloatArray(10)
        nGetImpressNodeRelativeTransform(
            getViewNativeHandle(view),
            impressNode.handle,
            relativeNode.handle,
            buffer,
        )

        return Matrix4.fromTrs(
            Vector3(buffer[0], buffer[1], buffer[2]),
            Quaternion(buffer[3], buffer[4], buffer[5], buffer[6]),
            Vector3(buffer[7], buffer[8], buffer[9]),
        )
    }

    override fun scheduleGltfReskinning(impressNode: ImpressNode) {
        nScheduleGltfReskinning(getViewNativeHandle(view), impressNode.handle)
    }

    override fun setGltfModelNodeMaterialOverride(
        impressNode: ImpressNode,
        nativeMaterial: Long,
        primitiveIndex: Int,
    ) {
        nSetGltfModelNodeMaterialOverride(
            getViewNativeHandle(view),
            impressNode.handle,
            nativeMaterial,
            primitiveIndex,
        )
    }

    override fun clearGltfModelNodeMaterialOverride(impressNode: ImpressNode, primitiveIndex: Int) {
        nClearGltfModelNodeMaterialOverride(
            getViewNativeHandle(view),
            impressNode.handle,
            primitiveIndex,
        )
    }

    override fun createStereoSurface(@StereoMode stereoMode: Int): ImpressNode =
        createStereoSurface(stereoMode, ContentSecurityLevel.NONE)

    override fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
    ): ImpressNode =
        createStereoSurface(stereoMode, contentSecurityLevel, /* useSuperSampling= */ false)

    override fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
        useSuperSampling: Boolean,
    ): ImpressNode =
        createStereoSurface(
            stereoMode,
            MediaBlendingMode.TRANSPARENT,
            contentSecurityLevel,
            useSuperSampling,
        )

    override fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @MediaBlendingMode mediaBlendingMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
        useSuperSampling: Boolean,
    ): ImpressNode =
        ImpressNode(
            nCreateStereoSurfaceEntity(
                getViewNativeHandle(view),
                validateStereoMode(stereoMode),
                validateMediaBlendingMode(mediaBlendingMode),
                validateContentSecurityLevel(contentSecurityLevel),
                useSuperSampling,
            )
        )

    override fun setStereoSurfaceEntityCanvasShapeQuad(
        impressNode: ImpressNode,
        width: Float,
        height: Float,
        cornerRadius: Float,
    ): Unit =
        nSetStereoSurfaceEntityCanvasShapeQuad(
            getViewNativeHandle(view),
            impressNode.handle,
            width,
            height,
            cornerRadius,
        )

    override fun setStereoSurfaceEntityCanvasShapeSphere(
        impressNode: ImpressNode,
        radius: Float,
    ): Unit =
        nSetStereoSurfaceEntityCanvasShapeSphere(
            getViewNativeHandle(view),
            impressNode.handle,
            radius,
        )

    override fun setStereoSurfaceEntityCanvasShapeHemisphere(
        impressNode: ImpressNode,
        radius: Float,
    ): Unit =
        nSetStereoSurfaceEntityCanvasShapeHemisphere(
            getViewNativeHandle(view),
            impressNode.handle,
            radius,
        )

    override fun setStereoSurfaceEntityCanvasShapeCustomMesh(
        impressNode: ImpressNode,
        leftPositions: FloatBuffer,
        leftTexCoords: FloatBuffer,
        leftIndices: IntBuffer?,
        rightPositions: FloatBuffer?,
        rightTexCoords: FloatBuffer?,
        rightIndices: IntBuffer?,
        @DrawMode drawMode: Int,
    ): Unit =
        nSetStereoSurfaceEntityCanvasShapeCustomMesh(
            getViewNativeHandle(view),
            impressNode.handle,
            leftPositions,
            leftTexCoords,
            leftIndices,
            rightPositions,
            rightTexCoords,
            rightIndices,
            drawMode,
        )

    override fun setStereoSurfaceEntityColliderEnabled(
        impressNode: ImpressNode,
        enableCollider: Boolean,
    ): Unit =
        nSetStereoSurfaceEntityColliderEnabled(
            getViewNativeHandle(view),
            impressNode.handle,
            enableCollider,
        )

    override fun setStereoModeForStereoSurface(
        panelImpressNode: ImpressNode,
        @StereoMode stereoMode: Int,
    ): Unit =
        nSetStereoModeForStereoSurfaceEntity(
            getViewNativeHandle(view),
            panelImpressNode.handle,
            validateStereoMode(stereoMode),
        )

    override fun setBlendingModeForStereoSurfaceEntity(
        panelImpressNode: ImpressNode,
        @MediaBlendingMode blendingMode: Int,
    ): Unit =
        nSetBlendingModeForStereoSurfaceEntity(
            getViewNativeHandle(view),
            panelImpressNode.handle,
            validateMediaBlendingMode(blendingMode),
        )

    override fun setContentColorMetadataForStereoSurface(
        stereoSurfaceNode: ImpressNode,
        @ColorSpace colorSpace: Int,
        @ColorTransfer colorTransfer: Int,
        @ColorRange colorRange: Int,
        maxLuminance: Int,
    ): Unit =
        nSetContentColorMetadataForStereoSurfaceEntity(
            getViewNativeHandle(view),
            stereoSurfaceNode.handle,
            validateColorSpace(colorSpace),
            validateColorTransfer(colorTransfer),
            validateColorRange(colorRange),
            validateMaxLuminance(maxLuminance),
        )

    override fun resetContentColorMetadataForStereoSurface(stereoSurfaceNode: ImpressNode): Unit =
        nResetContentColorMetadataForStereoSurfaceEntity(
            getViewNativeHandle(view),
            stereoSurfaceNode.handle,
        )

    override fun setFeatherRadiusForStereoSurface(
        panelImpressNode: ImpressNode,
        radiusX: Float,
        radiusY: Float,
    ): Unit =
        nSetFeatherRadiusForStereoSurfaceEntity(
            getViewNativeHandle(view),
            panelImpressNode.handle,
            radiusX,
            radiusY,
        )

    override fun getSurfaceFromStereoSurface(panelImpressNode: ImpressNode): Surface =
        nGetSurfaceFromStereoSurfaceEntity(getViewNativeHandle(view), panelImpressNode.handle)

    override fun setStereoSurfaceEntitySurfaceSize(
        impressNode: ImpressNode,
        width: Int,
        height: Int,
    ): Unit =
        nSetStereoSurfaceEntitySurfaceSize(
            getViewNativeHandle(view),
            impressNode.getHandle(),
            width,
            height,
        )

    override fun setPrimaryAlphaMaskForStereoSurface(
        panelImpressNode: ImpressNode,
        alphaMask: Long,
    ): Unit =
        nSetPrimaryAlphaMaskForStereoSurfaceEntity(
            getViewNativeHandle(view),
            panelImpressNode.handle,
            alphaMask,
        )

    override fun setAuxiliaryAlphaMaskForStereoSurface(
        panelImpressNode: ImpressNode,
        alphaMask: Long,
    ): Unit =
        nSetAuxiliaryAlphaMaskForStereoSurfaceEntity(
            getViewNativeHandle(view),
            panelImpressNode.handle,
            alphaMask,
        )

    override suspend fun loadTexture(path: String): Texture =
        suspendCancellableCoroutine { continuation ->
            // TODO: b/374216912 - Add a cancellationListener to the completer here when the
            // loading APIs support cancellation.
            nLoadTexture(
                getViewNativeHandle(view),
                // The underlying C++ code will hold a reference to this (anoynomous)
                // AssetLoader until the load is complete.
                // TODO(b/394349866): Revisit the way C++ --> Java code is called back
                // for the AssetLoader (proguard)
                object : AssetLoader {
                    override fun onSuccess(value: Long) {
                        val texture =
                            Texture.Builder()
                                .setImpressApi(this@ImpressApiImpl)
                                .setNativeTexture(value)
                                .build()
                        continuation.resume(texture)
                    }

                    override fun onFailure(message: String) {
                        // We can safely check for the CANCELLED string here since we
                        // know that the underlying absl Status code is being
                        // translated to a java Exception and the message is being
                        // propagated. Ideally the native code would generate a separate
                        // signal call for this.
                        // TODO: b/374217508 - Publish a more precisely typed Exception
                        // interface for this.
                        if (message.contains("CANCELLED")) {
                            onCancelled(message)
                        } else {
                            continuation.resumeWithException(Exception(message))
                        }
                    }

                    override fun onCancelled(message: String) {
                        continuation.cancel(Exception(message))
                    }
                },
                path,
            )
            "LoadTexture Operation"
        }

    override fun borrowReflectionTexture(): Texture {
        val textureHandle = nBorrowReflectionTexture(getViewNativeHandle(view))
        return Texture.Builder()
            .setImpressApi(this@ImpressApiImpl)
            .setNativeTexture(textureHandle)
            .build()
    }

    override fun getReflectionTextureFromIbl(iblToken: Long): Texture {
        val textureHandle = nGetReflectionTextureFromIbl(getViewNativeHandle(view), iblToken)
        return Texture.Builder()
            .setImpressApi(this@ImpressApiImpl)
            .setNativeTexture(textureHandle)
            .build()
    }

    override suspend fun createWaterMaterial(isAlphaMapVersion: Boolean): WaterMaterial =
        suspendCancellableCoroutine { continuation ->
            // TODO: b/374216912 - Add a cancellationListener to the completer here when the
            // loading APIs support cancellation.
            nCreateWaterMaterial(
                getViewNativeHandle(view),
                // The underlying C++ code will hold a reference to this (anoynomous)
                // AssetLoader until the load is complete.
                // TODO(b/394349866): Revisit the way C++ --> Java code is called back
                // for the AssetLoader (proguard)
                object : AssetLoader {
                    override fun onSuccess(value: Long) {
                        val waterMaterial =
                            WaterMaterial.Builder()
                                .setImpressApi(this@ImpressApiImpl)
                                .setNativeMaterial(value)
                                .build()
                        continuation.resume(waterMaterial)
                    }

                    override fun onFailure(message: String) {
                        // We can safely check for the CANCELLED string here since we
                        // know that the underlying absl Status code is being
                        // translated to a java Exception and the message is being
                        // propagated. Ideally the native code would generate a separate
                        // signal call for this.
                        // TODO: b/374217508 - Publish a more precisely typed Exception
                        // interface for this.
                        if (message.contains("CANCELLED")) {
                            onCancelled(message)
                        } else {
                            continuation.resumeWithException(Exception(message))
                        }
                    }

                    override fun onCancelled(message: String) {
                        continuation.cancel(Exception(message))
                    }
                },
                isAlphaMapVersion,
            )
            // This string is used for debugging purposes by the Coroutine.
            "CreateWaterMaterial Operation"
        }

    override fun setReflectionMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        reflectionMap: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetReflectionMapOnWaterMaterial(
            getViewNativeHandle(view),
            nativeWaterMaterial,
            reflectionMap,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setNormalMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        normalMap: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetNormalMapOnWaterMaterial(
            getViewNativeHandle(view),
            nativeWaterMaterial,
            normalMap,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setNormalTilingOnWaterMaterial(
        nativeWaterMaterial: Long,
        normalTiling: Float,
    ): Unit =
        nSetNormalTilingOnWaterMaterial(
            getViewNativeHandle(view),
            nativeWaterMaterial,
            normalTiling,
        )

    override fun setNormalSpeedOnWaterMaterial(
        nativeWaterMaterial: Long,
        normalSpeed: Float,
    ): Unit =
        nSetNormalSpeedOnWaterMaterial(getViewNativeHandle(view), nativeWaterMaterial, normalSpeed)

    override fun setAlphaStepMultiplierOnWaterMaterial(
        nativeWaterMaterial: Long,
        alphaStepMultiplier: Float,
    ): Unit =
        nSetAlphaStepMultiplierOnWaterMaterial(
            getViewNativeHandle(view),
            nativeWaterMaterial,
            alphaStepMultiplier,
        )

    override fun setAlphaMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        alphaMap: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetAlphaMapOnWaterMaterial(
            getViewNativeHandle(view),
            nativeWaterMaterial,
            alphaMap,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setNormalZOnWaterMaterial(nativeWaterMaterial: Long, normalZ: Float): Unit =
        nSetNormalZOnWaterMaterial(getViewNativeHandle(view), nativeWaterMaterial, normalZ)

    override fun setNormalBoundaryOnWaterMaterial(
        nativeWaterMaterial: Long,
        boundary: Float,
    ): Unit =
        nSetNormalBoundaryOnWaterMaterial(getViewNativeHandle(view), nativeWaterMaterial, boundary)

    override fun setAlphaStepUOnWaterMaterial(
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ): Unit = throw UnsupportedOperationException("Stub API to be removed.")

    override fun setAlphaStepVOnWaterMaterial(
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ): Unit = throw UnsupportedOperationException("Stub API to be removed.")

    override suspend fun createKhronosPbrMaterial(
        spec: KhronosPbrMaterialSpec
    ): KhronosPbrMaterial = suspendCancellableCoroutine { continuation ->
        // TODO: b/374216912 - Add a cancellationListener to the completer here when the
        // loading APIs support cancellation.
        nCreateGenericMaterial(
            getViewNativeHandle(view),
            // The underlying C++ code will hold a reference to this (anoynomous)
            // AssetLoader until the load is complete.
            // TODO(b/394349866): Revisit the way C++ --> Java code is called back
            // for the AssetLoader (proguard)
            object : AssetLoader {
                override fun onSuccess(value: Long) {
                    val khronosPbrMaterial =
                        KhronosPbrMaterial.Builder()
                            .setImpressApi(this@ImpressApiImpl)
                            .setNativeMaterial(value)
                            .build()
                    continuation.resume(khronosPbrMaterial)
                }

                override fun onFailure(message: String) {
                    // We can safely check for the CANCELLED string here since we
                    // know that the underlying absl Status code is being
                    // translated to a java Exception and the message is being
                    // propagated. Ideally the native code would generate a separate
                    // signal call for this.
                    // TODO: b/374217508 - Publish a more precisely typed Exception
                    // interface for this.
                    if (message.contains("CANCELLED")) {
                        onCancelled(message)
                    } else {
                        continuation.resumeWithException(Exception(message))
                    }
                }

                override fun onCancelled(message: String) {
                    continuation.cancel(Exception(message))
                }
            },
            spec.lightingModel,
            spec.blendMode,
            spec.doubleSidedMode,
        )
        // This string is used for debugging purposes by the Coroutine.
        "CreateKhronosPbrMaterial Operation"
    }

    override fun setBaseColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        baseColorTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetBaseColorTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            baseColorTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setBaseColorUvTransformOnKhronosPbrMaterial(
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
    ): Unit =
        nSetBaseColorUvTransformOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            wx,
            wy,
            wz,
        )

    override fun setBaseColorFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ): Unit =
        nSetBaseColorFactorsOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            x,
            y,
            z,
            w,
        )

    override fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        metallicRoughnessTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetMetallicRoughnessTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            metallicRoughnessTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
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
    ): Unit =
        nSetMetallicRoughnessUvTransformOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            wx,
            wy,
            wz,
        )

    override fun setMetallicFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ): Unit =
        nSetMetallicFactorOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            factor,
        )

    override fun setRoughnessFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ): Unit =
        nSetRoughnessFactorOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            factor,
        )

    override fun setNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        normalTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetNormalTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            normalTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setNormalUvTransformOnKhronosPbrMaterial(
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
    ): Unit =
        nSetNormalUvTransformOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            wx,
            wy,
            wz,
        )

    override fun setNormalFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ): Unit =
        nSetNormalFactorOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            factor,
        )

    override fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ambientOcclusionTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetAmbientOcclusionTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ambientOcclusionTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
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
    ): Unit =
        nSetAmbientOcclusionUvTransformOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            wx,
            wy,
            wz,
        )

    override fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ): Unit =
        nSetAmbientOcclusionFactorOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            factor,
        )

    override fun setEmissiveTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        emissiveTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetEmissiveTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            emissiveTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setEmissiveUvTransformOnKhronosPbrMaterial(
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
    ): Unit =
        nSetEmissiveUvTransformOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            wx,
            wy,
            wz,
        )

    override fun setEmissiveFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    ): Unit =
        nSetEmissiveFactorsOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            x,
            y,
            z,
        )

    override fun setClearcoatTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetClearcoatTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            clearcoatTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatNormalTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetClearcoatNormalTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            clearcoatNormalTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatRoughnessTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetClearcoatRoughnessTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            clearcoatRoughnessTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setClearcoatFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        intensity: Float,
        roughness: Float,
        normal: Float,
    ): Unit =
        nSetClearcoatFactorsOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            intensity,
            roughness,
            normal,
        )

    override fun setSheenColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenColorTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetSheenColorTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            sheenColorTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setSheenColorFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    ): Unit =
        nSetSheenColorFactorsOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            x,
            y,
            z,
        )

    override fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenRoughnessTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetSheenRoughnessTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            sheenRoughnessTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ): Unit =
        nSetSheenRoughnessFactorOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            factor,
        )

    override fun setTransmissionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        transmissionTexture: Long,
        sampler: TextureSampler,
    ): Unit =
        nSetTransmissionTextureOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            transmissionTexture,
            sampler.minFilter,
            sampler.magFilter,
            sampler.wrapModeS,
            sampler.wrapModeT,
            sampler.wrapModeR,
            sampler.compareMode,
            sampler.compareFunc,
            sampler.anisotropyLog2,
        )

    override fun setTransmissionUvTransformOnKhronosPbrMaterial(
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
    ): Unit =
        nSetTransmissionUvTransformOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            wx,
            wy,
            wz,
        )

    override fun setTransmissionFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ): Unit =
        nSetTransmissionFactorOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            factor,
        )

    override fun setIndexOfRefractionOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        indexOfRefraction: Float,
    ): Unit =
        nSetIndexOfRefractionOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            indexOfRefraction,
        )

    override fun setAlphaCutoffOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        alphaCutoff: Float,
    ): Unit =
        nSetAlphaCutoffOnGenericMaterial(
            getViewNativeHandle(view),
            nativeKhronosPbrMaterial,
            alphaCutoff,
        )

    override fun destroyNativeObject(nativeHandle: Long): Unit =
        nDestroyNativeObject(getViewNativeHandle(view), nativeHandle)

    override fun setPreferredEnvironmentLight(iblToken: Long): Unit =
        nSetEnvironmentLight(getViewNativeHandle(view), iblToken)

    override fun clearPreferredEnvironmentIblAsset(): Unit =
        nClearEnvironmentLight(getViewNativeHandle(view))

    override fun disposeAllResources(): Unit {
        resourceManager.disable()
        nDisposeAllResources(getViewNativeHandle(view))
    }

    private fun getViewNativeHandle(view: View?): Long {
        if (view != null) {
            return view.nativeHandle
        } else if (testViewHandle.toInt() != 0) {
            return testViewHandle
        }
        return -1
    }

    // returns the bridge handle after it's been initialized.
    private external fun nSetup(view: Long)

    private external fun nReleaseImageBasedLightingAsset(view: Long, assetToken: Long)

    private external fun nLoadImageBasedLightingAssetFromPath(
        view: Long,
        assetLoader: AssetLoader,
        path: String,
    )

    private external fun nLoadImageBasedLightingAssetFromByteArray(
        view: Long,
        assetLoader: AssetLoader,
        data: ByteArray,
        key: String,
    )

    private external fun nLoadGltfAssetFromPath(view: Long, assetLoader: AssetLoader, path: String)

    private external fun nLoadGltfAssetFromByteArray(
        view: Long,
        assetLoader: AssetLoader,
        data: ByteArray,
        key: String,
    )

    private external fun nReleaseGltfAsset(view: Long, gltfToken: Long)

    private external fun nInstanceGltfModel(
        view: Long,
        gltfToken: Long,
        enableCollider: Boolean,
    ): Int

    private external fun nSetGltfModelColliderEnabled(
        view: Long,
        gltfToken: Long,
        enableCollider: Boolean,
    )

    private external fun nSetGltfReformAffordanceEnabled(
        view: Long,
        impressNode: Int,
        enabled: Boolean,
        systemMovable: Boolean,
    )

    private external fun nAnimateGltfModel(
        view: Long,
        impressNode: Int,
        animationName: String?,
        loop: Boolean,
        speed: Float,
        startTime: Float,
        channelId: Int,
        assetAnimator: AssetAnimator,
    )

    private external fun nStopGltfModelAnimation(view: Long, impressNode: Int, channelId: Int)

    private external fun nToggleGltfModelAnimation(
        view: Long,
        impressNode: Int,
        toggle: Boolean,
        channelId: Int,
    )

    private external fun nSetGltfModelAnimationPlaybackTime(
        view: Long,
        impressNode: Int,
        playbackTime: Float,
        channelId: Int,
    )

    private external fun nSetGltfModelAnimationSpeed(
        view: Long,
        impressNode: Int,
        speed: Float,
        channelId: Int,
    )

    private external fun nGetGltfModelAnimationCount(view: Long, impressNode: Int): Int

    private external fun nGetGltfModelAnimationName(
        view: Long,
        impressNode: Int,
        index: Int,
    ): String?

    private external fun nGetGltfModelAnimationDurationSeconds(
        view: Long,
        impressNode: Int,
        index: Int,
    ): Float

    private external fun nGetGltfModelLocalBounds(
        view: Long,
        impressNode: Int,
        outCenter: FloatArray,
        outHalfExtent: FloatArray,
    )

    private external fun nCreateImpressNode(view: Long): Int

    private external fun nDestroyImpressNode(view: Long, impressNode: Int)

    private external fun nSetImpressNodeParent(
        view: Long,
        impressNodeChild: Int,
        impressNodeParent: Int,
    )

    private external fun nGetImpressNodeParent(view: Long, impressNode: Int): Int

    private external fun nGetImpressNodeChildCount(view: Long, impressNode: Int): Int

    private external fun nGetImpressNodeChildAt(view: Long, impressNode: Int, index: Int): Int

    private external fun nGetImpressNodeName(view: Long, impressNode: Int): String

    private external fun nSetImpressNodeLocalTransform(
        view: Long,
        impressNode: Int,
        tx: Float,
        ty: Float,
        tz: Float,
        qx: Float,
        qy: Float,
        qz: Float,
        qw: Float,
        sx: Float,
        sy: Float,
        sz: Float,
    )

    private external fun nGetImpressNodeLocalTransform(
        view: Long,
        impressNode: Int,
        outTransform: FloatArray,
    )

    private external fun nSetImpressNodeRelativeTransform(
        view: Long,
        impressNode: Int,
        relativeNode: Int,
        tx: Float,
        ty: Float,
        tz: Float,
        qx: Float,
        qy: Float,
        qz: Float,
        qw: Float,
        sx: Float,
        sy: Float,
        sz: Float,
    )

    private external fun nGetImpressNodeRelativeTransform(
        view: Long,
        impressNode: Int,
        relativeNode: Int,
        outTransform: FloatArray,
    )

    private external fun nScheduleGltfReskinning(view: Long, impressNode: Int)

    private external fun nSetGltfModelNodeMaterialOverride(
        view: Long,
        impressNode: Int,
        material: Long,
        primitiveIndex: Int,
    )

    private external fun nClearGltfModelNodeMaterialOverride(
        view: Long,
        impressNode: Int,
        primitiveIndex: Int,
    )

    private external fun nCreateStereoSurfaceEntity(
        view: Long,
        stereoMode: Int,
        blendingMode: Int,
        contentSecurityLevel: Int,
        useSuperSampling: Boolean,
    ): Int

    private external fun nSetStereoSurfaceEntitySurfaceSize(
        view: Long,
        impressNode: Int,
        width: Int,
        height: Int,
    )

    private external fun nSetStereoSurfaceEntityCanvasShapeQuad(
        view: Long,
        impressNode: Int,
        width: Float,
        height: Float,
        cornerRadius: Float,
    )

    private external fun nSetStereoSurfaceEntityCanvasShapeSphere(
        view: Long,
        impressNode: Int,
        radius: Float,
    )

    private external fun nSetStereoSurfaceEntityCanvasShapeHemisphere(
        view: Long,
        impressNode: Int,
        radius: Float,
    )

    private external fun nSetStereoSurfaceEntityCanvasShapeCustomMesh(
        view: Long,
        impressNode: Int,
        leftPositions: FloatBuffer,
        leftTexCoords: FloatBuffer,
        leftIndices: IntBuffer?,
        rightPositions: FloatBuffer?,
        rightTexCoords: FloatBuffer?,
        rightIndices: IntBuffer?,
        drawMode: Int,
    )

    private external fun nSetStereoSurfaceEntityColliderEnabled(
        view: Long,
        impressNode: Int,
        enableCollider: Boolean,
    )

    private external fun nGetSurfaceFromStereoSurfaceEntity(
        view: Long,
        panelImpressNode: Int,
    ): Surface

    private external fun nSetFeatherRadiusForStereoSurfaceEntity(
        view: Long,
        panelImpressNode: Int,
        radiusX: Float,
        radiusY: Float,
    )

    private external fun nSetStereoModeForStereoSurfaceEntity(
        view: Long,
        panelImpressNode: Int,
        stereoMode: Int,
    )

    private external fun nSetBlendingModeForStereoSurfaceEntity(
        view: Long,
        panelImpressNode: Int,
        blendingMode: Int,
    )

    private external fun nSetContentColorMetadataForStereoSurfaceEntity(
        view: Long,
        stereoSurfaceNode: Int,
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxLuminance: Int,
    )

    private external fun nResetContentColorMetadataForStereoSurfaceEntity(
        view: Long,
        stereoSurfaceNode: Int,
    )

    private external fun nSetPrimaryAlphaMaskForStereoSurfaceEntity(
        view: Long,
        panelImpressNode: Int,
        alphaMask: Long,
    )

    private external fun nSetAuxiliaryAlphaMaskForStereoSurfaceEntity(
        view: Long,
        panelImpressNode: Int,
        alphaMask: Long,
    )

    private external fun nLoadTexture(view: Long, assetLoader: AssetLoader, path: String)

    private external fun nBorrowReflectionTexture(view: Long): Long

    private external fun nGetReflectionTextureFromIbl(view: Long, iblToken: Long): Long

    private external fun nCreateWaterMaterial(
        view: Long,
        assetLoader: AssetLoader,
        isAlphaMapVersion: Boolean,
    )

    private external fun nSetReflectionMapOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        reflectionMap: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetNormalMapOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        normalMap: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetNormalTilingOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        normalTiling: Float,
    )

    private external fun nSetNormalSpeedOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        normalSpeed: Float,
    )

    private external fun nSetAlphaStepUOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    )

    private external fun nSetAlphaStepVOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    )

    private external fun nSetAlphaStepMultiplierOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        alphaStepMultiplier: Float,
    )

    private external fun nSetAlphaMapOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        alphaMap: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetNormalZOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        normalZ: Float,
    )

    private external fun nSetNormalBoundaryOnWaterMaterial(
        view: Long,
        nativeWaterMaterial: Long,
        normalBoundary: Float,
    )

    private external fun nCreateGenericMaterial(
        view: Long,
        assetLoader: AssetLoader,
        lightingModel: Int,
        blendMode: Int,
        doubleSidedMode: Int,
    )

    private external fun nSetBaseColorTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        baseColorTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetBaseColorUvTransformOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
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

    private external fun nSetBaseColorFactorsOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    )

    private external fun nSetMetallicRoughnessTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        metallicRoughnessTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetMetallicRoughnessUvTransformOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
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

    private external fun nSetMetallicFactorOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        factor: Float,
    )

    private external fun nSetRoughnessFactorOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        factor: Float,
    )

    private external fun nSetNormalTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        normalTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetNormalUvTransformOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
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

    private external fun nSetNormalFactorOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        factor: Float,
    )

    private external fun nSetAmbientOcclusionTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        ambientOcclusionTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetAmbientOcclusionUvTransformOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
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

    private external fun nSetAmbientOcclusionFactorOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        factor: Float,
    )

    private external fun nSetEmissiveTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        emissiveTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetEmissiveUvTransformOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
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

    private external fun nSetEmissiveFactorsOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    )

    private external fun nSetClearcoatTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        clearcoatTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetClearcoatNormalTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        clearcoatNormalTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetClearcoatRoughnessTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        clearcoatRoughnessTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetClearcoatFactorsOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        intensity: Float,
        roughness: Float,
        normal: Float,
    )

    private external fun nSetSheenColorTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        sheenColorTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetSheenColorFactorsOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    )

    private external fun nSetSheenRoughnessTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        sheenRoughnessTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetSheenRoughnessFactorOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        factor: Float,
    )

    private external fun nSetTransmissionTextureOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        transmissionTexture: Long,
        minFilter: Int,
        magFilter: Int,
        wrapModeS: Int,
        wrapModeT: Int,
        wrapModeR: Int,
        compareMode: Int,
        compareFunc: Int,
        anisotropyLog2: Int,
    )

    private external fun nSetTransmissionUvTransformOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
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

    private external fun nSetTransmissionFactorOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        factor: Float,
    )

    private external fun nSetIndexOfRefractionOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        indexOfRefraction: Float,
    )

    private external fun nSetAlphaCutoffOnGenericMaterial(
        view: Long,
        nativeGenericMaterial: Long,
        alphaCutoff: Float,
    )

    private external fun nDestroyNativeObject(view: Long, nativeHandle: Long)

    private external fun nSetEnvironmentLight(view: Long, iblToken: Long)

    private external fun nClearEnvironmentLight(view: Long)

    private external fun nDisposeAllResources(view: Long)
}
