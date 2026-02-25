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

package androidx.xr.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigTest {

    @Test
    fun equals_sameInstance_returnsTrue() {
        val config =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )

        assertThat(config).isEqualTo(config)
    }

    @Test
    fun equals_sameConfig_returnsTrue() {
        val config1 =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )
        val config2 =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )

        assertThat(config1).isEqualTo(config2)
    }

    @Test
    fun equals_differentPlaneTracking_returnsFalse() {
        val config1 = Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
        val config2 = Config(planeTracking = PlaneTrackingMode.DISABLED)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentHandTracking_returnsFalse() {
        val config1 = Config(handTracking = HandTrackingMode.BOTH)
        val config2 = Config(handTracking = HandTrackingMode.DISABLED)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentFaceTracking_returnsFalse() {
        val config1 = Config(faceTracking = FaceTrackingMode.BLEND_SHAPES)
        val config2 = Config(faceTracking = FaceTrackingMode.DISABLED)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentDepthEstimation_returnsFalse() {
        val config1 = Config(depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW)
        val config2 = Config(depthEstimation = DepthEstimationMode.DISABLED)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentAnchorPersistence_returnsFalse() {
        val config1 = Config(anchorPersistence = AnchorPersistenceMode.LOCAL)

        val config2 = Config(anchorPersistence = AnchorPersistenceMode.DISABLED)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun hashCode_sameConfig_returnsSameHashCode() {
        val config1 =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )
        val config2 =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )

        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun hashCode_differentConfig_returnsDifferentHashCode() {
        val config1 =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )
        val config2 =
            Config(planeTracking = PlaneTrackingMode.DISABLED, handTracking = HandTrackingMode.BOTH)

        assertThat(config1.hashCode()).isNotEqualTo(config2.hashCode())
    }

    @Test
    fun copy_createsNewInstanceWithSameValues() {
        val config =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )

        val copy = config.copy()

        assertThat(copy).isEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }

    @Test
    fun copy_withDifferentValues_createsNewInstanceWithSameValues() {
        val config =
            Config(
                planeTracking = PlaneTrackingMode.DISABLED,
                handTracking = HandTrackingMode.DISABLED,
            )
        val copy =
            config.copy(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )

        assertThat(copy).isNotEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }

    @Test
    fun copy_withDefaultValues_createsNewInstanceWithSameValues() {
        val config = Config()
        val copy = config.copy()

        assertThat(copy).isEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }
}
