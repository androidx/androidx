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

package androidx.camera.camera2.impl

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.util.Size
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.compat.workaround.getSupportedRepeatingSurfaceSizes
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
import androidx.camera.core.impl.StreamUseCase
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE
import androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME
import kotlin.collections.isEmpty
import kotlin.collections.sortBy
import kotlin.math.min

private val DEFAULT_PREVIEW_SIZE = Size(640, 480) // VGA

/**
 * A [UseCase] used to issue repeating requests when only [androidx.camera.core.ImageCapture] is
 * enabled, since taking a picture may require a repeating surface to perform pre-capture checks,
 * mainly around 3A.
 */
public class MeteringRepeating(
    private val cameraProperties: CameraProperties,
    config: MeteringRepeatingConfig,
    private val displayInfoManager: DisplayInfoManager,
) : UseCase(config) {

    private val meteringSurfaceSize = getProperPreviewSize(cameraProperties, displayInfoManager)

    private val deferrableSurfaceLock = Any()

    private var closeableErrorListener: CloseableErrorListener? = null

    @GuardedBy("deferrableSurfaceLock") private var deferrableSurface: DeferrableSurface? = null

    override fun getDefaultConfig(
        applyDefaultConfig: Boolean,
        factory: UseCaseConfigFactory,
    ): MeteringRepeatingConfig = Builder(cameraProperties, displayInfoManager).useCaseConfig

    override fun getUseCaseConfigBuilder(config: Config): Builder =
        Builder(cameraProperties, displayInfoManager)

    override fun onSuggestedStreamSpecUpdated(
        primaryStreamSpec: StreamSpec,
        secondaryStreamSpec: StreamSpec?,
    ): StreamSpec {
        updateSessionConfig(listOf(createPipeline(meteringSurfaceSize).build()))
        return primaryStreamSpec.toBuilder().setResolution(meteringSurfaceSize).build()
    }

    override fun onUnbind() {
        closeableErrorListener?.close()
        closeableErrorListener = null
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
        val surface =
            synchronized(deferrableSurfaceLock) { createAndManageDeferrableSurface(resolution) }

        // Closes the old error listener if there is
        closeableErrorListener?.close()
        val errorListener = CloseableErrorListener { _, _ ->
            updateSessionConfig(listOf(createPipeline(resolution).build()))
            notifyReset()
        }
        closeableErrorListener = errorListener

        return SessionConfig.Builder.createFrom(MeteringRepeatingConfig(), resolution).apply {
            setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
            addSurface(surface)
            setErrorListener(errorListener)
        }
    }

    /**
     * Creates a new DeferrableSurface and links its lifecycle to a new Surface/SurfaceTexture. This
     * method MUST be called inside the `deferrableSurfaceLock`.
     */
    @GuardedBy("deferrableSurfaceLock")
    private fun createAndManageDeferrableSurface(resolution: Size): DeferrableSurface {
        val surfaceTexture =
            SurfaceTexture(0).apply { setDefaultBufferSize(resolution.width, resolution.height) }
        val surface = Surface(surfaceTexture)

        // Close previous surface if any
        deferrableSurface?.close()

        // Create new surface and assign it
        val newSurface = ImmediateSurface(surface, resolution, imageFormat)
        deferrableSurface = newSurface

        // Link the new surface's lifetime to the native resources
        newSurface.terminationFuture.addListener(
            {
                surface.release()
                surfaceTexture.release()
            },
            CameraXExecutors.directExecutor(),
        )
        return newSurface
    }

    public class MeteringRepeatingConfig : UseCaseConfig<MeteringRepeating>, ImageInputConfig {
        private val config =
            MutableOptionsBundle.create().apply {
                insertOption(
                    OPTION_SESSION_CONFIG_UNPACKER,
                    CameraUseCaseAdapter.DefaultSessionOptionsUnpacker,
                )
                insertOption(OPTION_TARGET_NAME, "MeteringRepeating")
                insertOption(OPTION_CAPTURE_TYPE, CaptureType.METERING_REPEATING)
            }

        override fun getCaptureType(): CaptureType = CaptureType.METERING_REPEATING

        override fun getConfig(): MutableOptionsBundle = config

        override fun getInputFormat(): Int =
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
    }

    public class Builder(
        private val cameraProperties: CameraProperties,
        private val displayInfoManager: DisplayInfoManager,
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

        override fun setStreamUseCase(streamUseCase: StreamUseCase): Builder = this

        override fun build(): MeteringRepeating {
            return MeteringRepeating(cameraProperties, useCaseConfig, displayInfoManager)
        }
    }
}

internal fun getProperPreviewSize(
    cameraProperties: CameraProperties,
    displayInfoManager: DisplayInfoManager,
): Size {
    var outputSizes = cameraProperties.getOutputSizes() ?: return DEFAULT_PREVIEW_SIZE

    if (outputSizes.isEmpty()) {
        return DEFAULT_PREVIEW_SIZE
    }

    val supportedOutputSizes = outputSizes.getSupportedRepeatingSurfaceSizes()

    if (supportedOutputSizes.isNotEmpty()) {
        outputSizes = supportedOutputSizes
    } else {
        Camera2Logger.warn { "No supported output size list, fallback to current list" }
    }

    outputSizes.sortBy { size -> size.width.toLong() * size.height.toLong() }

    // Find maximum supported resolution that is <= min(VGA, display resolution)
    // Using minimum supported size could cause some issue on certain devices.
    val previewSize = displayInfoManager.getPreviewSize()
    val maxSizeProduct = min(640L * 480L, previewSize.width.toLong() * previewSize.height.toLong())

    var previousSize: Size? = null
    for (outputSize in outputSizes) {
        val product = outputSize.width.toLong() * outputSize.height.toLong()
        if (product == maxSizeProduct) {
            return outputSize
        } else if (product > maxSizeProduct) {
            // Returns the maximum supported resolution that is <= min(VGA, display resolution)
            // if it is found
            return previousSize ?: break
        }
        previousSize = outputSize
    }

    // If not found, return the minimum size.
    return previousSize ?: outputSizes[0]
}

private fun CameraProperties.getOutputSizes(): Array<Size>? {
    val map =
        metadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            ?: run {
                Camera2Logger.error { "Can not retrieve SCALER_STREAM_CONFIGURATION_MAP." }
                return null
            }

    return map.getOutputSizes(ImageFormat.PRIVATE)
}
