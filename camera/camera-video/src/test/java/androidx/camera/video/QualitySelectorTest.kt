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

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import android.os.Build
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeEncoderProfilesProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val CAMERA_ID_0 = "0"
private const val CAMERA_ID_1 = "1"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QualitySelectorTest {

    private val cameraInfo0 = FakeCameraInfoInternal(CAMERA_ID_0).apply {
        encoderProfilesProvider = FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_2160P)
            .add(QUALITY_2160P, PROFILES_2160P)
            .add(QUALITY_720P, PROFILES_720P)
            .add(QUALITY_LOW, PROFILES_720P)
            .build()
    }
    private val cameraInfo1 = FakeCameraInfoInternal(CAMERA_ID_1).apply {
        encoderProfilesProvider = FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_1080P)
            .add(QUALITY_1080P, PROFILES_1080P)
            .add(QUALITY_480P, PROFILES_480P)
            .add(QUALITY_LOW, PROFILES_480P)
            .build()
    }

    @Test
    fun getSortedQualities_fromLargeToSmall() {
        val sortedQualities = Quality.getSortedQualities()

        assertThat(sortedQualities[0]).isEqualTo(Quality.UHD)
        assertThat(sortedQualities[1]).isEqualTo(Quality.FHD)
        assertThat(sortedQualities[2]).isEqualTo(Quality.HD)
        assertThat(sortedQualities[3]).isEqualTo(Quality.SD)
    }

    @Test
    fun getSupportedQualities_fromLargeToSmall() {
        // camera0 supports 2160P(UHD) and 720P(HD)
        val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo0)

        assertThat(supportedQualities[0]).isEqualTo(Quality.UHD)
        assertThat(supportedQualities[1]).isEqualTo(Quality.HD)
    }

    @Test
    fun isQualitySupported_returnCorrectResult() {
        // camera0 supports 2160P(UHD) and 720P(HD)
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, Quality.HIGHEST)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, Quality.LOWEST)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, Quality.UHD)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, Quality.FHD)).isFalse()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, Quality.HD)).isTrue()
        assertThat(QualitySelector.isQualitySupported(cameraInfo0, Quality.SD)).isFalse()
    }

    @Test
    fun getResolution_returnCorrectResolution() {
        // camera0 supports 2160P(UHD) and 720P(HD)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, Quality.HIGHEST)
        ).isEqualTo(RESOLUTION_2160P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, Quality.LOWEST)
        ).isEqualTo(RESOLUTION_720P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, Quality.UHD)
        ).isEqualTo(RESOLUTION_2160P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, Quality.FHD)
        ).isNull()
        assertThat(
            QualitySelector.getResolution(cameraInfo0, Quality.HD)
        ).isEqualTo(RESOLUTION_720P)
        assertThat(
            QualitySelector.getResolution(cameraInfo0, Quality.SD)
        ).isNull()
    }

    @Test
    fun fromOrderedList_containNull_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.fromOrderedList(listOf(Quality.FHD, null))
        }
    }

    @Test
    fun fromOrderedList_setEmptyQualityList_throwException() {
        // Assert.
        assertThrows(IllegalArgumentException::class.java) {
            // Act.
            QualitySelector.fromOrderedList(emptyList())
        }
    }

    @Test
    fun getPrioritizedQualities_selectSingleQuality() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(Quality.UHD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_selectQualityByOrder() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector =
            QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.UHD, Quality.HD))

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.UHD, Quality.HD))
    }

    @Test
    fun getPrioritizedQualities_noFallbackStrategy() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(Quality.FHD)

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEmpty()
    }

    @Test
    fun getPrioritizedQualities_withFallbackStrategy() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(
            Quality.FHD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.HD, Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_containHighestQuality_addAll() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.HIGHEST))

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.UHD, Quality.HD))
    }

    @Test
    fun getPrioritizedQualities_containLowestQuality_addAllReversely() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.LOWEST))

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.HD, Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_addDuplicateQuality_getSingleQualityWithCorrectOrder() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(
                Quality.SD,
                Quality.FHD,
                Quality.HD,
                Quality.UHD,
                // start duplicate qualities
                Quality.SD,
                Quality.HD,
                Quality.FHD,
                Quality.UHD,
                Quality.LOWEST,
                Quality.HIGHEST
            ),
            FallbackStrategy.higherQualityThan(Quality.LOWEST)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.HD, Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLowerOrHigher_getLower() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(
            Quality.FHD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.HD, Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLowerOrHigher_fallbackQualityNotIncluded() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(
            Quality.SD,
            FallbackStrategy.higherQualityThan(Quality.HD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLowerOrHigher_getHigher() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(
            Quality.SD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.HD, Quality.UHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLower_getLower() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(
            Quality.FHD,
            FallbackStrategy.lowerQualityThan(Quality.FHD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.HD))
    }

    @Test
    fun getPrioritizedQualities_fallbackLower_getNone() {
        // Arrange.
        // camera0 supports 2160P(UHD) and 720P(HD)
        val qualitySelector = QualitySelector.from(
            Quality.SD,
            FallbackStrategy.lowerQualityThan(Quality.SD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo0)

        // Assert.
        assertThat(qualities).isEmpty()
    }

    @Test
    fun getPrioritizedQualities_fallbackHigherOrLower_getHigher() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.from(
            Quality.HD,
            FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.FHD, Quality.SD))
    }

    @Test
    fun getPrioritizedQualities_fallbackHigher_fallbackQualityNotIncluded() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.from(
            Quality.UHD,
            FallbackStrategy.higherQualityThan(Quality.SD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.FHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackHigherOrLower_getLower() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.from(
            Quality.UHD,
            FallbackStrategy.higherQualityOrLowerThan(Quality.UHD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.FHD, Quality.SD))
    }

    @Test
    fun getPrioritizedQualities_fallbackHigher_getHigher() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.from(
            Quality.HD,
            FallbackStrategy.higherQualityThan(Quality.HD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEqualTo(listOf(Quality.FHD))
    }

    @Test
    fun getPrioritizedQualities_fallbackHigher_getNone() {
        // Arrange.
        // camera1 supports 1080P(FHD) and 480P(SD)
        val qualitySelector = QualitySelector.from(
            Quality.UHD,
            FallbackStrategy.higherQualityThan(Quality.UHD)
        )

        // Act.
        val qualities = qualitySelector.getPrioritizedQualities(cameraInfo1)

        // Assert.
        assertThat(qualities).isEmpty()
    }
}
