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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.TestNavigatorProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class NavInflaterBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = ApplicationProvider.getApplicationContext() as android.content.Context

    private var navInflater: NavInflater = NavInflater(context, TestNavigatorProvider())

    @Test
    fun inflateSimple() {
        benchmarkRule.measureRepeated {
            navInflater.inflate(androidx.navigation.benchmark.test.R.navigation.nav_simple)
        }
    }

    @Test
    fun inflateDeepLink() {
        benchmarkRule.measureRepeated {
            navInflater.inflate(androidx.navigation.benchmark.test.R.navigation.nav_deep_link)
        }
    }
}
