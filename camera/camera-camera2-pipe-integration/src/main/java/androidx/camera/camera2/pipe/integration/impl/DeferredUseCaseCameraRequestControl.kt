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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * A proxy implementation of [UseCaseCameraRequestControlImpl] that allows for lazy initialization.
 *
 * This class ensures that the creation of the underlying [UseCaseCameraRequestControlImpl] (via
 * [Provider.get]) happens on the background sequential thread, preventing main-thread blocking
 * during startup. It also ensures strict ordering of requests by dispatching all calls to the
 * sequential scope.
 */
@UseCaseCameraScope
public class DeferredUseCaseCameraRequestControl
@Inject
constructor(
    private val implProvider: Provider<UseCaseCameraRequestControlImpl>,
    private val threads: UseCaseThreads,
) : UseCaseCameraRequestControl {

    @Volatile private var impl: UseCaseCameraRequestControlImpl? = null

    private val isClosed = AtomicBoolean(false)

    /**
     * Internal helper to initialize the implementation if needed. Must be called within the
     * sequential scope or a lock.
     */
    private fun getOrCreateImpl(): UseCaseCameraRequestControlImpl {
        if (isClosed.get()) {
            throw CancellationException("UseCaseCameraRequestControl is closed")
        }

        impl?.let {
            return it
        }
        val instance = implProvider.get()
        if (isClosed.get()) {
            // Re-check closed state in case close() was called during get()
            instance.close()
            throw CancellationException("UseCaseCameraRequestControl closed during initialization")
        }
        impl = instance
        return instance
    }

    /** Standard utility for methods returning Deferred<T>. */
    private inline fun <T> runOnSequential(
        crossinline action: UseCaseCameraRequestControl.() -> Deferred<T>
    ): Deferred<T> {
        // Fast path: if initialized, run immediately
        impl?.let {
            return it.action()
        }

        // Slow path: initialize on background thread
        return threads.sequentialScope.async { getOrCreateImpl().action().await() }
    }

    /** Utility for methods returning List<Deferred<T>> (e.g. issueSingleCaptureAsync). */
    private inline fun <T> runOnSequentialList(
        size: Int,
        crossinline action: UseCaseCameraRequestControl.() -> List<Deferred<T>>,
    ): List<Deferred<T>> {
        // Fast path
        impl?.let {
            return it.action()
        }

        // Slow path: Create a job that returns the list of deferreds
        val submissionJob = threads.sequentialScope.async { getOrCreateImpl().action() }

        return List(size) { index ->
            threads.sequentialScope.async {
                val realDeferreds = submissionJob.await()
                if (index < realDeferreds.size) {
                    realDeferreds[index].await()
                } else {
                    // This is safe because this method is currently only used with T=Void?,
                    // where null is the correct return value.
                    @Suppress("UNCHECKED_CAST")
                    null as T
                }
            }
        }
    }

    /** Utility for suspend functions (e.g. awaitSurfaceSetup). */
    private suspend inline fun <T> runOnSequentialSuspend(
        crossinline action: suspend UseCaseCameraRequestControl.() -> T
    ): T {
        // Fast path
        impl?.let {
            return it.action()
        }

        // Slow path
        return withContext(threads.sequentialExecutor.asCoroutineDispatcher()) {
            getOrCreateImpl().action()
        }
    }

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        type: UseCaseCameraRequestControl.Type,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> = runOnSequential { setParametersAsync(values, type, optionPriority) }

    override fun submitParameters(
        values: Map<CaptureRequest.Key<*>, Any>,
        type: UseCaseCameraRequestControl.Type,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> = runOnSequential { submitParameters(values, type, optionPriority) }

    override fun removeParametersAsync(
        keys: List<CaptureRequest.Key<*>>,
        type: UseCaseCameraRequestControl.Type,
    ): Deferred<Unit> = runOnSequential { removeParametersAsync(keys, type) }

    override fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Deferred<Unit> = runOnSequential { updateRepeatingRequestAsync(isPrimary, runningUseCases) }

    override fun updateCamera2ConfigAsync(config: Config, tags: Map<String, Any>): Deferred<Unit> =
        runOnSequential {
            updateCamera2ConfigAsync(config, tags)
        }

    override fun setTorchOnAsync(): Deferred<Result3A> = runOnSequential { setTorchOnAsync() }

    override fun setTorchOffAsync(aeMode: AeMode): Deferred<Result3A> = runOnSequential {
        setTorchOffAsync(aeMode)
    }

    override fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        afTriggerStartAeMode: AeMode?,
        timeLimitNs: Long,
    ): Deferred<Result3A> = runOnSequential {
        startFocusAndMeteringAsync(
            aeRegions,
            afRegions,
            awbRegions,
            aeLockBehavior,
            afLockBehavior,
            awbLockBehavior,
            afTriggerStartAeMode,
            timeLimitNs,
        )
    }

    override fun cancelFocusAndMeteringAsync(): Deferred<Result3A> = runOnSequential {
        cancelFocusAndMeteringAsync()
    }

    override fun update3aRegions(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> = runOnSequential { update3aRegions(aeRegions, afRegions, awbRegions) }

    override fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int,
    ): List<Deferred<Void?>> =
        runOnSequentialList(captureSequence.size) {
            issueSingleCaptureAsync(captureSequence, captureMode, flashType, flashMode)
        }

    override suspend fun awaitSurfaceSetup(): Boolean = runOnSequentialSuspend {
        awaitSurfaceSetup()
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return // Already closed
        }
        // Fire and forget close on the sequential thread
        threads.confineLaunch { impl?.close() }
    }
}
