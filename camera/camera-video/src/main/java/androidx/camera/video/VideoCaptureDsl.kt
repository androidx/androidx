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

package androidx.camera.video

import android.util.Range
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraXDsl
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.MirrorMode
import androidx.camera.core.impl.ImageOutputConfig.RotationValue

/**
 * Creates a [VideoCapture] using a Kotlin DSL block.
 *
 * @param videoOutput The [VideoOutput] to associate with the VideoCapture.
 * @param block A receiver lambda on [VideoCaptureScope] to configure the VideoCapture.
 * @sample androidx.camera.video.samples.videoCaptureDslSample
 */
@JvmSynthetic
public fun <T : VideoOutput> videoCapture(
    videoOutput: T,
    block: VideoCaptureScope<T>.() -> Unit,
): VideoCapture<T> {
    return VideoCaptureScope(videoOutput).apply(block).build()
}

/** Scope class for [VideoCapture] configuration DSL. */
@CameraXDsl
public class VideoCaptureScope<T : VideoOutput> internal constructor(videoOutput: T) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val builder: VideoCapture.Builder<T> = VideoCapture.Builder(videoOutput)

    /**
     * Sets the target name for this use case configuration.
     *
     * @see VideoCapture.Builder.setTargetName
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
     * @see VideoCapture.Builder.setTargetRotation
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
     * Sets the dynamic range for this use case.
     *
     * @see VideoCapture.Builder.setDynamicRange
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
     * @see VideoCapture.Builder.setMirrorMode
     */
    @get:ExperimentalMirrorMode
    @set:ExperimentalMirrorMode
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
     * Sets the target frame rate range for this use case.
     *
     * @see VideoCapture.Builder.setTargetFrameRate
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

    /**
     * Sets whether video stabilization is enabled for this use case.
     *
     * @see VideoCapture.Builder.setVideoStabilizationEnabled
     */
    public var isVideoStabilizationEnabled: Boolean
        @JvmSynthetic
        @Deprecated("Write-only", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        @JvmSynthetic
        set(value) {
            builder.setVideoStabilizationEnabled(value)
        }

    @JvmSynthetic internal fun build(): VideoCapture<T> = builder.build()
}
