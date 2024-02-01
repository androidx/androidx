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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavGraphTest {
    private val navGraphNavigator = NavGraphNavigator(mock(NavigatorProvider::class.java))
    private val navigator = NoOpNavigator()

    @Test
    fun plusAssign() {
        val graph = NavGraph(navGraphNavigator)
        val destination = navigator.createDestination().apply { id = DESTINATION_ID }
        graph += destination
        assertWithMessage("plusAssign destination should be retrieved with get")
            .that(graph[DESTINATION_ID])
            .isSameInstanceAs(destination)
    }

    @Test
    fun minusAssign() {
        val graph = NavGraph(navGraphNavigator)
        val destination = navigator.createDestination().apply { id = DESTINATION_ID }
        graph += destination
        assertWithMessage("plusAssign destination should be retrieved with get")
            .that(graph[DESTINATION_ID])
            .isSameInstanceAs(destination)
        graph -= destination
        assertWithMessage("Destination should be removed after minusAssign")
            .that(DESTINATION_ID in graph)
            .isFalse()
    }

    @Test
    fun plusAssignGraph() {
        val graph = NavGraph(navGraphNavigator)
        val other = NavGraph(navGraphNavigator)
        other += navigator.createDestination().apply { id = DESTINATION_ID }
        other += navigator.createDestination().apply { id = SECOND_DESTINATION_ID }
        graph += other
        assertWithMessage("NavGraph should have destination1 from other")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("other nav graph should not have destination1")
            .that(DESTINATION_ID in other)
            .isFalse()

        assertWithMessage("NavGraph should have destination2 from other")
            .that(SECOND_DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("other nav graph should not have destination2")
            .that(SECOND_DESTINATION_ID in other)
            .isFalse()
    }

    @Test
    fun plusAssignGraphRoute() {
        val graph = NavGraph(navGraphNavigator)
        val other = NavGraph(navGraphNavigator)
        other += navigator.createDestination().apply { route = DESTINATION_ROUTE }
        other += navigator.createDestination().apply { route = SECOND_DESTINATION_ROUTE }
        graph += other
        assertWithMessage("NavGraph should have destination1 from other")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("other nav graph should not have destination1")
            .that(DESTINATION_ROUTE in other)
            .isFalse()

        assertWithMessage("NavGraph should have destination2 from other")
            .that(SECOND_DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("other nav graph should not have destination2")
            .that(SECOND_DESTINATION_ROUTE in other)
            .isFalse()
    }

    @Test
    fun graphEqualsId() {
        val graph = NavGraph(navGraphNavigator)
        graph += navigator.createDestination().apply { id = DESTINATION_ID }
        graph += navigator.createDestination().apply { id = SECOND_DESTINATION_ID }
        val other = NavGraph(navGraphNavigator)
        other += navigator.createDestination().apply { id = DESTINATION_ID }
        other += navigator.createDestination().apply { id = SECOND_DESTINATION_ID }

        assertWithMessage("Graphs should be equal")
            .that(graph)
            .isEqualTo(other)
    }

    @Test
    fun graphNotEqualsId() {
        val graph = NavGraph(navGraphNavigator)
        graph += navigator.createDestination().apply { id = DESTINATION_ID }
        graph += navigator.createDestination().apply { id = SECOND_DESTINATION_ID }
        val other = NavGraph(navGraphNavigator)
        other += navigator.createDestination().apply { id = DESTINATION_ID }
        other += navigator.createDestination().apply { id = 3 }

        assertWithMessage("Graphs should not be equal")
            .that(graph)
            .isNotEqualTo(other)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getIllegalArgumentException() {
        val graph = NavGraph(navGraphNavigator)
        graph[DESTINATION_ID]
    }
}

private const val DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2
private const val DESTINATION_ROUTE = "first"
private const val SECOND_DESTINATION_ROUTE = "second"
