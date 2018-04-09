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

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import androidx.navigation.testing.TestNavigator
import androidx.navigation.testing.TestNavigatorProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavDestinationTest {
    private val provider = TestNavigatorProvider(InstrumentationRegistry.getTargetContext())

    @Test
    fun navDestination() {
        val destination = provider.navDestination(DESTINATION_ID) { }
        assertEquals("NavDestination should have id set",
                DESTINATION_ID, destination.id)
    }

    @Test
    fun navDestinationLabel() {
        val destination = provider.navDestination(DESTINATION_ID) {
            label = LABEL
        }
        assertEquals("NavDestination should have label set",
                LABEL, destination.label)
    }

    @Test
    fun navDestinationDefaultArguments() {
        val arguments = Bundle()
        val destination = provider.navDestination(DESTINATION_ID) {
            defaultArguments = arguments
        }
        assertEquals("NavDestination should have default arguments set",
                arguments, destination.defaultArguments)
    }

    @Test
    fun navDestinationAction() {
        val destination = provider.navDestination(DESTINATION_ID) {
            action(ACTION_ID) {
                destinationId = DESTINATION_ID
                navOptions {
                    popUpTo = DESTINATION_ID
                }
            }
        }
        val action = destination.getAction(ACTION_ID)
        assertNotNull("NavDestination should have action that was added", action)
        assertEquals("NavAction should have NavOptions set",
                DESTINATION_ID, action?.navOptions?.popUpTo)
    }
}

private const val DESTINATION_ID = 1
private const val LABEL = "TEST"
private const val ACTION_ID = 1

/**
 * Instead of constructing a NavGraph from the NavigatorProvider, construct
 * a NavDestination directly to allow for testing NavDestinationBuilder in
 * isolation.
 */
fun NavigatorProvider.navDestination(
        @IdRes id: Int,
        block: NavDestinationBuilder<NavDestination>.() -> Unit
): NavDestination = NavDestinationBuilder(this[TestNavigator::class], id).apply(block).build()
