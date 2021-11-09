/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test

class SimpleJavaVersionTest {

    @Test
    fun testTryParse() {
        assertThat(SimpleJavaVersion.tryParse("1.8.0_181-google-v7-238857965-238857965"))
            .isEqualTo(SimpleJavaVersion(8, 0, 181))
        assertThat(SimpleJavaVersion.tryParse("1.8.0_202-release-1483-b39-5396753"))
            .isEqualTo(SimpleJavaVersion(8, 0, 202))
        assertThat(SimpleJavaVersion.tryParse("11.0.1+13-LTS"))
            .isEqualTo(SimpleJavaVersion(11, 0, null))
        assertThat(SimpleJavaVersion.tryParse("11.0.6+10-post-Ubuntu-1ubuntu118.04.1"))
            .isEqualTo(SimpleJavaVersion(11, 0, null))
        assertThat(SimpleJavaVersion.tryParse("11.0.8+10-b944.6842174"))
            .isEqualTo(SimpleJavaVersion(11, 0, null))
        assertThat(SimpleJavaVersion.tryParse("14.1-ea"))
            .isEqualTo(SimpleJavaVersion(14, 1, null))
        assertThat(SimpleJavaVersion.tryParse("15+13"))
            .isEqualTo(SimpleJavaVersion(15, 0, null))
        assertThat(SimpleJavaVersion.tryParse("a.b.c")).isNull()
    }

    @Test
    fun testParse() {
        assertThat(SimpleJavaVersion.parse("1.8.0_181-google-v7-238857965-238857965"))
            .isEqualTo(SimpleJavaVersion(8, 0, 181))
        assertThat(SimpleJavaVersion.parse("1.8.0_202-release-1483-b39-5396753"))
            .isEqualTo(SimpleJavaVersion(8, 0, 202))
        assertThat(SimpleJavaVersion.parse("11.0.1+13-LTS"))
            .isEqualTo(SimpleJavaVersion(11, 0, null))
        assertThat(SimpleJavaVersion.parse("11.0.6+10-post-Ubuntu-1ubuntu118.04.1"))
            .isEqualTo(SimpleJavaVersion(11, 0, null))
        assertThat(SimpleJavaVersion.parse("11.0.8+10-b944.6842174"))
            .isEqualTo(SimpleJavaVersion(11, 0, null))
        assertThat(SimpleJavaVersion.parse("14.1-ea"))
            .isEqualTo(SimpleJavaVersion(14, 1, null))
        assertThat(SimpleJavaVersion.parse("15+13"))
            .isEqualTo(SimpleJavaVersion(15, 0, null))
        try {
            SimpleJavaVersion.parse("a.b.c")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assert(e.message == "Unable to parse Java version a.b.c")
        }
    }

    @Test
    fun testComparison() {
        assertThat(SimpleJavaVersion(11, 1)).isGreaterThan(SimpleJavaVersion(8, 2))
        assertThat(SimpleJavaVersion(8, 2)).isLessThan(SimpleJavaVersion(11, 1))
        assertThat(SimpleJavaVersion(8, 1)).isEqualTo(SimpleJavaVersion(8, 1))

        assertThat(SimpleJavaVersion(8, 2, 1)).isGreaterThan(SimpleJavaVersion(8, 1, 2))
        assertThat(SimpleJavaVersion(8, 1, 2)).isLessThan(SimpleJavaVersion(8, 2, 1))
        assertThat(SimpleJavaVersion(8, 1, null)).isEqualTo(SimpleJavaVersion(8, 1, null))

        assertThat(SimpleJavaVersion(8, 1, 2)).isGreaterThan(SimpleJavaVersion(8, 1, 1))
        assertThat(SimpleJavaVersion(8, 1, 1)).isLessThan(SimpleJavaVersion(8, 1, 2))
        assertThat(SimpleJavaVersion(8, 1, 1)).isEqualTo(SimpleJavaVersion(8, 1, 1))

        assertThat(SimpleJavaVersion(8, 1, 0)).isGreaterThan(SimpleJavaVersion(8, 1, null))
        assertThat(SimpleJavaVersion(8, 1, null)).isLessThan(SimpleJavaVersion(8, 1, 0))
    }
}