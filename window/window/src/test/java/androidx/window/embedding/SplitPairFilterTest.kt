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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    @Test(expected = IllegalArgumentException::class)
    fun packageNameMustNotBeEmpty_primary() {
        val emptyPackageComponent = ComponentName(EMPTY, FAKE_CLASS)
        SplitPairFilter(emptyPackageComponent, COMPONENT_1, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun packageNameMustNotBeEmpty_secondary() {
        val emptyPackageComponent = ComponentName(EMPTY, FAKE_CLASS)
        SplitPairFilter(COMPONENT_1, emptyPackageComponent, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameMustNotBeEmpty_primary() {
        val emptyClassComponent = ComponentName(FAKE_PACKAGE, EMPTY)
        SplitPairFilter(emptyClassComponent, COMPONENT_1, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameMustNotBeEmpty_secondary() {
        val emptyClassComponent = ComponentName(FAKE_PACKAGE, EMPTY)
        SplitPairFilter(COMPONENT_1, emptyClassComponent, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun packageNameCannotContainWildcard_primary() {
        val wildcardPackageComponent = ComponentName(FAKE_PACKAGE_WILDCARD_INSIDE, FAKE_CLASS)
        SplitPairFilter(wildcardPackageComponent, COMPONENT_1, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun packageNameCannotContainWildcard_secondary() {
        val wildcardPackageComponent = ComponentName(FAKE_PACKAGE_WILDCARD_INSIDE, FAKE_CLASS)
        SplitPairFilter(COMPONENT_1, wildcardPackageComponent, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameCannotContainWildcard_primary() {
        val wildcardInsideClassComponent = ComponentName(FAKE_PACKAGE, FAKE_CLASS_WILDCARD_INSIDE)
        SplitPairFilter(wildcardInsideClassComponent, COMPONENT_1, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameCannotContainWildcard_secondary() {
        val wildcardInsideClassComponent = ComponentName(FAKE_PACKAGE, FAKE_CLASS_WILDCARD_INSIDE)
        SplitPairFilter(COMPONENT_1, wildcardInsideClassComponent, null)
    }

    @Test
    fun sameComponentName() {
        val splitPairFilter = SplitPairFilter(COMPONENT_1, COMPONENT_2, INTENT_ACTION)
        assertEquals(COMPONENT_1.packageName, splitPairFilter.primaryActivityName.packageName)
        assertEquals(COMPONENT_1.className, splitPairFilter.primaryActivityName.className)
        assertEquals(
            COMPONENT_2.packageName,
            splitPairFilter.secondaryActivityName.packageName
        )
        assertEquals(COMPONENT_2.className, splitPairFilter.secondaryActivityName.className)
        assertEquals(INTENT_ACTION, splitPairFilter.secondaryActivityIntentAction)
    }

    @Test
    fun equalsImpliesSameHashCode() {
        val first = SplitPairFilter(COMPONENT_1, COMPONENT_2, INTENT_ACTION)
        val second = SplitPairFilter(COMPONENT_1, COMPONENT_2, INTENT_ACTION)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun testMatch_WithoutAction() {
        val filter = SplitPairFilter(
            COMPONENT_1,
            COMPONENT_2,
            null /* secondaryActivityIntentAction */
        )
        intent1.component = COMPONENT_1
        intent2.component = COMPONENT_2

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
        val filter = SplitPairFilter(COMPONENT_1, WILDCARD_COMPONENT, INTENT_ACTION)
        intent1.component = COMPONENT_1
        intent2.component = COMPONENT_2

        assertWithMessage("#matchesActivityPair must be false because intent has no action")
            .that(filter.matchesActivityPair(activity1, activity2)).isFalse()
        assertWithMessage("#matchesActivityIntentPair must be false because intent has no action")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isFalse()

        intent2.action = INTENT_ACTION

        assertWithMessage("#matchesActivityPair must be true because intent.action matches")
            .that(filter.matchesActivityPair(activity1, activity2)).isTrue()
        assertWithMessage("#matchesActivityIntentPair must be true because intent.action matches")
            .that(filter.matchesActivityIntentPair(activity1, intent2)).isTrue()
    }

    @Test
    fun testMatch_WithIntentPackage() {
        val filter = SplitPairFilter(
            COMPONENT_1,
            WILDCARD_CLASS_COMPONENT,
            null /* secondaryActivityIntentAction */
        )
        intent1.component = COMPONENT_1
        intent2.component = null
        intent2.`package` = WILDCARD_CLASS_COMPONENT.packageName
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
            WILDCARD_COMPONENT,
            WILDCARD_COMPONENT,
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
        private const val FAKE_PACKAGE: String = "fake.package"
        private const val FAKE_PACKAGE_WILDCARD_INSIDE = "fake.*.package"
        private const val FAKE_CLASS: String = "fake.class.test"
        private const val FAKE_CLASS_WILDCARD_INSIDE = "fake.*.class"

        private const val EMPTY: String = ""
        private const val INTENT_ACTION = "fake.action"
        private val COMPONENT_1 = ComponentName("a.b.c", "a.b.c.TestActivity")
        private val COMPONENT_2 = ComponentName("d.e.f", "d.e.f.TestActivity")
        private val WILDCARD_CLASS_COMPONENT = ComponentName(FAKE_PACKAGE, "*")
        private val WILDCARD_COMPONENT = ComponentName("*", "*")
    }
}
