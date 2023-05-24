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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe

import android.hardware.camera2.params.OutputConfiguration
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.OutputStream.DynamicRangeProfile.Companion.STANDARD
import androidx.camera.camera2.pipe.OutputStream.MirrorMode.Companion.MIRROR_MODE_AUTO
import androidx.camera.camera2.pipe.OutputStream.StreamUseCase.Companion.DEFAULT
import androidx.camera.camera2.pipe.OutputStream.TimestampBase.Companion.TIMESTAMP_BASE_DEFAULT
import androidx.camera.camera2.pipe.compat.Api33Compat

/**
 * A [CameraStream] is used on a [CameraGraph] to control what outputs that graph produces.
 * - Each [CameraStream] must have a surface associated with it in the [CameraGraph]. This surface
 *   may be changed, although this may cause the camera to stall and reconfigure.
 * - [CameraStream]'s may be added to [Request]'s that are sent to the [CameraGraph]. This causes
 *   the associated surface to be used by the Camera to produce one or more of the outputs (defined
 *   by outputs.
 *
 * [CameraStream] may be configured in several different ways with the requirement that each
 * [CameraStream] may only represent a single surface that is sent to Camera2, and that each
 * [CameraStream] must produce one or more distinct outputs.
 *
 * There are three main components that will be wired together, the [CameraStream], the Camera2
 * [OutputConfiguration], and the [OutputStream]'s. In each of these examples a [CameraStream] is
 * associated with a distinct surface that may be sent to camera2 to produce 1 or more distinct
 * outputs defined in the list of [OutputStream]'s.
 *
 * Simple 1:1 configuration
 *
 *   ```
 *   CameraStream-1 -> OutputConfig-1 -> OutputStream-1
 *   CameraStream-2 -> OutputConfig-2 -> OutputStream-2
 *   ```
 *
 * Stream sharing (Multiple surfaces use the same OutputConfig object)
 *
 *   ```
 *   CameraStream-1 --------------------> OutputStream-1
 *                  >- OutputConfig-1 -<
 *   CameraStream-2 --------------------> OutputStream-2
 *   ```
 *
 * Multi-Output / External OutputConfiguration (Camera2 may produce one or more of the outputs)
 *
 *   ```
 *   CameraStream-1 -> OutputConfig-1 -> OutputStream-1
 *                 \-> OutputConfig-2 -> OutputStream-2
 *   ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CameraStream
internal constructor(val id: StreamId, val outputs: List<OutputStream>) {
    override fun toString(): String = id.toString()

    /** Configuration that may be used to define a [CameraStream] on a [CameraGraph] */
    class Config internal constructor(val outputs: List<OutputStream.Config>) {
        companion object {
            /** Create a simple [CameraStream] to [OutputStream] configuration */
            fun create(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                outputType: OutputStream.OutputType = OutputStream.OutputType.SURFACE,
                mirrorMode: OutputStream.MirrorMode? = null,
                timestampBase: OutputStream.TimestampBase? = null,
                dynamicRangeProfile: OutputStream.DynamicRangeProfile? = null,
                streamUseCase: OutputStream.StreamUseCase? = null,
                streamUseHint: OutputStream.StreamUseHint? = null
            ): Config =
                create(
                    OutputStream.Config.create(
                        size,
                        format,
                        camera,
                        outputType,
                        mirrorMode,
                        timestampBase,
                        dynamicRangeProfile,
                        streamUseCase,
                        streamUseHint
                    )
                )

            /**
             * Create a simple [CameraStream] using a previously defined [OutputStream.Config]. This
             * allows multiple [CameraStream]s to share the same [OutputConfiguration].
             */
            fun create(output: OutputStream.Config) = Config(listOf(output))

            /**
             * Create a [CameraStream] from multiple [OutputStream.Config]s. This is used to to
             * define a [CameraStream] that may produce one or more of the outputs when used in a
             * request to the camera.
             */
            fun create(outputs: List<OutputStream.Config>) = Config(outputs)
        }
    }
}

