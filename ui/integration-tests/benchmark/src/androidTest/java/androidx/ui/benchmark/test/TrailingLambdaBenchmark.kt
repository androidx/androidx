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

package androidx.ui.benchmark.test

import android.app.Activity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.State
import androidx.test.filters.LargeTest
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.rule.ActivityTestRule
import androidx.ui.benchmark.measureFirstCompose
import androidx.ui.benchmark.toggleStateMeasureRecompose
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ToggleableTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class TrailingLambdaBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    private val activity: Activity get() = activityRule.activity

    @Test
    fun withTrailingLambdas_compose() {
        benchmarkRule.measureFirstCompose(activity, WithTrailingLambdas(activity))
    }

    @Test
    fun withTrailingLambdas_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity, WithTrailingLambdas(activity))
    }

    @Test
    fun withoutTrailingLambdas_compose() {
        benchmarkRule.measureFirstCompose(activity, WithoutTrailingLambdas(activity))
    }

    @Test
    fun withoutTrailingLambdas_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity, WithoutTrailingLambdas(activity))
    }
}

private sealed class TrailingLambdaTestCase(activity: Activity) : ComposeTestCase(activity),
    ToggleableTestCase {

    var numberState: State<Int>? = null

    override fun setComposeContent(activity: Activity) = activity.setContent {
        val number = +state { 5 }
        numberState = number

        val content = @Composable {
            Container(width = 10.dp, height = 10.dp) {}
        }

        Column {
            repeat(10) {
                emitContent(number = number.value, content = content)
            }
        }
    }!!

    override fun toggleState() {
        with(numberState!!) {
            value = if (value == 5) 10 else 5
        }
        FrameManager.nextFrame()
    }

    @Composable
    abstract fun emitContent(number: Int, content: @Composable() () -> Unit)
}

private class WithTrailingLambdas(activity: Activity) : TrailingLambdaTestCase(activity) {
    @Composable
    override fun emitContent(number: Int, content: @Composable() () -> Unit) {
        EmptyComposable(number = number) {
            content()
        }
    }
}

private class WithoutTrailingLambdas(activity: Activity) : TrailingLambdaTestCase(activity) {
    @Composable
    override fun emitContent(number: Int, content: @Composable() () -> Unit) {
        EmptyComposable(number = number, children = content)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun EmptyComposable(number: Int, children: @Composable() () -> Unit) {}
