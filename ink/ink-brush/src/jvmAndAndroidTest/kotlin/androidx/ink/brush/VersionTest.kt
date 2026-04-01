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

package androidx.ink.brush

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VersionTest {

    @Test
    fun constants_areCorrect() {
        assertThat(Version.MAX_SUPPORTED).isGreaterThan(Version.V0_JETPACK1_0_0)
        assertThat(Version.MAX_SUPPORTED).isEqualTo(Version.V1_JETPACK1_1_0_ALPHA01)
        assertThat(Version.DEVELOPMENT).isGreaterThan(Version.MAX_SUPPORTED)
    }

    @Test
    fun compareTo_isCorrect() {
        assertThat(Version.V0_JETPACK1_0_0).isLessThan(Version.V1_JETPACK1_1_0_ALPHA01)
        assertThat(Version.V1_JETPACK1_1_0_ALPHA01).isGreaterThan(Version.V0_JETPACK1_0_0)
        assertThat(Version.V1_JETPACK1_1_0_ALPHA01).isEqualTo(Version.V1_JETPACK1_1_0_ALPHA01)
        assertThat(Version.V1_JETPACK1_1_0_ALPHA01).isAtLeast(Version.V1_JETPACK1_1_0_ALPHA01)
        assertThat(Version.V1_JETPACK1_1_0_ALPHA01).isAtLeast(Version.V0_JETPACK1_0_0)
        assertThat(Version.V1_JETPACK1_1_0_ALPHA01).isAtMost(Version.V1_JETPACK1_1_0_ALPHA01)
        assertThat(Version.V1_JETPACK1_1_0_ALPHA01).isAtMost(Version.DEVELOPMENT)
    }

    @Test
    fun toString_isCorrect() {
        assertThat(Version.V0_JETPACK1_0_0.toString()).isEqualTo("v0")
        assertThat(Version.DEVELOPMENT.toString()).isEqualTo("experimental")
    }
}
