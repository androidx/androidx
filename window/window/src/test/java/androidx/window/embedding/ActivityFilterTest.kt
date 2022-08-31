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

import android.app.Activity
import androidx.window.core.ActivityComponentInfo
import androidx.window.core.ExperimentalWindowApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The unit tests for [ActivityFilter] that will test construction.
 */
@OptIn(ExperimentalWindowApi::class)
class ActivityFilterTest {

    @Test(expected = IllegalArgumentException::class)
    fun packageNameMustNotBeEmpty() {
        val activityComponentInfo = ActivityComponentInfo(EMPTY, FAKE_CLASS)
        ActivityFilter(activityComponentInfo, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameMustNotBeEmpty() {
        val activityComponentInfo = ActivityComponentInfo(FAKE_PACKAGE, EMPTY)
        ActivityFilter(activityComponentInfo, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun packageNameCannotContainWildcard() {
        val activityComponentInfo = ActivityComponentInfo(FAKE_PACKAGE_WILDCARD_INSIDE, FAKE_CLASS)
        ActivityFilter(activityComponentInfo, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameCannotContainWildcard() {
        val activityComponentInfo = ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS_WILDCARD_INSIDE)
        ActivityFilter(activityComponentInfo, null)
    }

    @Test
    fun equalsImpliesSameHashCode() {
        val first = ActivityFilter(ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS), null)
        val second = ActivityFilter(ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS), null)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun matchesClassName() {
        val filter = ActivityFilter(
            ActivityComponentInfo(FAKE_PACKAGE, Activity::class.java.name),
            null
        )
        assertTrue(filter.matchesClassName(Activity::class.java))
        assertFalse(filter.matchesClassName(FakeClass::class.java))
    }

    @Test
    fun matchesWildCard() {
        val filter = ActivityFilter(
            ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS_WILDCARD_VALID),
            null
        )

        assertTrue(filter.matchesClassNameOrWildCard<Activity>(null))
        assertFalse(filter.matchesClassNameOrWildCard(Activity::class.java))
    }

    private class FakeClass : Activity()

    companion object {
        private const val EMPTY: String = ""
        private const val FAKE_PACKAGE: String = "fake.package"
        private const val FAKE_PACKAGE_WILDCARD_INSIDE = "fake.*.package"
        private const val FAKE_CLASS: String = "FakeClass"
        private const val FAKE_CLASS_WILDCARD_INSIDE = "fake.*.FakeClass"
        private const val FAKE_CLASS_WILDCARD_VALID = "fake.class.*"
    }
}
