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

package androidx.camera.testing.impl

import android.graphics.Color
import android.opengl.EGL14
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.Executors
import java.util.concurrent.Phaser
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented headless EGL tests for [SurfaceTextureProvider]. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceTextureProviderDeviceTest {

    @Before
    fun setUp() {
        assumeFalse(
            "Auto-draining EGL path is bypassed on API 24 emulators (b/563154933)",
            AndroidUtil.isEmulator(24),
        )
    }

    @After
    fun tearDown() {
        val drainThreads = Thread.getAllStackTraces().keys.filter { it.name == DRAIN_THREAD_NAME }
        drainThreads.forEach { it.join(TIMEOUT_MS) }
        assertThat(drainThreads.filter { it.isAlive }).isEmpty()
    }

    @Test
    fun closingHolder_doesNotBreakOtherHolder() {
        val holders = mutableListOf<TestHolder>()
        try {
            val holderA = createHolder().also { holders.add(it) }
            val holderB = createHolder().also { holders.add(it) }

            // Act: close A and wait for its auto-drain thread, which terminates EGL, to exit.
            holderA.close()
            holderA.drainThread.join(TIMEOUT_MS)
            assertThat(holderA.drainThread.isAlive).isFalse()

            // Assert: B still drains frames.
            assertThat(holderB.drawFrameAndAwait()).isTrue()
        } finally {
            holders.forEach { it.close() }
        }
    }

    @Test
    fun defaultDisplayRemainsInitialized_afterAllHoldersClosed() {
        val holders = mutableListOf<TestHolder>()
        try {
            repeat(2) { holders.add(createHolder()) }
            for (holder in holders) {
                holder.close()
                holder.drainThread.join(TIMEOUT_MS)
                assertThat(holder.drainThread.isAlive).isFalse()
            }
        } finally {
            holders.forEach { it.close() }
        }

        // eglQueryString fails with EGL_NOT_INITIALIZED if the display was terminated.
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        assertWithMessage("Default EGLDisplay should remain initialized")
            .that(EGL14.eglQueryString(display, EGL14.EGL_VENDOR))
            .isNotEmpty()
    }

    @Test
    fun concurrentCreateCloseAndExternalEglTerminate_succeeds() {
        // Create the process-lifetime display pin before racing external eglTerminate calls.
        createHolder().close()

        val workerCount = 4
        val rounds = 20
        val phaser = Phaser(workerCount)
        val firstFailure = AtomicReference<Throwable?>()
        val executor = Executors.newFixedThreadPool(workerCount)
        try {
            val futures =
                (0 until workerCount).map { workerIndex ->
                    executor.submit {
                        for (round in 0 until rounds) {
                            if (firstFailure.get() != null) {
                                return@submit
                            }
                            try {
                                val phase = phaser.arrive()
                                if (
                                    phaser.awaitAdvanceInterruptibly(
                                        phase,
                                        TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS,
                                    ) < 0
                                ) {
                                    // Another worker failed and terminated the phaser.
                                    return@submit
                                }
                                if (workerIndex == 0 && round % 2 == 0) {
                                    initializeAndTerminateDefaultDisplay()
                                } else {
                                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
                                            160,
                                            120,
                                            null,
                                        )
                                        .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                                        .close()
                                }
                            } catch (t: Throwable) {
                                firstFailure.compareAndSet(null, t)
                                // Release the other workers waiting for this one.
                                phaser.forceTermination()
                                return@submit
                            }
                        }
                    }
                }
            futures.forEach { it.get(60, TimeUnit.SECONDS) }
            assertWithMessage("Unexpected auto-drain EGL failure: ${firstFailure.get()}")
                .that(firstFailure.get())
                .isNull()
        } finally {
            phaser.forceTermination()
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun initializeAndTerminateDefaultDisplay() {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val versions = IntArray(2)
        if (EGL14.eglInitialize(display, versions, 0, versions, 1)) {
            EGL14.eglTerminate(display)
            EGL14.eglReleaseThread()
        }
    }

    /**
     * Creates an auto-draining holder and captures its auto-drain thread, on which the frame
     * available listener is invoked, by drawing one frame.
     */
    private fun createHolder(): TestHolder {
        val drainThread = AtomicReference<Thread>()
        val frames = Semaphore(0)
        val holder =
            SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(320, 240) {
                    drainThread.compareAndSet(null, Thread.currentThread())
                    frames.release()
                }
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        val testHolder = TestHolder(holder, frames)
        try {
            assertThat(testHolder.drawFrameAndAwait()).isTrue()
            testHolder.drainThread = drainThread.get()
        } catch (t: Throwable) {
            testHolder.close()
            throw t
        }
        return testHolder
    }

    private class TestHolder(
        private val holder: SurfaceTextureProvider.SurfaceTextureHolder,
        private val frames: Semaphore,
    ) {
        lateinit var drainThread: Thread

        fun drawFrameAndAwait(): Boolean {
            val surface = Surface(holder.surfaceTexture)
            try {
                val canvas = surface.lockCanvas(null)
                canvas.drawColor(Color.GREEN)
                surface.unlockCanvasAndPost(canvas)
            } finally {
                surface.release()
            }
            return frames.tryAcquire(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }

        fun close() = holder.close()
    }

    private companion object {
        const val DRAIN_THREAD_NAME = "CameraX-AutoDrainThread"
        const val TIMEOUT_MS = 10_000L
    }
}
