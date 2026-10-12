/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CapabilityTest {

    @Test
    fun toString_returnsConstantName() {
        assertThat(Capability.VOICE.toString()).isEqualTo("VOICE")
        assertThat(Capability.VIDEO.toString()).isEqualTo("VIDEO")
        assertThat(Capability.MESSAGE.toString()).isEqualTo("MESSAGE")
    }

    @Test
    fun constants_areDistinct() {
        assertThat(setOf(Capability.VOICE, Capability.VIDEO, Capability.MESSAGE)).hasSize(3)
    }
}
