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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavDeepLinkBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun navGraphDestinations_withRoutes() = inflateNavGraph_withRoutes(1)

    @Test
    fun navGraphDestinations_withRoutes10() = inflateNavGraph_withRoutes(10)

    @Test
    fun navGraphDestinations_withRoutes50() = inflateNavGraph_withRoutes(50)

    @Test
    fun navGraphDestinations_withRoutes100() = inflateNavGraph_withRoutes(100)

    private fun inflateNavGraph_withRoutes(count: Int) {
        val navigatorProvider = NavigatorProvider().apply {
            addNavigator(NavGraphNavigator(this))
            addNavigator(NoOpNavigator())
        }
        val navigator = navigatorProvider.getNavigator(NoOpNavigator::class.java)
        benchmarkRule.measureRepeated {
            navigatorProvider.getNavigator(NavGraphNavigator::class.java)
                .createDestination().apply {
                    id = GRAPH_ID
                    setStartDestination(START_DESTINATION_ID)
                    for (i in 0 until count) {
                        addDestination(
                            navigator.createDestination().apply {
                                route = URI_PATH + i + URI_EXTRAS
                                id = i
                            }
                        )
                    }
                }
        }
    }

    companion object {
        const val URI_PATH = "example.com/"
        const val URI_EXTRAS = "test/{test}?param1={param}#fragment"
        const val START_DESTINATION_ID = 0
        const val GRAPH_ID = 111
    }
}
