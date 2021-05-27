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

package androidx.testutils

import androidx.navigation.contains
import androidx.navigation.get
import androidx.navigation.navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TestNavigatorDestinationBuilderTest {
    private val provider = TestNavigatorProvider()

    @Suppress("DEPRECATION")
    @Test
    fun test() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID)
        }
        assertTrue(
            "Destination should be added to the graph",
            DESTINATION_ID in graph
        )
    }

    @Test
    fun testRoute() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            test(DESTINATION_ROUTE)
        }
        assertTrue(
            "Destination should be added to the graph",
            DESTINATION_ROUTE in graph
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun testWithBody() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID) {
                label = LABEL
            }
        }
        assertTrue(
            "Destination should be added to the graph",
            DESTINATION_ID in graph
        )
        assertEquals(
            "Destination should have label set",
            LABEL, graph[DESTINATION_ID].label
        )
    }

    @Test
    fun testRouteWithBody() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            test(DESTINATION_ROUTE) {
                label = LABEL
            }
        }
        assertTrue(
            "Destination should be added to the graph",
            DESTINATION_ROUTE in graph
        )
        assertEquals(
            "Destination should have label set",
            LABEL, graph[DESTINATION_ROUTE].label
        )
    }
}

private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "route"
private const val LABEL = "Test"
