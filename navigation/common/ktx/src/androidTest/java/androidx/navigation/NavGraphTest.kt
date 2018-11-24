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
import androidx.test.runner.AndroidJUnit4
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
            .isSameAs(destination)
    }

    @Test
    fun minusAssign() {
        val graph = NavGraph(navGraphNavigator)
        val destination = navigator.createDestination().apply { id = DESTINATION_ID }
        graph += destination
        assertWithMessage("plusAssign destination should be retrieved with get")
            .that(graph[DESTINATION_ID])
            .isSameAs(destination)
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

    @Test(expected = IllegalArgumentException::class)
    fun getIllegalArgumentException() {
        val graph = NavGraph(navGraphNavigator)
        graph[DESTINATION_ID]
    }
}

private const val DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2
