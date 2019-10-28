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

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ActivityNavigatorDestinationBuilderTest {
    private val navController =
        NavController(ApplicationProvider.getApplicationContext() as android.content.Context)

    @Test
    fun activity() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                label = LABEL
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
        assertEquals("Destination should have label set",
                LABEL,
                graph[DESTINATION_ID].label)
    }

    @Test
    fun activityPackage() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                targetPackage = PACKAGE_NAME
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("Destination should have package name set")
            .that((graph[DESTINATION_ID] as ActivityNavigator.Destination).targetPackage)
            .isEqualTo(PACKAGE_NAME)
    }

    @Test
    fun activityClass() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                activityClass = TestActivity::class
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
        assertEquals("Destination should have ComponentName set",
                TestActivity::class.java.name,
                (graph[DESTINATION_ID] as ActivityNavigator.Destination).component?.className)
    }

    @Test
    fun action() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                action = ACTION
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
        assertEquals("Destination should have action set",
                ACTION,
                (graph[DESTINATION_ID] as ActivityNavigator.Destination).action)
    }

    @Test
    fun data() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                data = DATA
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
        assertEquals("Destination should have data set",
                DATA,
                (graph[DESTINATION_ID] as ActivityNavigator.Destination).data)
    }

    @Test
    fun dataPattern() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                dataPattern = DATA_PATTERN
            }
        }
        assertTrue("Destination should be added to the graph",
                DESTINATION_ID in graph)
        assertEquals("Destination should have data pattern set",
                DATA_PATTERN,
                (graph[DESTINATION_ID] as ActivityNavigator.Destination).dataPattern)
    }
}

private const val DESTINATION_ID = 1
private const val PACKAGE_NAME = "com.example"
private const val LABEL = "Test"
private const val ACTION = "ACTION_TEST"
private val DATA = Uri.parse("http://www.example.com")
private const val DATA_PATTERN = "http://www.example.com/{id}"
