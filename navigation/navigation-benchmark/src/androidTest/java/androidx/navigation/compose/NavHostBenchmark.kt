/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class NavHostBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun simpleBasicText() {
        val basicTextFactory = { BasicTextTestCase() }
        benchmarkRule.benchmarkToFirstPixel(basicTextFactory)
    }

    @Test
    fun singleComposable() {
        benchmarkRule.benchmarkToFirstPixel { NavHostTestCase() }
    }

    @Test
    fun twoComposable() {
        benchmarkRule.benchmarkToFirstPixel { NavHost2TestCase() }
    }

    @Test
    fun tenComposable() {
        benchmarkRule.benchmarkToFirstPixel { NavHost10TestCase() }
    }

    private class BasicTextTestCase() : LayeredComposeTestCase() {
        @Composable
        override fun MeasuredContent() {
            BasicText("Test")
        }
    }

    private class NavHostTestCase() : LayeredComposeTestCase() {

        @Composable
        override fun MeasuredContent() {
            val navController = rememberNavController()
            NavHost(navController, "first") { composable("first") { BasicText("Test") } }
        }
    }

    private class NavHost2TestCase() : LayeredComposeTestCase() {

        @Composable
        override fun MeasuredContent() {
            val navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") { BasicText("Test") }
                composable("second") { BasicText("Test") }
            }
        }
    }

    private class NavHost10TestCase() : LayeredComposeTestCase() {

        @Composable
        override fun MeasuredContent() {
            val navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") { BasicText("Test") }
                composable("second") { BasicText("Test") }
                composable("third") { BasicText("Test") }
                composable("forth") { BasicText("second") }
                composable("fifth") { BasicText("Test") }
                composable("sixth") { BasicText("second") }
                composable("seventh") { BasicText("Test") }
                composable("eighth") { BasicText("second") }
                composable("ninth") { BasicText("Test") }
                composable("tenth") { BasicText("second") }
            }
        }
    }
}
