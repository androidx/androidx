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

import android.content.Context
import android.support.annotation.IdRes
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(JUnit4::class)
@SmallTest
class NavGraphNavigatorTest {

    companion object {
        @IdRes
        private const val FIRST_DESTINATION_ID = 1
        @IdRes
        private const val SECOND_DESTINATION_ID = 2
    }

    private lateinit var provider: NavigatorProvider
    private lateinit var noOpNavigator: NoOpNavigator
    private lateinit var navGraphNavigator: NavGraphNavigator
    private lateinit var listener: Navigator.OnNavigatorNavigatedListener

    @Before
    fun setup() {
        provider = NavigatorProvider().apply {
            addNavigator(NoOpNavigator().also { noOpNavigator = it })
            addNavigator(NavGraphNavigator(mock(Context::class.java)).also {
                navGraphNavigator = it
            })
        }
        listener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        navGraphNavigator.addOnNavigatorNavigatedListener(listener)
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
        startDestination = startId
    }

    @Test(expected = IllegalStateException::class)
    fun navigateWithoutStartDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination, startId = 0)
        navGraphNavigator.navigate(graph, null, null, null)
    }

    @Test
    fun navigate() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)
        navGraphNavigator.navigate(graph, null, null, null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun popWithEmptyStack() {
        val success = navGraphNavigator.popBackStack()
        assertWithMessage("popBackStack should return false on an empty stack")
            .that(success)
            .isFalse()
    }

    @Test
    fun navigateThenPop() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)
        navGraphNavigator.navigate(graph, null, null, null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        val success = navGraphNavigator.popBackStack()
        assertWithMessage("popBackStack should return true")
            .that(success)
            .isTrue()
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_POPPED)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun navigateSingleTopOnEmptyStack() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)
        // singleTop should still show as added on an empty stack
        navGraphNavigator.navigate(graph, null,
            NavOptions.Builder().setLaunchSingleTop(true).build(), null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun navigateSingleTop() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)
        navGraphNavigator.navigate(graph, null, null, null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        navGraphNavigator.navigate(graph, null,
            NavOptions.Builder().setLaunchSingleTop(true).build(), null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun navigateSingleTopNotTop() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)
        val secondDestination = createSecondDestination()
        val secondGraph = createGraphWithDestination(secondDestination).apply {
            id = SECOND_DESTINATION_ID
        }
        navGraphNavigator.navigate(graph, null, null, null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        navGraphNavigator.navigate(secondGraph, null,
            NavOptions.Builder().setLaunchSingleTop(true).build(), null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                secondGraph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun navigateSingleTopNested() {
        val destination = createFirstDestination()
        val nestedGraph = createGraphWithDestination(destination).apply {
            id = FIRST_DESTINATION_ID
        }
        val graph = createGraphWithDestination(nestedGraph)
        navGraphNavigator.navigate(graph, null, null, null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                nestedGraph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)
        navGraphNavigator.navigate(graph, null,
            NavOptions.Builder().setLaunchSingleTop(true).build(), null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_UNCHANGED)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                nestedGraph.id,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(listener)
    }
}
