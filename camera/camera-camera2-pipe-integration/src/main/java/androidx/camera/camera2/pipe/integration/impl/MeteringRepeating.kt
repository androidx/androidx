/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.core.Log.error
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.core.util.Consumer

private val DEFAULT_PREVIEW_SIZE = Size(0, 0)

/**
 * A [UseCase] used to issue repeating requests when only [androidx.camera.core.ImageCapture] is
 * enabled, since taking a picture may require a repeating surface to perform pre-capture checks,
 * mainly around 3A.
 */
class MeteringRepeating(
    private val cameraProperties: CameraProperties,
    config: MeteringRepeatingConfig,
) : UseCase(config) {

    private val meteringSurfaceSize = cameraProperties.getMinimumPreviewSize()
    private var deferrableSurface: DeferrableSurface? = null

    override fun getDefaultConfig(applyDefaultConfig: Boolean, factory: UseCaseConfigFactory) =
        Builder(cameraProperties).useCaseConfig

    override fun getUseCaseConfigBuilder(config: Config) = Builder(cameraProperties)

    override fun onSuggestedResolutionUpdated(suggestedResolution: Size): Size {
        updateSessionConfig(createPipeline().build())
        notifyActive()
        return meteringSurfaceSize
    }

    override fun onDetached() {
        deferrableSurface?.close()
        deferrableSurface = null
    }

    /** Sets up the use case's session configuration, mainly its [DeferrableSurface]. */
    fun setupSession() {
        // The suggested resolution passed to `updateSuggestedResolution` doesn't matter since
        // this use case uses the min preview size.
        updateSuggestedResolution(DEFAULT_PREVIEW_SIZE)
    }

    private fun createPipeline(): SessionConfig.Builder {
        val surfaceTexture = SurfaceTexture(0).apply {
            setDefaultBufferSize(meteringSurfaceSize.width, meteringSurfaceSize.height)
        }
        val surface = Surface(surfaceTexture)

        deferrableSurface?.close()
        deferrableSurface = ImmediateSurface(surface, meteringSurfaceSize, imageFormat)
        deferrableSurface!!.terminationFuture
            .addListener(
                {
                    surface.release()
                    surfaceTexture.release()
                },
                CameraXExecutors.directExecutor()
            )

        return SessionConfig.Builder
            .createFrom(MeteringRepeatingConfig())
            .apply {
                setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                addSurface(deferrableSurface!!)
                addErrorListener { _, _ ->
                    updateSessionConfig(createPipeline().build())
                    notifyReset()
                }
            }
    }

    private fun CameraProperties.getMinimumPreviewSize(): Size {
        val map = metadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
        if (map == null) {
            error { "Can not retrieve SCALER_STREAM_CONFIGURATION_MAP." }
            return DEFAULT_PREVIEW_SIZE
        }

        val outputSizes = if (Build.VERSION.SDK_INT < 23) {
            // ImageFormat.PRIVATE is only public after Android level 23. Therefore, use
            // SurfaceTexture.class to get the supported output sizes before Android level 23.
            map.getOutputSizes(SurfaceTexture::class.java)
        } else {
            map.getOutputSizes(ImageFormat.PRIVATE)
        }

        if (outputSizes == null) {
            error { "Can not get output size list." }
            return DEFAULT_PREVIEW_SIZE
        }

        check(outputSizes.isNotEmpty()) { "Output sizes empty" }
        return outputSizes.minWithOrNull { size1, size2 -> size1.area().compareTo(size2.area()) }!!
    }

    class MeteringRepeatingConfig : UseCaseConfig<MeteringRepeating>, ImageInputConfig {
        private val config = MutableOptionsBundle.create().apply {
            insertOption(
                OPTION_SESSION_CONFIG_UNPACKER,
                CameraUseCaseAdapter.DefaultSessionOptionsUnpacker
            )
        }

        override fun getConfig() = config

        override fun getInputFormat() = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
    }

    class Builder(private val cameraProperties: CameraProperties) :
        UseCaseConfig.Builder<MeteringRepeating, MeteringRepeatingConfig, Builder> {

        override fun getMutableConfig() = MutableOptionsBundle.create()

        override fun getUseCaseConfig() = MeteringRepeatingConfig()

        override fun setTargetClass(targetClass: Class<MeteringRepeating>) = this

        override fun setTargetName(targetName: String) = this

        override fun setUseCaseEventCallback(eventCallback: EventCallback) = this

        override fun setDefaultSessionConfig(sessionConfig: SessionConfig) = this

        override fun setDefaultCaptureConfig(captureConfig: CaptureConfig) = this

        override fun setSessionOptionUnpacker(optionUnpacker: SessionConfig.OptionUnpacker) = this

        override fun setCaptureOptionUnpacker(optionUnpacker: CaptureConfig.OptionUnpacker) = this

        override fun setSurfaceOccupancyPriority(priority: Int) = this

        override fun setCameraSelector(cameraSelector: CameraSelector) = this

        override fun setAttachedUseCasesUpdateListener(
            attachedUseCasesUpdateListener: Consumer<MutableCollection<UseCase>>
        ): Builder = this

        override fun build(): MeteringRepeating {
            return MeteringRepeating(cameraProperties, useCaseConfig)
        }
    }
}
