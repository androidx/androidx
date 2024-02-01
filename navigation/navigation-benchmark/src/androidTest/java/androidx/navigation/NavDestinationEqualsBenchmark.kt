/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class NavDestinationEqualsBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Suppress("UnusedEquals")
    @Test
    fun navGraphEquals_100() {
        val navigatorProvider = NavigatorProvider().apply {
            addNavigator(NavGraphNavigator(this))
            addNavigator(NoOpNavigator())
        }
        val graph1 = navigatorProvider.createGraph(100)
        val graph2 = navigatorProvider.createGraph(100)
        benchmarkRule.measureRepeated {
            graph1 == graph2
        }
    }

    private fun NavigatorProvider.createGraph(
        count: Int
    ) = getNavigator(NavGraphNavigator::class.java).createDestination().apply {
        id = GRAPH_ID
        setStartDestination(START_DESTINATION_ID)
        val navigator = getNavigator(NoOpNavigator::class.java)
        for (i in 0 until count) {
            addDestination(
                navigator.createDestination().apply {
                    route = URI_PATH + i + URI_EXTRAS
                    id = i
                }
            )
        }
    }

    companion object {
        const val URI_PATH = "example.com/"
        const val URI_EXTRAS = "test/{test}?param1={param}#fragment"
        const val START_DESTINATION_ID = 0
        const val GRAPH_ID = 111
    }
}
