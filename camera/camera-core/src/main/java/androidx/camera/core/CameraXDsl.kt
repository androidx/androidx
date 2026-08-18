/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("CameraXDslUtils")
@file:Suppress("DEPRECATION")

package androidx.camera.core

import android.util.Range
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OptionalRotationValue
import androidx.camera.core.impl.ImageOutputConfig.RotationValue
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.resolutionselector.ResolutionSelector
import java.util.concurrent.Executor

@DslMarker public annotation class CameraXDsl

/**
 * Scope for CameraX DSL builders that supports applying interoperability options.
 *
 * Implemented by CameraX UseCase Kotlin DSL scope classes (such as [PreviewScope],
 * [ImageAnalysisScope], and [androidx.camera.video.VideoCaptureScope]) to expose their underlying
 * builder for interop extension functions like `camera2Interop { ... }`.
 *
 * @param B type of builder configured by this scope
 */
@CameraXDsl
public interface InteropConfigurableScope<out B> {
    /** Underlying builder configured by this scope. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val builder: B
        get() {
            // have a default implementation to avoid the HiddenAbstractMethodInInterface metalava
            // error
            throw NotImplementedError()
        }
}

/**
 * Creates a [Preview] using a Kotlin DSL block.
 *
 * @param block A receiver lambda on [PreviewScope] to configure the preview.
 * @sample androidx.camera.core.samples.useCaseDslSample
 */
public fun preview(block: PreviewScope.() -> Unit): Preview {
    return PreviewScope().apply(block).build()
}

/** Scope class for [Preview] configuration DSL. */
@CameraXDsl
public class PreviewScope
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val builder: Preview.Builder = Preview.Builder()
) : InteropConfigurableScope<Preview.Builder> {
    private var _targetName: String = ""

    /**
     * Sets or gets the target name for this use case configuration.
     *
     * @see Preview.Builder.setTargetName
     */
    @get:NonNull
    @set:NonNull
    public var targetName: String
        get() = _targetName
        set(@NonNull value) {
            _targetName = value
            builder.setTargetName(value)
        }

    private var _targetRotation: Int = ImageOutputConfig.ROTATION_NOT_SPECIFIED

    /**
     * Sets or gets the target rotation for this use case.
     *
     * @see Preview.Builder.setTargetRotation
     */
    @get:OptionalRotationValue
    @setparam:RotationValue
    public var targetRotation: Int
        get() = _targetRotation
        set(value) {
            _targetRotation = value
            builder.setTargetRotation(value)
        }

    private var _resolutionSelector: ResolutionSelector = ResolutionSelector.Builder().build()

    /**
     * Sets or gets the resolution selector for this use case.
     *
     * @see Preview.Builder.setResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var resolutionSelector: ResolutionSelector
        get() = _resolutionSelector
        set(@NonNull value) {
            _resolutionSelector = value
            builder.setResolutionSelector(value)
        }

    private var _dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED

    /**
     * Sets or gets the dynamic range for this use case.
     *
     * @see Preview.Builder.setDynamicRange
     */
    @get:NonNull
    @set:NonNull
    public var dynamicRange: DynamicRange
        get() = _dynamicRange
        set(@NonNull value) {
            _dynamicRange = value
            builder.setDynamicRange(value)
        }

    private var _mirrorMode: Int = MirrorMode.MIRROR_MODE_UNSPECIFIED

    /**
     * Sets or gets the mirror mode for this use case.
     *
     * @see Preview.Builder.setMirrorMode
     */
    @get:MirrorMode.Mirror
    @setparam:MirrorMode.Mirror
    public var mirrorMode: Int
        get() = _mirrorMode
        set(value) {
            _mirrorMode = value
            builder.setMirrorMode(value)
        }

    private var _isPreviewStabilizationEnabled: Boolean = false

    /**
     * Sets or gets whether preview stabilization is enabled for this use case.
     *
     * @see Preview.Builder.setPreviewStabilizationEnabled
     */
    public var isPreviewStabilizationEnabled: Boolean
        get() = _isPreviewStabilizationEnabled
        set(value) {
            _isPreviewStabilizationEnabled = value
            builder.setPreviewStabilizationEnabled(value)
        }

    private var _targetFrameRate: Range<Int> = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED

    /**
     * Sets or gets the target frame rate range for this use case.
     *
     * @see Preview.Builder.setTargetFrameRate
     */
    @get:NonNull
    @set:NonNull
    public var targetFrameRate: Range<Int>
        get() = _targetFrameRate
        set(@NonNull value) {
            _targetFrameRate = value
            builder.setTargetFrameRate(value)
        }

    internal fun build(): Preview = builder.build()
}

/**
 * Creates an [ImageCapture] using a Kotlin DSL block.
 *
 * @param block A receiver lambda on [ImageCaptureScope] to configure the image capture.
 * @sample androidx.camera.core.samples.useCaseDslSample
 */
