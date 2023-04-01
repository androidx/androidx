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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK
import android.hardware.camera2.CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER
import android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER
import android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
import android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_START
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.DEFAULT_FRAME_LIMIT
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.DEFAULT_TIME_LIMIT_NS
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.FlashMode
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.Result3A.Status
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.supportsAutoFocusTrigger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel

/** This class implements the 3A methods of [CameraGraphSessionImpl]. */
internal class Controller3A(
    private val graphProcessor: GraphProcessor,
    private val metadata: CameraMetadata,
    private val graphState3A: GraphState3A,
    private val graphListener3A: Listener3A
) {
    companion object {
        private val aeConvergedStateList =
            listOf(
                CaptureResult.CONTROL_AE_STATE_CONVERGED,
                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                CaptureResult.CONTROL_AE_STATE_LOCKED
            )

        private val awbConvergedStateList =
            listOf(
                CaptureResult.CONTROL_AWB_STATE_CONVERGED, CaptureResult.CONTROL_AWB_STATE_LOCKED
            )

        private val afConvergedStateList =
            listOf(
                CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            )

        private val aeLockedStateList = listOf(CaptureResult.CONTROL_AE_STATE_LOCKED)

        private val awbLockedStateList = listOf(CaptureResult.CONTROL_AWB_STATE_LOCKED)

        private val afLockedStateList =
            listOf(
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            )

        private val aePostPrecaptureStateList =
            listOf(
                CaptureResult.CONTROL_AE_STATE_CONVERGED,
                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                CaptureResult.CONTROL_AE_STATE_LOCKED
            )

        val parameterForAfTriggerStart =
            mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AF_TRIGGER to CONTROL_AF_TRIGGER_START)

        val parameterForAfTriggerCancel =
            mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AF_TRIGGER to CONTROL_AF_TRIGGER_CANCEL)

        private val parametersForAePrecaptureAndAfTrigger =
            mapOf<CaptureRequest.Key<*>, Any>(
                CONTROL_AF_TRIGGER to CONTROL_AF_TRIGGER_START,
                CONTROL_AE_PRECAPTURE_TRIGGER to CONTROL_AE_PRECAPTURE_TRIGGER_START
            )

        private val result3ASubmitFailed = Result3A(Status.SUBMIT_FAILED)

        private val aeUnlockedStateList =
            listOf(
                CaptureResult.CONTROL_AE_STATE_INACTIVE,
                CaptureResult.CONTROL_AE_STATE_SEARCHING,
                CaptureResult.CONTROL_AE_STATE_CONVERGED,
                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
            )

        private val afUnlockedStateList =
            listOf(
                CaptureResult.CONTROL_AF_STATE_INACTIVE,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN,
                CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED
            )

        private val awbUnlockedStateList =
            listOf(
                CaptureResult.CONTROL_AWB_STATE_INACTIVE,
                CaptureResult.CONTROL_AWB_STATE_SEARCHING,
                CaptureResult.CONTROL_AWB_STATE_CONVERGED
            )
    }

    // Keep track of the result associated with latest call to update3A. If update3A is called again
    // and the current result is not complete, we will cancel the current result.
    @GuardedBy("this")
    private var lastUpdate3AResult: Deferred<Result3A>? = null

    fun update3A(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        flashMode: FlashMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null
    ): Deferred<Result3A> {
        // Add the listener to a global pool of 3A listeners to monitor the state change to the
        // desired one.
        val listener = createListenerFor3AParams(aeMode, afMode, awbMode, flashMode)
        graphListener3A.addListener(listener)

        // Update the 3A state of the graph. This will make sure then when GraphProcessor builds
        // the next request it will apply the 3A parameters corresponding to the updated 3A state
        // to the request.
        graphState3A.update(aeMode, afMode, awbMode, flashMode, aeRegions, afRegions, awbRegions)
        // Try submitting a new repeating request with the 3A parameters corresponding to the new
        // 3A state and corresponding listeners.
        graphProcessor.invalidate()

        val result = listener.result
        synchronized(this) {
            lastUpdate3AResult?.cancel("A newer call for 3A state update initiated.")
            lastUpdate3AResult = result
        }
        return result
    }

    suspend fun submit3A(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null
    ): Deferred<Result3A> {
        // Add the listener to a global pool of 3A listeners to monitor the state change to the
        // desired one.
        val listener = createListenerFor3AParams(aeMode, afMode, awbMode)
        graphListener3A.addListener(listener)

        val extra3AParams = mutableMapOf<CaptureRequest.Key<*>, Any?>()
        aeMode?.let { extra3AParams.put(CaptureRequest.CONTROL_AE_MODE, it.value) }
        afMode?.let { extra3AParams.put(CaptureRequest.CONTROL_AF_MODE, it.value) }
        awbMode?.let { extra3AParams.put(CaptureRequest.CONTROL_AWB_MODE, it.value) }
        aeRegions?.let { extra3AParams.put(CaptureRequest.CONTROL_AE_REGIONS, it.toTypedArray()) }
        afRegions?.let { extra3AParams.put(CaptureRequest.CONTROL_AF_REGIONS, it.toTypedArray()) }
        awbRegions?.let { extra3AParams.put(CaptureRequest.CONTROL_AWB_REGIONS, it.toTypedArray()) }

        if (!graphProcessor.trySubmit(extra3AParams)) {
            graphListener3A.removeListener(listener)
            return CompletableDeferred(result3ASubmitFailed)
        }
        return listener.result
    }

    /**
     * Given the desired metering regions and lock behaviors for ae, af and awb, this method updates
     * the metering regions and then, (a) unlocks ae, af, awb and wait for them to converge, and
     * then (b) locks them.
     *
     * (a) In this step, as needed, we first send a single request with 'af trigger = cancel' to
     * unlock af, and then a repeating request to unlock ae and awb. We suspend till we receive a
     * response from the camera that each of the ae, af awb are converged. (b) In this step, as
     * needed, we submit a repeating request to lock ae and awb, and then a single request to lock
     * af by setting 'af trigger = start'. Once these requests are submitted we don't wait further
     * and immediately return a Deferred<Result3A> which gets completed when the capture result with
     * correct lock states for ae, af and awb is received.
     *
     * If we received an error when submitting any of the above requests or if waiting for the
     * desired 3A state times out then we return early with the appropriate status code.
     *
     * If the operation is not supported by the camera device then this method returns early with
     * Result3A made of 'OK' status and 'null' metadata.
     *
     * Note: the frameLimit and timeLimitNs applies to each of the above steps (a) and (b) and not
     * as a whole for the whole lock3A method. Thus, in the worst case this method including the
     * completion of returned Deferred<Result3A> can take 2 * min(time equivalent of frameLimit,
     * timeLimit) to complete
     */
    suspend fun lock3A(
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
        aeLockBehavior: Lock3ABehavior? = null,
        afLockBehavior: Lock3ABehavior? = null,
        awbLockBehavior: Lock3ABehavior? = null,
        afTriggerStartAeMode: AeMode? = null,
        frameLimit: Int = DEFAULT_FRAME_LIMIT,
        timeLimitNs: Long? = DEFAULT_TIME_LIMIT_NS
    ): Deferred<Result3A> {
        var afLockBehaviorSanitized = afLockBehavior
        if (!metadata.supportsAutoFocusTrigger) {
            afLockBehaviorSanitized = null
        }
        if (aeLockBehavior == null && afLockBehaviorSanitized == null && awbLockBehavior == null) {
            return CompletableDeferred(Result3A(Status.OK, /* frameMetadata= */ null))
        }
        // Update the 3A state of camera graph with the given metering regions. If metering regions
        // are given as null then they are ignored and the current metering regions continue to be
        // applied in subsequent requests to the camera device.
        graphState3A.update(aeRegions = aeRegions, afRegions = afRegions, awbRegions = awbRegions)

        // If we explicitly need to unlock af first before proceeding to lock it, we need to send
        // a single request with TRIGGER = TRIGGER_CANCEL so that af can start a fresh scan.
        if (afLockBehaviorSanitized.shouldUnlockAf()) {
            debug { "lock3A - sending a request to unlock af first." }
            if (!graphProcessor.trySubmit(parameterForAfTriggerCancel)) {
                return CompletableDeferred(result3ASubmitFailed)
            }
        }

        // As needed unlock ae, awb and wait for ae, af and awb to converge.
        if (aeLockBehavior.shouldWaitForAeToConverge() ||
            afLockBehaviorSanitized.shouldWaitForAfToConverge() ||
            awbLockBehavior.shouldWaitForAwbToConverge()
        ) {
            val converged3AExitConditions =
                createConverged3AExitConditions(
                    aeLockBehavior.shouldWaitForAeToConverge(),
                    afLockBehaviorSanitized.shouldWaitForAfToConverge(),
                    awbLockBehavior.shouldWaitForAwbToConverge()
                )
            val listener =
                Result3AStateListenerImpl(converged3AExitConditions, frameLimit, timeLimitNs)
            graphListener3A.addListener(listener)

            // If we have to explicitly unlock ae, awb, then update the 3A state of the camera
            // graph. This is because ae, awb lock values should stay as part of repeating
            // request to the camera device. For af we need only one single request to trigger it,
            // leaving it unset in the subsequent requests to the camera device will not affect the
            // previously sent af trigger.
            val aeLockValue = if (aeLockBehavior.shouldUnlockAe()) false else null
            val awbLockValue = if (awbLockBehavior.shouldUnlockAwb()) false else null
            if (aeLockValue != null || awbLockValue != null) {
                debug { "lock3A - setting aeLock=$aeLockValue, awbLock=$awbLockValue" }
                graphState3A.update(aeLock = aeLockValue, awbLock = awbLockValue)
            }
            graphProcessor.invalidate()

            debug {
                "lock3A - waiting for" +
                    (if (aeLockBehavior.shouldWaitForAeToConverge()) " ae" else "") +
                    (if (afLockBehaviorSanitized.shouldWaitForAfToConverge()) " af" else "") +
                    (if (awbLockBehavior.shouldWaitForAwbToConverge()) " awb" else "") +
                    " to converge before locking them."
            }
            val result = listener.result.await()
            debug {
                "lock3A - converged at frame number=${result.frameMetadata?.frameNumber?.value}, " +
                    "status=${result.status}"
            }
            // Return immediately if we encounter an error when unlocking and waiting for
            // convergence.
            if (result.status != Status.OK) {
                return listener.result
            }
        }

        return lock3ANow(
            aeLockBehavior,
            afLockBehaviorSanitized,
            awbLockBehavior,
            afTriggerStartAeMode,
            frameLimit,
            timeLimitNs
        )
    }

    /**
     * This method unlocks ae, af and awb, as specified by setting the corresponding parameter to
     * true. If this operation is not supported by the camera device, then we return early with
     * Result3A made of 'OK' status and metadata as null.
     *
     * There are two requests involved in this operation, (a) a single request with af trigger =
     * cancel, to unlock af, and then (a) a repeating request to unlock ae, awb.
     */
    suspend fun unlock3A(
        ae: Boolean? = null,
        af: Boolean? = null,
        awb: Boolean? = null,
        frameLimit: Int = DEFAULT_FRAME_LIMIT,
        timeLimitNs: Long? = DEFAULT_TIME_LIMIT_NS
    ): Deferred<Result3A> {
        var afSanitized = af
        if (!metadata.supportsAutoFocusTrigger) {
            afSanitized = null
        }
        if (!(ae == true || afSanitized == true || awb == true)) {
            return CompletableDeferred(Result3A(Status.OK, /* frameMetadata= */ null))
        }
        // If we explicitly need to unlock af first before proceeding to lock it, we need to send
        // a single request with TRIGGER = TRIGGER_CANCEL so that af can start a fresh scan.
        if (afSanitized == true) {
            debug { "unlock3A - sending a request to unlock af first." }
            if (!graphProcessor.trySubmit(parameterForAfTriggerCancel)) {
                debug { "unlock3A - request to unlock af failed, returning early." }
                return CompletableDeferred(result3ASubmitFailed)
            }
        }

        // As needed unlock ae, awb and wait for ae, af and awb to converge.
        val unlocked3AExitConditions =
            createUnLocked3AExitConditions(ae == true, afSanitized == true, awb == true)
        val listener = Result3AStateListenerImpl(unlocked3AExitConditions, frameLimit, timeLimitNs)
        graphListener3A.addListener(listener)

        // Update the 3A state of the camera graph and invalidate the repeating request with the
        // new state.
        val aeLockValue = if (ae == true) false else null
        val awbLockValue = if (awb == true) false else null
        if (aeLockValue != null || awbLockValue != null) {
            debug { "unlock3A - updating graph state, aeLock=$aeLockValue, awbLock=$awbLockValue" }
            graphState3A.update(aeLock = aeLockValue, awbLock = awbLockValue)
        }
        graphProcessor.invalidate()
        return listener.result
    }

    suspend fun lock3AForCapture(
        frameLimit: Int = DEFAULT_FRAME_LIMIT,
        timeLimitNs: Long = DEFAULT_TIME_LIMIT_NS
    ): Deferred<Result3A> {
        val listener =
            Result3AStateListenerImpl(
                mapOf<CaptureResult.Key<*>, List<Any>>(
                    CaptureResult.CONTROL_AE_STATE to aePostPrecaptureStateList,
                    CaptureResult.CONTROL_AF_STATE to afLockedStateList
                ),
                frameLimit,
                timeLimitNs
            )
        graphListener3A.addListener(listener)

        debug { "lock3AForCapture - sending a request to trigger ae precapture metering and af." }
        if (!graphProcessor.trySubmit(parametersForAePrecaptureAndAfTrigger)) {
            debug {
                "lock3AForCapture - request to trigger ae precapture metering and af failed, " +
                    "returning early."
            }
            graphListener3A.removeListener(listener)
            return CompletableDeferred(result3ASubmitFailed)
        }

        graphProcessor.invalidate()
        return listener.result
    }

    suspend fun unlock3APostCapture(): Deferred<Result3A> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return unlock3APostCaptureAndroidMAndAbove()
        }
        return unlock3APostCaptureAndroidLAndBelow()
    }

    /**
     * For api level below 23, to resume the normal scan of ae after precapture metering sequence,
     * we have to first send a request with ae lock = true and then a request with ae lock = false.
     * REF :
     * https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER
     */
    private suspend fun unlock3APostCaptureAndroidLAndBelow(): Deferred<Result3A> {
        debug { "unlock3AForCapture - sending a request to cancel af and turn on ae." }
        if (!graphProcessor.trySubmit(
                mapOf(CONTROL_AF_TRIGGER to CONTROL_AF_TRIGGER_CANCEL, CONTROL_AE_LOCK to true)
            )
        ) {
            debug { "unlock3AForCapture - request to cancel af and lock ae as failed." }
            return CompletableDeferred(result3ASubmitFailed)
        }

        // Listener to monitor when we receive the capture result corresponding to the request
        // below.
        val listener = Result3AStateListenerImpl(mapOf())
        graphListener3A.addListener(listener)

        debug { "unlock3AForCapture - sending a request to turn off ae." }
        if (!graphProcessor.trySubmit(
                mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to false)
            )
        ) {
            debug { "unlock3AForCapture - request to unlock ae failed." }
            graphListener3A.removeListener(listener)
            return CompletableDeferred(result3ASubmitFailed)
        }

        return listener.result
    }

    /**
     * For API level 23 or newer versions, the sending a request with CONTROL_AE_PRECAPTURE_TRIGGER
     * = CANCEL can be used to unlock the camera device's internally locked AE. REF :
     * https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER
     */
    @RequiresApi(23)
    private suspend fun unlock3APostCaptureAndroidMAndAbove(): Deferred<Result3A> {
        debug { "unlock3APostCapture - sending a request to reset af and ae precapture metering." }
        val parametersForAePrecaptureAndAfCancel =
            mapOf<CaptureRequest.Key<*>, Any>(
                CONTROL_AF_TRIGGER to CONTROL_AF_TRIGGER_CANCEL,
                CONTROL_AE_PRECAPTURE_TRIGGER to
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
            )
        if (!graphProcessor.trySubmit(parametersForAePrecaptureAndAfCancel)) {
            debug {
                "unlock3APostCapture - request to reset af and ae precapture metering failed, " +
                    "returning early."
            }
            return CompletableDeferred(result3ASubmitFailed)
        }

        // Sending a request with ae precapture trigger = cancel does not have any specific affect
        // on the ae state, so we don't need to listen for a specific state. As long as the request
        // successfully reaches the camera device and the capture request corresponding to that
        // request arrives back, it should suffice.
        val listener =
            Result3AStateListenerImpl(
                mapOf<CaptureResult.Key<*>, List<Any>>(
                    CaptureResult.CONTROL_AF_STATE to afUnlockedStateList
                )
            )
        graphListener3A.addListener(listener)
        graphProcessor.invalidate()
        return listener.result
    }

    fun setTorch(torchState: TorchState): Deferred<Result3A> {
        // Determine the flash mode based on the torch state.
        val flashMode = if (torchState == TorchState.ON) FlashMode.TORCH else FlashMode.OFF
        // To use the flash control, AE mode must be set to ON or OFF.
        val currAeMode = graphState3A.aeMode
        val desiredAeMode =
            if (currAeMode == AeMode.ON || currAeMode == AeMode.OFF) null else AeMode.ON
        return update3A(aeMode = desiredAeMode, flashMode = flashMode)
    }

    private suspend fun lock3ANow(
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        afTriggerStartAeMode: AeMode? = null,
        frameLimit: Int?,
        timeLimitNs: Long?
    ): Deferred<Result3A> {
        val finalAeLockValue = if (aeLockBehavior == null) null else true
        val finalAwbLockValue = if (awbLockBehavior == null) null else true
        val locked3AExitConditions =
            createLocked3AExitConditions(
                finalAeLockValue != null, afLockBehavior != null, finalAwbLockValue != null
            )

        var resultForLocked: Deferred<Result3A>? = null
        if (locked3AExitConditions.isNotEmpty()) {
            val listener =
                Result3AStateListenerImpl(locked3AExitConditions, frameLimit, timeLimitNs)
            graphListener3A.addListener(listener)
            graphState3A.update(aeLock = finalAeLockValue, awbLock = finalAwbLockValue)
            debug {
                "lock3A - submitting request with aeLock=$finalAeLockValue , " +
                    "awbLock=$finalAwbLockValue"
            }
            graphProcessor.invalidate()
            resultForLocked = listener.result
        }

        if (afLockBehavior == null) {
            return resultForLocked!!
        }

        var lastAeMode: AeMode? = null
        afTriggerStartAeMode?.let {
            lastAeMode = graphState3A.aeMode
            graphState3A.update(it)
        }

        debug { "lock3A - submitting a request to lock af." }
        val submitSuccess = graphProcessor.trySubmit(parameterForAfTriggerStart)

        lastAeMode?.let {
            graphState3A.update(aeMode = it)
            // TODO(sushilnath@): Should be able to remove this invalidate() call
            //  by making sure invalidate() and submit() don't interleave
            //  w.r.t. building the parameter snapshot
            graphProcessor.invalidate()
        }

        if (!submitSuccess) {
            // TODO(sushilnath@): Change the error code to a more specific code so it's clear
            // that one of the request in sequence of requests failed and the caller should
            // unlock 3A to bring the 3A system to an initial state and then try again if they
            // want to. The other option is to reset or restore the 3A state here.
            return CompletableDeferred(result3ASubmitFailed)
        }
        return resultForLocked!!
    }

    private fun createConverged3AExitConditions(
        waitForAeToConverge: Boolean,
        waitForAfToConverge: Boolean,
        waitForAwbToConverge: Boolean
    ): Map<CaptureResult.Key<*>, List<Any>> {
        if (!waitForAeToConverge && !waitForAfToConverge && !waitForAwbToConverge) {
            return mapOf()
        }
        val exitConditionMapForConverged = mutableMapOf<CaptureResult.Key<*>, List<Any>>()
        if (waitForAeToConverge) {
            exitConditionMapForConverged[CaptureResult.CONTROL_AE_STATE] = aeConvergedStateList
        }
        if (waitForAwbToConverge) {
            exitConditionMapForConverged[CaptureResult.CONTROL_AWB_STATE] = awbConvergedStateList
        }
        if (waitForAfToConverge) {
            exitConditionMapForConverged[CaptureResult.CONTROL_AF_STATE] = afConvergedStateList
        }
        return exitConditionMapForConverged
    }

    private fun createLocked3AExitConditions(
        waitForAeToLock: Boolean,
        waitForAfToLock: Boolean,
        waitForAwbToLock: Boolean
    ): Map<CaptureResult.Key<*>, List<Any>> {
        if (!waitForAeToLock && !waitForAfToLock && !waitForAwbToLock) {
            return mapOf()
        }
        val exitConditionMapForLocked = mutableMapOf<CaptureResult.Key<*>, List<Any>>()
        if (waitForAeToLock) {
            exitConditionMapForLocked[CaptureResult.CONTROL_AE_STATE] = aeLockedStateList
        }
        if (waitForAfToLock) {
            exitConditionMapForLocked[CaptureResult.CONTROL_AF_STATE] = afLockedStateList
        }
        if (waitForAwbToLock) {
            exitConditionMapForLocked[CaptureResult.CONTROL_AWB_STATE] = awbLockedStateList
        }
        return exitConditionMapForLocked
    }

    private fun createUnLocked3AExitConditions(
        ae: Boolean,
        af: Boolean,
        awb: Boolean
    ): Map<CaptureResult.Key<*>, List<Any>> {
        if (!ae && !af && !awb) {
            return mapOf()
        }
        val exitConditionMapForUnLocked = mutableMapOf<CaptureResult.Key<*>, List<Any>>()
        if (ae) {
            exitConditionMapForUnLocked[CaptureResult.CONTROL_AE_STATE] = aeUnlockedStateList
        }
        if (af) {
            exitConditionMapForUnLocked[CaptureResult.CONTROL_AF_STATE] = afUnlockedStateList
        }
        if (awb) {
            exitConditionMapForUnLocked[CaptureResult.CONTROL_AWB_STATE] = awbUnlockedStateList
        }
        return exitConditionMapForUnLocked
    }

    // We create a map for the 3A modes and the desired values and leave out the keys
    // corresponding to the metering regions. The reason being the camera framework can chose to
    // crop or modify the metering regions as per its constraints. So when we receive at least
    // one capture result corresponding to this request it is assumed that the framework has
    // applied the desired metering regions to the best of its judgement, and we don't need an
    // exact match between the metering regions sent in the capture request and the metering
    // regions received from the camera device.
    private fun createListenerFor3AParams(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        flashMode: FlashMode? = null,
    ): Result3AStateListenerImpl {
        val resultModesMap = mutableMapOf<CaptureResult.Key<*>, List<Any>>()
        aeMode?.let { resultModesMap.put(CaptureResult.CONTROL_AE_MODE, listOf(it.value)) }
        afMode?.let { resultModesMap.put(CaptureResult.CONTROL_AF_MODE, listOf(it.value)) }
        awbMode?.let { resultModesMap.put(CaptureResult.CONTROL_AWB_MODE, listOf(it.value)) }
        flashMode?.let { resultModesMap.put(CaptureResult.FLASH_MODE, listOf(it.value)) }
        return Result3AStateListenerImpl(resultModesMap.toMap())
    }
}

