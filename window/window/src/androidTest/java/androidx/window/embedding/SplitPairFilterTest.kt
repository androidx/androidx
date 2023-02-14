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
import com.google.common.truth.Truth.assertWithMessage
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test

class SplitPairFilterTest {
    private val intent1 = Intent()
    private val intent2 = Intent()
    private val activity1 = mock<Activity> {
        on { intent } doReturn intent1
        on { componentName } doReturn COMPONENT_1
    }
    private val activity2 = mock<Activity> {
        on { intent } doReturn intent2
        on { componentName } doReturn COMPONENT_2
    }

    @Before
    fun setUp() {
        intent1.component = COMPONENT_1
        intent2.component = COMPONENT_2
    }

    @Test
    fun testMatch_WithoutAction() {
        val filter = SplitPairFilter(
            COMPONENT_1,
            COMPONENT_2,
            null /* secondaryActivityIntentAction */
        )

        assertWithMessage("#matchesActivityPair must be true because intents match")
            .that(filter.matchesActivityPair(activity1, activity2)).isTrue()
        assertWithMessage("#matchesActivityIntentPair must be true because intents match")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isTrue()

        assertWithMessage("#matchesActivityPair must be false because secondary doesn't match")
            .that(filter.matchesActivityPair(activity1, activity1)).isFalse()
        assertWithMessage(
            "#matchesActivityIntentPair must be false because secondary doesn't match"
        )
            .that(filter.matchesActivityIntentPair(activity1, intent1)).isFalse()

        assertWithMessage("#matchesActivityPair must be false because primary doesn't match")
            .that(filter.matchesActivityPair(activity2, activity2)).isFalse()
        assertWithMessage(
            "#matchesActivityIntentPair must be false because primary doesn't match"
        )
            .that(filter.matchesActivityIntentPair(activity2, intent2)).isFalse()
    }

    @Test
    fun testMatch_WithAction() {
        val filter = SplitPairFilter(COMPONENT_1, WILDCARD, ACTION)

        assertWithMessage("#matchesActivityPair must be false because intent has no action")
            .that(filter.matchesActivityPair(activity1, activity2)).isFalse()
        assertWithMessage("#matchesActivityIntentPair must be false because intent has no action")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isFalse()

        intent2.action = ACTION

        assertWithMessage("#matchesActivityPair must be true because intent.action matches")
            .that(filter.matchesActivityPair(activity1, activity2)).isTrue()
        assertWithMessage("#matchesActivityIntentPair must be true because intent.action matches")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isTrue()
    }

    @Test
    fun testMatch_WithIntentPackage() {
        val filter = SplitPairFilter(
            COMPONENT_1,
            CLASS_WILDCARD,
            null /* secondaryActivityIntentAction */
        )
        intent2.component = null
        intent2.`package` = CLASS_WILDCARD.packageName
        doReturn(COMPONENT_1).whenever(activity2).componentName

        assertWithMessage("#matchesActivityPair must be true because intent.package matches")
            .that(filter.matchesActivityPair(activity1, activity2)).isTrue()
        assertWithMessage("#matchesActivityIntentPair must be true because intent.package matches")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isTrue()

        intent2.component = COMPONENT_1

        assertWithMessage(
            "#matchesActivityPair must be false because intent.component doesn't match"
        )
            .that(filter.matchesActivityPair(activity1, activity1)).isFalse()
        assertWithMessage(
            "#matchesActivityIntentPair must be false because intent.component doesn't match"
        )
            .that(filter.matchesActivityIntentPair(activity1, intent1)).isFalse()
    }

    @Test
    fun testMatch_EmptyIntentWithWildcard() {
        val filter = SplitPairFilter(
            WILDCARD,
            WILDCARD,
            null /* secondaryActivityIntentAction */
        )
        intent1.component = null
        intent2.component = null

        assertWithMessage("#matchesActivityPair must be true because rule is wildcard")
            .that(filter.matchesActivityPair(activity1, activity2)).isTrue()
        assertWithMessage("#matchesActivityIntentPair must be true because rule is wildcard")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isTrue()
    }

    companion object {
        private const val ACTION = "action.test"
        private val COMPONENT_1 = ComponentName("a.b.c", "a.b.c.TestActivity")
        private val COMPONENT_2 = ComponentName("d.e.f", "d.e.f.TestActivity")
        private val WILDCARD = ComponentName("*", "*")
        private val CLASS_WILDCARD = ComponentName("d.e.f", "*")
    }
}