public fun imageCapture(block: ImageCaptureScope.() -> Unit): ImageCapture {
    return ImageCaptureScope().apply(block).build()
}

/** Scope class for [ImageCapture] configuration DSL. */
@CameraXDsl
public class ImageCaptureScope
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val builder: ImageCapture.Builder = ImageCapture.Builder()
) {
    private var _targetName: String = ""

    /**
     * Sets or gets the target name for this use case configuration.
     *
     * @see ImageCapture.Builder.setTargetName
     */
    @get:NonNull
    @set:NonNull
    public var targetName: String
        get() = _targetName
        set(@NonNull value) {
            _targetName = value
            builder.setTargetName(value)
        }

    private var _targetRotation: Int = ImageOutputConfig.ROTATION_NOT_SPECIFIED

    /**
     * Sets or gets the target rotation for this use case.
     *
     * @see ImageCapture.Builder.setTargetRotation
     */
    @get:OptionalRotationValue
    @setparam:RotationValue
    public var targetRotation: Int
        get() = _targetRotation
        set(value) {
            _targetRotation = value
            builder.setTargetRotation(value)
        }

    private var _resolutionSelector: ResolutionSelector = ResolutionSelector.Builder().build()

    /**
     * Sets or gets the resolution selector for this use case.
     *
     * @see ImageCapture.Builder.setResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var resolutionSelector: ResolutionSelector
        get() = _resolutionSelector
        set(@NonNull value) {
            _resolutionSelector = value
            builder.setResolutionSelector(value)
        }

    private var _ioExecutor: Executor = CameraXExecutors.ioExecutor()

    /**
     * Sets or gets the background executor for I/O tasks.
     *
     * @see ImageCapture.Builder.setIoExecutor
     */
    @get:NonNull
    @set:NonNull
    public var ioExecutor: Executor
        get() = _ioExecutor
        set(@NonNull value) {
            _ioExecutor = value
            builder.setIoExecutor(value)
        }

    private var _captureMode: Int = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY

    /**
     * Sets or gets the capture mode for this use case.
     *
     * @see ImageCapture.Builder.setCaptureMode
     */
    @get:ImageCapture.CaptureMode
    @setparam:ImageCapture.CaptureMode
    public var captureMode: Int
        get() = _captureMode
        set(value) {
            _captureMode = value
            builder.setCaptureMode(value)
        }

    private var _flashMode: Int = ImageCapture.FLASH_MODE_OFF

    /**
     * Sets or gets the flash mode for this use case.
     *
     * @see ImageCapture.Builder.setFlashMode
     */
    @get:ImageCapture.FlashMode
    @setparam:ImageCapture.FlashMode
    public var flashMode: Int
        get() = _flashMode
        set(value) {
            _flashMode = value
            builder.setFlashMode(value)
        }

    private var _isPostviewEnabled: Boolean = false

    /**
     * Sets or gets whether postview is enabled for this use case.
     *
     * @see ImageCapture.Builder.setPostviewEnabled
     */
    public var isPostviewEnabled: Boolean
        get() = _isPostviewEnabled
        set(value) {
            _isPostviewEnabled = value
            builder.setPostviewEnabled(value)
        }

    private var _jpegQuality: Int = 100

    /**
     * Sets or gets the JPEG quality for this use case.
     *
     * @see ImageCapture.Builder.setJpegQuality
     */
    @get:IntRange(from = 1, to = 100)
    @setparam:IntRange(from = 1, to = 100)
    public var jpegQuality: Int
        get() = _jpegQuality
        set(value) {
            _jpegQuality = value
            builder.setJpegQuality(value)
        }

    private var _screenFlash: ImageCapture.ScreenFlash? = null

    /**
     * Sets or gets the ScreenFlash implementation for this use case.
     *
     * Note: Setting this property to `null` is a no-op on the underlying [ImageCapture.Builder]
     * because [ImageCapture.Builder.setScreenFlash] requires a non-null [ImageCapture.ScreenFlash].
     *
     * @see ImageCapture.Builder.setScreenFlash
     */
    @get:Nullable
    @set:Nullable
    public var screenFlash: ImageCapture.ScreenFlash?
        get() = _screenFlash
        set(value) {
            _screenFlash = value
            if (value != null) {
                builder.setScreenFlash(value)
            }
        }

    private var _outputFormat: @ImageCapture.OutputFormat Int =
        ImageCapture.Defaults.DEFAULT_OUTPUT_FORMAT

    /**
     * Sets or gets the output format for this use case.
     *
     * @see ImageCapture.Builder.setOutputFormat
     */
    public var outputFormat: @ImageCapture.OutputFormat Int
        get() = _outputFormat
        set(value) {
            _outputFormat = value
            builder.setOutputFormat(value)
        }

    private var _postviewResolutionSelector: ResolutionSelector =
        ResolutionSelector.Builder().build()

    /**
     * Sets or gets the resolution selector for postview images.
     *
     * @see ImageCapture.Builder.setPostviewResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var postviewResolutionSelector: ResolutionSelector
        get() = _postviewResolutionSelector
        set(@NonNull value) {
            _postviewResolutionSelector = value
            builder.setPostviewResolutionSelector(value)
        }

    internal fun build(): ImageCapture = builder.build()
}

/**
 * Creates an [ImageAnalysis] using a Kotlin DSL block.
 *
 * @param block A receiver lambda on [ImageAnalysisScope] to configure the image analysis.
 * @sample androidx.camera.core.samples.useCaseDslSample
 */
public fun imageAnalysis(block: ImageAnalysisScope.() -> Unit): ImageAnalysis {
    return ImageAnalysisScope().apply(block).build()
}

/** Scope class for [ImageAnalysis] configuration DSL. */
@CameraXDsl
public class ImageAnalysisScope
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val builder: ImageAnalysis.Builder = ImageAnalysis.Builder()
) : InteropConfigurableScope<ImageAnalysis.Builder> {
    private var _targetName: String = ""

    /**
     * Sets or gets the target name for this use case configuration.
     *
     * @see ImageAnalysis.Builder.setTargetName
     */
    @get:NonNull
    @set:NonNull
    public var targetName: String
        get() = _targetName
        set(@NonNull value) {
            _targetName = value
            builder.setTargetName(value)
        }

    private var _targetRotation: Int = ImageOutputConfig.ROTATION_NOT_SPECIFIED

    /**
     * Sets or gets the target rotation for this use case.
     *
     * @see ImageAnalysis.Builder.setTargetRotation
     */
    @get:OptionalRotationValue
    @setparam:RotationValue
    public var targetRotation: Int
        get() = _targetRotation
        set(value) {
            _targetRotation = value
            builder.setTargetRotation(value)
        }

    private var _resolutionSelector: ResolutionSelector = ResolutionSelector.Builder().build()

    /**
     * Sets or gets the resolution selector for this use case.
     *
     * @see ImageAnalysis.Builder.setResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var resolutionSelector: ResolutionSelector
        get() = _resolutionSelector
        set(@NonNull value) {
            _resolutionSelector = value
            builder.setResolutionSelector(value)
        }

    private var _backgroundExecutor: Executor = CameraXExecutors.highPriorityExecutor()

    /**
     * Sets or gets the background executor for image analysis tasks.
     *
     * @see ImageAnalysis.Builder.setBackgroundExecutor
     */
    @get:NonNull
    @set:NonNull
    public var backgroundExecutor: Executor
        get() = _backgroundExecutor
        set(@NonNull value) {
            _backgroundExecutor = value
            builder.setBackgroundExecutor(value)
        }

    private var _backpressureStrategy: Int = ImageAnalysis.DEFAULT_BACKPRESSURE_STRATEGY

    /**
     * Sets or gets the backpressure strategy for this use case.
     *
     * @see ImageAnalysis.Builder.setBackpressureStrategy
     */
    @get:ImageAnalysis.BackpressureStrategy
    @setparam:ImageAnalysis.BackpressureStrategy
    public var backpressureStrategy: Int
        get() = _backpressureStrategy
        set(value) {
            _backpressureStrategy = value
            builder.setBackpressureStrategy(value)
        }

    private var _imageQueueDepth: Int = ImageAnalysis.DEFAULT_IMAGE_QUEUE_DEPTH

    /**
     * Sets or gets the image queue depth for this use case.
     *
     * @see ImageAnalysis.Builder.setImageQueueDepth
     */
    public var imageQueueDepth: Int
        get() = _imageQueueDepth
        set(value) {
            _imageQueueDepth = value
            builder.setImageQueueDepth(value)
        }

    private var _outputImageFormat: Int = ImageAnalysis.DEFAULT_OUTPUT_IMAGE_FORMAT

    /**
     * Sets or gets the output image format for this use case.
     *
     * @see ImageAnalysis.Builder.setOutputImageFormat
     */
    @get:ImageAnalysis.OutputImageFormat
    @setparam:ImageAnalysis.OutputImageFormat
    public var outputImageFormat: Int
        get() = _outputImageFormat
        set(value) {
            _outputImageFormat = value
            builder.setOutputImageFormat(value)
        }

    private var _isOutputImageRotationEnabled: Boolean = false

    /**
     * Sets or gets whether output image rotation is enabled for this use case.
     *
     * @see ImageAnalysis.Builder.setOutputImageRotationEnabled
     */
    public var isOutputImageRotationEnabled: Boolean
        get() = _isOutputImageRotationEnabled
        set(value) {
            _isOutputImageRotationEnabled = value
            builder.setOutputImageRotationEnabled(value)
        }

    internal fun build(): ImageAnalysis = builder.build()
}
