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

import androidx.kruth.assertThat
import kotlin.test.Test

class VersionTest {

    @Test
    fun constants_areCorrect() {
        assertThat(Version.MAX_SUPPORTED).isGreaterThan(Version.V0)
        assertThat(Version.MAX_SUPPORTED).isEqualTo(Version.V1)
        assertThat(Version.DEVELOPMENT).isGreaterThan(Version.MAX_SUPPORTED)
    }

    @Test
    fun compareTo_isCorrect() {
        assertThat(Version.V0).isLessThan(Version.V1)
        assertThat(Version.V1).isGreaterThan(Version.V0)
        assertThat(Version.V1).isEqualTo(Version.V1)
        assertThat(Version.V1).isAtLeast(Version.V1)
        assertThat(Version.V1).isAtLeast(Version.V0)
        assertThat(Version.V1).isAtMost(Version.V1)
        assertThat(Version.V1).isAtMost(Version.DEVELOPMENT)
    }

    @Test
    fun toString_isCorrect() {
        assertThat(Version.V0.toString()).isEqualTo("v0")
        assertThat(Version.DEVELOPMENT.toString()).isEqualTo("experimental")
    }
}
