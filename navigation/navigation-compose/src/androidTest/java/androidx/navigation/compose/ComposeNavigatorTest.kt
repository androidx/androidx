/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.navigation.testing.TestNavigatorState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeNavigatorTest {

    @Test
    fun testBackStackPriorToAttach() {
        val navigator = ComposeNavigator()
        val beforeAttachBackStack = navigator.backStack
        assertThat(beforeAttachBackStack.value)
            .isEmpty()

        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)
        val afterAttachBackStack = navigator.backStack
        assertThat(afterAttachBackStack)
            .isNotSameInstanceAs(beforeAttachBackStack)
    }

    @Test
    fun testNavigateAndPopUpdatesBackStack() {
        val navigator = ComposeNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)

        val entry = navigatorState.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(entry), null, null)
        assertThat(navigator.backStack.value)
            .containsExactly(entry).inOrder()

        val secondEntry = navigatorState.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(navigator.backStack.value)
            .containsExactly(entry, secondEntry).inOrder()

        navigator.popBackStack(secondEntry, false)
        assertThat(navigator.backStack.value)
            .containsExactly(entry).inOrder()
    }
}
