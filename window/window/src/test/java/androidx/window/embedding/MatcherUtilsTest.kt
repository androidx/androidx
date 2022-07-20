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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MatcherUtils].
 */
@OptIn(ExperimentalWindowApi::class)
class MatcherUtilsTest {

    @Test
    fun areComponentsMatching_wildcardTrue() {
        assertTrue(areComponentsMatching(null, wildcardActivityComponentInfo))
    }

    @Test(expected = IllegalArgumentException::class)
    fun areComponentsMatching_wildcardActivityComponentThrows() {
        areComponentsMatching(wildcardActivityComponentInfo, fakeActivityComponentInfo)
    }

    companion object {
        private const val fakePackage = "fake.package"
        private const val fakeClass = "FakeClass"
        private const val wildcard = "*"
        private val wildcardActivityComponentInfo = ActivityComponentInfo(wildcard, wildcard)
        private val fakeActivityComponentInfo = ActivityComponentInfo(fakePackage, fakeClass)
    }
}
