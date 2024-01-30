/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.media.ImageReaderWrapper
import androidx.camera.camera2.pipe.media.ImageWrapper
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * Utility class for simulating [FakeImage] and testing code that uses an [ImageReaderWrapper].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class FakeImageReader private constructor(
    private val format: StreamFormat,
    override val capacity: Int,
    override val surface: Surface,
    private val outputs: Map<OutputId, Size>
) : ImageReaderWrapper {
    private val closed = atomic(false)
    private val onImageListener = atomic<ImageReaderWrapper.OnImageListener?>(null)

    val isClosed: Boolean
        get() = closed.value

    fun simulateImage(outputId: OutputId, timestamp: Long) {
        val size =
            checkNotNull(outputs[outputId]) {
                "Unexpected $outputId! Available outputs are $outputs"
            }
        val image = FakeImage(size.width, size.height, format.value, timestamp)
        simulateImage(outputId, image)
    }

    fun simulateImage(outputId: OutputId, image: ImageWrapper) {
        val size =
            checkNotNull(outputs[outputId]) {
                "Unexpected $outputId! Available outputs are $outputs"
            }
        check(image.width == size.width)
        check(image.height == size.height)
        onImageListener.value?.onImage(outputId, image)
    }

    override fun setOnImageListener(onImageListener: ImageReaderWrapper.OnImageListener) {
        this.onImageListener.value = onImageListener
    }

    override fun flush() {
        // NoOp
    }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        // Fake objects cannot be unwrapped.
        return null
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            surface.release()
        }
    }

    companion object {
        private val fakeSurfaceTextureNames = atomic(0)

        /** Create a [FakeImageReader] that can simulate images. */
        fun create(
            format: StreamFormat,
            outputId: OutputId,
            size: Size,
            capacity: Int
        ): FakeImageReader = create(format, mapOf(outputId to size), capacity)

        /** Create a [FakeImageReader] that can simulate different sized images. */
        fun create(
            format: StreamFormat,
            outputs: Map<OutputId, Size>,
            capacity: Int
        ): FakeImageReader {

            // Find smallest by areas to pick the default surface size. This matches the behavior of
            // MultiResolutionImageReader.
            val smallestOutput = outputs.values.minBy { it.width * it.height }

            val surface = Surface(
                SurfaceTexture(fakeSurfaceTextureNames.getAndIncrement()).also {
                    it.setDefaultBufferSize(smallestOutput.width, smallestOutput.height)
                }
            )
            return FakeImageReader(format, capacity, surface, outputs)
        }
    }
}
