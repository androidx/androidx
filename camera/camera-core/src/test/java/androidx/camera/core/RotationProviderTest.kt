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

package androidx.camera.core

import android.content.Context
import android.os.HandlerThread
import android.os.Looper
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument

private const val INVALID_ROTATION = -1

/** Unit tests for [RotationProvider]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class RotationProviderTest {
    private val rotationProvider =
        RotationProvider(
            InstrumentationRegistry.getInstrumentation().context,
            ignoreCanDetectForTest = true,
        )

    @Test
    fun addAndRemoveListener_noCallback() {
        var rotationNoChange = INVALID_ROTATION
        var rotationChanged = INVALID_ROTATION
        val listenerKept = RotationProvider.Listener { rotationChanged = it }
        val listenerRemoved = RotationProvider.Listener { rotationNoChange = it }
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor(), listenerKept)
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor(), listenerRemoved)

        // Act.
        rotationProvider.removeListener(listenerRemoved)
        rotationProvider.updateOrientationForTesting(0)

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert.
        assertThat(rotationNoChange).isEqualTo(INVALID_ROTATION)
        assertThat(rotationChanged).isEqualTo(Surface.ROTATION_0)
    }

    @Test
    fun addListener_receivesCallback() {
        // Arrange.
        var rotation = INVALID_ROTATION
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor()) { rotation = it }
        // Act.
        rotationProvider.updateOrientationForTesting(270)

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert.
        assertThat(rotation).isEqualTo(Surface.ROTATION_90)
    }

    @Test
    fun cannotDetectOrientation_addingReturnsFalse() {
        val rotationProvider =
            RotationProvider(InstrumentationRegistry.getInstrumentation().context, false)
        assertThat(rotationProvider.addListener(CameraXExecutors.mainThreadExecutor()) {}).isFalse()
    }

    @Test
    fun newListener_receivesCachedRotation() {
        // Arrange: set an initial rotation.
        rotationProvider.updateOrientationForTesting(90)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Act: add a new listener.
        var rotation = INVALID_ROTATION
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor()) { rotation = it }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert: the new listener receives the cached rotation value.
        assertThat(rotation).isEqualTo(Surface.ROTATION_270)
    }

    @Test
    fun assertBasicOrientationToSurfaceRotation() {
        // Arrange.
        var rotation = INVALID_ROTATION
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor()) { rotation = it }

        rotationProvider.updateOrientationForTesting(0)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)

        rotationProvider.updateOrientationForTesting(90)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_270)

        rotationProvider.updateOrientationForTesting(180)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_180)

        rotationProvider.updateOrientationForTesting(270)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_90)
    }

    @Test
    fun orientationChangesInHysteresisZone_rotationIsStable() {
        // Arrange.
        var rotation = INVALID_ROTATION
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor()) { rotation = it }

        // Act: set rotation to ROTATION_0
        rotationProvider.updateOrientationForTesting(0)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)

        // Act: orientation changes within the hysteresis zone around 45 degrees.
        // e.g. 45 +/- 5 degrees.
        rotationProvider.updateOrientationForTesting(42)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)
        rotationProvider.updateOrientationForTesting(48)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)

        // Act: orientation changes within the hysteresis zone around 315 degrees.
        // e.g. 315 +/- 5 degrees.
        rotationProvider.updateOrientationForTesting(312)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)
        rotationProvider.updateOrientationForTesting(318)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)
    }

    @Test
    fun orientationChangesOutOfHysteresisZone_rotationChanges() {
        // Arrange.
        var rotation = INVALID_ROTATION
        rotationProvider.addListener(CameraXExecutors.mainThreadExecutor()) { rotation = it }

        // Act: set rotation to ROTATION_0
        rotationProvider.updateOrientationForTesting(0)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)

        // Act: orientation changes from 0->90 degrees.
        // Change happens when it's outside of 45 +/- 5 degrees.
        rotationProvider.updateOrientationForTesting(51)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_270)

        // Act: orientation changes from 90->0 degrees.
        // Change happens when it's outside of 45 +/- 5 degrees.
        rotationProvider.updateOrientationForTesting(39)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)
    }

    @Test
    fun lazyInitialization() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val provider = RotationProvider(context, ignoreCanDetectForTest = true)

        // Assert that the internal orientationListener field and backgroundThread are null
        // initially
        assertThat(getOrientationListener(provider)).isNull()
        assertThat(getBackgroundThread(provider)).isNull()

        // Call addListener
        provider.addListener(CameraXExecutors.mainThreadExecutor()) {}

        // Assert background thread is created immediately
        val thread = getBackgroundThread(provider)
        assertThat(thread).isNotNull()

        // Idle the background thread looper to process the initialization task
        Shadows.shadowOf(thread!!.looper).idle()

        // Assert orientationListener is now non-null
        assertThat(getOrientationListener(provider)).isNotNull()
    }

    @Test
    fun nullableCleanup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val provider = RotationProvider(context, ignoreCanDetectForTest = true)

        val listener = RotationProvider.Listener {}
        provider.addListener(CameraXExecutors.mainThreadExecutor(), listener)

        val thread = getBackgroundThread(provider)
        assertThat(thread).isNotNull()
        Shadows.shadowOf(thread!!.looper).idle()

        assertThat(getOrientationListener(provider)).isNotNull()

        // Remove listener
        provider.removeListener(listener)

        // Idle the background looper
        runShutdownTask(thread.looper)

        // Assert that orientationListener and backgroundThread are now null/inactive
        assertThat(getOrientationListener(provider)).isNull()
        assertThat(getBackgroundThread(provider)).isNull()
    }

    @Test
    fun shutdown_cleansUpActiveThread() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val provider = RotationProvider(context, ignoreCanDetectForTest = true)

        val listener = RotationProvider.Listener {}
        provider.addListener(CameraXExecutors.mainThreadExecutor(), listener)

        val thread = getBackgroundThread(provider)
        assertThat(thread).isNotNull()
        Shadows.shadowOf(thread!!.looper).idle()

        assertThat(getOrientationListener(provider)).isNotNull()

        // Call shutdown to trigger full cleanup
        provider.shutdown()
        runShutdownTask(thread.looper)

        // Assert that orientationListener and backgroundThread are now nullified
        assertThat(getOrientationListener(provider)).isNull()
        assertThat(getBackgroundThread(provider)).isNull()
    }

    @Test
    @Config(shadows = [ShadowOrientationEventListener::class])
    @Suppress("DEPRECATION")
    fun threadIsolation() {
        ShadowOrientationEventListener.reset()

        val context = InstrumentationRegistry.getInstrumentation().context
        val provider = RotationProvider(context, ignoreCanDetectForTest = true)
        val listener = RotationProvider.Listener {}

        provider.addListener(CameraXExecutors.mainThreadExecutor(), listener)

        val thread = getBackgroundThread(provider)
        assertThat(thread).isNotNull()
        Shadows.shadowOf(thread!!.looper).idle()

        // Assert that constructor and enable() are run on the background thread
        assertThat(ShadowOrientationEventListener.constructorThreadId).isEqualTo(thread.id)
        assertThat(ShadowOrientationEventListener.enableThreadId).isEqualTo(thread.id)
        assertThat(ShadowOrientationEventListener.constructorThreadId)
            .isNotEqualTo(Looper.getMainLooper().thread.id)
        assertThat(ShadowOrientationEventListener.enableThreadId)
            .isNotEqualTo(Looper.getMainLooper().thread.id)

        // Remove listener to trigger cleanup
        provider.removeListener(listener)
        runShutdownTask(thread.looper)

        // Assert that disable() is run on the background thread
        assertThat(ShadowOrientationEventListener.disableThreadId).isEqualTo(thread.id)
        assertThat(ShadowOrientationEventListener.disableThreadId)
            .isNotEqualTo(Looper.getMainLooper().thread.id)

        // Assert that orientationListener and backgroundThread are now null/inactive
        assertThat(getOrientationListener(provider)).isNull()
        assertThat(getBackgroundThread(provider)).isNull()
    }

    /**
     * Executes the shutdown/cleanup task on the background looper.
     *
     * On some API levels (like API 23 and 33) under Robolectric, running a task that calls
     * quitSafely() triggers post-execution synchronization loops in ShadowPausedLooper. Since the
     * looper is already in a quitting/quitted state when this synchronization happens,
     * ShadowPausedLooper throws an IllegalStateException. Different API levels and Robolectric
     * versions throw IllegalStateException with varying messages (e.g., "Looper is quitting",
     * "failed. Is handler thread dead?"). Catching and ignoring all IllegalStateException instances
     * here is safe and robust because runShutdownTask is only called to execute the final looper
     * shutdown task, and the looper/thread is expected to be quitting or dead after execution.
     */
    private fun runShutdownTask(looper: Looper?) {
        // Under Robolectric, the background HandlerThread may finish executing and terminate
        // asynchronously after quitSafely() is called. Once the thread is dead, thread.looper
        // returns null. Returning early if looper is null is safe because the thread has
        // already shut down.
        if (looper == null) {
            return
        }
        try {
            Shadows.shadowOf(looper).runOneTask()
        } catch (_: IllegalStateException) {
            // Ignore IllegalStateException thrown by Robolectric when the looper is quitting or
            // the handler thread is dead.
        }
    }

    private fun getOrientationListener(provider: RotationProvider): OrientationEventListener? {
        val field = RotationProvider::class.java.getDeclaredField("orientationListener")
        field.isAccessible = true
        return field.get(provider) as? OrientationEventListener
    }

    private fun getBackgroundThread(provider: RotationProvider): HandlerThread? {
        val field = RotationProvider::class.java.getDeclaredField("backgroundThread")
        field.isAccessible = true
        return field.get(provider) as? HandlerThread
    }

    @Implements(OrientationEventListener::class)
    @Suppress("DEPRECATION")
    class ShadowOrientationEventListener {
        companion object {
            var constructorThreadId: Long = -1
            var enableThreadId: Long = -1
            var disableThreadId: Long = -1

            fun reset() {
                constructorThreadId = -1
                enableThreadId = -1
                disableThreadId = -1
            }
        }

        @Implementation
        fun __constructor__(context: Context) {
            constructorThreadId = Thread.currentThread().id
        }

        @Implementation
        fun __constructor__(context: Context, rate: Int) {
            constructorThreadId = Thread.currentThread().id
        }

        @Implementation
        fun enable() {
            enableThreadId = Thread.currentThread().id
        }

        @Implementation
        fun disable() {
            disableThreadId = Thread.currentThread().id
        }

        @Implementation
        fun canDetectOrientation(): Boolean {
            return true
        }
    }
}