internal fun Lock3ABehavior?.shouldUnlockAe(): Boolean = this == Lock3ABehavior.AFTER_NEW_SCAN

internal fun Lock3ABehavior?.shouldUnlockAf(): Boolean = this == Lock3ABehavior.AFTER_NEW_SCAN

internal fun Lock3ABehavior?.shouldUnlockAwb(): Boolean = this == Lock3ABehavior.AFTER_NEW_SCAN

// For ae and awb if we set the lock = true in the capture request the camera device
// locks them immediately. So when we want to wait for ae to converge we have to explicitly
// wait for it to converge.
internal fun Lock3ABehavior?.shouldWaitForAeToConverge(): Boolean =
    this != null && this != Lock3ABehavior.IMMEDIATE

internal fun Lock3ABehavior?.shouldWaitForAwbToConverge(): Boolean =
    this != null && this != Lock3ABehavior.IMMEDIATE

// TODO(sushilnath@): add the optimization to not wait for af to converge before sending the
// trigger for modes other than CONTINUOUS_VIDEO. The paragraph below explains the reasoning.
//
// For af, if the mode is MACRO, AUTO or CONTINUOUS_PICTURE and we send a capture request to
// start an af trigger then camera device starts a new scan(for AUTO mode) or waits for the
// current scan to finish(for CONTINUOUS_PICTURE) and then locks the auto-focus, so if we want
// to wait for af to converge before locking it, we don't have to explicitly wait for
// convergence, we can send the trigger right away, but if the mode is CONTINUOUS_VIDEO then
// sending a request to start a trigger locks the auto focus immediately, so if we want af to
// converge first then we have to explicitly wait for it.
// Ref:
// https://developer.android.com/reference/android/hardware/camera2/CaptureResult#CONTROL_AF_STATE
internal fun Lock3ABehavior?.shouldWaitForAfToConverge(): Boolean =
    this != null && this != Lock3ABehavior.IMMEDIATE
