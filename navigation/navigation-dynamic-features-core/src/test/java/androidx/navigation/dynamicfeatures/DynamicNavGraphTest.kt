/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures

import androidx.navigation.NavDestination
import androidx.navigation.NavigatorProvider
import androidx.navigation.NoOpNavigator
import androidx.navigation.dynamicfeatures.DynamicGraphNavigator.DynamicNavGraph
import androidx.navigation.dynamicfeatures.shared.TestDynamicInstallManager
import androidx.test.filters.SmallTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class DynamicNavGraphTest {

    private val progressId = 1
    private lateinit var provider: NavigatorProvider
    private lateinit var navigator: DynamicGraphNavigator
    private lateinit var dynamicNavGraph: DynamicNavGraph
    private lateinit var noOpNavigator: NoOpNavigator

    @Before
    fun setup() {
        provider = NavigatorProvider()
        noOpNavigator = NoOpNavigator()
        navigator = DynamicGraphNavigator(provider,
            TestDynamicInstallManager()
        )
        provider.addNavigator(noOpNavigator)
        dynamicNavGraph = navigator.createDestination()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetOrThrow_NoParent() {
        DynamicNavGraph.getOrThrow(noOpNavigator.createDestination())
    }

    @Test
    fun testGetOrThrow_CorrectParent() {
        setupProgressDestination(noOpNavigator.createDestination().apply {
            id = progressId
        })
        val progressDestination = navigator.navigateToProgressDestination(dynamicNavGraph, null)
        assertNotNull(progressDestination)
        progressDestination?.let {
            DynamicNavGraph.getOrThrow(progressDestination)
            // Assume not having thrown an exception
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testNavigateToProgressDestination_withoutProgressDestination() {
        setupProgressDestination(null)
        navigator.navigateToProgressDestination(dynamicNavGraph, null)
    }

    @Test
    fun testNavigateToProgressDestination_withProviderAndDestination() {
        setupProgressDestination(noOpNavigator.createDestination().apply {
            id = progressId
        })
        val destination = navigator.navigateToProgressDestination(dynamicNavGraph, null)
        assertTrue(destination?.parent is DynamicNavGraph)
    }

    private fun setupProgressDestination(progressDestination: NavDestination?) {
        progressDestination?.let {
            navigator.installDefaultProgressDestination { it }
        }
        provider.addNavigator(navigator)
        dynamicNavGraph = navigator.createDestination()
    }
}
