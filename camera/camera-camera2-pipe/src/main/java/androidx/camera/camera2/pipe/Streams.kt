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
public class CameraStream
internal constructor(public val id: StreamId, public val outputs: List<OutputStream>) {
    override fun toString(): String = id.toString()

    /** Configuration that may be used to define a [CameraStream] on a [CameraGraph] */
    public class Config
    internal constructor(
        public val outputs: List<OutputStream.Config>,
        public val imageSourceConfig: ImageSourceConfig? = null
    ) {
        init {
            val firstOutput = outputs.first()
            check(outputs.all { it.format == firstOutput.format }) {
                "All outputs must have the same format!"
            }
        }

        public companion object {
            /** Create a simple [CameraStream] to [OutputStream] configuration */
            public fun create(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                outputType: OutputStream.OutputType = OutputStream.OutputType.SURFACE,
                mirrorMode: OutputStream.MirrorMode? = null,
                timestampBase: OutputStream.TimestampBase? = null,
                dynamicRangeProfile: OutputStream.DynamicRangeProfile? = null,
                streamUseCase: OutputStream.StreamUseCase? = null,
                streamUseHint: OutputStream.StreamUseHint? = null,
                sensorPixelModes: List<OutputStream.SensorPixelMode> = emptyList(),
                imageSourceConfig: ImageSourceConfig? = null,
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
                        streamUseHint,
                        sensorPixelModes,
                    ),
                    imageSourceConfig
                )

            /**
             * Create a simple [CameraStream] using a previously defined [OutputStream.Config]. This
             * allows multiple [CameraStream]s to share the same [OutputConfiguration].
             */
            public fun create(
                output: OutputStream.Config,
                imageSourceConfig: ImageSourceConfig? = null
            ): Config = Config(listOf(output), imageSourceConfig)

            /**
             * Create a [CameraStream] from multiple [OutputStream.Config]s. This is used to to
             * define a [CameraStream] that may produce one or more of the outputs when used in a
             * request to the camera.
             */
            public fun create(
                outputs: List<OutputStream.Config>,
                imageSourceConfig: ImageSourceConfig? = null
            ): Config = Config(outputs, imageSourceConfig)
        }
    }
}

/**
 * This identifies a single surface that is used to tell the camera to produce one or more outputs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class StreamId(public val value: Int) {
    override fun toString(): String = "Stream-$value"
}

/**
 * A [OutputStream] represents one of the possible outputs that may be produced from a
 * [CameraStream]. Because some sensors are capable of producing images at different resolutions,
 * the underlying HAL on the device may produce different sized images for the same request. This
 * represents one of those potential outputs.
 */
