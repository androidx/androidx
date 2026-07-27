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

@file:Suppress("DEPRECATION")

package androidx.camera.core

import android.util.Range
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.camera.core.impl.ImageOutputConfig.RotationValue
import androidx.camera.core.resolutionselector.ResolutionSelector
import java.util.concurrent.Executor

@DslMarker public annotation class CameraXDsl

/**
 * Creates a [Preview] using a Kotlin DSL block.
 *
 * @param block A receiver lambda on [PreviewScope] to configure the preview.
 * @sample androidx.camera.core.samples.useCaseDslSample
 */
@JvmSynthetic
public fun preview(block: PreviewScope.() -> Unit): Preview {
    return PreviewScope().apply(block).build()
}

/** Scope class for [Preview] configuration DSL. */
@CameraXDsl
public class PreviewScope
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val builder: Preview.Builder = Preview.Builder()
) {
    /**
     * Sets the target name for this use case configuration.
     *
     * @see Preview.Builder.setTargetName
     */
    @get:NonNull
    @set:NonNull
    public var targetName: String
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setTargetName(value)
        }

    /**
     * Sets the target rotation for this use case.
     *
     * @see Preview.Builder.setTargetRotation
     */
    @get:RotationValue
    @set:RotationValue
    public var targetRotation: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setTargetRotation(value)
        }

    /**
     * Sets the resolution selector for this use case.
     *
     * @see Preview.Builder.setResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var resolutionSelector: ResolutionSelector
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setResolutionSelector(value)
        }

    /**
     * Sets the dynamic range for this use case.
     *
     * @see Preview.Builder.setDynamicRange
     */
    @get:NonNull
    @set:NonNull
    public var dynamicRange: DynamicRange
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setDynamicRange(value)
        }

    /**
     * Sets the mirror mode for this use case.
     *
     * @see Preview.Builder.setMirrorMode
     */
    @get:MirrorMode.Mirror
    @set:MirrorMode.Mirror
    public var mirrorMode: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setMirrorMode(value)
        }

    /**
     * Sets whether preview stabilization is enabled for this use case.
     *
     * @see Preview.Builder.setPreviewStabilizationEnabled
     */
    public var isPreviewStabilizationEnabled: Boolean
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setPreviewStabilizationEnabled(value)
        }

    /**
     * Sets the target frame rate range for this use case.
     *
     * @see Preview.Builder.setTargetFrameRate
     */
    @get:NonNull
    @set:NonNull
    public var targetFrameRate: Range<Int>
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setTargetFrameRate(value)
        }

    @JvmSynthetic internal fun build(): Preview = builder.build()
}

/**
 * Creates an [ImageCapture] using a Kotlin DSL block.
 *
 * @param block A receiver lambda on [ImageCaptureScope] to configure the image capture.
 * @sample androidx.camera.core.samples.useCaseDslSample
 */
