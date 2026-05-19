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

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigTest {

    @Test
    fun equals_sameInstance_returnsTrue() {
        val config =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setHandTracking(HandTrackingMode.BOTH)
                .build()

        assertThat(config).isEqualTo(config)
    }

    @Test
    fun equals_sameConfig_returnsTrue() {
        val config1 =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setHandTracking(HandTrackingMode.BOTH)
                .build()
        val config2 =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setHandTracking(HandTrackingMode.BOTH)
                .build()

        assertThat(config1).isEqualTo(config2)
    }

    @Test
    fun equals_differentPlaneTracking_returnsFalse() {
        val config1 =
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        val config2 = Config.Builder().setPlaneTracking(PlaneTrackingMode.DISABLED).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentHandTracking_returnsFalse() {
        val config1 = Config.Builder().setHandTracking(HandTrackingMode.BOTH).build()
        val config2 = Config.Builder().setHandTracking(HandTrackingMode.DISABLED).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentFaceTracking_returnsFalse() {
        val config1 = Config.Builder().setFaceTracking(FaceTrackingMode.BLEND_SHAPES).build()
        val config2 = Config.Builder().setFaceTracking(FaceTrackingMode.DISABLED).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentDepthEstimation_returnsFalse() {
        val config1 =
            Config.Builder().setDepthEstimation(DepthEstimationMode.SMOOTH_AND_RAW).build()
        val config2 = Config.Builder().setDepthEstimation(DepthEstimationMode.DISABLED).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentAnchorPersistence_returnsFalse() {
        val config1 = Config.Builder().setAnchorPersistence(AnchorPersistenceMode.LOCAL).build()

        val config2 = Config.Builder().setAnchorPersistence(AnchorPersistenceMode.DISABLED).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentImageTracking_returnsFalse() {
        val augmentedImageDatabase =
            AugmentedImageDatabase().apply {
                addAugmentedImageDatabaseEntry(
                    mode = AugmentedImageDatabaseEntryMode.DYNAMIC,
                    bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                )
            }
        val config1 = Config.Builder().setAugmentedImageDatabase(AugmentedImageDatabase()).build()
        val config2 = Config.Builder().setAugmentedImageDatabase(augmentedImageDatabase).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentQrCodeTracking_returnsFalse() {
        val config1 = Config(qrCodeTracking = QrCodeTrackingMode.DISABLED)
        val config2 = Config(qrCodeTracking = QrCodeTrackingMode.DYNAMIC)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun hashCode_sameConfig_returnsSameHashCode() {
        val config1 =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setHandTracking(HandTrackingMode.BOTH)
                .build()
        val config2 =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setHandTracking(HandTrackingMode.BOTH)
                .build()

        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun hashCode_differentConfig_returnsDifferentHashCode() {
        val config1 =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .setHandTracking(HandTrackingMode.BOTH)
                .build()
        val config2 =
            Config.Builder()
                .setPlaneTracking(PlaneTrackingMode.DISABLED)
                .setHandTracking(HandTrackingMode.BOTH)

        assertThat(config1.hashCode()).isNotEqualTo(config2.hashCode())
    }

    @Test
    fun builder_constructedWithConfig_createsSameConfig() {
        val config =
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                handTracking = HandTrackingMode.BOTH,
            )
        val copy = Config.Builder(config).build()

        assertThat(copy).isEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }
}
