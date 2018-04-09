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

package androidx.navigation.testing

import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import androidx.navigation.contains
import androidx.navigation.get
import androidx.navigation.navigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TestNavigatorDestinationBuilderTest {
    private val provider = TestNavigatorProvider(InstrumentationRegistry.getTargetContext())

    @Test
    fun test() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID)
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
    }

    @Test
    fun testWithBody() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID) {
                label = LABEL
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
        assertEquals("Destination should have label set",
                LABEL, graph[DESTINATION_ID].label)
    }
}

private const val DESTINATION_ID = 1
private const val LABEL = "Test"
