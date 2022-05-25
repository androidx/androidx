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
import android.graphics.Point
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory

/**
 * This class builds [Config] objects for a given [UseCaseConfigFactory.CaptureType].
 *
 * This includes things like default template and session parameters, as well as maximum resolution
 * and aspect ratios for the display.
 */
@Suppress("DEPRECATION")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraUseCaseAdapter(context: Context) : UseCaseConfigFactory {
    private val MAX_PREVIEW_SIZE = Size(1920, 1080)

    private val displayManager: DisplayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    private val defaultDisplay: Display by lazy {
        getMaxSizeDisplay()
    }
    private val previewSize: Size by lazy {
        calculatePreviewSize()
    }

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
        captureType: UseCaseConfigFactory.CaptureType,
        captureMode: Int
    ): Config? {
        debug { "Creating config for $captureType" }

        // TODO: quirks for ImageCapture are not ready for the UseCaseConfigFactory. Will need to
        //  move the quirk related item to this change.

        val mutableConfig = MutableOptionsBundle.create()
        val sessionBuilder = SessionConfig.Builder()
        when (captureType) {
            UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
            UseCaseConfigFactory.CaptureType.PREVIEW,
            UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS -> sessionBuilder.setTemplateType(
                CameraDevice.TEMPLATE_PREVIEW
            )
            UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE -> sessionBuilder.setTemplateType(
                CameraDevice.TEMPLATE_RECORD
            )
        }
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG,
            sessionBuilder.build()
        )
        val captureBuilder = CaptureConfig.Builder()
        when (captureType) {
            UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE ->
                captureBuilder.templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            UseCaseConfigFactory.CaptureType.PREVIEW,
            UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS,
            UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE ->
                captureBuilder.templateType = CameraDevice.TEMPLATE_RECORD
        }
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG,
            captureBuilder.build()
        )

        // TODO: the ImageCaptureOptionUnpacker not porting yet. Will need porting the
        //  ImageCaptureOptionUnpacker.

        // Only CAPTURE_TYPE_IMAGE_CAPTURE has its own ImageCaptureOptionUnpacker. Other
        // capture types all use the standard Camera2CaptureOptionUnpacker.
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER,
            DefaultCaptureOptionsUnpacker
        )
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER,
            DefaultSessionOptionsUnpacker
        )

        if (captureType == UseCaseConfigFactory.CaptureType.PREVIEW) {
            mutableConfig.insertOption(
                ImageOutputConfig.OPTION_MAX_RESOLUTION,
                previewSize
            )
        }

        mutableConfig.insertOption(
            ImageOutputConfig.OPTION_TARGET_ROTATION,
            defaultDisplay.rotation
        )
        return OptionsBundle.from(mutableConfig)
    }

    private fun getMaxSizeDisplay(): Display {
        val displays = displayManager.displays
        var maxDisplay: Display? = null
        var maxDisplaySize = -1
        for (display: Display in displays) {
            val displaySize = Point()
            // TODO(b/230400472): Use WindowManager#getCurrentWindowMetrics(). Display#getRealSize()
            //  is deprecated since API level 31.
            display.getRealSize(displaySize)
            if (displaySize.x * displaySize.y > maxDisplaySize) {
                maxDisplaySize = displaySize.x * displaySize.y
                maxDisplay = display
            }
        }
        return checkNotNull(maxDisplay) { "No displays found from ${displayManager.displays}!" }
    }

    /**
     * Calculates the device's screen resolution, or MAX_PREVIEW_SIZE, whichever is smaller.
     */
    private fun calculatePreviewSize(): Size {
        val displaySize = Point()
        val display: Display = defaultDisplay
        display.getRealSize(displaySize)
        var displayViewSize: Size
        displayViewSize = if (displaySize.x > displaySize.y) {
            Size(displaySize.x, displaySize.y)
        } else {
            Size(displaySize.y, displaySize.x)
        }
        if (displayViewSize.width * displayViewSize.height
            > MAX_PREVIEW_SIZE.width * MAX_PREVIEW_SIZE.height
        ) {
            displayViewSize = MAX_PREVIEW_SIZE
        }
        // TODO(b/230402463): Migrate extra cropping quirk from CameraX.
        return displayViewSize
    }

    object DefaultCaptureOptionsUnpacker : CaptureConfig.OptionUnpacker {
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
    }

    object DefaultSessionOptionsUnpacker : SessionConfig.OptionUnpacker {
        @OptIn(ExperimentalCamera2Interop::class)
        override fun unpack(config: UseCaseConfig<*>, builder: SessionConfig.Builder) {
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
