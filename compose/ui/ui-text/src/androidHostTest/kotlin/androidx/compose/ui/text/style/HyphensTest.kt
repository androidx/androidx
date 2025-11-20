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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text.style

import androidx.compose.ui.text.ExperimentalTextApi
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HyphensTest {

    @Test
    fun equals_returns_false_for_different_hyphens() {
        val hyphens = Hyphens.Auto
        val otherHyphens = Hyphens.None
        assertThat(hyphens).isNotEqualTo(otherHyphens)
    }

    @Test
    fun equals_returns_true_for_same_hyphens() {
        val hyphens = Hyphens.Auto
        val otherHyphens = Hyphens.Auto
        assertThat(hyphens).isEqualTo(otherHyphens)
    }

    @Test
    fun hashCode_is_different_for_different_hyphens() {
        val hyphens = Hyphens.Auto
        val otherHyphens = Hyphens.None
        assertThat(hyphens.hashCode()).isNotEqualTo(otherHyphens.hashCode())
    }

    @Test
    fun hashCode_is_same_for_same_hyphens() {
        val hyphens = Hyphens.Auto
        val otherHyphens = Hyphens.Auto
        assertThat(hyphens.hashCode()).isEqualTo(otherHyphens.hashCode())
    }

    @Test
    fun isSpecified_returns_true_for_specified_hyphens() {
        assertThat(Hyphens.Auto.isSpecified).isTrue()
        assertThat(Hyphens.None.isSpecified).isTrue()
        assertThat(Hyphens.Unspecified.isSpecified).isFalse()
    }

    @Test
    fun valueOf_throws_unknown_value() {
        assertFailsWith<IllegalArgumentException> { Hyphens.valueOf(-1) }

        assertFailsWith<IllegalArgumentException> { Hyphens.valueOf(3) }
    }

    @Test
    fun valueOf_reconstructs_hyphens() {
        assertThat(Hyphens.valueOf(Hyphens.None.value)).isEqualTo(Hyphens.None)
        assertThat(Hyphens.valueOf(Hyphens.Auto.value)).isEqualTo(Hyphens.Auto)
        assertThat(Hyphens.valueOf(Hyphens.Unspecified.value)).isEqualTo(Hyphens.Unspecified)
    }

    @Test
    fun takeOrElse_returns_this_if_isSpecified() {
        assertThat(Hyphens.None.takeOrElse { Hyphens.Auto }).isEqualTo(Hyphens.None)
        assertThat(Hyphens.Auto.takeOrElse { Hyphens.None }).isEqualTo(Hyphens.Auto)
        assertThat(Hyphens.Unspecified.takeOrElse { Hyphens.Auto }).isEqualTo(Hyphens.Auto)
    }
}
