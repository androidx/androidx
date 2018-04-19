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

import android.support.annotation.IdRes
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import androidx.navigation.testing.TestNavigator
import androidx.navigation.testing.TestNavigatorProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavGraphBuilderTest {
    private val provider = TestNavigatorProvider(InstrumentationRegistry.getTargetContext())

    @Test
    fun navigation() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            navDestination(DESTINATION_ID) {}
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }

    @Test
    fun navigationUnaryPlus() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            +NavDestination(provider[TestNavigator::class]).apply {
                id = DESTINATION_ID
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }

    @Test
    fun navigationAddDestination() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            val destination = NavDestination(provider[TestNavigator::class]).apply {
                id = DESTINATION_ID
            }
            addDestination(destination)
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }

    @Test(expected = IllegalStateException::class)
    fun navigationMissingStartDestination() {
        provider.navigation(startDestination = 0) {
            navDestination(DESTINATION_ID) {}
        }
        fail("NavGraph should throw IllegalStateException if startDestination is zero")
    }

    @Test
    fun navigationNested() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            navigation(DESTINATION_ID, startDestination = SECOND_DESTINATION_ID) {
                navDestination(SECOND_DESTINATION_ID) {}
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }
}

private const val DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be
 * added to a NavGraph (hence why this is not in the common-ktx library)
 */
fun NavGraphBuilder.navDestination(
        @IdRes id: Int,
        block: NavDestinationBuilder<NavDestination>.() -> Unit
) = destination(NavDestinationBuilder(provider[TestNavigator::class], id).apply(block))
