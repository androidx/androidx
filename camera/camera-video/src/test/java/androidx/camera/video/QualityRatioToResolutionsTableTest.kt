/*
 * Copyright 2022 The Android Open Source Project
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
import android.util.Size
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.AspectRatio.RATIO_DEFAULT
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QualityRatioToResolutionsTableTest {

    companion object {
        private val qualitySizeMap = mapOf(
            SD to Size(640, 480),
            HD to Size(1280, 720),
            FHD to Size(1920, 1080),
            UHD to Size(3840, 2160),
        )
    }

    @Test
    fun aspectRatioAutoMapToQualitySize() {
        // Act.
        val table = QualityRatioToResolutionsTable(emptyList(), qualitySizeMap)

        // Assert.
        assertThat(table.getResolutions(SD, RATIO_DEFAULT))
            .containsExactly(Size(640, 480))
        assertThat(table.getResolutions(HD, RATIO_DEFAULT))
            .containsExactly(Size(1280, 720))
        assertThat(table.getResolutions(FHD, RATIO_DEFAULT))
            .containsExactly(Size(1920, 1080))
        assertThat(table.getResolutions(UHD, RATIO_DEFAULT))
            .containsExactly(Size(3840, 2160))
    }

    @Test
    fun commonSD() {
        // Arrange.
        val common4By3 = listOf(
            Size(800, 600),
            Size(720, 540),
            Size(640, 480),
            Size(544, 408),
            Size(480, 360),
        )
        val common16By9 = listOf(
            Size(960, 540),
            Size(864, 480),
            Size(736, 412),
            Size(640, 360),
        )
        val otherRatios = listOf(
            Size(400, 400),
        )
        val otherQualities = listOf(
            Size(1280, 720),
            Size(480, 240),
        )
        val input = common4By3 + common16By9 + otherRatios + otherQualities

        // Act.
        val table = QualityRatioToResolutionsTable(input, qualitySizeMap)

        // Assert.
        assertThat(table.getResolutions(SD, RATIO_4_3)).containsExactly(
            Size(640, 480),
            Size(720, 540),
            Size(544, 408),
            Size(480, 360),
            Size(800, 600),
        ).inOrder()

        assertThat(table.getResolutions(SD, RATIO_16_9)).containsExactly(
            Size(736, 412),
            Size(640, 360),
            Size(864, 480),
            Size(960, 540),
        ).inOrder()
    }

    @Test
    fun commonHD() {
        // Arrange.
        val common4By3 = listOf(
            Size(1280, 960),
            Size(1024, 768),
            Size(960, 720),
        )
        val common16By9 = listOf(
            Size(1280, 720),
        )
        val otherRatios = listOf(
            Size(1280, 768),
            Size(1440, 720),
        )
        val otherQualities = listOf(
            Size(1920, 1080),
            Size(640, 480),
        )
        val input = common4By3 + common16By9 + otherRatios + otherQualities

        // Act.
        val table = QualityRatioToResolutionsTable(input, qualitySizeMap)

        // Assert.
        assertThat(table.getResolutions(HD, RATIO_4_3)).containsExactly(
            Size(1024, 768),
            Size(960, 720),
            Size(1280, 960),
        ).inOrder()

        assertThat(table.getResolutions(HD, RATIO_16_9)).containsExactly(
            Size(1280, 720),
        ).inOrder()
    }

    @Test
    fun commonFHD() {
        // Arrange.
        val common4By3 = listOf(
            Size(1632, 1224),
            Size(1440, 1080),
        )
        val common16By9 = listOf(
            Size(1920, 1080),
        )
        val otherRatios = listOf(
            Size(1080, 1080),
            Size(1440, 720),
        )
        val otherQualities = listOf(
            Size(2560, 1440),
            Size(1280, 720),
        )
        val input = common4By3 + common16By9 + otherRatios + otherQualities

        // Act.
        val table = QualityRatioToResolutionsTable(input, qualitySizeMap)

        // Assert.
        assertThat(table.getResolutions(FHD, RATIO_4_3)).containsExactly(
            Size(1632, 1224),
            Size(1440, 1080),
        ).inOrder()

        assertThat(table.getResolutions(FHD, RATIO_16_9)).containsExactly(
            Size(1920, 1080),
        ).inOrder()
    }

    @Test
    fun commonUHD() {
        // Arrange.
        val common4By3 = listOf(
            Size(3280, 2448),
            Size(3264, 2448),
            Size(3200, 2400),
            Size(3120, 2340),
            Size(3648, 2736),
            Size(4000, 3000),
        )
        val common16By9 = listOf(
            Size(4128, 2322),
            Size(3840, 2160),
        )
        val otherRatios = listOf(
            Size(3456, 3456),
            Size(2736, 2736),
        )
        val otherQualities = listOf(
            Size(7680, 4320),
            Size(1920, 1080),
        )
        val input = common4By3 + common16By9 + otherRatios + otherQualities

        // Act.
        val table = QualityRatioToResolutionsTable(input, qualitySizeMap)

        // Assert.
        assertThat(table.getResolutions(UHD, RATIO_4_3)).containsExactly(
            Size(3280, 2448),
            Size(3264, 2448),
            Size(3200, 2400),
            Size(3120, 2340),
            Size(3648, 2736),
            Size(4000, 3000),
        ).inOrder()

        assertThat(table.getResolutions(UHD, RATIO_16_9)).containsExactly(
            Size(3840, 2160),
            Size(4128, 2322),
        ).inOrder()
    }
}
