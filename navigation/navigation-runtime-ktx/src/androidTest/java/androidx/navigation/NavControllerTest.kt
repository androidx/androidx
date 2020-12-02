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
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class NavControllerTest {
    private val navController =
        NavController(ApplicationProvider.getApplicationContext() as Context).apply {
            navigatorProvider += TestNavigator()
        }

    @Test
    fun createGraph() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID)
        }
        assertTrue(
            "Destination should be added to the graph",
            DESTINATION_ID in graph
        )
    }

    @Test
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun currentBackStackEntryFlow() = runBlocking {
        navController.graph = navController.createGraph(startDestination = 1) {
            test(1)
            test(2)
            test(3)
        }

        navController.currentBackStackEntryFlow
            .take(navController.graph.count())
            .withIndex()
            .onEach { (index, backStackEntry) ->
                val expectedDestination = index + 1
                assertWithMessage("Flow emitted unexpected back stack entry (wrong destination)")
                    .that(backStackEntry.destination.id)
                    .isEqualTo(expectedDestination)

                if (expectedDestination < navController.graph.count()) {
                    navController.navigate(expectedDestination + 1)
                }
            }
            .collect()
    }
}

private const val DESTINATION_ID = 1
