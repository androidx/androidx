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

package androidx.camera.camera2.pipe.integration.compat

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.annotation.ExecutedBy
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG_KEY = "Camera2CameraControl.tag"

@ExperimentalCamera2Interop
interface Camera2CameraControlCompat : Request.Listener {
    fun addRequestOption(bundle: CaptureRequestOptions)
    fun getRequestOption(): CaptureRequestOptions
    fun clearRequestOption()
    fun cancelCurrentTask()

    fun applyAsync(camera: UseCaseCamera?): Deferred<Void?>

    @Module
    abstract class Bindings {
        @Binds
        abstract fun bindCamera2CameraControlCompImpl(
            impl: Camera2CameraControlCompatImpl
        ): Camera2CameraControlCompat
    }
}

@CameraScope
@ExperimentalCamera2Interop
class Camera2CameraControlCompatImpl @Inject constructor(
    private val threads: UseCaseThreads,
) : Camera2CameraControlCompat {

    private val lock = Any()

    @GuardedBy("lock")
    private var configBuilder = Camera2ImplConfig.Builder()

    private var updateSignal: CompletableDeferred<Void?>? = null
    private var pendingSignal: CompletableDeferred<Void?>? = null

    override fun addRequestOption(bundle: CaptureRequestOptions) {
        synchronized(lock) {
            for (option in bundle.listOptions()) {
                @Suppress("UNCHECKED_CAST")
                val objectOpt = option as Config.Option<Any>
                configBuilder.mutableConfig.insertOption(
                    objectOpt,
                    Config.OptionPriority.ALWAYS_OVERRIDE,
                    bundle.retrieveOption(objectOpt)
                )
            }
        }
    }

    override fun getRequestOption(): CaptureRequestOptions =
        synchronized(lock) {
            CaptureRequestOptions.Builder.from(
                configBuilder.build()
            ).build()
        }

    override fun clearRequestOption() {
        synchronized(lock) {
            configBuilder = Camera2ImplConfig.Builder()
        }
    }

    override fun cancelCurrentTask() {
        threads.sequentialScope.launch {
            updateSignal?.also {
                updateSignal = null
            }?.cancelSignal("The camera control has became inactive.")
            pendingSignal?.also {
                pendingSignal = null
            }?.cancelSignal("The camera control has became inactive.")
        }
    }

    override fun applyAsync(camera: UseCaseCamera?): Deferred<Void?> {
        val signal: CompletableDeferred<Void?> = CompletableDeferred()
        val config = synchronized(lock) {
            configBuilder.build()
        }
        threads.sequentialScope.launch {
            if (camera != null) {
                // Cancel the previous request signal if exist.
                updateSignal?.cancelSignal()
                updateSignal = signal
                camera.requestControl.setConfigAsync(
                    type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
                    config = config,
                    tags = mapOf(TAG_KEY to signal.hashCode())
                )
            } else {
                // If there is no camera for the parameter update, the signal would be treated as a
                // pending signal, and the pending signal would be completed after the camera
                // applied the parameter.

                // Cancel the previous request signal if it exists. Only keep the latest signal.
                pendingSignal?.cancelSignal()
                pendingSignal = signal
            }
        }

        return signal
    }

    private fun CompletableDeferred<Void?>.cancelSignal(
        msg: String = "Camera2CameraControl was updated with new options."
    ) = this.apply {
        completeExceptionally(CameraControl.OperationCanceledException(msg))
    }

    @ExecutedBy("UseCaseThreads")
    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        updateSignal?.apply {
            val repeatingTagValue = requestMetadata.getOrDefault(
                CAMERAX_TAG_BUNDLE,
                TagBundle.emptyBundle()
            ).getTag(TAG_KEY)

            if (repeatingTagValue == hashCode()) {
                // Going to complete the [updateSignal] if the result contains the [TAG_KEY]
                complete(null)
                updateSignal = null

                // Also complete the [pendingSignal] if it exists.
                pendingSignal?.also {
                    it.complete(null)
                    pendingSignal = null
                }
            }
        }
    }
}