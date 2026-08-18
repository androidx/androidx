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

@file:JvmName("VideoCaptureDsl")

package androidx.camera.video

import android.util.Range
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraXDsl
import androidx.camera.core.DynamicRange
import androidx.camera.core.InteropConfigurableScope
import androidx.camera.core.MirrorMode
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OptionalRotationValue
import androidx.camera.core.impl.ImageOutputConfig.RotationValue
import androidx.camera.core.impl.StreamSpec

/**
 * Creates a [VideoCapture] using a Kotlin DSL block.
 *
 * @param videoOutput The [VideoOutput] to associate with the VideoCapture.
 * @param block A receiver lambda on [VideoCaptureScope] to configure the VideoCapture.
 * @sample androidx.camera.video.samples.videoCaptureDslSample
 */
public fun <T : VideoOutput> videoCapture(
    videoOutput: T,
    block: VideoCaptureScope<T>.() -> Unit,
): VideoCapture<T> {
    return VideoCaptureScope(videoOutput).apply(block).build()
}

/** Scope class for [VideoCapture] configuration DSL. */
@CameraXDsl
public class VideoCaptureScope<T : VideoOutput>
internal constructor(
    videoOutput: T,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val builder: VideoCapture.Builder<T> = VideoCapture.Builder(videoOutput),
) : InteropConfigurableScope<VideoCapture.Builder<T>> {
    private var _targetName: String = ""

    /**
     * Sets or gets the target name for this use case configuration.
     *
     * @see VideoCapture.Builder.setTargetName
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
     * @see VideoCapture.Builder.setTargetRotation
     */
    @get:OptionalRotationValue
    @setparam:RotationValue
    public var targetRotation: Int
        get() = _targetRotation
        set(value) {
            _targetRotation = value
            builder.setTargetRotation(value)
        }

    private var _dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED

    /**
     * Sets or gets the dynamic range for this use case.
     *
     * @see VideoCapture.Builder.setDynamicRange
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
     * @see VideoCapture.Builder.setMirrorMode
     */
    @get:MirrorMode.Mirror
    @setparam:MirrorMode.Mirror
    public var mirrorMode: Int
        get() = _mirrorMode
        set(value) {
            _mirrorMode = value
            builder.setMirrorMode(value)
        }

    private var _targetFrameRate: Range<Int> = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED

    /**
     * Sets or gets the target frame rate range for this use case.
     *
     * @see VideoCapture.Builder.setTargetFrameRate
     */
    @get:NonNull
    @set:NonNull
    public var targetFrameRate: Range<Int>
        get() = _targetFrameRate
        set(@NonNull value) {
            _targetFrameRate = value
            builder.setTargetFrameRate(value)
        }

    private var _isVideoStabilizationEnabled: Boolean = false

    /**
     * Sets or gets whether video stabilization is enabled for this use case.
     *
     * @see VideoCapture.Builder.setVideoStabilizationEnabled
     */
    public var isVideoStabilizationEnabled: Boolean
        get() = _isVideoStabilizationEnabled
        set(value) {
            _isVideoStabilizationEnabled = value
            builder.setVideoStabilizationEnabled(value)
        }

    internal fun build(): VideoCapture<T> = builder.build()
}
