/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.embedding

import androidx.window.core.ActivityComponentInfo
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.MatcherUtils.areComponentsMatching
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MatcherUtils].
 */
@OptIn(ExperimentalWindowApi::class)
class MatcherUtilsTest {

    @Test
    fun areComponentsMatching_wildcardTrue() {
        assertTrue(areComponentsMatching(null, WILDCARD_ACTIVITY_COMPONENT_INFO))
    }

    @Test(expected = IllegalArgumentException::class)
    fun areComponentsMatching_wildcardActivityComponentThrows() {
        areComponentsMatching(WILDCARD_ACTIVITY_COMPONENT_INFO, FAKE_ACTIVITY_COMPONENT_INFO)
    }

    @Test
    fun areComponentsMatching_samePackageWildcardClass() {
        assertTrue(
            areComponentsMatching(
                FAKE_ACTIVITY_COMPONENT_INFO,
                PACKAGE_WILDCARD_ACTIVITY_COMPONENT_INFO
            )
        )
    }

    @Test
    fun areComponentsMatching_sameClassWildcardPackage() {
        assertTrue(
            areComponentsMatching(
                FAKE_ACTIVITY_COMPONENT_INFO,
                CLASS_WILDCARD_ACTIVITY_COMPONENT_INFO
            )
        )
    }

    @Test
    fun areComponentsMatching_falseIfDifferent() {
        val nonMatchingActivityComponentInfo = ActivityComponentInfo(
            "other.$FAKE_PACKAGE",
            "Other$FAKE_CLASS"
        )
        assertFalse(
            areComponentsMatching(FAKE_ACTIVITY_COMPONENT_INFO, nonMatchingActivityComponentInfo)
        )
    }

    companion object {
        private const val FAKE_PACKAGE = "fake.package"
        private const val FAKE_CLASS = "FakeClass"
        private const val WILDCARD = "*"
        private val WILDCARD_ACTIVITY_COMPONENT_INFO = ActivityComponentInfo(WILDCARD, WILDCARD)
        private val FAKE_ACTIVITY_COMPONENT_INFO = ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS)
        private val PACKAGE_WILDCARD_ACTIVITY_COMPONENT_INFO = ActivityComponentInfo(
            WILDCARD,
            FAKE_CLASS
        )
        private val CLASS_WILDCARD_ACTIVITY_COMPONENT_INFO = ActivityComponentInfo(
            FAKE_PACKAGE,
            WILDCARD
        )
    }
}
