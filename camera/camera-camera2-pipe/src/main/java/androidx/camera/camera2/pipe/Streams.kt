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

/**
 * A [CameraStream] is used on a [CameraGraph] to control what outputs that graph produces.
 *
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
 * There are three main components that will be wired together, the [CameraStream], the
 * Camera2 [OutputConfiguration], and the [OutputStream]'s. In each of these examples a
 * [CameraStream] is associated with a distinct surface that may be sent to camera2 to produce 1
 * or more distinct outputs defined in the list of [OutputStream]'s.
 *
 * Simple 1:1 configuration
 *   ```
 *   CameraStream-1 -> OutputConfig-1 -> OutputStream-1
 *   CameraStream-2 -> OutputConfig-2 -> OutputStream-2
 *   ```
 *
 * Stream sharing (Multiple surfaces use the same OutputConfig object)
 *   ```
 *   CameraStream-1 --------------------> OutputStream-1
 *                  >- OutputConfig-1 -<
 *   CameraStream-2 --------------------> OutputStream-2
 *   ```
 *
 * Multi-Output / External OutputConfiguration (Camera2 may produce one or more of the outputs)
 *   ```
 *   CameraStream-1 -> OutputConfig-1 -> OutputStream-1
 *                 \-> OutputConfig-2 -> OutputStream-2
 *   ```
 */
public class CameraStream internal constructor(
    public val id: StreamId,
    public val outputs: List<OutputStream>
) {
    override fun toString(): String = id.toString()

    /** Configuration that may be used to define a [CameraStream] on a [CameraGraph] */
    public class Config internal constructor(
        val outputs: List<OutputStream.Config>
    ) {
        companion object {
            /** Create a simple [CameraStream] to [OutputStream] configuration */
            fun create(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                outputType: OutputStream.OutputType = OutputStream.OutputType.SURFACE,
                mirrorMode: OutputStream.MirrorMode? = null,
            ): Config = create(
                OutputStream.Config.create(size, format, camera, outputType, mirrorMode)
            )

            /**
             * Create a simple [CameraStream] using a previously defined [OutputStream.Config].
             * This allows multiple [CameraStream]s to share the same [OutputConfiguration].
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
@JvmInline
public value class StreamId(public val value: Int) {
    override fun toString(): String = "Stream-$value"
}

/**
 * A [OutputStream] represents one of the possible outputs that may be produced from a
 * [CameraStream]. Because some sensors are capable of producing images at different resolutions,
 * the underlying HAL on the device may produce different sized images for the same request.
 * This represents one of those potential outputs.
 */
public interface OutputStream {
    // Every output comes from one, and exactly one, CameraStream
    public val stream: CameraStream

    public val id: OutputId
    public val size: Size
    public val format: StreamFormat
    public val camera: CameraId
    public val mirrorMode: MirrorMode?
    // TODO: Consider adding sensor mode and/or other metadata

    /**
     * Configuration object that provides the parameters for a specific input / output stream on Camera.
     */
    sealed class Config(
        public val size: Size,
        public val format: StreamFormat,
        public val camera: CameraId?,
        public val mirrorMode: MirrorMode?,
    ) {
        companion object {
            fun create(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                outputType: OutputType = OutputType.SURFACE,
                mirrorMode: MirrorMode? = null,
            ): Config =
                if (
                    outputType == OutputType.SURFACE_TEXTURE ||
                    outputType == OutputType.SURFACE_VIEW
                ) {
                    LazyOutputConfig(size, format, camera, outputType, mirrorMode)
                } else {
                    check(outputType == OutputType.SURFACE)
                    SimpleOutputConfig(size, format, camera, mirrorMode)
                }

            /** Create a stream configuration from an externally created [OutputConfiguration] */
            @RequiresApi(24)
            fun external(
                size: Size,
                format: StreamFormat,
                camera: CameraId? = null,
                externalOutputConfig: OutputConfiguration
            ): Config {
                return ExternalOutputConfig(size, format, camera, output = externalOutputConfig)
            }
        }

        /**
         * Most outputs only need to define size, format, and cameraId.
         */
        internal class SimpleOutputConfig(
            size: Size,
            format: StreamFormat,
            camera: CameraId?,
            mirrorMode: MirrorMode?,
        ) : Config(size, format, camera, mirrorMode)

        /**
         * Used to configure an output with a surface that may be provided after the camera is running.
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
        ) : Config(size, format, camera, mirrorMode)

        /**
         * Used to define an output that comes from an externally managed OutputConfiguration object.
         *
         * The configuration logic has the following behavior:
         * - Assumes [OutputConfiguration] has a valid surface
         * - Assumes [OutputConfiguration] surfaces will not be added / removed / changed.
         * - If the CameraCaptureSession must be recreated, the [OutputConfiguration] will be reused.
         */
        internal class ExternalOutputConfig(
            size: Size,
            format: StreamFormat,
            camera: CameraId?,
            val output: OutputConfiguration,
        ) : Config(size, format, camera, null)
    }

    enum class OutputType {
        SURFACE,
        SURFACE_VIEW,
        SURFACE_TEXTURE,
    }

    /**
     * Adds the ability to define the mirrorMode of the OutputStream.
     * [MIRROR_MODE_AUTO] is the default mirroring mode for the camera device.
     * With this mode, the camera output is mirrored horizontally for front-facing cameras,
     * and there is no mirroring for rear-facing and external cameras.
     *
     * See the documentation on [OutputConfiguration.setMirrorMode] for more details.
     */
    @JvmInline
    value class MirrorMode private constructor(val value: Int) {
        companion object {
            val MIRROR_MODE_AUTO = MirrorMode(0)
            val MIRROR_MODE_NONE = MirrorMode(1)
            val MIRROR_MODE_H = MirrorMode(2)
            val MIRROR_MODE_V = MirrorMode(3)
        }
    }
}

/**
 * This identifies a single output.
 */
@JvmInline
public value class OutputId(public val value: Int) {
    override fun toString(): String = "Output-$value"
}

/**
 * Configuration for defining the properties of a Camera2 InputStream for reprocessing
 * requests.
 */
public interface InputStream {
    public val id: InputId
    public val format: StreamFormat
    // TODO: This may accept

    public class Config(val stream: CameraStream.Config)
}

/**
 * This identifies a single input.
 */
@JvmInline
public value class InputId(public val value: Int) {
    override fun toString(): String = "Input-$value"
}
