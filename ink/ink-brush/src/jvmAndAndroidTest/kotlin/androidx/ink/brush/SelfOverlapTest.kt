/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.brush

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelfOverlapTest {

    @Test
    fun selfOverlapConstants_areDistinct() {
        val set = setOf(SelfOverlap.ANY, SelfOverlap.ACCUMULATE, SelfOverlap.DISCARD)
        assertThat(set).hasSize(3)
    }

    @Test
    fun selfOverlapHashCode_withIdenticalValues_match() {
        assertThat(SelfOverlap.DISCARD.hashCode()).isEqualTo(SelfOverlap.DISCARD.hashCode())
        assertThat(SelfOverlap.ACCUMULATE.hashCode()).isEqualTo(SelfOverlap.ACCUMULATE.hashCode())
    }

    @Test
    fun selfOverlapEquals_checksEqualityOfValues() {
        assertThat(SelfOverlap.ACCUMULATE).isEqualTo(SelfOverlap.ACCUMULATE)
        assertThat(SelfOverlap.ACCUMULATE).isNotEqualTo(SelfOverlap.DISCARD)
    }

    @Test
    fun selfOverlapToString_returnsCorrectString() {
        assertThat(SelfOverlap.ANY.toString()).isEqualTo("SelfOverlap.ANY")
        assertThat(SelfOverlap.ACCUMULATE.toString()).isEqualTo("SelfOverlap.ACCUMULATE")
        assertThat(SelfOverlap.DISCARD.toString()).isEqualTo("SelfOverlap.DISCARD")
    }
}
