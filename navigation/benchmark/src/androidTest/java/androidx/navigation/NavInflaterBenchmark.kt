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

import androidx.benchmark.BenchmarkRule
import androidx.navigation.testing.TestNavigatorProvider
import androidx.test.InstrumentationRegistry
import androidx.test.filters.MediumTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class NavInflaterBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getContext()

    private var navInflater: NavInflater = NavInflater(context, TestNavigatorProvider())

    @Test
    fun inflateSimple() {
        val state = benchmarkRule.state
        while (state.keepRunning()) {
            navInflater.inflate(androidx.navigation.benchmark.test.R.navigation.nav_simple)
        }
    }

    @Test
    fun inflateDeepLink() {
        val state = benchmarkRule.state
        while (state.keepRunning()) {
            navInflater.inflate(androidx.navigation.benchmark.test.R.navigation.nav_deep_link)
        }
    }
}
