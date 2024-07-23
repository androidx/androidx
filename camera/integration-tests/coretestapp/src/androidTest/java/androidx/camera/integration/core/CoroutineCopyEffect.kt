/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.core

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.annotation.WorkerThread
import androidx.camera.core.CameraEffect
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.processing.OpenGlRenderer
import androidx.camera.core.processing.util.GLUtils.InputFormat
import java.util.concurrent.Executors
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val GL_THREAD_NAME = "GLThread-CopyEffect"

class CopyingSurfaceProcessor : SurfaceProcessor, AutoCloseable {

    private val surfaceRequestChannel =
        Channel<SurfaceRequest>(capacity = CONFLATED) { undeliveredSurfaceRequest ->
            undeliveredSurfaceRequest.willNotProvideSurface()
        }
    private val surfaceOutputChannel =
        Channel<SurfaceOutput>(capacity = CONFLATED) { undeliveredSurfaceOutput ->
            undeliveredSurfaceOutput.close()
        }

    @WorkerThread // Should only be called with a single-threaded GL thread dispatcher
    suspend fun process() {
        val glRenderer = OpenGlRenderer()
        var dynamicRange: DynamicRange? = null
        surfaceRequestChannel.receiveAsFlow().collectLatest { surfaceRequest ->
            coroutineScope requestScope@{
                // Reset renderer if dynamic range has changed
                val newDynamicRange = surfaceRequest.dynamicRange
                if (dynamicRange != newDynamicRange) {
                    dynamicRange?.let { glRenderer.release() }
                    glRenderer.init(newDynamicRange)
                    dynamicRange = newDynamicRange
                }

                // Update the input format from the transformation info.
                // Initially initialize it to UNKNOWN until first transformation info.
                glRenderer.setInputFormat(InputFormat.UNKNOWN)
                launch {
                    surfaceRequest
                        .flowOnTransformationInfo()
                        .map {
                            if (it.hasCameraTransform()) {
                                InputFormat.YUV
                            } else {
                                InputFormat.DEFAULT
                            }
                        }
                        .collectLatest { glRenderer.setInputFormat(it) }
                }

                val surfaceTexture = SurfaceTexture(glRenderer.textureName)
                surfaceTexture.setDefaultBufferSize(
                    surfaceRequest.resolution.width,
                    surfaceRequest.resolution.height
                )
                val inputSurface = Surface(surfaceTexture)
                val inputUseCount = SimpleRefCount {
                    inputSurface.release()
                    surfaceTexture.release()
                }
                surfaceRequest.provideSurface(inputSurface, Runnable::run) {
                    this@requestScope.cancel()
                    inputUseCount.decRef()
                }

                val inputInUse = inputUseCount.incRef()
                surfaceOutputChannel
                    .receiveAsFlow()
                    .onCompletion {
                        if (inputInUse) {
                            inputUseCount.decRef()
                        }
                    }
                    .collectLatest { surfaceOutput ->
                        coroutineScope outputScope@{
                            checkGlThread()
                            val outputUseCount = SimpleRefCount { surfaceOutput.close() }
                            val surfaceOutputComplete = atomic(false)
                            val outputSurface =
                                surfaceOutput.getSurface(Runnable::run) {
                                    // Ensure this is only called once
                                    if (!surfaceOutputComplete.getAndSet(true)) {
                                        this@outputScope.cancel()
                                        outputUseCount.decRef()
                                    }
                                }

                            if (outputUseCount.incRef()) {
                                try {
                                    glRenderer.registerOutputSurface(outputSurface)

                                    val textureTransform = FloatArray(16)
                                    val surfaceTransform = FloatArray(16)
                                    surfaceTexture
                                        .flowOnFrameAvailability()
                                        .onCompletion {
                                            glRenderer.unregisterOutputSurface(outputSurface)
                                        }
                                        .collect {
                                            checkGlThread()
                                            surfaceTexture.updateTexImage()
                                            surfaceTexture.getTransformMatrix(textureTransform)
                                            surfaceOutput.updateTransformMatrix(
                                                surfaceTransform,
                                                textureTransform
                                            )
                                            glRenderer.render(
                                                surfaceTexture.timestamp,
                                                surfaceTransform,
                                                outputSurface
                                            )
                                        }
                                } finally {
                                    outputUseCount.decRef()
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun SurfaceTexture.flowOnFrameAvailability() =
        callbackFlow {
                val frameCount = atomic(0)
                setOnFrameAvailableListener { trySend(frameCount.incrementAndGet()) }

                awaitClose { setOnFrameAvailableListener(null) }
            }
            .buffer(capacity = CONFLATED)

    private fun SurfaceRequest.flowOnTransformationInfo() =
        callbackFlow {
                setTransformationInfoListener(Runnable::run) { trySend(it) }

                awaitClose { clearTransformationInfoListener() }
            }
            .buffer(capacity = CONFLATED)

    private fun checkGlThread() {
        check(GL_THREAD_NAME == Thread.currentThread().name)
    }

    override fun onInputSurface(request: SurfaceRequest) {
        val sendResult = surfaceRequestChannel.trySend(request)
        if (!sendResult.isSuccess) {
            request.willNotProvideSurface()
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        val sendResult = surfaceOutputChannel.trySend(surfaceOutput)
        if (!sendResult.isSuccess) {
            surfaceOutput.close()
        }
    }

    override fun close() {
        surfaceRequestChannel.close()
        surfaceOutputChannel.close()
    }
}

private class SimpleRefCount(initialCount: Int = 1, private val onRelease: () -> Unit) {
    private val refCount = atomic(initialCount)

    fun incRef(): Boolean {
        refCount.loop { old ->
            if (old == 0) {
                return false
            }

            val newValue = old + 1
            if (refCount.compareAndSet(old, newValue)) {
                return true
            }
        }
    }

    fun decRef() {
        refCount.loop { old ->
            if (old == 0) {
                throw IllegalStateException("decRef called too many times.")
            }

            val newValue = old - 1
            if (refCount.compareAndSet(old, newValue)) {
                if (newValue == 0) {
                    onRelease()
                }
                return
            }
        }
    }
}

private const val TARGETS =
    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE

/**
 * Creates a copy [CameraEffect] that processes in a launched coroutine.
 *
 * This copies the camera input to the targets, Preview, VideoCapture and ImageCapture.
 */
fun CoroutineScope.launchCopyEffect(
    targets: Int = TARGETS,
    onPreLaunch: (CameraEffect) -> Unit
): Job {
    val processor = CopyingSurfaceProcessor()
    val cameraEffect = object : CameraEffect(targets, Runnable::run, processor, {}) {}

    onPreLaunch(cameraEffect)

    return this.launch {
        val glExecutor = Executors.newSingleThreadExecutor()
        try {
            processor.use {
                withContext(glExecutor.asCoroutineDispatcher()) {
                    Thread.currentThread().name = GL_THREAD_NAME
                    processor.process()
                }
            }
        } finally {
            glExecutor.shutdown()
        }
    }
}
