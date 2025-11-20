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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InputToolTypeTest {

    @Test
    fun toString_returnsCorrectString() {
        assertThat(InputToolType.UNKNOWN.toString()).isEqualTo("InputToolType.UNKNOWN")
        assertThat(InputToolType.MOUSE.toString()).isEqualTo("InputToolType.MOUSE")
        assertThat(InputToolType.TOUCH.toString()).isEqualTo("InputToolType.TOUCH")
        assertThat(InputToolType.STYLUS.toString()).isEqualTo("InputToolType.STYLUS")
    }

    @Test
    fun from_createsCorrectInputToolType() {
        assertThat(InputToolType.fromInt(0)).isEqualTo(InputToolType.UNKNOWN)
        assertThat(InputToolType.fromInt(1)).isEqualTo(InputToolType.MOUSE)
        assertThat(InputToolType.fromInt(2)).isEqualTo(InputToolType.TOUCH)
        assertThat(InputToolType.fromInt(3)).isEqualTo(InputToolType.STYLUS)
        assertFailsWith<IllegalStateException> { InputToolType.fromInt(4) }
    }
}
