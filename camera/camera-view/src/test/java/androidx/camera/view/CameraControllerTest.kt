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

import android.content.Context
import android.os.Build
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CameraController].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraControllerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    public fun setUp() {
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).build()
        CameraX.initialize(context, cameraXConfig).get()
    }

    @After
    public fun shutDown() {
        CameraX.shutdown().get()
    }

    @UiThreadTest
    @Test
    public fun sensorRotationChanges_useCaseTargetRotationUpdated() {
        // Arrange.
        val controller = LifecycleCameraController(context)

        // Act.
        controller.mRotationReceiver.onRotationChanged(Surface.ROTATION_180)

        // Assert.
        assertThat(controller.mImageAnalysis.targetRotation).isEqualTo(Surface.ROTATION_180)
        assertThat(controller.mImageCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
        // TODO(b/177276479): verify VideoCapture once it supports getTargetRotation().
    }

    @UiThreadTest
    @Test
    public fun setSelectorBeforeBound_selectorSet() {
        // Arrange.
        val controller = LifecycleCameraController(context)
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)

        // Act.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Assert.
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_FRONT)
    }
}
