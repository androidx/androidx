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

package androidx.camera.camera2.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.MediaCodec
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.CameraUnavailableExceptionHelper
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.camera2.internal.compat.params.DynamicRangeConversions
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat
import androidx.camera.core.Logger
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory

// TODO: b/417839748 - Decide on the appropriate API level for CameraX feature combo API
@RequiresApi(35)
public class FeatureCombinationQueryImpl(
    private val context: Context,
    private val cameraId: String,
    private val cameraManagerCompat: CameraManagerCompat,
) : FeatureCombinationQuery {
    // creating cameraDeviceSetupCompat may have some latency since it leads to binder call
    private val cameraDeviceSetupCompat by lazy {
        CameraDeviceSetupCompatFactory(context).getCameraDeviceSetupCompat(cameraId)
    }

    /**
     * This non-compat cameraDeviceSetupCompat is required because [CameraDeviceSetupCompat] doesn't
     * expose an API to create CaptureRequest, it should only be used to create a
     * [CaptureRequest.Builder].
     *
     * There may be some latency involved due to binder call under-the-hood, so the value is cached
     * through lazy initialization.
     *
     * While [CameraDeviceSetupCompat] is used for querying the feature combination, the actual
     * creation of the CaptureRequest needed for the query requires the internal CameraDeviceSetup
     * which is not exposed by the Jetpack feature combination query library.
     */
    private val cameraDeviceSetup: CameraDevice.CameraDeviceSetup? by lazy {
        if (cameraManagerCompat.unwrap().isCameraDeviceSetupSupported(cameraId)) {
            cameraManagerCompat.unwrap().getCameraDeviceSetup(cameraId)
        } else {
            null
        }
    }

    private val cameraCharacteristics: CameraCharacteristicsCompat by lazy {
        try {
            cameraManagerCompat.getCameraCharacteristicsCompat(cameraId)
        } catch (e: CameraAccessExceptionCompat) {
            throw CameraUnavailableExceptionHelper.createFrom(e)
        }
    }

    private val dynamicRangeProfiles by lazy {
        DynamicRangesCompat.fromCameraCharacteristics(cameraCharacteristics)
            .toDynamicRangeProfiles()
    }

    private val isDeferredSurfaceSupported by lazy {
        // Deferred surface is not supported right now for the Play Services implementation.
        hasPlayServicesDependency() == false
    }

    override fun isSupported(sessionConfig: SessionConfig): Boolean {
        return createOutputConfigurations(sessionConfig).use { outputConfigs ->
            val camera2SessionConfiguration =
                getCamera2SessionConfiguration(outputConfigs.map { it.value }, sessionConfig)
                    ?: return false

            val supported =
                cameraDeviceSetupCompat
                    .isSessionConfigurationSupported(camera2SessionConfiguration)
                    .supported

            Logger.d(
                TAG,
                "isSupported: supported = $supported for session config" +
                    " with ${sessionConfig.toLogString()}",
            )

            supported == CameraDeviceSetupCompat.SupportQueryResult.RESULT_SUPPORTED
        }
    }

    private fun createOutputConfigurations(
        sessionConfig: SessionConfig
    ): List<CloseableOutputConfiguration> {
        val outputConfigs =
            sessionConfig.outputConfigs.map { outputConfig ->
                if (isDeferredSurfaceSupported) {
                        outputConfig.toDeferredOutputConfiguration()
                    } else {
                        outputConfig.toConcreteOutputConfiguration()
                    }
                    .apply {
                        if (
                            outputConfig.surface.containerClass != null // i.e. Preview/VideoCapture
                        ) {
                            value.applyDynamicRange(outputConfig)
                        }
                    }
            }

        return outputConfigs
    }

    /**
     * Creates concrete surfaces to represent CameraX surfaces.
     *
     * This function utilizes ImageReader + Usage Flags to represent specialized surfaces like
     * SurfaceTexture based on the instructions in
     * [CameraDeviceSetupCompat.isSessionConfigurationSupported] documentation.
     */
    @SuppressLint("WrongConstant") // for OutputConfiguration(...) format parameter
    private fun SessionConfig.OutputConfig.toConcreteOutputConfiguration():
        CloseableOutputConfiguration {
        val usageFlag =
            when (surface.containerClass) {
                // Used for VideoCapture use case
                MediaCodec::class.java -> HardwareBuffer.USAGE_VIDEO_ENCODE

                // Preview may use either SurfaceView or SurfaceTexture
                SurfaceHolder::class.java -> HardwareBuffer.USAGE_COMPOSER_OVERLAY
                SurfaceTexture::class.java -> HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE

                // 0 represents no usage flag and is used as the default value in ImageReader
                else -> 0
            }

        Logger.d(
            TAG,
            "toConcreteOutputConfiguration:" +
                " surface containerClass = ${surface.containerClass}, usageFlag = $usageFlag",
        )

        val imageReader =
            ImageReader.newInstance(
                surface.prescribedSize.width,
                surface.prescribedSize.height,
                surface.prescribedStreamFormat,
                1,
                usageFlag,
            )

        return CloseableOutputConfiguration(OutputConfiguration(imageReader.surface), imageReader)
    }

    @SuppressLint("WrongConstant") // for OutputConfiguration(...) format parameter
    private fun SessionConfig.OutputConfig.toDeferredOutputConfiguration():
        CloseableOutputConfiguration {
        val surfaceClass = surface.containerClass

        Logger.d(
            TAG,
            "toDeferredOutputConfiguration: surface containerClass = ${surface.containerClass}",
        )

        return CloseableOutputConfiguration(
            if (surfaceClass != null) {
                // e.g. Preview, VideoCapture
                OutputConfiguration(requireNotNull(surface.prescribedSize), surfaceClass)
            } else {
                // TODO: b/402156713 - Support ImageCapture output config in older devices,
                //  possibly through a temporary ImageReader

                // e.g. ImageCapture
                OutputConfiguration(surface.prescribedStreamFormat, surface.prescribedSize)
            }
        )
    }

    private fun OutputConfiguration.applyDynamicRange(outputConfig: SessionConfig.OutputConfig) {
        val dynamicRangeProfiles = dynamicRangeProfiles // snapshot for smart-cast
        if (dynamicRangeProfiles == null) {
            return
        }

        dynamicRangeProfile =
            requireNotNull(
                DynamicRangeConversions.dynamicRangeToFirstSupportedProfile(
                    outputConfig.dynamicRange,
                    dynamicRangeProfiles,
                )
            )
    }

    private fun getCamera2SessionConfiguration(
        outputConfigs: List<OutputConfiguration>,
        cameraXSessionConfig: SessionConfig,
    ): SessionConfiguration? {
        val camera2SessionConfig =
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigs,
                CameraXExecutors.directExecutor(),
                NO_OP_CALLBACK,
            )

        val cameraDeviceSetup = cameraDeviceSetup ?: return null

        camera2SessionConfig.sessionParameters =
            cameraDeviceSetup
                .createCaptureRequest(cameraXSessionConfig.templateType)
                .apply {
                    set(CONTROL_AE_TARGET_FPS_RANGE, cameraXSessionConfig.expectedFrameRateRange)

                    if (
                        cameraXSessionConfig.repeatingCaptureConfig.previewStabilizationMode ==
                            StabilizationMode.ON
                    ) {
                        set(
                            CONTROL_VIDEO_STABILIZATION_MODE,
                            CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
                        )
                    }
                }
                .build()

        return camera2SessionConfig
    }

    private fun SessionConfig.toLogString(): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append("sessionParameters=[")
        stringBuilder.append("fpsRange=$expectedFrameRateRange")
        stringBuilder.append(
            ", previewStabilizationMode=" + repeatingCaptureConfig.previewStabilizationMode
        )
        stringBuilder.append("], ")

        stringBuilder.append("outputConfigurations=[")
        outputConfigs.forEachIndexed { index, outputConfig ->
            if (index != 0) {
                stringBuilder.append(",")
            }
            stringBuilder.append(
                "{format=${outputConfig.surface.prescribedStreamFormat}" +
                    ", size=${outputConfig.surface.prescribedSize}" +
                    ", dynamicRange=${outputConfig.dynamicRange}" +
                    ", class=${outputConfig.surface.containerClass}}"
            )
        }
        stringBuilder.append("]")

        return stringBuilder.toString()
    }

    /**
     * Returns whether play services dependency exists, a null value represents it was not possible
     * to determine the result.
     */
    // TODO: b/428899069 - Remove this API when fixed, this is based on the logic implemented for
    //  CameraDeviceSetupCompatFactory#getPlayServicesCameraDeviceSetupCompatProvider API.
    private fun hasPlayServicesDependency(): Boolean? {
        // Create Play Services implementation if there isn't a cached one.
        val packageInfo =
            try {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA or PackageManager.GET_SERVICES,
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // Since the context comes from the app itself, it should always be found.
                return null
            }

        if (packageInfo.services == null) {
            return false
        }

        for (serviceInfo in packageInfo.services) {
            // Try to load the play services impl class name from the Service metadata
            // in feature-combination-query-play-services manifest.
            if (
                serviceInfo.metaData != null &&
                    serviceInfo.metaData.getString(FCQ_PLAY_SERVICES_IMPL_KEY) != null
            ) {
                return true
            }
        }

        return false
    }

    /**
     * A class to store both an [OutputConfiguration] and its related to resources (i.e. the
     * [backingImageReader] property) so that the resources can be closed properly.
     */
    private data class CloseableOutputConfiguration(
        val value: OutputConfiguration,
        private val backingImageReader: ImageReader? = null,
    ) : AutoCloseable {
        override fun close() {
            backingImageReader?.close()
        }
    }

    /**
     * Extension function of a list of [AutoCloseable] to close all elements in a single call,
     * similar to [AutoCloseable.use] API.
     */
    private inline fun <T : AutoCloseable, R> List<T>.use(block: (List<T>) -> R): R {
        return block(this).also { forEach { it.close() } }
    }

    internal companion object {
        private const val TAG = "FeatureCombinationQuery"

        private val NO_OP_CALLBACK =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(p0: CameraCaptureSession) {
                    // no-op
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                    // no-op
                }
            }

        // TODO: b/428899069 - Remove when fixed, this is copy pasted from
        //  CameraDeviceSetupCompatFactory.PLAY_SERVICES_IMPL_KEY
        private const val FCQ_PLAY_SERVICES_IMPL_KEY: String =
            "androidx.camera.featurecombinationquery.PLAY_SERVICES_IMPL_PROVIDER_KEY"
    }
}
