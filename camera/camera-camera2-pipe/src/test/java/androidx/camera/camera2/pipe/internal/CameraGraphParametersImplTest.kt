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

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.graph.SessionLock
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [CameraGraphParametersImpl] */
@RunWith(RobolectricTestRunner::class)
class CameraGraphParametersImplTest {
    private lateinit var parameters: CameraGraphParametersImpl
    private var counter: Int = 0

    @Before
    fun setUp() {
        parameters = CameraGraphParametersImpl(SessionLock())
        parameters.setListener(this::increment)
        counter = 0
    }

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
    fun setNotDirty_listenerTriggered() {
        parameters[TEST_KEY] = 42

        assertEquals(counter, 1)
    }

    @Test
    fun setSameParameters_listenerNotTriggered() {
        parameters[TEST_KEY] = 42
        assertEquals(counter, 1)

        parameters[CAPTURE_REQUEST_KEY] = 2
        assertEquals(counter, 1)
    }

    @Test
    fun multipleSetNoFetchUpdatedParameters_listenerTriggeredOnce() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2

        assertEquals(counter, 1)
    }

    @Test
    fun multipleSetFetchUpdatedParameters_listenerTriggeredWhenNotDirty() {
        parameters[TEST_KEY] = 42
        parameters.fetchUpdatedParameters()
        parameters[CAPTURE_REQUEST_KEY] = 2

        assertEquals(counter, 2)
    }

    @Test
    fun remove_parameterRemoved() {
        parameters[TEST_KEY] = 42

        parameters.remove(TEST_KEY)

        assertNull(parameters[TEST_KEY])
    }

    @Test
    fun removeNotDirty_listenerTriggered() {
        parameters[TEST_KEY] = 42
        parameters.fetchUpdatedParameters()
        assertEquals(counter, 1)

        parameters.remove(TEST_KEY)

        assertEquals(counter, 2)
    }

    @Test
    fun removeEmptyParameter_listenerNotTriggered() {
        parameters.remove(TEST_KEY)

        assertEquals(counter, 0)
    }

    @Test
    fun multipleRemoveNoFetchUpdatedParameters_listenerTriggeredOnce() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2
        parameters.fetchUpdatedParameters()
        assertEquals(counter, 1)

        parameters.remove(TEST_KEY)
        parameters.remove(CAPTURE_REQUEST_KEY)

        assertEquals(counter, 2)
    }

    @Test
    fun clearNotDirty_listenerTriggered() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2
        parameters.fetchUpdatedParameters()
        assertEquals(counter, 1)

        parameters.clear()

        assertEquals(counter, 2)
    }

    @Test
    fun clearParameters_valuesEmpty() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2

        parameters.clear()

        assertNull(parameters[TEST_KEY])
        assertNull(parameters[CAPTURE_REQUEST_KEY])
    }

    private fun increment() {
        counter += 1
    }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
