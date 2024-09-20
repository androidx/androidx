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

package androidx.camera.integration.core

import android.os.Build
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.testing.rules.FakeCameraTestRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraXInitTest {
    @get:Rule val fakeCameraRule = FakeCameraTestRule(ApplicationProvider.getApplicationContext())

    @Test
    fun hasBackCamera_whenInitSuccessfully() = runBlocking {
        assertThat(fakeCameraRule.cameraProvider.hasCamera(DEFAULT_BACK_CAMERA)).isTrue()
    }

    @Test
    fun hasFrontCamera_whenInitSuccessfully() = runBlocking {
        assertThat(fakeCameraRule.cameraProvider.hasCamera(DEFAULT_FRONT_CAMERA)).isTrue()
    }
}