@JvmDefaultWithCompatibility
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface OutputStream {
    // Every output comes from one, and exactly one, CameraStream
    public val stream: CameraStream

    public val id: OutputId
    public val size: Size
    public val format: StreamFormat
    public val camera: CameraId
    public val mirrorMode: MirrorMode?
    public val timestampBase: TimestampBase?
    public val dynamicRangeProfile: DynamicRangeProfile?
    public val streamUseCase: StreamUseCase?
    public val outputType: OutputType?
    public val streamUseHint: StreamUseHint?

    // TODO: Consider adding sensor mode and/or other metadata

    /**
     * Configuration object that provides the parameters for a specific input / output stream on
     * Camera.
     */
    public sealed class Config(
        public val size: Size,
        public val format: StreamFormat,
        public val camera: CameraId?,
        public val mirrorMode: MirrorMode?,
        public val timestampBase: TimestampBase?,
        public val dynamicRangeProfile: DynamicRangeProfile?,
        public val streamUseCase: StreamUseCase?,
        public val streamUseHint: StreamUseHint?,
        public val sensorPixelModes: List<SensorPixelMode>,
    ) {
        public companion object {
            public fun create(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                outputType: OutputType = OutputType.SURFACE,
                mirrorMode: MirrorMode? = null,
                timestampBase: TimestampBase? = null,
                dynamicRangeProfile: DynamicRangeProfile? = null,
                streamUseCase: StreamUseCase? = null,
                streamUseHint: StreamUseHint? = null,
                sensorPixelModes: List<SensorPixelMode> = emptyList(),
            ): Config =
                if (
                    outputType == OutputType.SURFACE_TEXTURE ||
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
                        streamUseHint,
                        sensorPixelModes,
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
                        streamUseHint,
                        sensorPixelModes,
                    )
                }

            /** Create a stream configuration from an externally created [OutputConfiguration] */
            @RequiresApi(33)
            public fun external(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                externalOutputConfig: OutputConfiguration,
                streamUseHint: StreamUseHint?,
                sensorPixelModes: List<SensorPixelMode> = emptyList(),
            ): Config {
                return ExternalOutputConfig(
                    size,
                    format,
                    camera,
                    output = externalOutputConfig,
                    streamUseHint,
                    sensorPixelModes,
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
            streamUseHint: StreamUseHint?,
            sensorPixelModes: List<SensorPixelMode>,
        ) :
            Config(
                size,
                format,
                camera,
                mirrorMode,
                timestampBase,
                dynamicRangeProfile,
                streamUseCase,
                streamUseHint,
                sensorPixelModes,
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
            streamUseHint: StreamUseHint?,
            sensorPixelModes: List<SensorPixelMode>,
        ) :
            Config(
                size,
                format,
                camera,
                mirrorMode,
                timestampBase,
                dynamicRangeProfile,
                streamUseCase,
                streamUseHint,
                sensorPixelModes,
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
            streamUseHint: StreamUseHint?,
            sensorPixelModes: List<SensorPixelMode>,
        ) :
            Config(
                size,
                format,
                camera,
                MirrorMode(Api33Compat.getMirrorMode(output)),
                TimestampBase(Api33Compat.getTimestampBase(output)),
                DynamicRangeProfile(Api33Compat.getDynamicRangeProfile(output)),
                StreamUseCase(Api33Compat.getStreamUseCase(output)),
                streamUseHint,
                sensorPixelModes,
            )
    }

    public class OutputType private constructor() {
        public companion object {
            public val SURFACE: OutputType = OutputType()
            public val SURFACE_VIEW: OutputType = OutputType()
            public val SURFACE_TEXTURE: OutputType = OutputType()
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
    public value class MirrorMode(public val value: Int) {
        public companion object {
            public val MIRROR_MODE_AUTO: MirrorMode = MirrorMode(0)
            public val MIRROR_MODE_NONE: MirrorMode = MirrorMode(1)
            public val MIRROR_MODE_H: MirrorMode = MirrorMode(2)
            public val MIRROR_MODE_V: MirrorMode = MirrorMode(3)
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
    public value class TimestampBase(public val value: Int) {
        public companion object {
            public val TIMESTAMP_BASE_DEFAULT: TimestampBase = TimestampBase(0)
            public val TIMESTAMP_BASE_SENSOR: TimestampBase = TimestampBase(1)
            public val TIMESTAMP_BASE_MONOTONIC: TimestampBase = TimestampBase(2)
            public val TIMESTAMP_BASE_REALTIME: TimestampBase = TimestampBase(3)
            public val TIMESTAMP_BASE_CHOREOGRAPHER_SYNCED: TimestampBase = TimestampBase(4)
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
    public value class DynamicRangeProfile(public val value: Long) {
        public companion object {
            public val STANDARD: DynamicRangeProfile = DynamicRangeProfile(1)
            public val HLG10: DynamicRangeProfile = DynamicRangeProfile(2)
            public val HDR10: DynamicRangeProfile = DynamicRangeProfile(4)
            public val HDR10_PLUS: DynamicRangeProfile = DynamicRangeProfile(8)
            public val DOLBY_VISION_10B_HDR_REF: DynamicRangeProfile = DynamicRangeProfile(16)
            public val DOLBY_VISION_10B_HDR_REF_PO: DynamicRangeProfile = DynamicRangeProfile(32)
            public val DOLBY_VISION_10B_HDR_OEM: DynamicRangeProfile = DynamicRangeProfile(64)
            public val DOLBY_VISION_10B_HDR_OEM_PO: DynamicRangeProfile = DynamicRangeProfile(128)
            public val DOLBY_VISION_8B_HDR_REF: DynamicRangeProfile = DynamicRangeProfile(256)
            public val DOLBY_VISION_8B_HDR_REF_PO: DynamicRangeProfile = DynamicRangeProfile(512)
            public val DOLBY_VISION_8B_HDR_OEM: DynamicRangeProfile = DynamicRangeProfile(1024)
            public val DOLBY_VISION_8B_HDR_OEM_PO: DynamicRangeProfile = DynamicRangeProfile(2048)
            public val PUBLIC_MAX: DynamicRangeProfile = DynamicRangeProfile(4096)
        }
    }

    /**
     * Until all devices can support StreamUseCases and edge cases are resolved, [StreamUseHint] can
     * temporarily be used to give a hint on the purpose of the stream.
     */
    @JvmInline
    public value class StreamUseHint(public val value: Long) {

        public companion object {
            public val DEFAULT: StreamUseHint = StreamUseHint(0)
            public val VIDEO_RECORD: StreamUseHint = StreamUseHint(1)
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
    public value class StreamUseCase(public val value: Long) {
        public companion object {
            public val DEFAULT: StreamUseCase = StreamUseCase(0)
            public val PREVIEW: StreamUseCase = StreamUseCase(1)
            public val STILL_CAPTURE: StreamUseCase = StreamUseCase(2)
            public val VIDEO_RECORD: StreamUseCase = StreamUseCase(3)
            public val PREVIEW_VIDEO_STILL: StreamUseCase = StreamUseCase(4)
            public val VIDEO_CALL: StreamUseCase = StreamUseCase(5)
        }
    }

    /**
     * Used to set the sensor pixel mode the OutputStream will be used in.
     *
     * See the documentation on [OutputConfiguration.addSensorPixelModeUsed] for more details.
     */
    @JvmInline
    public value class SensorPixelMode(public val value: Int) {
        public companion object {
            public val DEFAULT: SensorPixelMode = SensorPixelMode(0)
            public val MAXIMUM_RESOLUTION: SensorPixelMode = SensorPixelMode(1)
        }
    }

    /**
     * If this OutputStream is a valid stream for HIGH_SPEED recording. The requirement is that the
     * surface must be either video encoder surface or preview surface. The checks below can be used
     * to ensure that the we are passing along the right intention for any further checks when
     * actually configuring and using this stream.
     *
     * [Camera2 reference]
     * [https://developer.android.com/reference/android/hardware/camera2/CameraDevice#constrained-high-speed-recording]
     */
    public fun isValidForHighSpeedOperatingMode(): Boolean {
        return this.streamUseCase == null ||
            this.streamUseCase == OutputStream.StreamUseCase.DEFAULT ||
            this.streamUseCase == OutputStream.StreamUseCase.PREVIEW ||
            this.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD ||
            this.streamUseHint == null ||
            this.streamUseHint == OutputStream.StreamUseHint.DEFAULT ||
            this.streamUseHint == OutputStream.StreamUseHint.VIDEO_RECORD
    }
}

/** Configuration for a CameraStream that will be internally configured to produce images. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImageSourceConfig(
    public val capacity: Int,
    public val usageFlags: Long? = null,
    public val defaultDataSpace: Int? = null,
    public val defaultHardwareBufferFormat: Int? = null
)

/** This identifies a single output. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class OutputId(public val value: Int) {
    override fun toString(): String = "Output-$value"
}

/** Configuration for defining the properties of a Camera2 InputStream for reprocessing requests. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface InputStream {
    public val id: InputStreamId
    public val maxImages: Int
    public val format: StreamFormat

    public class Config(
        public val stream: CameraStream.Config,
        public val maxImages: Int,
        public var streamFormat: StreamFormat
    )
}

/** This identifies a single input. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class InputStreamId(public val value: Int) {
    override fun toString(): String = "Input-$value"
}
