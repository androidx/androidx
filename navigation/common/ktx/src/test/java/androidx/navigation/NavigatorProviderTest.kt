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

package androidx.navigation

import android.support.test.filters.SmallTest
import androidx.navigation.testing.TestNavigator
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class NavigatorProviderTest {
    private val provider = SimpleNavigatorProvider()

    @Test
    fun set() {
        val navigator = TestNavigator()
        provider[NAME] = navigator
        val foundNavigator: Navigator<NavDestination> = provider[NAME]
        assertSame("Set destination should be retrieved with get", navigator,
                foundNavigator)
    }

    @Test
    fun plusAssign() {
        val navigator = TestNavigator()
        provider += navigator
        assertSame("Set destination should be retrieved with get", navigator,
                provider[TestNavigator::class])
    }
}

private const val NAME = "TEST"
