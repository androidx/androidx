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

package androidx.camera.view

import android.os.Build
import android.os.Looper.getMainLooper
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [RotationProvider].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RotationProviderTest {

    private val rotationProvider = RotationProvider(getInstrumentation().context)

    @Before
    fun setUp() {
        rotationProvider.mIgnoreCanDetectForTest = true
    }

    @After
    fun tearDown() {
        rotationProvider.mIgnoreCanDetectForTest = false
    }

    @Test
    fun addListener_receivesCallback() {
        // Arrange.
        var rotation = -1
        rotationProvider.setListener {
            rotation = it
        }
        // Act.
        rotationProvider.mOrientationListener.onOrientationChanged(0)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(rotation).isEqualTo(Surface.ROTATION_0)
    }

    @Test
    fun cannotDetectOrientation_addingReturnsFalse() {
        rotationProvider.mIgnoreCanDetectForTest = false
        assertThat(rotationProvider.setListener {}).isFalse()
    }

    @Test
    fun assertBasicOrientationToSurfaceRotation() {
        assertThat(RotationProvider.orientationToSurfaceRotation(0))
            .isEqualTo(Surface.ROTATION_0)
        assertThat(RotationProvider.orientationToSurfaceRotation(90))
            .isEqualTo(Surface.ROTATION_270)
        assertThat(RotationProvider.orientationToSurfaceRotation(180))
            .isEqualTo(Surface.ROTATION_180)
        assertThat(RotationProvider.orientationToSurfaceRotation(270))
            .isEqualTo(Surface.ROTATION_90)
    }
}