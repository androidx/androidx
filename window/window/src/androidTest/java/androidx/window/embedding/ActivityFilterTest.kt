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

package androidx.window.embedding

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.window.core.ExperimentalWindowApi
import com.google.common.truth.Truth.assertWithMessage
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Integration test for [ActivityFilter] to test construction from [ComponentName].
 */
@OptIn(ExperimentalWindowApi::class)
class ActivityFilterTest {

    private val intent = Intent()
    private val activity = mock<Activity> {
        on { intent } doReturn intent
        on { componentName } doReturn COMPONENT_2
    }

    @Before
    fun setUp() {
        intent.component = COMPONENT_1
    }

    @Test
    fun idempotenceFromComponentName() {
        val filter = ActivityFilter(COMPONENT_1, ACTION)

        assertEquals(COMPONENT_1.packageName, filter.activityComponentInfo.packageName)
        assertEquals(COMPONENT_1.className, filter.activityComponentInfo.className)
    }

    @Test
    fun testMatchActivity_MatchIntentWithoutAction() {
        val filter = ActivityFilter(COMPONENT_1, null /* intentAction */)

        assertWithMessage("#matchActivity must be true because intent.component matches")
            .that(filter.matchesActivity(activity)).isTrue()

        intent.component = COMPONENT_2

        assertWithMessage("#matchActivity must be false because component mismatches")
            .that(filter.matchesActivity(activity)).isFalse()

        doReturn(COMPONENT_1).whenever(activity).componentName

        assertWithMessage("#matchActivity must be true because activity.componentName matches")
            .that(filter.matchesActivity(activity)).isTrue()
    }

    @Test
    fun testMatchActivity_MatchIntentWithAction() {
        val filter = ActivityFilter(COMPONENT_1, ACTION)

        assertWithMessage("#matchActivity must be false because intent has no action")
            .that(filter.matchesActivity(activity)).isFalse()

        intent.action = ACTION

        assertWithMessage("#matchActivity must be true because intent matches")
            .that(filter.matchesActivity(activity)).isTrue()
    }

    @Test
    fun testMatchActivity_MatchWildcardWithAction() {
        val filter = ActivityFilter(WILDCARD, ACTION)

        assertWithMessage("#matchActivity must be false because intent has no action")
            .that(filter.matchesActivity(activity)).isFalse()

        intent.action = ACTION

        assertWithMessage("#matchActivity must be true because intent.action matches")
            .that(filter.matchesActivity(activity)).isTrue()

        intent.component = null

        assertWithMessage(
            "#matchActivity must be true because intent.action matches regardless " +
                "of null component"
        )
            .that(filter.matchesActivity(activity)).isTrue()
    }

    @Test
    fun testMatchActivity_MatchIntentWithPackage() {
        val filter = ActivityFilter(WILDCARD, null /* intentAction */)

        intent.component = null
        intent.`package` = WILDCARD.packageName

        assertWithMessage("#matchActivity must be true because intent.package matches")
            .that(filter.matchesActivity(activity)).isTrue()
    }

    companion object {
        private const val ACTION = "action.test"
        private val COMPONENT_1 = ComponentName("a.b.c", "a.b.c.TestActivity")
        private val COMPONENT_2 = ComponentName("d.e.f", "d.e.f.TestActivity")
        private val WILDCARD = ComponentName("*", "*")
    }
}
