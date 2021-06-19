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
import com.google.common.truth.Truth.assertWithMessage
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
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun testRoute() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            test(DESTINATION_ROUTE)
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    fun testWithBody() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID) {
                label = LABEL
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("Destination should have label set")
            .that(graph[DESTINATION_ID].label)
            .isEqualTo(LABEL)
    }

    @Test
    fun testRouteWithBody() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            test(DESTINATION_ROUTE) {
                label = LABEL
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("Destination should have label set")
            .that(graph[DESTINATION_ROUTE].label)
            .isEqualTo(LABEL)
    }
}

private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "route"
private const val LABEL = "Test"
