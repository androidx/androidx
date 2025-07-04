/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.camera.camera2.pipe.internal

import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.GraphProcessorImpl
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import androidx.camera.camera2.pipe.graph.Listener3A
import androidx.camera.camera2.pipe.graph.SessionLock
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.graphParameters
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

/** Tests for [CameraGraphParametersImpl] */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
class CameraGraphParametersImplTest {
    private val testScope = TestScope()

    private var parameters =
        CameraGraphParametersImpl(SessionLock(), FakeGraphProcessor(), testScope)

    private val graphProcessor =
        GraphProcessorImpl(
            FakeThreads.fromTestScope(testScope),
            CameraGraphId.nextId(),
            FakeGraphConfigs.graphConfig,
            Listener3A(),
            arrayListOf(FakeRequestListener()),
        )
    private val surfaceMap = mapOf(StreamId(0) to Surface(SurfaceTexture(1)))
    private val csp1 = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }
    private val grp1 = GraphRequestProcessor.from(csp1)
    private val request1 = Request(listOf(StreamId(0)), listeners = listOf(FakeRequestListener()))

    @Test
    fun get_returnLatestValue() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2
        parameters[TEST_NULLABLE_KEY] = null

        assertEquals(parameters[TEST_KEY], 42)
        assertEquals(parameters[CAPTURE_REQUEST_KEY], 2)
        assertNull(parameters[TEST_NULLABLE_KEY])
    }

    @Test
    fun setAll_multipleEntriesSet() {
        parameters.setAll(
            mapOf(TEST_KEY to 42, CAPTURE_REQUEST_KEY to 2, TEST_NULLABLE_KEY to null)
        )

        assertEquals(parameters[TEST_KEY], 42)
        assertEquals(parameters[CAPTURE_REQUEST_KEY], 2)
        assertNull(parameters[TEST_NULLABLE_KEY])
    }

    @Test
    fun remove_parameterRemoved() {
        parameters[TEST_KEY] = 42

        parameters.remove(TEST_KEY)

        assertNull(parameters[TEST_KEY])
    }

    @Test
    fun removeAll_valuesEmpty() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2

        parameters.clear()

        assertNull(parameters[TEST_KEY])
        assertNull(parameters[CAPTURE_REQUEST_KEY])
    }

    @Test
    fun set_invokesUpdate() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1

            val parameters = CameraGraphParametersImpl(SessionLock(), graphProcessor, testScope)
            parameters[TEST_KEY] = 42
            advanceUntilIdle()

            // Check that the latest request with existing repeatingRequest has graphParameters
            assertEquals(csp1.events.size, 2)
            assertTrue(csp1.events[1].isRepeating)
            assertThat(csp1.events[1].graphParameters).containsExactly(TEST_KEY, 42)
        }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
