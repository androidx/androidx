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
import androidx.window.core.ExperimentalWindowApi
import com.google.common.truth.Truth.assertWithMessage
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

@OptIn(ExperimentalWindowApi::class)
class SplitPairFilterTest {
    private val component1 = ComponentName("a.b.c", "a.b.c.TestActivity")
    private val component2 = ComponentName("d.e.f", "d.e.f.TestActivity")
    private val intentClassWildcard = ComponentName("d.e.f", "*")
    private val intent = Intent()
    private val activity = mock<Activity> {
        on { intent } doReturn intent
        on { componentName } doReturn component1
    }
    private val filter =
        SplitPairFilter(component1, intentClassWildcard, null /* secondaryActivityIntentAction */)

    @Test
    fun testMatchActivityIntentPair_MatchIntentComponent() {
        assertWithMessage("#matchesActivityIntentPair must be false because intent is empty")
            .that(filter.matchesActivityIntentPair(activity, intent)).isFalse()

        intent.component = component2

        assertWithMessage("#matchesActivityIntentPair must be true because intent.component" +
            " matches")
            .that(filter.matchesActivityIntentPair(activity, intent)).isTrue()
    }

    @Test
    fun testMatchActivityIntentPair_MatchIntentPackage() {
        intent.`package` = intentClassWildcard.packageName

        assertWithMessage("#matchesActivityIntentPair must be true because intent.package matches")
            .that(filter.matchesActivityIntentPair(activity, intent)).isTrue()

        intent.component = component1

        assertWithMessage("#matchesActivityIntentPair must be false because intent.component" +
            " doesn't match")
            .that(filter.matchesActivityIntentPair(activity, intent)).isFalse()
    }
}