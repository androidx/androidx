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

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.DEFAULT_FRAME_LIMIT
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.DEFAULT_TIME_LIMIT_NS
import androidx.camera.camera2.pipe.CameraGraph.Flags.FinalizeSessionOnCloseBehavior.Companion.OFF
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode.Companion.EXTENSION
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode.Companion.HIGH_SPEED
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode.Companion.NORMAL
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.core.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

/** A [CameraGraph] represents the combined configuration and state of a camera. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
interface CameraGraph : AutoCloseable {
    val streams: StreamGraph

    /**
     * Returns the state flow of [GraphState], which emits the current state of the [CameraGraph],
     * including when a [CameraGraph] is stopped, starting or started.
     */
    val graphState: StateFlow<GraphState>

    /**
     * This is a hint an app can give to a camera graph to indicate whether the camera is being used
     * in a foreground setting, for example whether the user could see the app itself. This would
     * inform the underlying implementation to open cameras more actively (e.g., longer timeout).
     */
    var isForeground: Boolean

    /**
     * This will cause the [CameraGraph] to start opening the [CameraDevice] and configuring a
     * [CameraCaptureSession]. While the CameraGraph is alive it will attempt to keep the camera
     * open, active, and in a configured running state.
     */
    fun start()

    /**
     * This will cause the [CameraGraph] to stop executing requests and close the current Camera2
     * [CameraCaptureSession] (if one is active). The most recent repeating request will be
     * preserved, and any calls to submit a request to a session will be enqueued. To stop requests
     * from being enqueued, close the [CameraGraph].
     */
    fun stop()

    /** Acquire and exclusive access to the [CameraGraph] in a suspending fashion. */
    suspend fun acquireSession(): Session

    /**
     * Try acquiring an exclusive access the [CameraGraph]. Returns null if it can't be acquired
     * immediately.
     */
    fun acquireSessionOrNull(): Session?

    /**
     * This configures the camera graph to use a specific Surface for the given stream.
     *
     * Changing a surface may cause the camera to stall and/or reconfigure.
     */
    fun setSurface(stream: StreamId, surface: Surface?)

    /**
     * This defines the configuration, flags, and pre-defined structure of a [CameraGraph] instance.
     * Note that for parameters, null is considered a valid value, and unset keys are ignored.
     *
     * @param camera The Camera2 [CameraId] that this [CameraGraph] represents.
     * @param streams A list of [CameraStream]s to use when building the configuration.
     * @param exclusiveStreamGroups A list of [CameraStream] groups where the [CameraStream]s in
     *   a group aren't expected to used simultaneously.
     * @param input A list of input configurations to support Camera2 Reprocessing.
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
     * @param requiredParameters Will override any other configured parameter, and can be used to
     *   enforce that specific keys are always set to specific value for every [CaptureRequest].
     * @param cameraBackendId If defined, this tells the [CameraGraph] to use a specific
     *   [CameraBackend] to open and operate the camera. The defined [camera] parameter must be a
     *   camera that can be opened by this [CameraBackend]. If this value is null it will use the
     *   default backend that has been configured by [CameraPipe].
     * @param customCameraBackend If defined, this [customCameraBackend] will be created an used for
     *   _only_ this [CameraGraph]. This cannot be defined if [cameraBackendId] is defined.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Config(
        val camera: CameraId,
        val streams: List<CameraStream.Config>,
        val exclusiveStreamGroups: List<List<CameraStream.Config>> = listOf(),
        val input: List<InputStream.Config>? = null,
        val postviewStream: CameraStream.Config? = null,
        val sessionTemplate: RequestTemplate = RequestTemplate(1),
        val sessionParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
        val sessionMode: OperatingMode = NORMAL,
        val defaultTemplate: RequestTemplate = RequestTemplate(1),
        val defaultParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
        val defaultListeners: List<Request.Listener> = listOf(),
        val requiredParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
        val cameraBackendId: CameraBackendId? = null,
        val customCameraBackend: CameraBackendFactory? = null,
        val metadataTransform: MetadataTransform = MetadataTransform(),
        val flags: Flags = Flags(),
        // TODO: Internal error handling. May be better at the CameraPipe level.
    ) {
        internal var sharedCameraIds: List<CameraId> = emptyList()

        init {
            check(cameraBackendId == null || customCameraBackend == null) {
                "Setting both cameraBackendId and customCameraBackend is not supported."
            }
        }
    }

    /**
     * Flags define boolean values that are used to adjust the behavior and interactions with
     * camera2. These flags should default to the ideal behavior and should be overridden on
     * specific devices to be faster or to work around bad behavior.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Flags(
        val configureBlankSessionOnStop: Boolean = false,

        /**
         * When creating a new capture session, the camera framework waits for all the inflight
         * capture requests from the prior session before creating the new session. Calling
         * abortCaptures() triggers an explicit flush on the camera HAL side. Therefore, aborting
         * the captures allows us to switch to a new capture session sooner (see the referenced bug
         * for more info).
         *
         * However, there might be cases where we might not want to trigger the flush. For example,
         * if we're recording a video, we may not want the video recording to be disrupted too
         * early. Hence, this flag is provided so that we can override this behavior.
         *
         * Ideally we should be able to invoke abortCaptures() every time during close. However,
         * improper flush implementations, which seem to occur largely on older devices, have shown
         * to cause irregular behaviors, such as NPEs (b/139448807), capture session entering
         * abnormal states (b/162314023), and (potentially) camera device close stalling based on
         * testing, etc. Hence, we're enabling this behavior by default on API level >= R (30) for
         * now.
         *
         * - Bug(s): b/287020251
         * - API levels: R (30) and above
         */
        val abortCapturesOnStop: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,

        /**
         * A quirk that waits for the last repeating capture request to start before stopping the
         * current capture session. Please refer to the bugs linked here, or
         * [androidx.camera.camera2.pipe.compat.Camera2Quirks.shouldWaitForRepeatingRequest] for
         * more information.
         *
         * This flag provides the overrides for you to override the default behavior (CameraPipe
         * would turn on/off the quirk automatically based on device information).
         *
         * - Bug(s): b/146773463, b/267557892
         * - Device(s): Camera devices on hardware level LEGACY
         * - API levels: All
         */
        val quirkWaitForRepeatingRequestOnDisconnect: Boolean? = null,

        /**
         * A quirk that finalizes [androidx.camera.camera2.pipe.compat.CaptureSessionState] when
         * the CameraGraph is stopped or closed. When a CameraGraph is started, the app might
         * wait for the Surfaces to be released before setting the new Surfaces. This creates a
         * potential deadlock, and this quirk is aimed to mitigate such behavior by releasing the
         * Surfaces (finalizing the session) when the graph is stopped or closed.
         *
         * - Bug(s): b/277310425
         * - Device(s): All (but behaviors might differ across devices)
         * - API levels: All
         */
        val quirkFinalizeSessionOnCloseBehavior: FinalizeSessionOnCloseBehavior = OFF,

        /**
         * A quirk that closes the camera capture session when the CameraGraph is stopped or closed.
         * This is needed in cases where the app that do not wish to receive further frames, or
         * in cases where not closing the capture session before closing the camera device might
         * cause the camera close call itself to hang indefinitely.
         *
         * - Bug(s): b/277310425, b/277310425
         * - Device(s): Depends on the situation and the use case.
         * - API levels: All
         */
        val quirkCloseCaptureSessionOnDisconnect: Boolean = false,

        /**
         * A quirk that closes the camera device when the CameraGraph is closed. This is needed on
         * devices where not closing the camera device before creating a new capture session can
         * lead to crashes.
         *
         * - Bug(s): b/282871038
         * - Device(s): Exynos7870 platforms.
         * - API levels: All
         */
        val quirkCloseCameraDeviceOnClose: Boolean = false,
    ) {

        @JvmInline
        value class FinalizeSessionOnCloseBehavior private constructor(val value: Int) {
            companion object {
                /**
                 * OFF indicates that the CameraGraph only finalizes capture session under regular
                 *  conditions, i.e., when the camera device is closed, or when a new capture
                 *  session is created.
                 */
                val OFF = FinalizeSessionOnCloseBehavior(0)

                /**
                 * IMMEDIATE indicates that the CameraGraph will finalize the current session
                 *  immediately when the CameraGraph is stopped or closed. This should be the
                 *  default behavior for devices that allows for immediate Surface reuse.
                 */
                val IMMEDIATE = FinalizeSessionOnCloseBehavior(1)

                /**
                 * TIMEOUT indicates that the CameraGraph will finalize the current session on a 2s
                 *  timeout when the CameraGraph is stopped or closed. This should only be enabled
                 *  for devices that require waiting for Surfaces to be released.
                 */
                val TIMEOUT = FinalizeSessionOnCloseBehavior(2)
            }
        }
    }

    /**
     * Operating mode defines the major categories of how a CameraGraph instance will operate when
     * not operating a [NORMAL] camera graph.
     *
     * @property NORMAL represents standard camera operation and behavior.
     * @property HIGH_SPEED represents a camera operating at high frame rate, usually used to
     *   produce slow motion videos.
     * @property EXTENSION represents device-specific modes that may operate differently or have
     *   significant limitations in order to produce specific kinds of camera results.
     */
    @JvmInline
    value class OperatingMode private constructor(internal val mode: Int) {
        companion object {
            val NORMAL = OperatingMode(0)
            val HIGH_SPEED = OperatingMode(1)
            val EXTENSION = OperatingMode(2)

            fun custom(mode: Int): OperatingMode {
                require(mode != NORMAL.mode && mode != HIGH_SPEED.mode) {
                    Log.error { "Custom operating mode $mode conflicts with standard modes" }
                }
                return OperatingMode(mode)
            }
        }
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    object Constants3A {
        // Constants related to controlling the time or frame budget a 3A operation should get.
        const val DEFAULT_FRAME_LIMIT: Int = 60
        const val DEFAULT_TIME_LIMIT_MS: Int = 3_000
        const val DEFAULT_TIME_LIMIT_NS: Long = 3_000_000_000L

        // Constants related to metering regions.
        /** No metering region is specified. */
        val METERING_REGIONS_EMPTY: Array<MeteringRectangle> = emptyArray()

        /**
         * No-op metering regions, this will tell camera device to pick the right metering region
         * for us.
         */
        val METERING_REGIONS_DEFAULT: Array<MeteringRectangle> =
            arrayOf(MeteringRectangle(0, 0, 0, 0, 0))

        /** Placeholder frame number for [Result3A] when a 3A method encounters an error. */
        val FRAME_NUMBER_INVALID: FrameNumber = FrameNumber(-1L)
    }

    /**
     * A [Session] is an interactive lock for [CameraGraph] and allows state to be changed.
     *
     * Holding this object prevents other systems from acquiring a [Session] until the currently
     * held session is released. Because of its exclusive nature, [Session]s are intended for fast,
     * short-lived state updates, or for interactive capture sequences that must not be altered.
     * (Flash photo sequences, for example).
     *
     * While this object is thread-safe, it should not shared or held for long periods of time.
     * Example: A [Session] should *not* be held during video recording.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Session : AutoCloseable {
        /**
         * Causes the CameraGraph to start or update the current repeating request with the provided
         * [Request] object. The [Request] object may be cached, and may be used for other
         * interactions with the camera (such as updating 3A, or issuing 3A triggers).
         */
        fun startRepeating(request: Request)

        /** Stop the current repeating request. */
        fun stopRepeating()

        /**
         * Submit the [Request] to the camera. Requests are issued to the Camera, in order, on a
         * background queue. Each call to submit will issue the [Request] to the camera exactly
         * once unless the request is invalid, or unless the requests are aborted via [abort].
         * The same request can be submitted multiple times.
         */
        fun submit(request: Request)

        /**
         * Submit the [Request]s to the camera. [Request]s are issued to the Camera, in order, on a
         * background queue. Each call to submit will issue the List of [Request]s to the camera
         * exactly once unless the one or more of the requests are invalid, or unless the requests
         * are aborted via [abort]. The same list of [Request]s may be submitted multiple times.
         */
        fun submit(requests: List<Request>)

        /**
         * Submit the [Request] to the camera, and aggregate the results into a [FrameCapture],
         * which can be used to wait for the [Frame] to start using [FrameCapture.awaitFrame].
         *
         * The [FrameCapture] **must** be closed, or it will result in a memory leak.
         */
        fun capture(request: Request): FrameCapture

        /**
         * Submit the [Request]s to the camera, and aggregate the results into a list of
         * [FrameCapture]s, which can be used to wait for the associated [Frame]
         * using [FrameCapture.awaitFrame].
         *
         * Each [FrameCapture] **must** be closed, or it will result in a memory leak.
         */
        fun capture(requests: List<Request>): List<FrameCapture>

        /**
         * Abort in-flight requests. This will abort *all* requests in the current
         * CameraCaptureSession as well as any requests that are enqueued, but that have not yet
         * been submitted to the camera.
         */
        fun abort()

        /**
         * Applies the given 3A parameters to the camera device.
         *
         * @return earliest FrameNumber at which the parameters were successfully applied.
         */
        fun update3A(
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
        suspend fun submit3A(
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
         * To use the flash control, AE mode must be set to ON or OFF. So if the AE mode is already
         * not either ON or OFF, we will need to update the AE mode to one of those states, here we
         * will choose ON. It is the responsibility of the application layer above CameraPipe to
         * restore the AE mode after the torch control has been used. The [update3A] method can be
         * used to restore the AE state to a previous value.
         *
         * @return the FrameNumber at which the turn was fully turned on if switch was ON, or the
         *   FrameNumber at which it was completely turned off when the switch was OFF.
         */
        fun setTorch(torchState: TorchState): Deferred<Result3A>

        /**
         * Locks the auto-exposure, auto-focus and auto-whitebalance as per the given desired
         * behaviors. This given 3A parameters are applied before the lock is obtained. If 'null'
         * value is passed for a parameter, that parameter is ignored, and the current value for
         * that parameter continues to be applied.
         *
         * @param afTriggerStartAeMode the AeMode value that should override current AeMode for
         *   AF_TRIGGER_START request, this value should not be retained for following requests
         * @param convergedCondition an optional function can be used to identify if the result
         * frame with correct 3A converge state is received. Returns true to complete the 3A scan
         * and going to lock the 3A state, otherwise it will continue to receive the frame results
         * until the [frameLimit] or [timeLimitNs] is reached.
         * @param lockedCondition an optional function can be used to identify if the result frame
         * with correct 3A lock states are received. Returns true to complete lock 3A task,
         * otherwise it will continue to receive the frame results until the [frameLimit]
         * or [timeLimitNs] is reached.
         * @param frameLimit the maximum number of frames to wait before we give up waiting for this
         *   operation to complete.
         * @param timeLimitNs the maximum time limit in ms we wait before we give up waiting for
         *   this operation to complete.
         * @return [Result3A], which will contain the latest frame number at which the locks were
         *   applied or the frame number at which the method returned early because either frame
         *   limit or time limit was reached.
         *
         * TODO(sushilnath@): Add support for specifying the AE, AF and AWB modes as well. The
         *   update of modes require special care if the desired lock behavior is immediate. In that
         *   case we have to submit a combination of repeating and single requests so that the AF
         *   skips the initial state of the new mode's state machine and stays locks in the new mode
         *   as well.
         */
        suspend fun lock3A(
            aeMode: AeMode? = null,
            afMode: AfMode? = null,
            awbMode: AwbMode? = null,
            aeRegions: List<MeteringRectangle>? = null,
            afRegions: List<MeteringRectangle>? = null,
            awbRegions: List<MeteringRectangle>? = null,
            aeLockBehavior: Lock3ABehavior? = null,
            afLockBehavior: Lock3ABehavior? = null,
            awbLockBehavior: Lock3ABehavior? = null,
            afTriggerStartAeMode: AeMode? = null,
            convergedCondition: ((FrameMetadata) -> Boolean)? = null,
            lockedCondition: ((FrameMetadata) -> Boolean)? = null,
            frameLimit: Int = DEFAULT_FRAME_LIMIT,
            timeLimitNs: Long = DEFAULT_TIME_LIMIT_NS
        ): Deferred<Result3A>

        /**
         * Unlocks auto-exposure, auto-focus, auto-whitebalance. Once they are unlocked they get
         * back to their initial state or resume their auto scan depending on the current mode they
         * are operating in.
         *
         * Providing 'true' for a parameter in this method will unlock that component and if 'false'
         * is provided or the parameter is not specified then it will have no effect on the lock of
         * that component, i.e. if it was locked earlier it will stay locked and if it was already
         * unlocked, it will stay unlocked.
         *
         * @param unlockedCondition an optional function can be used to identify if the result frame
         * with correct ae, af and awb states are received. Returns true to complete the unlock
         * 3A task, otherwise it will continue to receive the frame results until the [frameLimit]
         * or [timeLimitNs] is reached.
         * @param frameLimit the maximum number of frames to wait before we give up waiting for this
         *   operation to complete.
         * @param timeLimitNs the maximum time limit in ms we wait before we give up waiting for
         *   this operation to complete.
         *
         * @return [Result3A], which will contain the latest frame number at which the auto-focus,
         *   auto-exposure, auto-white balance were unlocked as per the method arguments.
         */
        suspend fun unlock3A(
            ae: Boolean? = null,
            af: Boolean? = null,
            awb: Boolean? = null,
            unlockedCondition: ((FrameMetadata) -> Boolean)? = null,
            frameLimit: Int = DEFAULT_FRAME_LIMIT,
            timeLimitNs: Long = DEFAULT_TIME_LIMIT_NS
        ): Deferred<Result3A>

        /**
         * This methods does pre-capture metering sequence and locks auto-focus. Once the operation
         * completes, we can proceed to take high-quality pictures.
         *
         * Note: Flash will be used during pre-capture metering and during image capture if the AE
         * mode was set to [AeMode.ON_AUTO_FLASH] or [AeMode.ON_ALWAYS_FLASH], thus firing it for
         * low light captures or for every capture, respectively.
         *
         * @param lockedCondition an optional function can be used to identify if the result frame
         * with correct lock states for ae, af and awb is received. Returns true to complete lock
         * 3A task, otherwise it will continue to receive the frame results until the [frameLimit]
         * or [timeLimitNs] is reached.
         * @param frameLimit the maximum number of frames to wait before we give up waiting for this
         *   operation to complete.
         * @param timeLimitNs the maximum time limit in ms we wait before we give up waiting for
         *   this operation to complete.
         * @return [Result3A], which will contain the latest frame number at which the locks were
         *   applied or the frame number at which the method returned early because either frame
         *   limit or time limit was reached.
         */
        suspend fun lock3AForCapture(
            lockedCondition: ((FrameMetadata) -> Boolean)? = null,
            frameLimit: Int = DEFAULT_FRAME_LIMIT,
            timeLimitNs: Long = DEFAULT_TIME_LIMIT_NS
        ): Deferred<Result3A>

        /**
         * After submitting pre-capture metering sequence needed by [lock3AForCapture] method, the
         * camera system can internally lock the auto-exposure routine for subsequent still image
         * capture, and if not image capture request is submitted the auto-exposure may not resume
         * it's normal scan. This method brings focus and exposure back to normal after high quality
         * image captures using [lock3AForCapture] method.
         */
        suspend fun unlock3APostCapture(): Deferred<Result3A>
    }
}

/**
 * GraphState represents public the public facing state of a [CameraGraph] instance. When created,
 * a [CameraGraph] starts in [GraphStateStopped]. Calling [CameraGraph.start] puts the graph into
 * [GraphStateStarting], and [CameraGraph.stop] puts the graph into [GraphStateStopping]. Remaining
 * states are produced by the underlying camera as a result of these start/stop calls.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class GraphState internal constructor() {
    /**
     * When the [CameraGraph] is starting. This means we're in the process of opening a (virtual)
     * camera and creating a capture session.
     */
    object GraphStateStarting : GraphState()

    /**
     * When the [CameraGraph] is started. This means a capture session has been successfully created
     * for the [CameraGraph].
     */
    object GraphStateStarted : GraphState()

    /**
     * When the [CameraGraph] is stopping. This means we're in the process of stopping the graph.
     */
    object GraphStateStopping : GraphState()

    /**
     * When the [CameraGraph] hasn't been started, or stopped. This does not guarantee the closure
     * of the capture session or the camera device itself.
     */
    object GraphStateStopped : GraphState()

    /**
     * When the [CameraGraph] has encountered an error. If [willAttemptRetry] is true, CameraPipe
     * will retry opening the camera (and creating a capture session).
     */
    class GraphStateError(val cameraError: CameraError, val willAttemptRetry: Boolean) :
        GraphState() {
        override fun toString(): String =
            super.toString() + "(cameraError = $cameraError, willAttemptRetry = $willAttemptRetry)"
    }
}
