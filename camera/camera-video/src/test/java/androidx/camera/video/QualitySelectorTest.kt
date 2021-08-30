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

package androidx.camera.video

import android.os.Build
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_1080P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_480P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_2160P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_720P
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.video.QualitySelector.FALLBACK_STRATEGY_HIGHER
import androidx.camera.video.QualitySelector.FALLBACK_STRATEGY_LOWER
import androidx.camera.video.QualitySelector.FALLBACK_STRATEGY_STRICTLY_HIGHER
import androidx.camera.video.QualitySelector.FALLBACK_STRATEGY_STRICTLY_LOWER
import androidx.camera.video.QualitySelector.QUALITY_FHD
import androidx.camera.video.QualitySelector.QUALITY_HD
import androidx.camera.video.QualitySelector.QUALITY_HIGHEST
import androidx.camera.video.QualitySelector.QUALITY_LOWEST
import androidx.camera.video.QualitySelector.QUALITY_NONE
import androidx.camera.video.QualitySelector.QUALITY_SD
import androidx.camera.video.QualitySelector.QUALITY_UHD
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val NOT_QUALITY_CONSTANT = -111
private const val NOT_FALLBACK_STRATEGY_CONSTANT = -1
private const val CAMERA_ID_0 = "0"
private const val CAMERA_ID_1 = "1"
private val CAMERA_0_PROFILE_HIGH = CamcorderProfileUtil.asHighQuality(PROFILE_2160P)
private val CAMERA_0_PROFILE_LOW = CamcorderProfileUtil.asLowQuality(PROFILE_720P)
private val CAMERA_1_PROFILE_HIGH = CamcorderProfileUtil.asHighQuality(PROFILE_1080P)
private val CAMERA_1_PROFILE_LOW = CamcorderProfileUtil.asLowQuality(PROFILE_480P)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QualitySelectorTest {

    private val cameraInfo0 = FakeCameraInfoInternal(CAMERA_ID_0).apply {
        camcorderProfileProvider = FakeCamcorderProfileProvider.Builder()
            .addProfile(
                CAMERA_0_PROFILE_HIGH,
                PROFILE_2160P,
                PROFILE_720P,
                CAMERA_0_PROFILE_LOW
            ).build()
    }
    private val cameraInfo1 = FakeCameraInfoInternal(CAMERA_ID_1).apply {
        camcorderProfileProvider = FakeCamcorderProfileProvider.Builder()
            .addProfile(
                CAMERA_1_PROFILE_HIGH,
                PROFILE_1080P,
                PROFILE_480P,
                CAMERA_1_PROFILE_LOW
            ).build()
    }

    @Test
    fun getSortedQualities_fromLargeToSmall() {
        val sortedQualities = QualitySelector.getSortedQualities()

        assertThat(sortedQualities[0]).isEqualTo(QUALITY_UHD)
        assertThat(sortedQualities[1]).isEqualTo(QUALITY_FHD)
        assertThat(sortedQualities[2]).isEqualTo(QUALITY_HD)
        assertThat(sortedQualities[3]).isEqualTo(QUALITY_SD)
    }

    @Test
    fun getSupportedQualities_fromLargeToSmall() {
        // camera0 supports 2160P(UHD) and 720P(HD)
        val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo0)

        assertThat(supportedQualities[0]).isEqualTo(QUALITY_UHD)
        assertThat(supportedQualities[1]).isEqualTo(QUALITY_HD)
    }

    @Test
    fun isQualitySupported_returnCorrectResult() {
        // camera0 supports 2160P(UHD) and 720P(HD)
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_NONE)).isFalse()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_HIGHEST)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_LOWEST)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_UHD)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_FHD)).isFalse()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_HD)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, QUALITY_SD)).isFalse()
    }

    @Test
    fun getResolution_returnCorrectResolution() {
        // camera0 supports 2160P(UHD) and 720P(HD)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_NONE)
        ).isNull()
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_HIGHEST)
        ).isEqualTo(RESOLUTION_2160P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_LOWEST)
        ).isEqualTo(RESOLUTION_720P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_UHD)
        ).isEqualTo(RESOLUTION_2160P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_FHD)
        ).isNull()
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_HD)
        ).isEqualTo(RESOLUTION_720P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, QUALITY_SD)
        ).isNull()
    }

    @Test
    fun of_setNonQualityConstant_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.of(NOT_QUALITY_CONSTANT)
        }
    }

    @Test
    fun thenTry_setNonQualityConstant_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.firstTry(QUALITY_FHD)
                .thenTry(NOT_QUALITY_CONSTANT)
        }
    }

    @Test
    fun finallyTry_setNonQualityConstant_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.firstTry(QUALITY_FHD)
                .thenTry(QUALITY_HD)
                .finallyTry(NOT_QUALITY_CONSTANT)
        }
    }

    @Test
    fun of_setNonFallbackStrategyConstant_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.of(QUALITY_FHD, NOT_FALLBACK_STRATEGY_CONSTANT)
        }
    }

    @Test
    fun finallyTry_setNonFallbackStrategyConstant_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.firstTry(QUALITY_FHD)
                .finallyTry(QUALITY_HD, NOT_FALLBACK_STRATEGY_CONSTANT)
        }
    }

    @Test
    fun getPrioritizedQualities_byFirstTry() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_UHD)
            .finallyTry(QUALITY_SD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_byOf() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(QUALITY_UHD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_byOf_noFallbackStrategy() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(QUALITY_FHD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEmpty()
    }

    @Test
    fun getPrioritizedQualities_byOf_withFallbackStrategy() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(
            QUALITY_FHD,
            FALLBACK_STRATEGY_LOWER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD, QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_byThenTry() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_FHD)
            .thenTry(QUALITY_UHD)
            .finallyTry(QUALITY_FHD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_byFinallyTry() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_FHD)
            .thenTry(QUALITY_SD)
            .finallyTry(QUALITY_UHD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_byFinallyTry_noFallbackStrategy() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_FHD)
            .finallyTry(QUALITY_SD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEmpty()
    }

    @Test
    fun getPrioritizedQualities_byFinallyTry_withFallbackStrategy() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_FHD)
            .finallyTry(QUALITY_SD, FALLBACK_STRATEGY_HIGHER)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD, QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_containHighestQuality_addAll() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_FHD)
            .finallyTry(QUALITY_HIGHEST)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_UHD, QUALITY_HD))
    }

    @Test
    fun getPrioritizedQualities_containLowestQuality_addAllReversely() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_FHD)
            .finallyTry(QUALITY_LOWEST)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD, QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_addDuplicateQuality_getSingleQualityWithCorrectOrder() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.firstTry(QUALITY_SD)
            .thenTry(QUALITY_FHD)
            .thenTry(QUALITY_HD)
            .thenTry(QUALITY_UHD)
            // start duplicate qualities
            .thenTry(QUALITY_SD)
            .thenTry(QUALITY_HD)
            .thenTry(QUALITY_FHD)
            .thenTry(QUALITY_UHD)
            .thenTry(QUALITY_LOWEST)
            .thenTry(QUALITY_HIGHEST)
            .finallyTry(QUALITY_LOWEST, FALLBACK_STRATEGY_STRICTLY_HIGHER)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD, QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLower_getLower() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(
            QUALITY_FHD, FALLBACK_STRATEGY_LOWER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD, QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLower_getHigher() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(
            QUALITY_SD, FALLBACK_STRATEGY_LOWER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD, QUALITY_UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackStrictLower_getLower() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(
            QUALITY_FHD, FALLBACK_STRATEGY_STRICTLY_LOWER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_HD))
    }

    @Test
    fun getPrioritizedQualities_fallbackStrictLower_getNone() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.of(
            QUALITY_SD, FALLBACK_STRATEGY_STRICTLY_LOWER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEmpty()
    }

    @Test
    fun getPrioritizedQualities_fallbackHigher_getHigher() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.of(
            QUALITY_HD,
            FALLBACK_STRATEGY_HIGHER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_FHD, QUALITY_SD))
    }

    @Test
    fun getPrioritizedQualities_fallbackHigher_getLower() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.of(
            QUALITY_UHD, FALLBACK_STRATEGY_HIGHER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_FHD, QUALITY_SD))
    }

    @Test
    fun getPrioritizedQualities_fallbackStrictHigher_getHigher() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.of(
            QUALITY_HD, FALLBACK_STRATEGY_STRICTLY_HIGHER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(QUALITY_FHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackStrictHigher_getNone() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.of(
            QUALITY_UHD, FALLBACK_STRATEGY_STRICTLY_HIGHER
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEmpty()
    }
}
