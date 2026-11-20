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

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Size
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.testing.fakes.FakeCamera
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** Unit tests for the EGL resource handling of [SurfaceTextureProvider]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    sdk = [Config.TARGET_SDK],
    shadows = [SurfaceTextureProviderTest.TrackingShadowEGL14::class],
)
class SurfaceTextureProviderTest {

    @Before
    fun setUp() {
        resetDefaultDisplayPinState()
        TrackingShadowEGL14.reset()
    }

    @After
    fun tearDown() {
        awaitAutoDrainThreadsExited()
        resetDefaultDisplayPinState()
        TrackingShadowEGL14.reset()
    }

    @Test
    fun close_keepsDefaultDisplayPinnedAndReleasesPerInstanceEglResources() {
        createHolder().close()
        awaitAutoDrainThreadsExited()

        assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.terminateCalls.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.destroyedContexts.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.destroyedSurfaces.get()).isEqualTo(1)
    }

    @Test
    fun closeTwice_terminatesPerInstanceDisplayRefOnce() {
        val holder = createHolder()

        holder.close()
        holder.close()
        awaitAutoDrainThreadsExited()

        assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.terminateCalls.get()).isEqualTo(1)
    }

    @Test
    fun eglInitializeFails_failsFutureWithoutTerminatingDisplay() {
        TrackingShadowEGL14.failInitialize = true

        val exception = assertThrows(ExecutionException::class.java) { createHolder() }

        assertThat(exception.cause).isInstanceOf(UnsupportedOperationException::class.java)
        awaitAutoDrainThreadsExited()
        assertThat(TrackingShadowEGL14.terminateCalls.get()).isEqualTo(0)
        assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(0)
    }

    @Test
    fun noEglConfig_failsFutureAndReleasesResources() {
        TrackingShadowEGL14.numConfigs = 0

        val exception = assertThrows(ExecutionException::class.java) { createHolder() }

        assertThat(exception.cause).isInstanceOf(UnsupportedOperationException::class.java)
        awaitAutoDrainThreadsExited()
        assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.terminateCalls.get()).isEqualTo(1)
    }

    @Test
    fun makeCurrentFails_failsFutureAndReleasesResources() {
        TrackingShadowEGL14.failMakeCurrent = true

        val exception = assertThrows(ExecutionException::class.java) { createHolder() }

        assertThat(exception.cause).isInstanceOf(UnsupportedOperationException::class.java)
        awaitAutoDrainThreadsExited()
        assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.destroyedContexts.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.destroyedSurfaces.get()).isEqualTo(1)
    }

    @Test
    fun cancelledFuture_releasesResources() {
        // Hold the EGL setup until the future is cancelled.
        val setupLatch = CountDownLatch(1)
        TrackingShadowEGL14.chooseConfigLatch = setupLatch
        val future = SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(16, 16, null)

        future.cancel(true)
        setupLatch.countDown()
        awaitAutoDrainThreadsExited()

        assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(1)
        assertThat(TrackingShadowEGL14.destroyedContexts.get()).isEqualTo(1)
    }

    @Test
    fun autoDrainingProvider_whenEglSetupFails_callsWillNotProvideSurfaceWithoutUncaughtError() {
        TrackingShadowEGL14.numConfigs = 0
        val uncaughtError = AtomicReference<Throwable?>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (thread.name == DRAIN_THREAD_NAME) {
                uncaughtError.compareAndSet(null, throwable)
            } else {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
        val surfaceRequest = SurfaceRequest(Size(16, 16), FakeCamera()) {}
        try {
            val provider = SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            provider.onSurfaceRequested(surfaceRequest)

            val exception =
                assertThrows(ExecutionException::class.java) {
                    surfaceRequest.deferrableSurface.surface.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
            assertThat(exception.cause)
                .isInstanceOf(DeferrableSurface.SurfaceUnavailableException::class.java)
            awaitAutoDrainThreadsExited()
            assertThat(uncaughtError.get()).isNull()
            assertThat(TrackingShadowEGL14.displayRefs.get()).isEqualTo(1)
        } finally {
            surfaceRequest.deferrableSurface.close()
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

    private fun createHolder(): SurfaceTextureProvider.SurfaceTextureHolder =
        SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(16, 16, null)
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private fun awaitAutoDrainThreadsExited() {
        val drainThreads = Thread.getAllStackTraces().keys.filter { it.name == DRAIN_THREAD_NAME }
        drainThreads.forEach { it.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) }
        assertThat(drainThreads.filter { it.isAlive }).isEmpty()
    }

    private fun resetDefaultDisplayPinState() {
        val lock =
            ReflectionHelpers.getStaticField<Any>(
                SurfaceTextureProvider::class.java,
                "DEFAULT_DISPLAY_PIN_LOCK",
            )
        synchronized(lock) {
            ReflectionHelpers.setStaticField(
                SurfaceTextureProvider::class.java,
                "sDefaultDisplayPinned",
                false,
            )
        }
    }

    /** A fake EGL that tracks [displayRefs] and destroyed EGL objects per isolated test. */
    @Implements(EGL14::class)
    class TrackingShadowEGL14 {
        companion object {
            val displayRefs = AtomicInteger(0)
            val terminateCalls = AtomicInteger(0)
            val destroyedContexts = AtomicInteger(0)
            val destroyedSurfaces = AtomicInteger(0)
            @Volatile var failInitialize = false
            @Volatile var numConfigs = 1
            @Volatile var failMakeCurrent = false
            @Volatile var chooseConfigLatch: CountDownLatch? = null

            fun reset() {
                displayRefs.set(0)
                terminateCalls.set(0)
                destroyedContexts.set(0)
                destroyedSurfaces.set(0)
                failInitialize = false
                numConfigs = 1
                failMakeCurrent = false
                chooseConfigLatch = null
            }

            @JvmStatic
            @Implementation
            fun eglGetDisplay(displayId: Int): EGLDisplay = createHandle(EGLDisplay::class.java)

            @JvmStatic
            @Implementation
            fun eglInitialize(
                dpy: EGLDisplay?,
                major: IntArray?,
                majorOffset: Int,
                minor: IntArray?,
                minorOffset: Int,
            ): Boolean {
                if (failInitialize) {
                    return false
                }
                displayRefs.incrementAndGet()
                return true
            }

            @JvmStatic
            @Implementation
            fun eglTerminate(dpy: EGLDisplay?): Boolean {
                terminateCalls.incrementAndGet()
                displayRefs.decrementAndGet()
                return true
            }

            @JvmStatic
            @Implementation
            fun eglChooseConfig(
                dpy: EGLDisplay?,
                attribList: IntArray?,
                attribListOffset: Int,
                configs: Array<EGLConfig?>?,
                configsOffset: Int,
                configSize: Int,
                numConfig: IntArray,
                numConfigOffset: Int,
            ): Boolean {
                chooseConfigLatch?.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                numConfig[numConfigOffset] = numConfigs
                if (numConfigs > 0 && configs != null) {
                    configs[configsOffset] = createHandle(EGLConfig::class.java)
                }
                return true
            }

            @JvmStatic
            @Implementation
            fun eglCreateContext(
                dpy: EGLDisplay?,
                config: EGLConfig?,
                shareContext: EGLContext?,
                attribList: IntArray?,
                offset: Int,
            ): EGLContext = createHandle(EGLContext::class.java)

            @JvmStatic
            @Implementation
            fun eglCreatePbufferSurface(
                dpy: EGLDisplay?,
                config: EGLConfig?,
                attribList: IntArray?,
                offset: Int,
            ): EGLSurface = createHandle(EGLSurface::class.java)

            @JvmStatic @Implementation fun eglGetError(): Int = EGL14.EGL_SUCCESS

            @JvmStatic
            @Implementation
            fun eglMakeCurrent(
                dpy: EGLDisplay?,
                draw: EGLSurface?,
                read: EGLSurface?,
                ctx: EGLContext?,
            ): Boolean = !(failMakeCurrent && ctx != EGL14.EGL_NO_CONTEXT)

            @JvmStatic
            @Implementation
            fun eglDestroyContext(dpy: EGLDisplay?, ctx: EGLContext?): Boolean {
                destroyedContexts.incrementAndGet()
                return true
            }

            @JvmStatic
            @Implementation
            fun eglDestroySurface(dpy: EGLDisplay?, surface: EGLSurface?): Boolean {
                destroyedSurfaces.incrementAndGet()
                return true
            }

            @JvmStatic @Implementation fun eglReleaseThread(): Boolean = true

            private fun <T> createHandle(handleClass: Class<T>): T =
                ReflectionHelpers.callConstructor(
                    handleClass,
                    ClassParameter.from(Long::class.javaPrimitiveType, 1L),
                )
        }
    }

    private companion object {
        const val DRAIN_THREAD_NAME = "CameraX-AutoDrainThread"
        const val TIMEOUT_SECONDS = 5L
    }
}
