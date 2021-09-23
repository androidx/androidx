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

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.MeteringRectangle
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.DEFAULT_FRAME_LIMIT
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.DEFAULT_TIME_LIMIT_NS
import kotlinx.coroutines.Deferred
import java.io.Closeable

/**
 * A [CameraGraph] represents the combined configuration and state of a camera.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraGraph : Closeable {
    public val streams: StreamGraph

    /**
     * This will cause the [CameraGraph] to start opening the [CameraDevice] and configuring a
     * [CameraCaptureSession]. While the CameraGraph is alive it will attempt to keep the camera
     * open, active, and in a configured running state.
     */
    public fun start()

    /**
     * This will cause the [CameraGraph] to stop executing requests and close the current Camera2
     * [CameraCaptureSession] (if one is active). The most recent repeating request will be
     * preserved, and any calls to submit a request to a session will be enqueued. To stop
     * requests from being enqueued, close the [CameraGraph].
     */
    public fun stop()

    /**
     * Acquire and exclusive access to the [CameraGraph] in a suspending fashion.
     */
    public suspend fun acquireSession(): Session

    /**
     * Try acquiring an exclusive access the [CameraGraph]. Returns null if it can't be acquired
     * immediately.
     */
    public fun acquireSessionOrNull(): Session?

    /**
     * This configures the camera graph to use a specific Surface for the given stream.
     *
     * Changing a surface may cause the camera to stall and/or reconfigure.
     */
    public fun setSurface(stream: StreamId, surface: Surface?)

    /**
     * This defines the configuration, flags, and pre-defined structure of a [CameraGraph] instance.
     *
     * @param camera The Camera2 [CameraId] that this [CameraGraph] represents.
     * @param streams A list of [CameraStream]s to use when building the configuration.
     * @param streamSharingGroups A list of [CameraStream]s to apply buffer sharing to.
     * @param input An input configuration to support Camera2 Reprocessing.
     * @param sessionTemplate The template id to use when creating the [CaptureRequest] to supply
     *   the default parameters for a [SessionConfiguration] object.
     * @param sessionParameters the extra parameters to apply to the [CaptureRequest] used to supply
     *   the default parameters for a [SessionConfiguration] object. These parameters are *only*
     *   used to create the [CaptureRequest] for session configuration. Use [defaultParameters] or
     *   [requiredParameters] to enforce that the key is set for every request.
     * @param sessionMode defines the [OperatingMode] of the session. May be used to configure a
     *   [CameraConstrainedHighSpeedCaptureSession] for slow motion capture (If available)
     * @param defaultTemplate The default template to be used if a [Request] does not specify one.
     * @param defaultParameters The default parameters to be used for a [Request].
     * @param defaultListeners A default set of listeners that will be added to every [Request].
     * @param requiredParameters Will override any other configured parameter, and can be used
     *   to enforce that specific keys are always set to specific value for every [CaptureRequest].
     */
    public data class Config(
        val camera: CameraId,
        val streams: List<CameraStream.Config>,
        val streamSharingGroups: List<List<CameraStream.Config>> = listOf(),
        val input: InputStream.Config? = null,
        val sessionTemplate: RequestTemplate = RequestTemplate(1),
        val sessionParameters: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
        val sessionMode: OperatingMode = OperatingMode.NORMAL,
        val defaultTemplate: RequestTemplate = RequestTemplate(1),
        val defaultParameters: Map<*, Any> = emptyMap<Any, Any>(),
        val defaultListeners: List<Request.Listener> = listOf(),
        val requiredParameters: Map<Any, Any?> = emptyMap(),

        val metadataTransform: MetadataTransform = MetadataTransform(),
        val flags: Flags = Flags()
        // TODO: Internal error handling. May be better at the CameraPipe level.
    )

    /**
     * Flags define boolean values that are used to adjust the behavior and interactions with
     * camera2. These flags should default to the ideal behavior and should be overridden on
     * specific devices to be faster or to work around bad behavior.
     */
    public data class Flags(
        val configureBlankSessionOnStop: Boolean = false,
        val abortCapturesOnStop: Boolean = false,
        val allowMultipleActiveCameras: Boolean = false
    )

    public enum class OperatingMode {
        NORMAL,
        HIGH_SPEED,
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public object Constants3A {
        // Constants related to controlling the time or frame budget a 3A operation should get.
        public const val DEFAULT_FRAME_LIMIT: Int = 60
        public const val DEFAULT_TIME_LIMIT_MS: Int = 3_000
        public const val DEFAULT_TIME_LIMIT_NS: Long = 3_000_000_000L

        // Constants related to metering regions.
        /** No metering region is specified. */
        public val METERING_REGIONS_EMPTY: Array<MeteringRectangle> = emptyArray()

        /**
         * No-op metering regions, this will tell camera device to pick the right metering region
         * for us.
         */
        public val METERING_REGIONS_DEFAULT: Array<MeteringRectangle> =
            arrayOf(MeteringRectangle(0, 0, 0, 0, 0))

        /**
         * Placeholder frame number for [Result3A] when a 3A method encounters an error.
         */
        public val FRAME_NUMBER_INVALID: FrameNumber = FrameNumber(-1L)
    }

    /**
     * A [Session] is an interactive lock for [CameraGraph] and allows state to be changed.
     *
     * Holding this object prevents other systems from acquiring a [Session] until the currently
     * held session is released. Because of it's exclusive nature, [Session]s are intended for
     * fast, short-lived state updates, or for interactive capture sequences that must not be
     * altered. (Flash photo sequences, for example).
     *
     * While this object is thread-safe, it should not shared or held for long periods of time.
     * Example: A [Session] should *not* be held during video recording.
     */
    public interface Session : Closeable {
        /**
         * Causes the CameraGraph to start or update the current repeating request with the
         * provided [Request] object. The [Request] object may be cached, and may be used for
         * other interactions with the camera (such as updating 3A, or issuing 3A triggers).
         */
        public fun startRepeating(request: Request)

        /**
         * Stop the current repeating request.
         */
        public fun stopRepeating()

        /**
         * Add the [Request] into an in-flight request queue. Requests will be issued to the
         * Camera exactly once.
         */
        public fun submit(request: Request)

        /**
         * Add the [Request] into an in-flight request queue. Requests will be issued to the
         * Camera exactly once. The list of [Request]s is guaranteed to be submitted together.
         */
        public fun submit(requests: List<Request>)

        /**
         * Abort in-flight requests. This will abort *all* requests in the current
         * CameraCaptureSession as well as any requests that are enqueued, but that have not yet
         * been submitted to the camera.
         */
        public fun abort()

        /**
         * Applies the given 3A parameters to the camera device.
         *
         * @return earliest FrameNumber at which the parameters were successfully applied.
         */
        public fun update3A(
            aeMode: AeMode? = null,
            afMode: AfMode? = null,
            awbMode: AwbMode? = null,
            aeRegions: List<MeteringRectangle>? = null,
            afRegions: List<MeteringRectangle>? = null,
            awbRegions: List<MeteringRectangle>? = null
        ): Deferred<Result3A>

        /**
         * Applies the given 3A parameters to the camera device but for only one frame.
         *
         * @return the FrameNumber for which these parameters were applied.
         */
        public suspend fun submit3A(
            aeMode: AeMode? = null,
            afMode: AfMode? = null,
            awbMode: AwbMode? = null,
            aeRegions: List<MeteringRectangle>? = null,
            afRegions: List<MeteringRectangle>? = null,
            awbRegions: List<MeteringRectangle>? = null
        ): Deferred<Result3A>

        /**
         * Turns the torch to ON or OFF.
         *
         * This method has a side effect on the currently set AE mode. Ref:
         * https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#FLASH_MODE
         * To use the flash control, AE mode must be set to ON or OFF. So if the AE mode is
         * already not either ON or OFF, we will need to update the AE mode to one of those states,
         * here we will choose ON. It is the responsibility of the application layer above
         * CameraPipe to restore the AE mode after the torch control has been used. The
         * [update3A] method can be used to restore the AE state to a previous value.
         *
         * @return the FrameNumber at which the turn was fully turned on if switch was ON, or the
         * FrameNumber at which it was completely turned off when the switch was OFF.
         */
        public fun setTorch(torchState: TorchState): Deferred<Result3A>

        /**
         * Locks the auto-exposure, auto-focus and auto-whitebalance as per the given desired
         * behaviors. This given 3A parameters are applied before the lock is obtained. If 'null'
         * value is passed for a parameter, that parameter is ignored, and the current value for
         * that parameter continues to be applied.
         *
         * TODO(sushilnath@): Add support for specifying the AE, AF and AWB modes as well. The
         * update of modes require special care if the desired lock behavior is immediate. In
         * that case we have to submit a combination of repeating and single requests so that the
         * AF skips the initial state of the new mode's state machine and stays locks in the new
         * mode as well.
         *
         * @param frameLimit the maximum number of frames to wait before we give up waiting for
         * this operation to complete.
         * @param timeLimitNs the maximum time limit in ms we wait before we give up waiting for
         * this operation to complete.
         *
         * @return [Result3A], which will contain the latest frame number at which the locks were
         * applied or the frame number at which the method returned early because either frame limit
         * or time limit was reached.
         */
        public suspend fun lock3A(
            aeMode: AeMode? = null,
            afMode: AfMode? = null,
            awbMode: AwbMode? = null,
            aeRegions: List<MeteringRectangle>? = null,
            afRegions: List<MeteringRectangle>? = null,
            awbRegions: List<MeteringRectangle>? = null,
            aeLockBehavior: Lock3ABehavior? = null,
            afLockBehavior: Lock3ABehavior? = null,
            awbLockBehavior: Lock3ABehavior? = null,
            frameLimit: Int = DEFAULT_FRAME_LIMIT,
            timeLimitNs: Long = DEFAULT_TIME_LIMIT_NS
        ): Deferred<Result3A>

        /**
         * Unlocks auto-exposure, auto-focus, auto-whitebalance. Once they are unlocked they get
         * back to their initial state or resume their auto scan depending on the current mode
         * they are operating in.
         *
         * Providing 'true' for a parameter in this method will unlock that component and if 'false'
         * is provided or the parameter is not specified then it will have no effect on the lock of
         * that component, i.e if it was locked earlier it will stay locked and if it was already
         * unlocked, it will stay unlocked.
         *
         * @return [Result3A], which will contain the latest frame number at which the auto-focus,
         * auto-exposure, auto-white balance were unlocked as per the method arguments.
         *
         */
        public suspend fun unlock3A(ae: Boolean? = null, af: Boolean? = null, awb: Boolean? = null):
            Deferred<Result3A>

        /**
         * This methods does pre-capture metering sequence and locks auto-focus. Once the
         * operation completes, we can proceed to take high-quality pictures.
         *
         * Note: Flash will be used during pre-capture metering and during image capture if the
         * AE mode was set to [AeMode.ON_AUTO_FLASH] or [AeMode.ON_ALWAYS_FLASH], thus firing it
         * for low light captures or for every capture, respectively.
         *
         * @param frameLimit the maximum number of frames to wait before we give up waiting for
         * this operation to complete.
         * @param timeLimitNs the maximum time limit in ms we wait before we give up waiting for
         * this operation to complete.
         *
         * @return [Result3A], which will contain the latest frame number at which the locks were
         * applied or the frame number at which the method returned early because either frame limit
         * or time limit was reached.
         */
        public suspend fun lock3AForCapture(
            frameLimit: Int = DEFAULT_FRAME_LIMIT,
            timeLimitNs: Long = DEFAULT_TIME_LIMIT_NS
        ): Deferred<Result3A>

        /**
         * After submitting pre-capture metering sequence needed by [lock3AForCapture] method, the
         * camera system can internally lock the auto-exposure routine for subsequent still image
         * capture, and if not image capture request is submitted the auto-exposure may not resume
         * it's normal scan.
         * This method brings focus and exposure back to normal after high quality image captures
         * using [lock3AForCapture] method.
         */
        public suspend fun unlock3APostCapture(): Deferred<Result3A>
    }
}