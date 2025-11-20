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

import androidx.xr.runtime.TrackingState
import com.google.ar.core.TrackingState as ARCore1xTrackingState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TrackingStateExtensionsTest {

    @Test
    fun fromArCoreTrackingState_paused_returnsPaused() {
        assertThat(TrackingState.fromArCoreTrackingState(ARCore1xTrackingState.PAUSED))
            .isEqualTo(TrackingState.PAUSED)
    }

    @Test
    fun fromArCoreTrackingState_tracking_returnsTracking() {
        assertThat(TrackingState.fromArCoreTrackingState(ARCore1xTrackingState.TRACKING))
            .isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun fromArCoreTrackingState_stopped_returnsStopped() {
        assertThat(TrackingState.fromArCoreTrackingState(ARCore1xTrackingState.STOPPED))
            .isEqualTo(TrackingState.STOPPED)
    }
}
