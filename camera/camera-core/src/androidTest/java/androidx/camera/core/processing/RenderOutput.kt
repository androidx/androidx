/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.GLUtil
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private const val WIDTH = 640
private const val HEIGHT = 480
private const val RELEASE_TIMEOUT_MS = 5_000L

abstract class RenderOutput {
    abstract val surface: Surface
    abstract val dataSpace: Int
    private val handlerThread: HandlerThread =
        HandlerThread("RenderOutput Thread").apply { start() }
    protected val handler: Handler = Handler(handlerThread.looper)
    private val totalFrames = MutableStateFlow(0)
    private val consumedFrames = AtomicInteger(0)
    private val isReleased = AtomicBoolean(false)
    private var isClosedOnHandler = false

    companion object {
        @JvmStatic
        fun createRenderOutput(outputType: OutputType): RenderOutput {
            return when (outputType) {
                OutputType.IMAGE_READER -> ImageReaderOutput()
                OutputType.SURFACE_TEXTURE -> SurfaceTextureOutput()
            }
        }
    }

    protected fun onHandlerIfOpen(block: () -> Unit) {
        if (!isClosedOnHandler) {
            block()
        }
    }

    protected fun onFrameAvailable() {
        totalFrames.update { it + 1 }
    }

    suspend fun await(imageCount: Int, timeoutInMs: Long): Boolean {
        val target = consumedFrames.get() + imageCount
        val received =
            withTimeoutOrNull(timeoutInMs) {
                totalFrames.first { it >= target }
                true
            } ?: false
        if (received) {
            consumedFrames.set(target)
        }
        return received
    }

    fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }
        if (Looper.myLooper() == handler.looper) {
            releaseOnHandler()
            return
        }
        if (handler.post(::releaseOnHandler)) {
            handlerThread.join(RELEASE_TIMEOUT_MS)
        } else {
            handlerThread.join(RELEASE_TIMEOUT_MS)
            onRelease()
        }
    }

    private fun releaseOnHandler() {
        if (isClosedOnHandler) {
            return
        }
        isClosedOnHandler = true
        try {
            onRelease()
        } finally {
            // Purge all queued callbacks immediately so closed resources are never touched again.
            handlerThread.quit()
        }
    }

    protected abstract fun onRelease()
}

enum class OutputType {
    IMAGE_READER,
    SURFACE_TEXTURE,
}

private class SurfaceTextureOutput : RenderOutput() {
    override val surface: Surface by ::outputSurface

    override val dataSpace: Int
        @RequiresApi(33) get() = outputSurfaceTexture.dataSpace

    private lateinit var outputSurfaceTexture: SurfaceTexture

    init {
        runBlocking(handler.asCoroutineDispatcher()) {
            outputSurfaceTexture =
                SurfaceTexture(GLUtil.getTexIdFromGLContext()).apply {
                    setDefaultBufferSize(WIDTH, HEIGHT)
                    // Register eagerly so BufferQueue frames queued before await() are drained.
                    setOnFrameAvailableListener(
                        { surfaceTexture ->
                            onHandlerIfOpen {
                                surfaceTexture.updateTexImage()
                                onFrameAvailable()
                            }
                        },
                        handler,
                    )
                }
        }
    }

    private val outputSurface = Surface(outputSurfaceTexture)

    override fun onRelease() {
        outputSurfaceTexture.setOnFrameAvailableListener(null)
        outputSurfaceTexture.release()
        outputSurface.release()
    }
}

private class ImageReaderOutput : RenderOutput() {
    private val imageReader =
        ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.PRIVATE, 2).apply {
            // Register eagerly so frames arriving before await() do not stall BufferQueue, and
            // close the Image synchronously so maxImages (2) is never held across async channels.
            setOnImageAvailableListener(
                { reader ->
                    onHandlerIfOpen {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            image.close()
                            onFrameAvailable()
                        }
                    }
                },
                handler,
            )
        }

    override val surface: Surface
        get() = imageReader.surface

    override val dataSpace: Int
        @RequiresApi(33) get() = imageReader.dataSpace

    override fun onRelease() {
        imageReader.setOnImageAvailableListener(null, null)
        imageReader.close()
    }
}
