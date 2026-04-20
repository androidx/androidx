/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore

import android.Manifest.permission.CAMERA
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestDepth
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class DepthMapTest {
    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var session: Session

    private val expectedWidth: Int = 2
    private val expectedHeight: Int = 2
    private val expectedRawDepthBuffer: FloatBuffer =
        FloatBuffer.allocate(expectedWidth * expectedHeight)
            .put(floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f))
    private val expectedRawConfidenceBuffer: ByteBuffer =
        ByteBuffer.allocate(expectedWidth * expectedHeight).put(byteArrayOf(1, 1, 1, 1))
    private val expectedSmoothDepthBuffer: FloatBuffer =
        FloatBuffer.allocate(expectedWidth * expectedHeight)
            .put(floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f))
    private val expectedSmoothConfidenceBuffer: ByteBuffer =
        ByteBuffer.allocate(expectedWidth * expectedHeight).put(byteArrayOf(1, 1, 1, 1))

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(CAMERA, SCENE_UNDERSTANDING_FINE)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_depthEstimationDisabled_doesNotUpdate() =
        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.leftDepth)
            advanceUntilIdle()

            val underTest = DepthMap.left(session)!!

            assertThat(underTest.state.value.width).isEqualTo(0)
            assertThat(underTest.state.value.height).isEqualTo(0)
            assertThat(underTest.state.value.rawDepthMap).isNull()
            assertThat(underTest.state.value.rawConfidenceMap).isNull()
            assertThat(underTest.state.value.smoothDepthMap).isNull()
            assertThat(underTest.state.value.smoothConfidenceMap).isNull()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_depthEstimationDisabled_doesNotUpdate() =
        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.rightDepth)
            advanceUntilIdle()

            val underTest = DepthMap.right(session)!!

            assertThat(underTest.state.value.width).isEqualTo(0)
            assertThat(underTest.state.value.height).isEqualTo(0)
            assertThat(underTest.state.value.rawDepthMap).isNull()
            assertThat(underTest.state.value.rawConfidenceMap).isNull()
            assertThat(underTest.state.value.smoothDepthMap).isNull()
            assertThat(underTest.state.value.smoothConfidenceMap).isNull()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_depthEstimationDisabled_doesNotUpdate() =
        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.monoDepth)
            advanceUntilIdle()

            val underTest = DepthMap.mono(session)!!

            assertThat(underTest.state.value.width).isEqualTo(0)
            assertThat(underTest.state.value.height).isEqualTo(0)
            assertThat(underTest.state.value.rawDepthMap).isNull()
            assertThat(underTest.state.value.rawConfidenceMap).isNull()
            assertThat(underTest.state.value.smoothDepthMap).isNull()
            assertThat(underTest.state.value.smoothConfidenceMap).isNull()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_rawOnly_updatesRawDepthMap() {
        session.configure(Config(depthEstimation = DepthEstimationMode.RAW_ONLY))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.leftDepth)
            advanceUntilIdle()

            val underTest = DepthMap.left(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthBuffer)
            assertThat(underTest.state.value.rawConfidenceMap)
                .isEqualTo(expectedRawConfidenceBuffer)
            assertThat(underTest.state.value.smoothDepthMap).isNull()
            assertThat(underTest.state.value.smoothConfidenceMap).isNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_smoothOnly_updatesSmoothDepthMap() {
        session.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_ONLY))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.leftDepth)
            advanceUntilIdle()

            val underTest = DepthMap.left(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isNull()
            assertThat(underTest.state.value.rawConfidenceMap).isNull()
            assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthBuffer)
            assertThat(underTest.state.value.smoothConfidenceMap)
                .isEqualTo(expectedSmoothConfidenceBuffer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_smoothAndRaw_updatesSmoothAndRawDepthMaps() {
        session.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.leftDepth)
            advanceUntilIdle()

            val underTest = DepthMap.left(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthBuffer)
            assertThat(underTest.state.value.rawConfidenceMap)
                .isEqualTo(expectedRawConfidenceBuffer)
            assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthBuffer)
            assertThat(underTest.state.value.smoothConfidenceMap)
                .isEqualTo(expectedSmoothConfidenceBuffer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_rawOnly_updatesRawDepthMap() {
        session.configure(Config(depthEstimation = DepthEstimationMode.RAW_ONLY))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.rightDepth)
            advanceUntilIdle()

            val underTest = DepthMap.right(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthBuffer)
            assertThat(underTest.state.value.rawConfidenceMap)
                .isEqualTo(expectedRawConfidenceBuffer)
            assertThat(underTest.state.value.smoothDepthMap).isNull()
            assertThat(underTest.state.value.smoothConfidenceMap).isNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_smoothOnly_updatesSmoothDepthMap() {
        session.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_ONLY))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.rightDepth)
            advanceUntilIdle()

            val underTest = DepthMap.right(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isNull()
            assertThat(underTest.state.value.rawConfidenceMap).isNull()
            assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthBuffer)
            assertThat(underTest.state.value.smoothConfidenceMap)
                .isEqualTo(expectedSmoothConfidenceBuffer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_smoothAndRaw_updatesSmoothAndRawDepthMaps() {
        session.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.rightDepth)
            advanceUntilIdle()

            val underTest = DepthMap.right(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthBuffer)
            assertThat(underTest.state.value.rawConfidenceMap)
                .isEqualTo(expectedRawConfidenceBuffer)
            assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthBuffer)
            assertThat(underTest.state.value.smoothConfidenceMap)
                .isEqualTo(expectedSmoothConfidenceBuffer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_rawOnly_updatesRawDepthMap() {
        session.configure(Config(depthEstimation = DepthEstimationMode.RAW_ONLY))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.monoDepth)
            advanceUntilIdle()

            val underTest = DepthMap.mono(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthBuffer)
            assertThat(underTest.state.value.rawConfidenceMap)
                .isEqualTo(expectedRawConfidenceBuffer)
            assertThat(underTest.state.value.smoothDepthMap).isNull()
            assertThat(underTest.state.value.smoothConfidenceMap).isNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_smoothOnly_updatesSmoothDepthMap() {
        session.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_ONLY))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.monoDepth)
            advanceUntilIdle()

            val underTest = DepthMap.mono(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isNull()
            assertThat(underTest.state.value.rawConfidenceMap).isNull()
            assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthBuffer)
            assertThat(underTest.state.value.smoothConfidenceMap)
                .isEqualTo(expectedSmoothConfidenceBuffer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_smoothAndRaw_updatesSmoothAndRawDepthMaps() {
        session.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW))

        runTest(testDispatcher) {
            applyExpectedValues(arCoreTestRule.monoDepth)
            advanceUntilIdle()

            val underTest = DepthMap.mono(session)!!

            assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
            assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
            assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthBuffer)
            assertThat(underTest.state.value.rawConfidenceMap)
                .isEqualTo(expectedRawConfidenceBuffer)
            assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthBuffer)
            assertThat(underTest.state.value.smoothConfidenceMap)
                .isEqualTo(expectedSmoothConfidenceBuffer)
        }
    }

    private fun applyExpectedValues(testDepth: TestDepth) =
        testDepth.apply {
            width = expectedWidth
            height = expectedHeight
            rawDepthMap = expectedRawDepthBuffer
            rawConfidenceMap = expectedRawConfidenceBuffer
            smoothDepthMap = expectedSmoothDepthBuffer
            smoothConfidenceMap = expectedSmoothConfidenceBuffer
        }
}
