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
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.core.Log.error
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.pipe.integration.compat.workaround.getSupportedRepeatingSurfaceSizes
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.CloseableErrorListener
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE
import androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME
import kotlin.math.min

private val DEFAULT_PREVIEW_SIZE = Size(0, 0)

/**
 * A [UseCase] used to issue repeating requests when only [androidx.camera.core.ImageCapture] is
 * enabled, since taking a picture may require a repeating surface to perform pre-capture checks,
 * mainly around 3A.
 */
public class MeteringRepeating(
    private val cameraProperties: CameraProperties,
    config: MeteringRepeatingConfig,
    private val displayInfoManager: DisplayInfoManager
) : UseCase(config) {

    private val meteringSurfaceSize = getProperPreviewSize()

    private val deferrableSurfaceLock = Any()

    private var closeableErrorListener: CloseableErrorListener? = null

    @GuardedBy("deferrableSurfaceLock") private var deferrableSurface: DeferrableSurface? = null

    override fun getDefaultConfig(
        applyDefaultConfig: Boolean,
        factory: UseCaseConfigFactory
    ): MeteringRepeatingConfig = Builder(cameraProperties, displayInfoManager).useCaseConfig

    override fun getUseCaseConfigBuilder(config: Config): Builder =
        Builder(cameraProperties, displayInfoManager)

    override fun onSuggestedStreamSpecUpdated(
        primaryStreamSpec: StreamSpec,
        secondaryStreamSpec: StreamSpec?,
    ): StreamSpec {
        updateSessionConfig(listOf(createPipeline(meteringSurfaceSize).build()))
        notifyActive()
        return primaryStreamSpec.toBuilder().setResolution(meteringSurfaceSize).build()
    }

    override fun onUnbind() {
        synchronized(deferrableSurfaceLock) {
            deferrableSurface?.close()
            deferrableSurface = null
        }
    }

    /** Sets up the use case's session configuration, mainly its [DeferrableSurface]. */
    public fun setupSession() {
        // The suggested stream spec passed to `updateSuggestedStreamSpec` doesn't matter since
        // this use case uses the min preview size.
        updateSuggestedStreamSpec(StreamSpec.builder(DEFAULT_PREVIEW_SIZE).build(), null)
    }

    private fun createPipeline(resolution: Size): SessionConfig.Builder {
        synchronized(deferrableSurfaceLock) {
            val surfaceTexture =
                SurfaceTexture(0).apply {
                    setDefaultBufferSize(resolution.width, resolution.height)
                }
            val surface = Surface(surfaceTexture)

            deferrableSurface?.close()
            deferrableSurface = ImmediateSurface(surface, resolution, imageFormat)
            deferrableSurface!!
                .terminationFuture
                .addListener(
                    {
                        surface.release()
                        surfaceTexture.release()
                    },
                    CameraXExecutors.directExecutor()
                )
        }

        // Closes the old error listener if there is
        closeableErrorListener?.close()
        val errorListener = CloseableErrorListener { _, _ ->
            updateSessionConfig(listOf(createPipeline(resolution).build()))
            notifyReset()
        }
        closeableErrorListener = errorListener

        return SessionConfig.Builder.createFrom(MeteringRepeatingConfig(), resolution).apply {
            setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
            addSurface(deferrableSurface!!)
            setErrorListener(errorListener)
        }
    }

    private fun CameraProperties.getOutputSizes(): Array<Size>? {
        val map =
            metadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: run {
                    error { "Can not retrieve SCALER_STREAM_CONFIGURATION_MAP." }
                    return null
                }

        return if (Build.VERSION.SDK_INT < 23) {
            // ImageFormat.PRIVATE is only public after Android level 23. Therefore, use
            // SurfaceTexture.class to get the supported output sizes before Android level 23.
            map.getOutputSizes(SurfaceTexture::class.java)
        } else {
            map.getOutputSizes(ImageFormat.PRIVATE)
        }
    }

    private fun getProperPreviewSize(): Size {
        var outputSizes = cameraProperties.getOutputSizes()

        if (outputSizes == null) {
            error { "Can not get output size list." }
            return DEFAULT_PREVIEW_SIZE
        }

        if (outputSizes.isEmpty()) {
            error { "Output sizes empty" }
            return DEFAULT_PREVIEW_SIZE
        }

        val supportedOutputSizes = outputSizes.getSupportedRepeatingSurfaceSizes()

        if (supportedOutputSizes.isNotEmpty()) {
            outputSizes = supportedOutputSizes
        } else {
            warn { "No supported output size list, fallback to current list" }
        }

        outputSizes.sortBy { size -> size.width.toLong() * size.height.toLong() }

        // Find maximum supported resolution that is <= min(VGA, display resolution)
        // Using minimum supported size could cause some issue on certain devices.
        val previewSize = displayInfoManager.getPreviewSize()
        val maxSizeProduct =
            min(640L * 480L, previewSize.width.toLong() * previewSize.height.toLong())

        var previousSize: Size? = null
        for (outputSize in outputSizes) {
            val product = outputSize.width.toLong() * outputSize.height.toLong()
            if (product == maxSizeProduct) {
                return outputSize
            } else if (product > maxSizeProduct) {
                return previousSize ?: break // fallback to minimum size.
            }
            previousSize = outputSize
        }

        // If not found, return the minimum size.
        return outputSizes[0]
    }

    public class MeteringRepeatingConfig : UseCaseConfig<MeteringRepeating>, ImageInputConfig {
        private val config =
            MutableOptionsBundle.create().apply {
                insertOption(
                    OPTION_SESSION_CONFIG_UNPACKER,
                    CameraUseCaseAdapter.DefaultSessionOptionsUnpacker
                )
                insertOption(OPTION_TARGET_NAME, "MeteringRepeating")
                insertOption(OPTION_CAPTURE_TYPE, CaptureType.METERING_REPEATING)
            }

        override fun getCaptureType(): CaptureType =
            UseCaseConfigFactory.CaptureType.METERING_REPEATING

        override fun getConfig(): MutableOptionsBundle = config

        override fun getInputFormat(): Int =
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
    }

    public class Builder(
        private val cameraProperties: CameraProperties,
        private val displayInfoManager: DisplayInfoManager
    ) : UseCaseConfig.Builder<MeteringRepeating, MeteringRepeatingConfig, Builder> {

        override fun getMutableConfig(): MutableOptionsBundle = MutableOptionsBundle.create()

        override fun getUseCaseConfig(): MeteringRepeatingConfig = MeteringRepeatingConfig()

        override fun setTargetClass(targetClass: Class<MeteringRepeating>): Builder = this

        override fun setTargetName(targetName: String): Builder = this

        override fun setDefaultSessionConfig(sessionConfig: SessionConfig): Builder = this

        override fun setDefaultCaptureConfig(captureConfig: CaptureConfig): Builder = this

        override fun setSessionOptionUnpacker(
            optionUnpacker: SessionConfig.OptionUnpacker
        ): Builder = this

        override fun setCaptureOptionUnpacker(
            optionUnpacker: CaptureConfig.OptionUnpacker
        ): Builder = this

        override fun setSurfaceOccupancyPriority(priority: Int): Builder = this

        override fun setZslDisabled(disabled: Boolean): Builder = this

        override fun setHighResolutionDisabled(disabled: Boolean): Builder = this

        override fun setCaptureType(captureType: UseCaseConfigFactory.CaptureType): Builder = this

        override fun build(): MeteringRepeating {
            return MeteringRepeating(cameraProperties, useCaseConfig, displayInfoManager)
        }
    }
}