@JvmSynthetic
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
    /**
     * Sets the target name for this use case configuration.
     *
     * @see ImageCapture.Builder.setTargetName
     */
    @get:NonNull
    @set:NonNull
    public var targetName: String
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setTargetName(value)
        }

    /**
     * Sets the target rotation for this use case.
     *
     * @see ImageCapture.Builder.setTargetRotation
     */
    @get:RotationValue
    @set:RotationValue
    public var targetRotation: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setTargetRotation(value)
        }

    /**
     * Sets the resolution selector for this use case.
     *
     * @see ImageCapture.Builder.setResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var resolutionSelector: ResolutionSelector
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setResolutionSelector(value)
        }

    /**
     * Sets the background executor for I/O tasks.
     *
     * @see ImageCapture.Builder.setIoExecutor
     */
    @get:NonNull
    @set:NonNull
    public var ioExecutor: Executor
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setIoExecutor(value)
        }

    /**
     * Sets the capture mode for this use case.
     *
     * @see ImageCapture.Builder.setCaptureMode
     */
    @get:ImageCapture.CaptureMode
    @set:ImageCapture.CaptureMode
    public var captureMode: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setCaptureMode(value)
        }

    /**
     * Sets the flash mode for this use case.
     *
     * @see ImageCapture.Builder.setFlashMode
     */
    @get:ImageCapture.FlashMode
    @set:ImageCapture.FlashMode
    public var flashMode: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setFlashMode(value)
        }

    /**
     * Sets whether postview is enabled for this use case.
     *
     * @see ImageCapture.Builder.setPostviewEnabled
     */
    public var isPostviewEnabled: Boolean
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setPostviewEnabled(value)
        }

    /**
     * Sets the JPEG quality for this use case.
     *
     * @see ImageCapture.Builder.setJpegQuality
     */
    @get:IntRange(from = 1, to = 100)
    @set:IntRange(from = 1, to = 100)
    public var jpegQuality: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setJpegQuality(value)
        }

    /**
     * Sets the ScreenFlash implementation for this use case.
     *
     * @see ImageCapture.Builder.setScreenFlash
     */
    @get:NonNull
    @set:NonNull
    public var screenFlash: ImageCapture.ScreenFlash
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setScreenFlash(value)
        }

    /**
     * Sets the output format for this use case.
     *
     * @see ImageCapture.Builder.setOutputFormat
     */
    public var outputFormat: @ImageCapture.OutputFormat Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setOutputFormat(value)
        }

    /**
     * Sets the resolution selector for postview images.
     *
     * @see ImageCapture.Builder.setPostviewResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var postviewResolutionSelector: ResolutionSelector
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setPostviewResolutionSelector(value)
        }

    @JvmSynthetic internal fun build(): ImageCapture = builder.build()
}

/**
 * Creates an [ImageAnalysis] using a Kotlin DSL block.
 *
 * @param block A receiver lambda on [ImageAnalysisScope] to configure the image analysis.
 * @sample androidx.camera.core.samples.useCaseDslSample
 */
@JvmSynthetic
public fun imageAnalysis(block: ImageAnalysisScope.() -> Unit): ImageAnalysis {
    return ImageAnalysisScope().apply(block).build()
}

/** Scope class for [ImageAnalysis] configuration DSL. */
@CameraXDsl
public class ImageAnalysisScope
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val builder: ImageAnalysis.Builder = ImageAnalysis.Builder()
) {
    /**
     * Sets the target name for this use case configuration.
     *
     * @see ImageAnalysis.Builder.setTargetName
     */
    @get:NonNull
    @set:NonNull
    public var targetName: String
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setTargetName(value)
        }

    /**
     * Sets the target rotation for this use case.
     *
     * @see ImageAnalysis.Builder.setTargetRotation
     */
    @get:RotationValue
    @set:RotationValue
    public var targetRotation: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setTargetRotation(value)
        }

    /**
     * Sets the resolution selector for this use case.
     *
     * @see ImageAnalysis.Builder.setResolutionSelector
     */
    @get:NonNull
    @set:NonNull
    public var resolutionSelector: ResolutionSelector
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setResolutionSelector(value)
        }

    /**
     * Sets the background executor for image analysis tasks.
     *
     * @see ImageAnalysis.Builder.setBackgroundExecutor
     */
    @get:NonNull
    @set:NonNull
    public var backgroundExecutor: Executor
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(@NonNull value) {
            builder.setBackgroundExecutor(value)
        }

    /**
     * Sets the backpressure strategy for this use case.
     *
     * @see ImageAnalysis.Builder.setBackpressureStrategy
     */
    @get:ImageAnalysis.BackpressureStrategy
    @set:ImageAnalysis.BackpressureStrategy
    public var backpressureStrategy: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setBackpressureStrategy(value)
        }

    /**
     * Sets the image queue depth for this use case.
     *
     * @see ImageAnalysis.Builder.setImageQueueDepth
     */
    public var imageQueueDepth: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setImageQueueDepth(value)
        }

    /**
     * Sets the output image format for this use case.
     *
     * @see ImageAnalysis.Builder.setOutputImageFormat
     */
    @get:ImageAnalysis.OutputImageFormat
    @set:ImageAnalysis.OutputImageFormat
    public var outputImageFormat: Int
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setOutputImageFormat(value)
        }

    /**
     * Sets whether output image rotation is enabled for this use case.
     *
     * @see ImageAnalysis.Builder.setOutputImageRotationEnabled
     */
    public var isOutputImageRotationEnabled: Boolean
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setOutputImageRotationEnabled(value)
        }

    @JvmSynthetic internal fun build(): ImageAnalysis = builder.build()
}
