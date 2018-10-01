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

import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

import android.support.annotation.IdRes

import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest

import org.junit.Before
import org.junit.Test

@SmallTest
class NavGraphNavigatorStateTest {

    companion object {
        @IdRes
        private const val FIRST_DESTINATION_ID = 1
    }

    private lateinit var navGraphNavigator: NavGraphNavigator
    private lateinit var listener: Navigator.OnNavigatorNavigatedListener

    @Before
    fun setup() {
        navGraphNavigator = NavGraphNavigator(InstrumentationRegistry.getTargetContext())
        listener = mock(Navigator.OnNavigatorNavigatedListener::class.java)
        navGraphNavigator.addOnNavigatorNavigatedListener(listener)
    }

    @Test
    fun navigateSingleTopSaveState() {
        val destination = NavDestination(mock(Navigator::class.java)).apply {
            id = FIRST_DESTINATION_ID
        }
        val graph = navGraphNavigator.createDestination().apply {
            addDestination(destination)
            startDestination = FIRST_DESTINATION_ID
        }
        graph.navigate(null, null, null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_DESTINATION_ADDED)

        // Save and restore the state, effectively resetting the NavGraphNavigator
        val saveState = navGraphNavigator.onSaveState()
        navGraphNavigator.onRestoreState(saveState)

        graph.navigate(null, NavOptions.Builder().setLaunchSingleTop(true).build(), null)
        verify(listener).onNavigatorNavigated(navGraphNavigator,
                graph.id,
                Navigator.BACK_STACK_UNCHANGED)
        verifyNoMoreInteractions(listener)
    }
}
