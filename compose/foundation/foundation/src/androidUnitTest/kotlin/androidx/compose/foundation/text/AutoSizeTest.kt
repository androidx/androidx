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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.modifiers.AutoSizeTextLayoutScope
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class AutoSizeTest {
    @Test
    fun stepBased_valid_args() {
        // we shouldn't throw here
        AutoSize.StepBased(1.sp, 2.sp, 3.sp)

        AutoSize.StepBased(0.sp, 0.1.sp, 0.0001.sp)

        AutoSize.StepBased(1.em, 2.em, 0.1.em)

        AutoSize.StepBased(2.sp, 1.em, 0.1.sp)
    }

    @Test
    fun stepBased_minFontSize_greaterThan_maxFontSize_coercesTo_maxFontSize() {
        var autoSize1 = AutoSize.StepBased(2.sp, 1.sp)
        var autoSize2 = AutoSize.StepBased(1.sp, 1.sp)
        assertThat(autoSize1).isEqualTo(autoSize2)
        assertThat(autoSize2).isEqualTo(autoSize1)

        autoSize1 = AutoSize.StepBased(3.6.em, 2.em)
        autoSize2 = AutoSize.StepBased(2.em, 2.em)
        assertThat(autoSize1).isEqualTo(autoSize2)
        assertThat(autoSize2).isEqualTo(autoSize1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_stepSize_tooSmall() {
        AutoSize.StepBased(stepSize = 0.00000134.sp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_minFontSize_unspecified() {
        AutoSize.StepBased(minFontSize = TextUnit.Unspecified, maxFontSize = 1.sp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_maxFontSize_unspecified() {
        AutoSize.StepBased(minFontSize = 2.sp, maxFontSize = TextUnit.Unspecified)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_stepSize_unspecified() {
        AutoSize.StepBased(stepSize = TextUnit.Unspecified)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_minFontSize_negative() {
        AutoSize.StepBased(minFontSize = (-1).sp, maxFontSize = 0.sp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_maxFontSize_negative() {
        AutoSize.StepBased(minFontSize = 0.sp, maxFontSize = (-1).sp)
    }

    @Test
    fun stepBased_equals() {
        var autoSize1 = AutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 10.sp, stepSize = 2.sp)
        var autoSize2 =
            AutoSize.StepBased(minFontSize = 1.0.sp, maxFontSize = 10.0.sp, stepSize = 2.0.sp)
        assertThat(autoSize1).isEqualTo(autoSize2)
        assertThat(autoSize2).isEqualTo(autoSize1)

        autoSize2 = AutoSize.StepBased(minFontSize = 1.1.sp, maxFontSize = 10.sp, stepSize = 2.sp)
        assertThat(autoSize1).isNotEqualTo(autoSize2)
        assertThat(autoSize2).isNotEqualTo(autoSize1)

        autoSize2 = AutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 11.1.sp, stepSize = 2.sp)
        assertThat(autoSize1).isNotEqualTo(autoSize2)
        assertThat(autoSize2).isNotEqualTo(autoSize1)

        autoSize2 = AutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 10.sp, stepSize = 2.5.sp)
        assertThat(autoSize1).isNotEqualTo(autoSize2)
        assertThat(autoSize2).isNotEqualTo(autoSize1)

        autoSize2 = TestAutoSize(7)
        assertThat(autoSize1).isNotEqualTo(autoSize2)

        autoSize1 = AutoSize.StepBased(minFontSize = 1.em, maxFontSize = 2.em, stepSize = 0.1.em)
        autoSize2 =
            AutoSize.StepBased(minFontSize = 1.0.em, maxFontSize = 2.0.em, stepSize = 0.1.em)
        assertThat(autoSize1).isEqualTo(autoSize2)
        assertThat(autoSize2).isEqualTo(autoSize1)
    }

    @Test
    fun stepBased_getFontSize_alwaysOverflows() {
        val autoSize =
            AutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = 0.25.sp)
                as TextAutoSize
        val searchScope: AutoSizeTextLayoutScope = AlwaysOverflows()
        with(autoSize) { assertThat(searchScope.getFontSize().value).isEqualTo(12) }
    }

    @Test
    fun stepBased_getFontSize_neverOverflows() {
        val autoSize =
            AutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = 0.25.sp)
                as TextAutoSize
        val searchScope: AutoSizeTextLayoutScope = NeverOverflows()
        with(autoSize) { assertThat(searchScope.getFontSize().value).isEqualTo(112) }
    }

    @Test
    fun stepBased_getFontSize_cappedAtMaxSize_beforeOverflow() {
        val autoSize =
            AutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = 0.25.sp)
                as TextAutoSize
        val searchScope: AutoSizeTextLayoutScope = OverflowsWhenFontSizeIsGreaterThan60px()
        with(autoSize) { assertThat(searchScope.getFontSize().value).isEqualTo(60) }
    }

    @Test
    fun stepBased_getFontSize_searchRangeMidpoint_overflows() {
        val autoSize =
            AutoSize.StepBased(minFontSize = 0.sp, maxFontSize = 100.sp, stepSize = 70.sp)
                as TextAutoSize
        val searchScope: AutoSizeTextLayoutScope = OverflowsWhenFontSizeIsGreaterThan60px()
        // Here we're testing when (max - min) / 2 overflows and min doesn't overflow
        with(autoSize) { assertThat(searchScope.getFontSize().value) }.isEqualTo(0)
    }

    @Test
    fun stepBased_getFontSize_differentStepSizes() {
        val autoSize1 =
            AutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 100.sp, stepSize = 10.sp)
                as TextAutoSize
        val autoSize2 =
            AutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 100.sp, stepSize = 20.sp)
                as TextAutoSize
        val searchScope: AutoSizeTextLayoutScope = OverflowsWhenFontSizeIsGreaterThan60px()

        with(autoSize1) { assertThat(searchScope.getFontSize().value).isEqualTo(60) }
        with(autoSize2) { assertThat(searchScope.getFontSize().value).isEqualTo(50) }
    }

    @Test
    fun stepBased_getFontSize_stepSize_greaterThan_maxFontSize_minus_minFontSize() {
        // regardless of the bounds of the container, the only potential font size is minFontSize
        val autoSize =
            AutoSize.StepBased(minFontSize = 45.sp, maxFontSize = 55.sp, stepSize = 15.sp)
                as TextAutoSize
        with(autoSize) {
            var searchScope: AutoSizeTextLayoutScope = AlwaysOverflows()
            assertThat(searchScope.getFontSize().value).isEqualTo(45)

            searchScope = NeverOverflows()
            assertThat(searchScope.getFontSize().value).isEqualTo(45)

            searchScope = OverflowsWhenFontSizeIsGreaterThan60px()
            assertThat(searchScope.getFontSize().value).isEqualTo(45)
        }
    }

    private class TestAutoSize(private val testParam: Int) : TextAutoSize {
        override fun AutoSizeTextLayoutScope.getFontSize(): TextUnit {
            return if (!performLayoutAndGetOverflow(testParam.sp)) testParam.sp else 3.sp
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TestAutoSize) return false

            return testParam == other.testParam
        }

        override fun hashCode(): Int {
            return testParam
        }
    }

    private class AlwaysOverflows : AutoSizeTextLayoutScope {
        override val density = 1f
        override val fontScale = 1f

        override fun performLayoutAndGetOverflow(fontSize: TextUnit): Boolean {
            return true
        }
    }

    private class NeverOverflows : AutoSizeTextLayoutScope {
        override val density = 1f
        override val fontScale = 1f

        override fun performLayoutAndGetOverflow(fontSize: TextUnit): Boolean {
            return false
        }
    }

    private class OverflowsWhenFontSizeIsGreaterThan60px : AutoSizeTextLayoutScope {
        override val density = 1f
        override val fontScale = 1f

        override fun performLayoutAndGetOverflow(fontSize: TextUnit): Boolean {
            return fontSize.toPx() > 60
        }
    }
}
