/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.foundation.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PaddingValuesTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun zeroPaddingValues() {
        var width = 0
        var height = 0
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                val sizeDp = with(LocalDensity.current) { 100.toDp() }
                Box(
                    Modifier.size(sizeDp).padding(PaddingValues.Zero).onPlaced {
                        width = it.size.width
                        height = it.size.height
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(width).isEqualTo(100)
            assertThat(height).isEqualTo(100)
        }
    }

    @Test
    fun paddingValuesAdditionOperation() {
        val p1 = PaddingValues(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)
        val p2 = PaddingValues(start = 5.dp, top = 6.dp, end = 7.dp, bottom = 8.dp)
        val result = p1 + p2

        assertThat(result.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(1.dp + 5.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(3.dp + 7.dp)
        assertThat(result.calculateTopPadding()).isEqualTo(2.dp + 6.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(4.dp + 8.dp)
        assertThat(result.calculateLeftPadding(LayoutDirection.Rtl)).isEqualTo(3.dp + 7.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Rtl)).isEqualTo(1.dp + 5.dp)
    }

    @Test
    fun paddingValuesSubtractionOperation() {
        val p1 = PaddingValues(start = 5.dp, top = 6.dp, end = 7.dp, bottom = 8.dp)
        val p2 = PaddingValues(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)
        val result = p1 - p2

        assertThat(result.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(5.dp - 1.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(7.dp - 3.dp)
        assertThat(result.calculateTopPadding()).isEqualTo(6.dp - 2.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(8.dp - 4.dp)
        assertThat(result.calculateLeftPadding(LayoutDirection.Rtl)).isEqualTo(7.dp - 3.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Rtl)).isEqualTo(5.dp - 1.dp)
    }

    @Test
    fun paddingValuesSubtraction_coercesToZero() {
        val p1 = PaddingValues(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)
        val p2 = PaddingValues(start = 5.dp, top = 6.dp, end = 7.dp, bottom = 8.dp)
        val result = p1 - p2

        // Since all paddings of p1 are > than p2, the resultant paddings should be zero
        assertThat(result.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(0.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(0.dp)
        assertThat(result.calculateTopPadding()).isEqualTo(0.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(0.dp)
        assertThat(result.calculateLeftPadding(LayoutDirection.Rtl)).isEqualTo(0.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Rtl)).isEqualTo(0.dp)
    }

    @Test
    fun absolutePaddingValuesAddedToPaddingValues() {
        val p1 = PaddingValues(start = 10.dp, top = 20.dp, end = 30.dp, bottom = 40.dp)
        val p2 = PaddingValues.Absolute(left = 1.dp, top = 2.dp, right = 3.dp, bottom = 4.dp)
        val result = p1 + p2

        assertThat(result.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(10.dp + 1.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(30.dp + 3.dp)
        assertThat(result.calculateTopPadding()).isEqualTo(20.dp + 2.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(40.dp + 4.dp)
        assertThat(result.calculateLeftPadding(LayoutDirection.Rtl)).isEqualTo(30.dp + 1.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Rtl)).isEqualTo(10.dp + 3.dp)
    }

    @Test
    fun absolutePaddingValuesSubtractedFromPaddingValues() {
        val p1 = PaddingValues(start = 10.dp, top = 20.dp, end = 30.dp, bottom = 40.dp)
        val p2 = PaddingValues.Absolute(left = 1.dp, top = 2.dp, right = 3.dp, bottom = 4.dp)
        val result = p1 - p2

        assertThat(result.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(10.dp - 1.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(30.dp - 3.dp)
        assertThat(result.calculateTopPadding()).isEqualTo(20.dp - 2.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(40.dp - 4.dp)
        assertThat(result.calculateLeftPadding(LayoutDirection.Rtl)).isEqualTo(30.dp - 1.dp)
        assertThat(result.calculateRightPadding(LayoutDirection.Rtl)).isEqualTo(10.dp - 3.dp)
    }
}
