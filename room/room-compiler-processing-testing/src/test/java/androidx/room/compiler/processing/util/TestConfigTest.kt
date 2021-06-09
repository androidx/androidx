/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.util.CompilationTestCapabilities.Config
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TestConfigTest {
    @Test
    fun checkConfigExists() {
        val config = Config.load()
        assertThat(config.kspVersion.isNotBlank()).isTrue()
        assertThat(config.kotlinVersion.isNotBlank()).isTrue()
    }

    @Test
    fun compatibilities() {
        assertThat(
            Config(
                kotlinVersion = "1.4.20",
                kspVersion = "1.4.20-blah-blah"
            ).canEnableKsp()
        ).isTrue()
        assertThat(
            Config(
                kotlinVersion = "1.4.30",
                kspVersion = "1.4.20-blah-blah"
            ).canEnableKsp()
        ).isTrue()
        assertThat(
            Config(
                kotlinVersion = "1.5.30",
                kspVersion = "1.4.20-blah-blah"
            ).canEnableKsp()
        ).isFalse()
        assertThat(
            Config(
                kotlinVersion = "1.5",
                kspVersion = "1.4.20-blah-blah"
            ).canEnableKsp()
        ).isFalse()
        assertThat(
            Config(
                kotlinVersion = "1.5",
                kspVersion = "1.5.20-blah-blah"
            ).canEnableKsp()
        ).isTrue()
    }
}