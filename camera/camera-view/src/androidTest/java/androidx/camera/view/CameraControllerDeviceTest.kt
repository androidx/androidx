/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.camera.testing.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [CameraController].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraControllerDeviceTest {

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    private val controller = LifecycleCameraController(ApplicationProvider.getApplicationContext())
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        controller.initializationFuture.get()
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            controller.shutDownForTests()
        }
    }

    @Test
    fun previewViewNotAttached_useCaseGroupIsBuilt() {
        instrumentation.runOnMainSync {
            assertThat(controller.createUseCaseGroup()).isNotNull()
        }
    }
}