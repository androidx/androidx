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
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.GLUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private const val WIDTH = 640
private const val HEIGHT = 480

abstract class RenderOutput<T> {
    abstract val surface: Surface
    protected abstract val imageFlow: Flow<T>
    abstract val dataSpace: Int
    private val handlerThread: HandlerThread =
        HandlerThread("RenderOutput Thread").apply { start() }
    protected val handler: Handler = Handler(handlerThread.looper)

    companion object {
        @JvmStatic
        fun createRenderOutput(outputType: OutputType): RenderOutput<*> {
            return when (outputType) {
                OutputType.IMAGE_READER -> ImageReaderOutput()
                OutputType.SURFACE_TEXTURE -> SurfaceTextureOutput()
            }
        }
    }

    suspend fun await(imageCount: Int, timeoutInMs: Long): Boolean {
        val scope = CoroutineScope(handler.asCoroutineDispatcher())
        val imageCollectJob =
            scope.launch {
                imageFlow.collectIndexed { index, image ->
                    releaseImage(image)
                    if (index >= imageCount) {
                        scope.cancel()
                    }
                }
            }

        return withTimeoutOrNull(timeoutInMs) {
            imageCollectJob.join()
            true
        } ?: false
    }

    open fun releaseImage(image: T) {}

    open fun release() {
        handlerThread.quitSafely()
    }
}

enum class OutputType {
    IMAGE_READER,
    SURFACE_TEXTURE,
}

private class SurfaceTextureOutput : RenderOutput<Unit>() {
    override val surface: Surface by ::outputSurface

    override val imageFlow = callbackFlow {
        outputSurfaceTexture.setOnFrameAvailableListener(
            {
                it.updateTexImage()
                trySend(Unit)
            },
            handler,
        )
        awaitClose { outputSurfaceTexture.setOnFrameAvailableListener(null) }
    }
    override val dataSpace: Int
        @RequiresApi(33) get() = outputSurfaceTexture.dataSpace

    override fun release() {
        super.release()
        outputSurfaceTexture.release()
        outputSurface.release()
    }

    private var outputSurfaceTexture: SurfaceTexture

    init {
        runBlocking(handler.asCoroutineDispatcher()) {
            outputSurfaceTexture =
                SurfaceTexture(GLUtil.getTexIdFromGLContext()).apply {
                    setDefaultBufferSize(WIDTH, HEIGHT)
                }
        }
    }

    private val outputSurface = Surface(outputSurfaceTexture)
}

private class ImageReaderOutput : RenderOutput<Image>() {
    override val surface: Surface
        get() = imageReader.surface

    override val imageFlow = callbackFlow {
        val listener =
            ImageReader.OnImageAvailableListener {
                val image = it.acquireLatestImage()
                if (image != null) {
                    trySend(image)
                }
            }
        imageReader.setOnImageAvailableListener(listener, handler)
        awaitClose { imageReader.setOnImageAvailableListener({}, Handler(Looper.getMainLooper())) }
    }

    override val dataSpace: Int
        @RequiresApi(33) get() = imageReader.dataSpace

    override fun releaseImage(image: Image) {
        image.close()
    }

    override fun release() {
        super.release()
        imageReader.close()
    }

    val imageReader = ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.PRIVATE, 2)
}
