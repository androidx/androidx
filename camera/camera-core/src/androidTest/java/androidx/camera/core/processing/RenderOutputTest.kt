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

package androidx.camera.core.processing

import android.graphics.Color
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Device tests for [RenderOutput]. */
@RunWith(AndroidJUnit4::class)
@MediumTest
class RenderOutputTest {

    private var renderOutput: RenderOutput? = null

    @After
    fun tearDown() {
        renderOutput?.release()
    }

    @Test
    fun imageReaderOutput_preAwaitFrameBurst_doesNotDeadlockOrExceedMaxImages() = runBlocking {
        val output =
            RenderOutput.createRenderOutput(OutputType.IMAGE_READER).also {
                renderOutput = it
            }
        // Post more frames than ImageReader maxImages (2) before await() is called.
        assertThat(postFramesWithTimeout(output, frameCount = 6)).isTrue()

        assertThat(output.await(imageCount = 1, timeoutInMs = 5_000L)).isTrue()
    }

    @Test
    fun imageReaderOutput_concurrentFrameBurst_doesNotExceedMaxImages() = runBlocking {
        val output =
            RenderOutput.createRenderOutput(OutputType.IMAGE_READER).also {
                renderOutput = it
            }
        val keepProducing = AtomicBoolean(true)
        val producer =
            launch(Dispatchers.Default) {
                while (keepProducing.get()) {
                    try {
                        postFrame(output.surface)
                    } catch (e: IllegalStateException) {
                        if (keepProducing.get()) {
                            throw e
                        }
                    }
                }
            }

        try {
            assertThat(output.await(imageCount = 20, timeoutInMs = 5_000L)).isTrue()
        } finally {
            keepProducing.set(false)
            if (withTimeoutOrNull(2_000L) { producer.join() } == null) {
                output.release()
                withTimeoutOrNull(2_000L) { producer.join() }
            }
        }
    }

    @Test
    fun imageReaderOutput_awaitTimeout_returnsFalseAndReleaseIsIdempotent() = runBlocking {
        val output =
            RenderOutput.createRenderOutput(OutputType.IMAGE_READER).also {
                renderOutput = it
            }

        assertThat(output.await(imageCount = 1, timeoutInMs = 100L)).isFalse()
        output.release()
        output.release()
    }

    @Test
    fun surfaceTextureOutput_preAwaitFramesAndIdempotentRelease() = runBlocking {
        val output =
            RenderOutput.createRenderOutput(OutputType.SURFACE_TEXTURE).also {
                renderOutput = it
            }
        assertThat(postFramesWithTimeout(output, frameCount = 5)).isTrue()

        assertThat(output.await(imageCount = 1, timeoutInMs = 5_000L)).isTrue()
        output.release()
        output.release()
    }

    private suspend fun CoroutineScope.postFramesWithTimeout(
        output: RenderOutput,
        frameCount: Int,
        timeoutMs: Long = 5_000L,
    ): Boolean {
        val timedOut = AtomicBoolean(false)
        val poster =
            launch(Dispatchers.Default) {
                repeat(frameCount) {
                    if (timedOut.get()) {
                        return@launch
                    }
                    try {
                        postFrame(output.surface)
                    } catch (e: IllegalStateException) {
                        if (!timedOut.get()) {
                            throw e
                        }
                    }
                }
            }
        val completed =
            withTimeoutOrNull(timeoutMs) {
                poster.join()
                true
            } ?: false
        if (!completed) {
            timedOut.set(true)
            output.release()
            withTimeoutOrNull(2_000L) { poster.join() }
        }
        return completed
    }

    private fun postFrame(surface: Surface) {
        val canvas = surface.lockHardwareCanvas()
        try {
            canvas.drawColor(Color.BLACK)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }
}
