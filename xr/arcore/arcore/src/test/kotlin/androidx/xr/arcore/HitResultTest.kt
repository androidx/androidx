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

package androidx.xr.arcore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.Pose
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HitResultTest {

    class TestTrackable : Trackable<Trackable.State> {
        override fun createAnchor(pose: Pose): AnchorCreateResult {
            throw NotImplementedError("Not implemented")
        }

        override val state: StateFlow<Trackable.State> =
            MutableStateFlow(
                object : Trackable.State {
                    override val trackingState = TrackingState.Stopped
                }
            )
    }

    @Test
    fun equals_sameObject_returnsTrue() {
        val underTest = HitResult(1.0f, Pose(), TestTrackable())

        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun equals_differentObjectsSameValues_returnsTrue() {
        val distance = 1.0f
        val pose = Pose()
        val trackable = TestTrackable()
        val underTest1 = HitResult(distance, pose, trackable)
        val underTest2 = HitResult(distance, pose, trackable)

        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun equals_differentObjectsDifferentValues_returnsFalse() {
        val underTest1 = HitResult(1.0f, Pose(), TestTrackable())
        val underTest2 = HitResult(2.0f, Pose(), TestTrackable())

        assertThat(underTest1.equals(underTest2)).isFalse()
    }

    @Test
    fun hashCode_differentObjectsSameValues_returnsSameHashCode() {
        val distance = 1.0f
        val pose = Pose()
        val trackable = TestTrackable()
        val underTest1 = HitResult(distance, pose, trackable)
        val underTest2 = HitResult(distance, pose, trackable)

        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentObjectsDifferentValues_returnsDifferentHashCodes() {
        val underTest1 = HitResult(1.0f, Pose(), TestTrackable())
        val underTest2 = HitResult(2.0f, Pose(), TestTrackable())

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }
}
