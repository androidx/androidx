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

import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import androidx.navigation.testing.TestNavigator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavGraphTest {
    private val navGraphNavigator = NavGraphNavigator(InstrumentationRegistry.getTargetContext())
    private val navigator = TestNavigator()

    @Test
    fun plusAssign() {
        val graph = NavGraph(navGraphNavigator)
        val destination = NavDestination(navigator).apply { id = DESTINATION_ID }
        graph += destination
        assertSame("plusAssign destination should be retrieved with get", destination,
                graph[DESTINATION_ID])
    }

    @Test
    fun minusAssign() {
        val graph = NavGraph(navGraphNavigator)
        val destination = NavDestination(navigator).apply { id = DESTINATION_ID }
        graph += destination
        assertSame("plusAssign destination should be retrieved with get", destination,
                graph[DESTINATION_ID])
        graph -= destination
        assertFalse("Destination should be removed after minusAssign",
                DESTINATION_ID in graph)
    }

    @Test
    fun plusAssignGraph() {
        val graph = NavGraph(navGraphNavigator)
        val other = NavGraph(navGraphNavigator)
        other += NavDestination(navigator).apply { id = DESTINATION_ID }
        other += NavDestination(navigator).apply { id = SECOND_DESTINATION_ID }
        graph += other
        assertTrue("NavGraph should have destination1 from other",
                DESTINATION_ID in graph)
        assertFalse("other nav graph should not have destination1",
                DESTINATION_ID in other)

        assertTrue("NavGraph should have destination2 from other",
                SECOND_DESTINATION_ID in graph)
        assertFalse("other nav graph should not have destination2",
                SECOND_DESTINATION_ID in other)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getIllegalArgumentException() {
        val graph = NavGraph(navGraphNavigator)
        graph[DESTINATION_ID]
    }
}

private const val DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2
