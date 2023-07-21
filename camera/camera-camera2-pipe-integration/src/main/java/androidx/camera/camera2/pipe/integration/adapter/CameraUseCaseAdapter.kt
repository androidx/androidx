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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.integration.compat.workaround.setupHDRnet
import androidx.camera.camera2.pipe.integration.compat.workaround.toggleHDRPlus
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.impl.SESSION_PHYSICAL_CAMERA_ID_OPTION
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_CASE_OPTION
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy

/**
 * This class builds [Config] objects for a given [UseCaseConfigFactory.CaptureType].
 *
 * This includes things like default template and session parameters, as well as maximum resolution
 * and aspect ratios for the display.
 */
@Suppress("DEPRECATION")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraUseCaseAdapter(context: Context) : UseCaseConfigFactory {
    private val displayInfoManager by lazy { DisplayInfoManager(context) }

    init {
        if (context === context.applicationContext) {
            info {
                "The provided context ($context) is application scoped and will be used to infer " +
                    "the default display for computing the default preview size, orientation, " +
                    "and default aspect ratio for UseCase outputs."
            }
        }
        debug { "Created UseCaseConfigurationMap" }
    }

    // TODO: the getConfig() is not fully verified and porting. Please do verify.
    /**
     * Returns the configuration for the given capture type, or `null` if the
     * configuration cannot be produced.
     */
    override fun getConfig(
        captureType: CaptureType,
        captureMode: Int
    ): Config? {
        debug { "Creating config for $captureType" }

        val mutableConfig = MutableOptionsBundle.create()
        val sessionBuilder = SessionConfig.Builder()
        when (captureType) {
            CaptureType.IMAGE_CAPTURE,
            CaptureType.PREVIEW,
            CaptureType.IMAGE_ANALYSIS -> sessionBuilder.setTemplateType(
                CameraDevice.TEMPLATE_PREVIEW
            )

            CaptureType.VIDEO_CAPTURE,
            CaptureType.STREAM_SHARING -> sessionBuilder.setTemplateType(
                CameraDevice.TEMPLATE_RECORD
            )
        }
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG,
            sessionBuilder.build()
        )
        val captureBuilder = CaptureConfig.Builder()
        when (captureType) {
            CaptureType.IMAGE_CAPTURE ->
                captureBuilder.templateType = CameraDevice.TEMPLATE_STILL_CAPTURE

            CaptureType.PREVIEW,
            CaptureType.IMAGE_ANALYSIS,
            CaptureType.VIDEO_CAPTURE,
            CaptureType.STREAM_SHARING ->
                captureBuilder.templateType = CameraDevice.TEMPLATE_RECORD
        }
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG,
            captureBuilder.build()
        )

        // Only CAPTURE_TYPE_IMAGE_CAPTURE has its own ImageCaptureOptionUnpacker. Other
        // capture types all use the standard DefaultCaptureOptionsUnpacker.
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER,
            if (captureType == CaptureType.IMAGE_CAPTURE) {
                ImageCaptureOptionUnpacker.INSTANCE
            } else {
                DefaultCaptureOptionsUnpacker.INSTANCE
            }
        )
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER,
            DefaultSessionOptionsUnpacker
        )

        if (captureType == CaptureType.PREVIEW) {
            val previewSize = displayInfoManager.getPreviewSize()
            mutableConfig.insertOption(
                ImageOutputConfig.OPTION_MAX_RESOLUTION,
                previewSize
            )
            mutableConfig.insertOption(
                OPTION_RESOLUTION_SELECTOR,
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        previewSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                    )
                ).build()
            )
        }

        mutableConfig.insertOption(
            ImageOutputConfig.OPTION_TARGET_ROTATION,
            displayInfoManager.defaultDisplay.rotation
        )
        return OptionsBundle.from(mutableConfig)
    }

    open class DefaultCaptureOptionsUnpacker : CaptureConfig.OptionUnpacker {
        @OptIn(ExperimentalCamera2Interop::class)
        override fun unpack(config: UseCaseConfig<*>, builder: CaptureConfig.Builder) {
            val defaultCaptureConfig = config.getDefaultCaptureConfig(null)

            var implOptions: Config = OptionsBundle.emptyBundle()
            var templateType = CaptureConfig.defaultEmptyCaptureConfig().templateType

            // Apply/extract defaults from session config
            if (defaultCaptureConfig != null) {
                templateType = defaultCaptureConfig.templateType
                builder.addAllCameraCaptureCallbacks(defaultCaptureConfig.cameraCaptureCallbacks)
                implOptions = defaultCaptureConfig.implementationOptions

                // Also copy these info to the CaptureConfig
                builder.setUseRepeatingSurface(defaultCaptureConfig.isUseRepeatingSurface)
                builder.addAllTags(defaultCaptureConfig.tagBundle)
                defaultCaptureConfig.surfaces.forEach { builder.addSurface(it) }
            }

            // Set the any additional implementation options
            builder.implementationOptions = implOptions

            // Get Camera2 extended options
            val camera2Config = Camera2ImplConfig(config)

            // Apply template type
            builder.templateType = camera2Config.getCaptureRequestTemplate(templateType)

            // Add extension callbacks
            camera2Config.getSessionCaptureCallback()?.let {
                builder.addCameraCaptureCallback(CaptureCallbackContainer.create(it))
            }

            // Copy extension keys
            builder.addImplementationOptions(camera2Config.captureRequestOptions)
        }

        companion object {
            val INSTANCE = DefaultCaptureOptionsUnpacker()
        }
    }

    class ImageCaptureOptionUnpacker : DefaultCaptureOptionsUnpacker() {

        override fun unpack(config: UseCaseConfig<*>, builder: CaptureConfig.Builder) {
            super.unpack(config, builder)
            require(config is ImageCaptureConfig) { "config is not ImageCaptureConfig" }
            builder.addImplementationOptions(
                Camera2ImplConfig.Builder().apply { toggleHDRPlus(config) }.build()
            )
        }

        companion object {
            val INSTANCE = ImageCaptureOptionUnpacker()
        }
    }

    object DefaultSessionOptionsUnpacker : SessionConfig.OptionUnpacker {
        @OptIn(ExperimentalCamera2Interop::class)
        override fun unpack(
            resolution: Size,
            config: UseCaseConfig<*>,
            builder: SessionConfig.Builder
        ) {
            val defaultSessionConfig = config.getDefaultSessionConfig( /*valueIfMissing=*/null)

            var implOptions: Config = OptionsBundle.emptyBundle()
            var templateType = SessionConfig.defaultEmptySessionConfig().templateType

            // Apply/extract defaults from session config
            if (defaultSessionConfig != null) {
                templateType = defaultSessionConfig.templateType
                builder.addAllDeviceStateCallbacks(defaultSessionConfig.deviceStateCallbacks)
                builder.addAllSessionStateCallbacks(defaultSessionConfig.sessionStateCallbacks)
                builder.addAllRepeatingCameraCaptureCallbacks(
                    defaultSessionConfig.repeatingCameraCaptureCallbacks
                )
                implOptions = defaultSessionConfig.implementationOptions
            }

            // Set any additional implementation options
            builder.setImplementationOptions(implOptions)

            if (config is PreviewConfig) {
                // Set the WYSIWYG preview for CAPTURE_TYPE_PREVIEW
                builder.setupHDRnet(resolution)
            }

            // Get Camera2 extended options
            val camera2Config = Camera2ImplConfig(config)

            // Apply template type
            builder.setTemplateType(camera2Config.getCaptureRequestTemplate(templateType))

            // Add extension callbacks
            camera2Config.getDeviceStateCallback()?.let {
                builder.addDeviceStateCallback(it)
            }
            camera2Config.getSessionStateCallback()?.let {
                builder.addSessionStateCallback(it)
            }
            camera2Config.getSessionCaptureCallback()?.let {
                builder.addCameraCaptureCallback(CaptureCallbackContainer.create(it))
            }

            // TODO: Copy CameraEventCallback (used for extension)

            // Copy extended Camera2 configurations
            val extendedConfig = MutableOptionsBundle.create().apply {
                camera2Config.getPhysicalCameraId()?.let { physicalCameraId ->
                    insertOption(
                        SESSION_PHYSICAL_CAMERA_ID_OPTION,
                        physicalCameraId
                    )
                }

                camera2Config.getStreamUseCase()?.let { streamUseCase ->
                    insertOption(
                        STREAM_USE_CASE_OPTION,
                        streamUseCase
                    )
                }
            }
            builder.addImplementationOptions(extendedConfig)

            // Copy extension keys
            builder.addImplementationOptions(camera2Config.captureRequestOptions)
        }
    }

    /**
     * A [CameraCaptureCallback] which contains an [CaptureCallback] and doesn't handle the
     * callback.
     */
    internal class CaptureCallbackContainer private constructor(
        val captureCallback: CaptureCallback
    ) : CameraCaptureCallback() {
        // TODO(b/192980959): Find a way to receive the CameraCaptureSession signal
        //  from the camera-pipe library and redirect to the [captureCallback].
        companion object {
            fun create(captureCallback: CaptureCallback): CaptureCallbackContainer {
                return CaptureCallbackContainer(captureCallback)
            }
        }
    }
}
