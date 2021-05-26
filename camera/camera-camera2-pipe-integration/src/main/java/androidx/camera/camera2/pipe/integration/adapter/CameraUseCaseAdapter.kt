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
import android.hardware.camera2.CameraDevice
import android.util.Size
import android.view.Display
import android.view.WindowManager
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.integration.impl.asLandscape
import androidx.camera.camera2.pipe.integration.impl.minByArea
import androidx.camera.camera2.pipe.integration.impl.toSize
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
class CameraUseCaseAdapter(context: Context) : UseCaseConfigFactory {

    private val display: Display by lazy {
        @Suppress("deprecation")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay!!
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

    /**
     * Returns the configuration for the given capture type, or `null` if the
     * configuration cannot be produced.
     */
    override fun getConfig(captureType: UseCaseConfigFactory.CaptureType): Config? {
        debug { "Creating config for $captureType" }

        val mutableConfig = MutableOptionsBundle.create()
        val sessionBuilder = SessionConfig.Builder()
        // TODO(b/114762170): Must set to preview here until we allow for multiple template
        //  types
        sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
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
                captureBuilder.templateType = CameraDevice.TEMPLATE_PREVIEW
        }
        mutableConfig.insertOption(
            UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG,
            captureBuilder.build()
        )

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
                getPreviewSize()
            )
        }

        mutableConfig.insertOption(
            ImageOutputConfig.OPTION_TARGET_ROTATION,
            display.rotation
        )
        return OptionsBundle.from(mutableConfig)
    }

    /**
     * Returns the device's screen resolution, or 1080p, whichever is smaller.
     */
    private fun getPreviewSize(): Size? {
        val displaySize = Point()
        display.getRealSize(displaySize)
        return minByArea(MAXIMUM_PREVIEW_SIZE, displaySize.toSize().asLandscape())
    }

    object DefaultCaptureOptionsUnpacker : CaptureConfig.OptionUnpacker {
        override fun unpack(config: UseCaseConfig<*>, builder: CaptureConfig.Builder) {
            val defaultCaptureConfig = config.defaultCaptureConfig
            builder.templateType = defaultCaptureConfig.templateType
            builder.implementationOptions = defaultCaptureConfig.implementationOptions
            builder.addAllCameraCaptureCallbacks(defaultCaptureConfig.cameraCaptureCallbacks)
            builder.setUseRepeatingSurface(defaultCaptureConfig.isUseRepeatingSurface)
            builder.addAllTags(defaultCaptureConfig.tagBundle)
            defaultCaptureConfig.surfaces.forEach { builder.addSurface(it) }

            // TODO: Add extensions-specific capture request options
        }
    }

    object DefaultSessionOptionsUnpacker : SessionConfig.OptionUnpacker {
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

            // Set the template type from default session config
            builder.setTemplateType(templateType)

            // TODO: Add Camera2 options and callbacks
        }
    }
}