/**
 * This identifies a single surface that is used to tell the camera to produce one or more outputs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class StreamId(val value: Int) {
    override fun toString(): String = "Stream-$value"
}

/**
 * A [OutputStream] represents one of the possible outputs that may be produced from a
 * [CameraStream]. Because some sensors are capable of producing images at different resolutions,
 * the underlying HAL on the device may produce different sized images for the same request. This
 * represents one of those potential outputs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface OutputStream {
    // Every output comes from one, and exactly one, CameraStream
    val stream: CameraStream

    val id: OutputId
    val size: Size
    val format: StreamFormat
    val camera: CameraId
    val mirrorMode: MirrorMode?
    val timestampBase: TimestampBase?
    val dynamicRangeProfile: DynamicRangeProfile?
    val streamUseCase: StreamUseCase?
    val outputType: OutputType?
    val streamUseHint: StreamUseHint?

    // TODO: Consider adding sensor mode and/or other metadata

    /**
     * Configuration object that provides the parameters for a specific input / output stream on
     * Camera.
     */
    sealed class Config(
        val size: Size,
        val format: StreamFormat,
        val camera: CameraId?,
        val mirrorMode: MirrorMode?,
        val timestampBase: TimestampBase?,
        val dynamicRangeProfile: DynamicRangeProfile?,
        val streamUseCase: StreamUseCase?,
        val streamUseHint: StreamUseHint?
    ) {
        companion object {
            fun create(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                outputType: OutputType = OutputType.SURFACE,
                mirrorMode: MirrorMode? = null,
                timestampBase: TimestampBase? = null,
                dynamicRangeProfile: DynamicRangeProfile? = null,
                streamUseCase: StreamUseCase? = null,
                streamUseHint: StreamUseHint? = null
            ): Config =
                if (outputType == OutputType.SURFACE_TEXTURE ||
                    outputType == OutputType.SURFACE_VIEW
                ) {
                    LazyOutputConfig(
                        size,
                        format,
                        camera,
                        outputType,
                        mirrorMode,
                        timestampBase,
                        dynamicRangeProfile,
                        streamUseCase,
                        streamUseHint
                    )
                } else {
                    check(outputType == OutputType.SURFACE)
                    SimpleOutputConfig(
                        size,
                        format,
                        camera,
                        mirrorMode,
                        timestampBase,
                        dynamicRangeProfile,
                        streamUseCase,
                        streamUseHint
                    )
                }

            /** Create a stream configuration from an externally created [OutputConfiguration] */
            @RequiresApi(33)
            fun external(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                externalOutputConfig: OutputConfiguration,
                streamUseHint: StreamUseHint?
            ): Config {
                return ExternalOutputConfig(
                    size,
                    format,
                    camera,
                    output = externalOutputConfig,
                    streamUseHint
                )
            }
        }

        /** Most outputs only need to define size, format, and cameraId. */
        internal class SimpleOutputConfig(
            size: Size,
            format: StreamFormat,
            camera: CameraId?,
            mirrorMode: MirrorMode?,
            timestampBase: TimestampBase?,
            dynamicRangeProfile: DynamicRangeProfile?,
            streamUseCase: StreamUseCase?,
            streamUseHint: StreamUseHint?
        ) :
            Config(
                size,
                format,
                camera,
                mirrorMode,
                timestampBase,
                dynamicRangeProfile,
                streamUseCase,
                streamUseHint
            )

        /**
         * Used to configure an output with a surface that may be provided after the camera is
         * running.
         *
         * This behavior is allowed on newer versions of the OS and allows the camera to start
         * running before the UI is fully available. This configuration mode is only allowed for
         * SurfaceHolder and SurfaceTexture output targets, and must be defined ahead of time (along
         * with the size, and format) for these [OutputConfiguration]'s to be created.
         */
        internal class LazyOutputConfig(
            size: Size,
            format: StreamFormat,
            camera: CameraId?,
            internal val outputType: OutputType,
            mirrorMode: MirrorMode?,
            timestampBase: TimestampBase?,
            dynamicRangeProfile: DynamicRangeProfile?,
            streamUseCase: StreamUseCase?,
            streamUseHint: StreamUseHint?
        ) :
            Config(
                size,
                format,
                camera,
                mirrorMode,
                timestampBase,
                dynamicRangeProfile,
                streamUseCase,
                streamUseHint
            )

        /**
         * Used to define an output that comes from an externally managed OutputConfiguration
         * object.
         *
         * The configuration logic has the following behavior:
         * - Assumes [OutputConfiguration] has a valid surface
         * - Assumes [OutputConfiguration] surfaces will not be added / removed / changed.
         * - If the CameraCaptureSession must be recreated, the [OutputConfiguration] will be
         *   reused.
         */
        @RequiresApi(33)
        internal class ExternalOutputConfig(
            size: Size,
            format: StreamFormat,
            camera: CameraId?,
            val output: OutputConfiguration,
            streamUseHint: StreamUseHint?
        ) :
            Config(
                size,
                format,
                camera,
                MirrorMode(Api33Compat.getMirrorMode(output)),
                TimestampBase(Api33Compat.getTimestampBase(output)),
                DynamicRangeProfile(Api33Compat.getDynamicRangeProfile(output)),
                StreamUseCase(Api33Compat.getStreamUseCase(output)),
                streamUseHint
            )
    }

    class OutputType private constructor() {
        companion object {
            val SURFACE = OutputType()
            val SURFACE_VIEW = OutputType()
            val SURFACE_TEXTURE = OutputType()
        }
    }

    /**
     * Adds the ability to define the mirrorMode of the OutputStream. [MIRROR_MODE_AUTO] is the
     * default mirroring mode for the camera device. With this mode, the camera output is mirrored
     * horizontally for front-facing cameras, and there is no mirroring for rear-facing and external
     * cameras.
     *
     * See the documentation on [OutputConfiguration.setMirrorMode] for more details.
     */
    @JvmInline
    value class MirrorMode(val value: Int) {
        companion object {
            val MIRROR_MODE_AUTO = MirrorMode(0)
            val MIRROR_MODE_NONE = MirrorMode(1)
            val MIRROR_MODE_H = MirrorMode(2)
            val MIRROR_MODE_V = MirrorMode(3)
        }
    }

    /**
     * Adds the ability to define the timestamp base of the OutputStream. [TIMESTAMP_BASE_DEFAULT]
     * is the default timestamp base, with which the camera device adjusts timestamps based on the
     * output target.
     *
     * See the documentation on [OutputConfiguration.setTimestampBase] for more details.
     */
    @JvmInline
    value class TimestampBase(val value: Int) {
        companion object {
            val TIMESTAMP_BASE_DEFAULT = TimestampBase(0)
            val TIMESTAMP_BASE_SENSOR = TimestampBase(1)
            val TIMESTAMP_BASE_MONOTONIC = TimestampBase(2)
            val TIMESTAMP_BASE_REALTIME = TimestampBase(3)
            val TIMESTAMP_BASE_CHOREOGRAPHER_SYNCED = TimestampBase(4)
        }
    }

    /**
     * Adds the ability to define the dynamic range profile of the OutputStream. [STANDARD] is the
     * default dynamic range profile for the camera device, with which the camera device uses an
     * 8-bit standard profile.
     *
     * See the documentation on [OutputConfiguration.setDynamicRangeProfile] for more details.
     */
    @JvmInline
    value class DynamicRangeProfile(val value: Long) {
        companion object {
            val STANDARD = DynamicRangeProfile(1)
            val HLG10 = DynamicRangeProfile(2)
            val HDR10 = DynamicRangeProfile(4)
            val HDR10_PLUS = DynamicRangeProfile(8)
            val DOLBY_VISION_10B_HDR_REF = DynamicRangeProfile(16)
            val DOLBY_VISION_10B_HDR_REF_PO = DynamicRangeProfile(32)
            val DOLBY_VISION_10B_HDR_OEM = DynamicRangeProfile(64)
            val DOLBY_VISION_10B_HDR_OEM_PO = DynamicRangeProfile(128)
            val DOLBY_VISION_8B_HDR_REF = DynamicRangeProfile(256)
            val DOLBY_VISION_8B_HDR_REF_PO = DynamicRangeProfile(512)
            val DOLBY_VISION_8B_HDR_OEM = DynamicRangeProfile(1024)
            val DOLBY_VISION_8B_HDR_OEM_PO = DynamicRangeProfile(2048)
            val PUBLIC_MAX = DynamicRangeProfile(4096)
        }
    }

    /**
     * Until all devices can support StreamUseCases and edge cases are resolved, [StreamUseHint]
     * can temporarily be used to give a hint on the purpose of the stream.
     *
     */
    @JvmInline
    value class StreamUseHint(val value: Long) {

        companion object {
            val DEFAULT = StreamUseHint(0)
            val VIDEO_RECORD = StreamUseHint(1)
        }
    }

    /**
     * Adds the ability to define the stream specific use case of the OutputStream. [DEFAULT] is the
     * default stream use case, with which the camera device uses the properties of the output
     * target, such as format, dataSpace, or surface class type, to optimize the image processing
     * pipeline.
     *
     * See the documentation on [OutputConfiguration.setStreamUseCase] for more details.
     */
    @JvmInline
    value class StreamUseCase(val value: Long) {
        companion object {
            val DEFAULT = StreamUseCase(0)
            val PREVIEW = StreamUseCase(1)
            val STILL_CAPTURE = StreamUseCase(2)
            val VIDEO_RECORD = StreamUseCase(3)
            val PREVIEW_VIDEO_STILL = StreamUseCase(4)
            val VIDEO_CALL = StreamUseCase(5)
        }
    }
}

/** This identifies a single output. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class OutputId(val value: Int) {
    override fun toString(): String = "Output-$value"
}

/** Configuration for defining the properties of a Camera2 InputStream for reprocessing requests. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InputStream {
    val id: InputId
    val format: StreamFormat
    // TODO: This may accept

    class Config(val stream: CameraStream.Config)
}

/** This identifies a single input. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class InputId(val value: Int) {
    override fun toString(): String = "Input-$value"
}
