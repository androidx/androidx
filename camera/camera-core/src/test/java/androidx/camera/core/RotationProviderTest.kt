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

import android.os.Looper
import android.view.Surface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
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
}
