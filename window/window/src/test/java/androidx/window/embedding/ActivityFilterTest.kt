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
import android.content.ComponentName
import android.content.Intent
import androidx.window.core.ActivityComponentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * The unit tests for [ActivityFilter].
 */
@RunWith(RobolectricTestRunner::class) // Used for initializing ComponentName
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
    fun testActivityFilterProperties() {
        val componentName = ComponentName(FAKE_PACKAGE, FAKE_CLASS)
        val intentAction = FAKE_ACTION
        val filter = ActivityFilter(componentName, intentAction)

        assertEquals(componentName, filter.componentName)
        assertEquals(intentAction, filter.intentAction)
    }

    @Test
    fun testToString() {
        val componentName = ComponentName(FAKE_PACKAGE, FAKE_CLASS)
        val intentAction = FAKE_ACTION
        val filterString = ActivityFilter(componentName, intentAction).toString()

        assertTrue(filterString.contains(ActivityComponentInfo(componentName).toString()))
        assertTrue(filterString.contains(intentAction))
    }

    @Test
    fun testMatchesActivity_wildcard() {
        val filter = ActivityFilter(ActivityComponentInfo(WILDCARD, WILDCARD), null)

        assertTrue(filter.matchesActivity(createTestActivity()))
    }

    @Test
    fun testMatchesActivity_componentName() {
        val filter = ActivityFilter(ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS), null)

        assertTrue(filter.matchesActivity(createTestActivity()))
        assertFalse(
            filter.matchesActivity(
                createTestActivity(ComponentName("another.packager", "another.class"))
            )
        )
    }

    @Test
    fun testMatchesIntent_wildcard() {
        val filter = ActivityFilter(ActivityComponentInfo(WILDCARD, WILDCARD), null)

        assertTrue(filter.matchesIntent(createTestIntent()))
    }

    @Test
    fun testMatchesIntent_componentName() {
        val filter = ActivityFilter(ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS), null)

        assertTrue(filter.matchesIntent(createTestIntent()))
        assertFalse(
            filter.matchesIntent(
                createTestIntent(ComponentName("another.packager", "another.class"))
            )
        )
    }

    @Test
    fun testMatchesIntent_action() {
        val filter = ActivityFilter(ActivityComponentInfo(WILDCARD, WILDCARD), FAKE_ACTION)

        assertFalse(filter.matchesIntent(createTestIntent()))
        assertTrue(filter.matchesIntent(createTestIntent(action = FAKE_ACTION)))
    }

    @Test
    fun testMatchesIntent_componentNameAndAction() {
        val filter = ActivityFilter(ActivityComponentInfo(FAKE_PACKAGE, FAKE_CLASS), FAKE_ACTION)

        assertFalse(filter.matchesIntent(createTestIntent()))
        assertFalse(
            filter.matchesIntent(
                createTestIntent(
                    componentName = ComponentName("another.packager", "another.class"),
                    action = FAKE_ACTION
                )
            )
        )
        assertTrue(filter.matchesIntent(createTestIntent(action = FAKE_ACTION)))
    }

    private fun createTestActivity(
        componentName: ComponentName = ComponentName(FAKE_PACKAGE, FAKE_CLASS)
    ): Activity {
        val activity = mock<Activity>()
        doReturn(componentName).whenever(activity).componentName

        val intent = mock<Intent>()
        doReturn(componentName).whenever(intent).component
        doReturn(intent).whenever(activity).intent

        return activity
    }

    private fun createTestIntent(
        componentName: ComponentName = ComponentName(FAKE_PACKAGE, FAKE_CLASS),
        action: String? = null
    ): Intent {
        val intent = mock<Intent>()
        doReturn(componentName).whenever(intent).component
        doReturn(action).whenever(intent).action

        return intent
    }

    private class FakeClass : Activity()

    companion object {
        private const val EMPTY: String = ""
        private const val FAKE_ACTION = "fake.action"
        private const val FAKE_PACKAGE: String = "fake.package"
        private const val FAKE_PACKAGE_WILDCARD_INSIDE = "fake.*.package"
        private const val FAKE_CLASS: String = "FakeClass"
        private const val FAKE_CLASS_WILDCARD_INSIDE = "fake.*.FakeClass"
        private const val FAKE_CLASS_WILDCARD_VALID = "fake.class.*"
        private const val WILDCARD = "*"
    }
}
