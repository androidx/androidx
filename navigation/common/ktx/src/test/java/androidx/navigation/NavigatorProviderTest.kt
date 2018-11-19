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

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class NavigatorProviderTest {
    private val provider = NavigatorProvider()

    @Test
    fun set() {
        val navigator = NoOpNavigator()
        provider[NAME] = navigator
        val foundNavigator: Navigator<NavDestination> = provider[NAME]
        assertWithMessage("Set destination should be retrieved with get")
            .that(foundNavigator)
            .isSameAs(navigator)
    }

    @Test
    fun plusAssign() {
        val navigator = NoOpNavigator()
        provider += navigator
        assertWithMessage("Set destination should be retrieved with get")
            .that(provider[NoOpNavigator::class])
            .isSameAs(navigator)
    }
}

private const val NAME = "TEST"
