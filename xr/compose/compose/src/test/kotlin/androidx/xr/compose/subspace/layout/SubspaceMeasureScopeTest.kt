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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

class TestSubspaceMeasureScope : SubspaceMeasureScope {
    override val density: Float
        get() = 2f

    override val fontScale: Float
        get() = 1f

    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr
}

/** Tests for [SubspaceMeasureScope]. */
@RunWith(AndroidJUnit4::class)
class SubspaceMeasureScopeTest {
    @Test
    fun toPx_convertsCorrectly() {
        with(TestSubspaceMeasureScope()) {
            assertThat(Dp(0f).toPx()).isEqualTo(0f)
            assertThat(Dp(10.3f).toPx()).isEqualTo(20.6f)
            assertThat(Dp(-10.3f).toPx()).isEqualTo(-20.6f)
            assertThat(Dp(Float.POSITIVE_INFINITY).toPx()).isEqualTo(Float.POSITIVE_INFINITY)
            assertThat(Dp(Float.NEGATIVE_INFINITY).toPx()).isEqualTo(Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun roundToPx_convertsCorrectly() {
        with(TestSubspaceMeasureScope()) {
            assertThat(Dp(0f).roundToPx()).isEqualTo(0)
            assertThat(Dp(10.3f).roundToPx()).isEqualTo(21)
            assertThat(Dp(-10.3f).roundToPx()).isEqualTo(-21)
            assertThat(Dp(Float.POSITIVE_INFINITY).roundToPx()).isEqualTo(Constraints.Infinity)
            assertThat(Dp(Float.NEGATIVE_INFINITY).roundToPx()).isEqualTo(Constraints.Infinity)
        }
    }
}
