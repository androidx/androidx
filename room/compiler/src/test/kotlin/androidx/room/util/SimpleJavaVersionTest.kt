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

import org.junit.Assert.fail
import org.junit.Test

class SimpleJavaVersionTest {

    @Test
    fun testTryParse() {
        assert(SimpleJavaVersion.tryParse("11.0.1+13-LTS") == SimpleJavaVersion(11, 0, null))
        assert(
            SimpleJavaVersion.tryParse("1.8.0_202-release-1483-b39-5396753")
                    == SimpleJavaVersion(8, 0, 202)
        )
        assert(
            SimpleJavaVersion.tryParse("1.8.0_181-google-v7-238857965-238857965")
                    == SimpleJavaVersion(8, 0, 181)
        )
        assert(SimpleJavaVersion.tryParse("a.b.c") == null)
    }

    @Test
    fun testParse() {
        assert(SimpleJavaVersion.parse("11.0.1+13-LTS") == SimpleJavaVersion(11, 0, null))
        assert(
            SimpleJavaVersion.parse("1.8.0_202-release-1483-b39-5396753")
                    == SimpleJavaVersion(8, 0, 202)
        )
        assert(
            SimpleJavaVersion.parse("1.8.0_181-google-v7-238857965-238857965")
                    == SimpleJavaVersion(8, 0, 181)
        )
        try {
            SimpleJavaVersion.parse("a.b.c")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assert(e.message == "Unable to parse Java version a.b.c")
        }
    }

    @Test
    fun testComparison() {
        assert(SimpleJavaVersion(11, 1) > SimpleJavaVersion(8, 2))
        assert(SimpleJavaVersion(8, 2) < SimpleJavaVersion(11, 1))
        assert(SimpleJavaVersion(8, 1) == SimpleJavaVersion(8, 1))

        assert(SimpleJavaVersion(8, 2, 1) > SimpleJavaVersion(8, 1, 2))
        assert(SimpleJavaVersion(8, 1, 2) < SimpleJavaVersion(8, 2, 1))
        assert(SimpleJavaVersion(8, 1, null) == SimpleJavaVersion(8, 1, null))

        assert(SimpleJavaVersion(8, 1, 2) > SimpleJavaVersion(8, 1, 1))
        assert(SimpleJavaVersion(8, 1, 1) < SimpleJavaVersion(8, 1, 2))
        assert(SimpleJavaVersion(8, 1, 1) == SimpleJavaVersion(8, 1, 1))

        assert(SimpleJavaVersion(8, 1, 0) > SimpleJavaVersion(8, 1, null))
        assert(SimpleJavaVersion(8, 1, null) < SimpleJavaVersion(8, 1, 0))
    }
}