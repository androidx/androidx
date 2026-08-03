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

@file:Suppress("DEPRECATION")

package androidx.camera.camera2.compat

import androidx.annotation.GuardedBy
import androidx.camera.camera2.adapter.propagateTo
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.impl.containsTag
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.core.CameraControl
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.MutableConfig
import androidx.camera.core.impl.annotation.ExecutedBy
import dagger.Binds
import dagger.Module
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

private const val TAG_KEY = "Camera2CameraControl.tag"

@JvmDefaultWithCompatibility
@ExperimentalCamera2Interop
public interface Camera2CameraControlCompat : Request.Listener {
    public fun addRequestOption(bundle: CaptureRequestOptions)

    public fun getRequestOption(): CaptureRequestOptions

    public fun clearRequestOption()

    public fun cancelCurrentTask()

    public fun getSynchronizedMutableConfig(): MutableConfig

    public fun applyAsync(
        requestControl: UseCaseCameraRequestControl?,
        cancelPreviousTask: Boolean = true,
    ): Deferred<Void?>

    @Module
    public abstract class Bindings {
        @Binds
        public abstract fun bindCamera2CameraControlCompImpl(
            impl: Camera2CameraControlCompatImpl
        ): Camera2CameraControlCompat
    }
}

@CameraScope
@ExperimentalCamera2Interop
public class Camera2CameraControlCompatImpl @Inject constructor() : Camera2CameraControlCompat {

    private val lock = Any()
    private val updateSignalLock = Any()

    @GuardedBy("lock") private var configBuilder = Camera2ImplConfig.Builder()
    @GuardedBy("updateSignalLock") private var nextSignalId = 0L
    @GuardedBy("updateSignalLock") private var updateSignal: SignalWithId? = null
    @GuardedBy("updateSignalLock") private var pendingSignal: SignalWithId? = null

    override fun addRequestOption(bundle: CaptureRequestOptions) {
        synchronized(lock) {
            for (option in bundle.listOptions()) {
                @Suppress("UNCHECKED_CAST") val objectOpt = option as Config.Option<Any>
                configBuilder.mutableConfig.insertOption(
                    objectOpt,
                    Config.OptionPriority.ALWAYS_OVERRIDE,
                    bundle.retrieveOption(objectOpt),
                )
            }
        }
    }

    override fun getSynchronizedMutableConfig(): MutableConfig {
        return object : MutableConfig {
            override fun <ValueT> insertOption(opt: Config.Option<ValueT?>, value: ValueT?) {
                synchronized(lock) { configBuilder.mutableConfig.insertOption(opt, value) }
            }

            override fun <ValueT> insertOption(
                opt: Config.Option<ValueT?>,
                priority: Config.OptionPriority,
                value: ValueT?,
            ) {
                synchronized(lock) {
                    configBuilder.mutableConfig.insertOption(opt, priority, value)
                }
            }

            override fun <ValueT> removeOption(opt: Config.Option<ValueT?>): ValueT? {
                return synchronized(lock) { configBuilder.mutableConfig.removeOption(opt) }
            }

            override fun containsOption(id: Config.Option<*>): Boolean {
                return synchronized(lock) { configBuilder.mutableConfig.containsOption(id) }
            }

            override fun <ValueT> retrieveOption(id: Config.Option<ValueT?>): ValueT? {
                return synchronized(lock) { configBuilder.mutableConfig.retrieveOption(id) }
            }

            override fun <ValueT> retrieveOption(
                id: Config.Option<ValueT?>,
                valueIfMissing: ValueT?,
            ): ValueT? {
                return synchronized(lock) {
                    configBuilder.mutableConfig.retrieveOption(id, valueIfMissing)
                }
            }

            override fun <ValueT> retrieveOptionWithPriority(
                id: Config.Option<ValueT?>,
                priority: Config.OptionPriority,
            ): ValueT? {
                return synchronized(lock) {
                    configBuilder.mutableConfig.retrieveOptionWithPriority(id, priority)
                }
            }

            override fun getOptionPriority(opt: Config.Option<*>): Config.OptionPriority {
                return synchronized(lock) { configBuilder.mutableConfig.getOptionPriority(opt) }
            }

            override fun findOptions(idSearchString: String, matcher: Config.OptionMatcher) {
                return synchronized(lock) {
                    configBuilder.mutableConfig.findOptions(idSearchString, matcher)
                }
            }

            override fun listOptions(): Set<Config.Option<*>?> {
                return synchronized(lock) { configBuilder.mutableConfig.listOptions() }
            }

            override fun getPriorities(option: Config.Option<*>): Set<Config.OptionPriority?> {
                return synchronized(lock) { configBuilder.mutableConfig.getPriorities(option) }
            }
        }
    }

    override fun getRequestOption(): CaptureRequestOptions =
        synchronized(lock) { CaptureRequestOptions.Builder.from(configBuilder.build()).build() }

    override fun clearRequestOption() {
        synchronized(lock) { configBuilder = Camera2ImplConfig.Builder() }
    }

    override fun cancelCurrentTask(): Unit =
        synchronized(updateSignalLock) {
            updateSignal
                ?.also { updateSignal = null }
                ?.signal
                ?.cancelSignal("The camera control has became inactive.")
            pendingSignal
                ?.also { pendingSignal = null }
                ?.signal
                ?.cancelSignal("The camera control has became inactive.")
        }

    override fun applyAsync(
        requestControl: UseCaseCameraRequestControl?,
        cancelPreviousTask: Boolean,
    ): Deferred<Void?> {
        val signal: CompletableDeferred<Void?> = CompletableDeferred()
        val config = synchronized(lock) { configBuilder.build() }
        synchronized(updateSignalLock) {
            val requestId = nextSignalId++

            if (requestControl != null) {
                if (cancelPreviousTask) {
                    // Cancel the previous request signal if exist.
                    updateSignal?.signal?.cancelSignal()
                } else {
                    // propagate the result to the previous updateSignal
                    updateSignal?.signal?.let { previousUpdateSignal ->
                        signal.propagateTo(previousUpdateSignal)
                    }
                }

                updateSignal = SignalWithId(requestId, signal)
                requestControl.updateCamera2ConfigAsync(
                    config = config,
                    tags = mapOf(TAG_KEY to requestId),
                )
            } else {
                // If there is no camera for the parameter update, the signal would be treated as a
                // pending signal, and the pending signal would be completed after the camera
                // applied the parameter.

                // Cancel the previous request signal if it exists. Only keep the latest signal.
                pendingSignal?.signal?.cancelSignal()
                pendingSignal = SignalWithId(requestId, signal)
            }
        }

        return signal
    }

    private fun CompletableDeferred<Void?>.cancelSignal(
        msg: String = "Camera2CameraControl was updated with new options."
    ) = this.apply { completeExceptionally(CameraControl.OperationCanceledException(msg)) }

    @ExecutedBy("UseCaseThreads")
    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo,
    ): Unit =
        synchronized(updateSignalLock) {
            updateSignal?.let { (id, updateDef) ->
                if (requestMetadata.containsTag(TAG_KEY, id)) {
                    // Going to complete the [updateSignal] if the result contains the [TAG_KEY]
                    updateDef.complete(null)
                    updateSignal = null

                    // Also complete the [pendingSignal] if it exists.
                    pendingSignal?.also { (_, pendingDef) ->
                        pendingDef.complete(null)
                        pendingSignal = null
                    }
                }
            }
        }
}

private data class SignalWithId(val id: Long, val signal: CompletableDeferred<Void?>)
