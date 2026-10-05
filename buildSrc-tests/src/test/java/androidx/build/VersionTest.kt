/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VersionTest {
    @Test
    fun testComparisons() {
        val version2600 = Version.parse("26.0.0")
        val version2610 = Version.parse("26.1.0")
        val version2611 = Version.parse("26.1.1")
        val version2620 = Version.parse("26.2.0")
        val version2621 = Version.parse("26.2.1")
        val version2700 = Version.parse("27.0.0")
        val version2700SNAPSHOT = Version.parse("27.0.0-SNAPSHOT")
        val version2700TNAPSHOT = Version.parse("27.0.0-TNAPSHOT")

        assertEquals(version2600, version2600)

        assert(version2600 < version2700)

        assert(version2600 < version2700)

        assert(version2610 < version2611)
        assert(version2610 < version2620)
        assert(version2610 < version2621)
        assert(version2610 < version2700)

        assert(version2611 < version2620)
        assert(version2611 < version2621)
        assert(version2611 < version2700)

        assert(version2700 > version2600)
        assert(version2700 > version2700SNAPSHOT)
        assert(version2700SNAPSHOT < version2700)

        assert(version2700TNAPSHOT > version2700SNAPSHOT)
        assert(version2700SNAPSHOT < version2700TNAPSHOT)
    }

    @Test
    fun testParsingDependencyRanges() {
        assert(Version.isDependencyRange("[1.0.0]") == false)
        assert(Version.isDependencyRange("[1.0.0,2.0.0]") == true)
        assert(Version.isDependencyRange("1.0.0+") == true)
        assert(Version.isDependencyRange("1.0.0") == false)
        assert(Version.isDependencyRange("") == false)
        assert(Version.isDependencyRange("(1.0.0)") == false)
        assert(Version.isDependencyRange("(1.0.0,2.0.0)") == true)
        assert(Version.isDependencyRange("(1.0.0-beta01,2.0.0)") == true)
    }

    @Test
    fun testParsingPreReleaseIteration() {
        assert(Version.parse("1.0.0").preReleaseIteration == null)
        assert(Version.parse("1.0.0-SNAPSHOT").preReleaseIteration == null)
        assert(Version.parse("1.0.0-alpha01").preReleaseIteration == 1)
        assert(Version.parse("1.0.0-alpha10").preReleaseIteration == 10)
        assert(Version.parse("1.0.0-beta01").preReleaseIteration == 1)
        assert(Version.parse("1.0.0-dev01").preReleaseIteration == 1)
        assert(Version.parse("1.0.0-rc01").preReleaseIteration == 1)
        assert(Version.parse("1.0.0-rc02").preReleaseIteration == 2)
    }
}
