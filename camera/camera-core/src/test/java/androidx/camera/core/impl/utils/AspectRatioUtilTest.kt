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

package androidx.camera.core.impl.utils

import android.os.Build
import android.util.Rational
import android.util.Size
import androidx.camera.core.impl.utils.AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AspectRatioUtilTest {

    @Test
    fun testHasMatchingAspectRatio_withNullAspectRatio() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(16, 9),
                null
            )
        ).isFalse()
    }

    @Test
    fun testHasMatchingAspectRatio_withSameAspectRatio() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(16, 9),
                Rational(16, 9)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withMod16AspectRatio_720p() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(1280, 720),
                Rational(16, 9)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withMod16AspectRatio_1080p() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(1920, 1088),
                Rational(16, 9)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withMod16AspectRatio_1440p() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(2560, 1440),
                Rational(16, 9)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withMod16AspectRatio_2160p() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(3840, 2160),
                Rational(16, 9)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withMod16AspectRatio_1x1() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(1088, 1088),
                Rational(1, 1)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withMod16AspectRatio_4x3() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(1024, 768),
                Rational(4, 3)
            )
        ).isTrue()
    }

    @Test
    fun testHasMatchingAspectRatio_withNonMod16AspectRatio() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(1281, 721),
                Rational(16, 9)
            )
        ).isFalse()
    }

    @Test
    fun testHasMatchingAspectRatio_smallerThanDefaultMod16LowerBound() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(640, 358),
                Rational(16, 9)
            )
        ).isFalse()
    }

    @Test
    fun testHasMatchingAspectRatio_setCustomMod16LowerBound() {
        assertThat(
            AspectRatioUtil.hasMatchingAspectRatio(
                Size(640, 358),
                Rational(16, 9),
                Size(320, 240)
            )
        ).isTrue()
    }

    @Test
    fun sortAspectRatios() {
        // Sort the aspect ratio key set by the target aspect ratio.
        val aspectRatios = listOf(
            Rational(1, 1),
            Rational(4, 3),
            Rational(16, 9),
            Rational(18, 9),
            Rational(14, 9),
        )

        val targetAspectRatio = Rational(16, 9)
        val fullFovAspectRatio = Rational(4, 3)

        Collections.sort(
            aspectRatios,
            CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                targetAspectRatio,
                fullFovAspectRatio
            )
        )

        val expectedResult = listOf(
            Rational(16, 9),
            Rational(14, 9),
            Rational(4, 3),
            Rational(18, 9),
            Rational(1, 1),
        )

        assertThat(aspectRatios == expectedResult).isTrue()
    }
}
