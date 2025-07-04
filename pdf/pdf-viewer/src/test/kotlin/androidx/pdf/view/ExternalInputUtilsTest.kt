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

package androidx.pdf.view

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExternalInputUtilsTest {

    @Test
    fun calculateScroll_returnsScrollAmount() {
        val viewportHeight = 1000
        val expectedScroll = 50
        assertThat(ExternalInputUtils.calculateScroll(viewportHeight, SCROLL_FACTOR))
            .isEqualTo(expectedScroll)
    }

    @Test
    fun calculateScroll_withNonMultipleHeight_roundsUpResult() {
        val viewportHeight = 1019
        // 1019 / 20 = 50.95, round up to 51
        val expectedScroll = 51
        assertThat(ExternalInputUtils.calculateScroll(viewportHeight, SCROLL_FACTOR))
            .isEqualTo(expectedScroll)
    }

    @Test
    fun calculateScroll_withNonMultipleHeight_roundsDownResult() {
        val viewportHeight = 1009
        // 1019 / 20 = 50.45, round down to 50
        val expectedScroll = 50
        assertThat(ExternalInputUtils.calculateScroll(viewportHeight, SCROLL_FACTOR))
            .isEqualTo(expectedScroll)
    }

    @Test
    fun calculateScroll_withZeroHeight_returnsZero() {
        assertThat(ExternalInputUtils.calculateScroll(0, SCROLL_FACTOR)).isEqualTo(0)
    }

    private companion object {
        const val SCROLL_FACTOR = 20
    }
}
