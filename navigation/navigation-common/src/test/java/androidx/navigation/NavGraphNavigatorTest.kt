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

import androidx.annotation.IdRes
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.testing.TestNavigatorState
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavGraphNavigatorTest {

    companion object {
        @IdRes
        private const val FIRST_DESTINATION_ID = 1
        @IdRes
        private const val SECOND_DESTINATION_ID = 2
    }

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var provider: NavigatorProvider
    private lateinit var noOpState: TestNavigatorState
    private lateinit var noOpNavigator: NoOpNavigator
    private lateinit var navGraphState: TestNavigatorState
    private lateinit var navGraphNavigator: NavGraphNavigator

    @Before
    fun setup() {
        provider = NavigatorProvider().apply {
            addNavigator(NoOpNavigator().also { noOpNavigator = it })
            addNavigator(
                NavGraphNavigator(this).also {
                    navGraphNavigator = it
                }
            )
        }
        noOpState = TestNavigatorState()
        noOpNavigator.onAttach(noOpState)
        navGraphState = TestNavigatorState()
        navGraphNavigator.onAttach(navGraphState)
    }

    private fun createFirstDestination() = noOpNavigator.createDestination().apply {
        id = FIRST_DESTINATION_ID
    }

    private fun createSecondDestination() = noOpNavigator.createDestination().apply {
        id = SECOND_DESTINATION_ID
    }

    private fun createGraphWithDestination(
        destination: NavDestination,
        startId: Int = destination.id
    ) = navGraphNavigator.createDestination().apply {
        addDestination(destination)
        setStartDestination(startId)
    }

    @Test(expected = IllegalStateException::class)
    fun navigateWithoutStartDestination() {
        val destination = createFirstDestination()
        val graph = navGraphNavigator.createDestination().apply {
            addDestination(destination)
            id = 2 // can't match id of first destination or the start destination
            setStartDestination(0)
        }
        val entry = navGraphState.createBackStackEntry(graph, null)
        navGraphNavigator.navigate(listOf(entry), null, null)
    }

    @Test
    fun navigate() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)
        val entry = navGraphState.createBackStackEntry(graph, null)
        navGraphNavigator.navigate(listOf(entry), null, null)
        assertThat(noOpState.backStack.value.map { it.destination })
            .containsExactly(destination)
    }
}
