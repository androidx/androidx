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

package androidx.xr.arcore.playservices

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.TrackingState
import com.google.ar.core.Anchor as ARCore1xAnchor
import com.google.ar.core.Pose as ARCore1xPose
import com.google.ar.core.TrackingState as ARCore1xTrackingState
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreAnchorTest {

    private lateinit var mockAnchor: ARCore1xAnchor
    private lateinit var underTest: ArCoreAnchor

    @Before
    fun setUp() {
        mockAnchor = mock<ARCore1xAnchor>()
        underTest = ArCoreAnchor(mockAnchor)
    }

    @Test
    fun pose_callsArCoreAnchorPose() {
        val pose =
            ARCore1xPose(floatArrayOf(1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))
        whenever(mockAnchor.pose).thenReturn(pose)

        assertThat(underTest.pose).isEqualTo(pose.toRuntimePose())
        verify(mockAnchor).pose
    }

    @Test
    fun trackingState_callsArCoreAnchorTrackingState() {
        whenever(mockAnchor.trackingState).thenReturn(ARCore1xTrackingState.PAUSED)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
        verify(mockAnchor).trackingState
    }

    @Test
    fun persist_throwsNotImplementedError() {
        assertThrows(NotImplementedError::class.java) { underTest.persist() }
    }

    @Test
    fun detach_callsArCoreAnchorDetach() {
        underTest.detach()

        verify(mockAnchor).detach()
    }
}
