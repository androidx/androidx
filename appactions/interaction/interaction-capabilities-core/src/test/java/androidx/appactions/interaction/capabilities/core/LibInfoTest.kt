/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core

import androidx.appactions.interaction.capabilities.core.LibInfo.Version
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LibInfoTest {
    @Test
    fun getVersion_fromValidString_returnsVersion() {
        val versionString = "1.0.0-alpha01"
        val version: Version = Version.parse(versionString)
        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(0)
        assertThat(version.patch).isEqualTo(0)
        assertThat(version.preReleaseId).isEqualTo("alpha01")
    }

    @Test
    fun getVersion_fromInvalidString_throwsException() {
        val versionString = "1A"
        var version: Version? = null
        var exceptionMsg: String? = null
        try {
            version = Version.parse(versionString)
        } catch (e: IllegalArgumentException) {
            exceptionMsg = e.message
        }
        assertThat(version).isNull()
        assertThat(exceptionMsg).isEqualTo("Can not parse version: 1A")
    }
}
