/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget.ui.components

import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MetricTextTest {

    @Test
    fun metric_isLeftAloneWhenItFits() {
        assertThat(fitMetric("11,056", maxWidthDp = 200f, fontSize = 20.sp)).isEqualTo("11,056")
    }

    @Test
    fun metric_dropsItsGroupSeparatorFirst() {
        // Wide enough for five glyphs at 20 sp (58 dp) but not six.
        assertThat(fitMetric("11,056", maxWidthDp = 62f, fontSize = 20.sp)).isEqualTo("11056")
    }

    @Test
    fun metric_abbreviatesToWholeUnitsWhenTheBreakpointIsNarrow() {
        // The W1 card's budget: 88 dp wide less 16 dp padding either side.
        assertThat(fitMetric("11,056", maxWidthDp = 56f, fontSize = 20.sp)).isEqualTo("11K")
    }

    @Test
    fun metric_keepsADecimalPlaceWhereThereIsRoomForOne() {
        assertThat(fitMetric("1,150,000", maxWidthDp = 60f, fontSize = 20.sp)).isEqualTo("1.1M")
    }

    @Test
    fun metric_dropsTheDecimalPlaceWhenEvenThatIsTooWide() {
        assertThat(fitMetric("1,150,000", maxWidthDp = 30f, fontSize = 20.sp)).isEqualTo("1M")
    }

    @Test
    fun metric_truncatesRatherThanRounds() {
        // 11,999 is not yet 12K, and a widget should never claim progress that has not happened.
        assertThat(fitMetric("11,999", maxWidthDp = 40f, fontSize = 20.sp)).isEqualTo("11K")
    }

    @Test
    fun metric_underAThousandHasNoAbbreviation() {
        assertThat(fitMetric("956", maxWidthDp = 10f, fontSize = 20.sp)).isEqualTo("956")
    }

    @Test
    fun metric_thatIsNotANumberIsLeftToEllipsize() {
        assertThat(fitMetric("Morning Run", maxWidthDp = 20f, fontSize = 20.sp))
            .isEqualTo("Morning Run")
        assertThat(fitMetric("5.2 km", maxWidthDp = 20f, fontSize = 20.sp)).isEqualTo("5.2 km")
    }

    @Test
    fun metric_acceptsLocaleGroupSeparatorsOtherThanTheComma() {
        assertThat(fitMetric("11.056", maxWidthDp = 56f, fontSize = 20.sp)).isEqualTo("11K")
        assertThat(fitMetric("11\u00A0056", maxWidthDp = 56f, fontSize = 20.sp)).isEqualTo("11K")
    }

    @Test
    fun metric_fallsBackToTheShortestFormWhenNothingFits() {
        assertThat(fitMetric("11,056", maxWidthDp = 1f, fontSize = 20.sp)).isEqualTo("11K")
    }

    @Test
    fun textWidth_growsWithBothLengthAndTypeSize() {
        val short = estimateTextWidthDp("11K", 20f)
        val long = estimateTextWidthDp("11,056", 20f)
        val large = estimateTextWidthDp("11K", 40f)

        assertThat(long).isGreaterThan(short)
        assertThat(large).isWithin(0.01f).of(short * 2)
    }
}
