/*
 * Copyright (C) 2024 The Android Open Source Project
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
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InputToolTypeTest {

    @Test
    fun constants_areDistinct() {
        val set =
            setOf(
                InputToolType.UNKNOWN,
                InputToolType.MOUSE,
                InputToolType.STYLUS,
                InputToolType.TOUCH
            )
        assertThat(set).hasSize(4)
    }

    @Test
    fun toString_returnsCorrectString() {
        assertThat(InputToolType.UNKNOWN.toString()).isEqualTo("InputToolType.UNKNOWN")
        assertThat(InputToolType.MOUSE.toString()).isEqualTo("InputToolType.MOUSE")
        assertThat(InputToolType.TOUCH.toString()).isEqualTo("InputToolType.TOUCH")
        assertThat(InputToolType.STYLUS.toString()).isEqualTo("InputToolType.STYLUS")
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(InputToolType.MOUSE.hashCode()).isEqualTo(InputToolType.MOUSE.hashCode())

        assertThat(InputToolType.MOUSE.hashCode()).isNotEqualTo(InputToolType.TOUCH.hashCode())
    }

    @Test
    fun equals_checksEqualityOfValues() {
        assertThat(InputToolType.MOUSE).isEqualTo(InputToolType.MOUSE)
        assertThat(InputToolType.MOUSE).isNotEqualTo(InputToolType.TOUCH)
        assertThat(InputToolType.MOUSE).isNotEqualTo(null)
    }

    @Test
    fun from_createsCorrectInputToolType() {
        assertThat(InputToolType.from(0)).isEqualTo(InputToolType.UNKNOWN)
        assertThat(InputToolType.from(1)).isEqualTo(InputToolType.MOUSE)
        assertThat(InputToolType.from(2)).isEqualTo(InputToolType.TOUCH)
        assertThat(InputToolType.from(3)).isEqualTo(InputToolType.STYLUS)
        assertFailsWith<IllegalArgumentException> { InputToolType.from(4) }
    }
}
