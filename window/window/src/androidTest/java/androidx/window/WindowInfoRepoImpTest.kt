/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window

import android.app.Activity
import androidx.core.util.Consumer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
public class WindowInfoRepoImpTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val testScope = TestCoroutineScope()

    @Test
    public fun testGetCurrentWindowMetrics() {
        activityScenario.scenario.onActivity { testActivity ->
            val windowBoundsHelper = WindowBoundsHelper.instance
            val repo = WindowInfoRepoImp(
                testActivity,
                windowBoundsHelper,
                FakeWindowBackend()
            )
            val expectedBounds = windowBoundsHelper.computeCurrentWindowBounds(testActivity)
            val expected = WindowMetrics(expectedBounds)
            val actual = repo.currentWindowMetrics
            assertEquals(expected, actual)
        }
    }

    @Test
    public fun testGetMaximumWindowMetrics() {
        activityScenario.scenario.onActivity { testActivity ->
            val windowBoundsHelper = WindowBoundsHelper.instance
            val repo = WindowInfoRepoImp(
                testActivity,
                windowBoundsHelper,
                FakeWindowBackend()
            )
            val expectedBounds = windowBoundsHelper.computeMaximumWindowBounds(testActivity)
            val expected = WindowMetrics(expectedBounds)
            val actual = repo.maximumWindowMetrics
            assertEquals(expected, actual)
        }
    }

    @Test
    public fun testWindowLayoutFeatures(): Unit = testScope.runBlockingTest {
        activityScenario.scenario.onActivity { testActivity ->
            val windowBoundsHelper = WindowBoundsHelper.instance
            val fakeBackend = FakeWindowBackend()
            val repo = WindowInfoRepoImp(
                testActivity,
                windowBoundsHelper,
                fakeBackend
            )
            val collector = TestConsumer<WindowLayoutInfo>()
            testScope.launch {
                repo.windowLayoutInfo.collect(collector::accept)
            }
            fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))
            collector.assertValue(WindowLayoutInfo(emptyList()))
        }
    }

    private class FakeWindowBackend : WindowBackend {

        private class CallbackHolder(
            val executor: Executor,
            val callback: Consumer<WindowLayoutInfo>
        ) : Consumer<WindowLayoutInfo> {

            override fun accept(t: WindowLayoutInfo?) {
                executor.execute { callback.accept(t) }
            }
        }

        private val consumers = mutableListOf<CallbackHolder>()

        fun triggerSignal(info: WindowLayoutInfo) {
            consumers.forEach { consumer -> consumer.accept(info) }
        }

        override fun registerLayoutChangeCallback(
            activity: Activity,
            executor: Executor,
            callback: Consumer<WindowLayoutInfo>
        ) {
            consumers.add(CallbackHolder(executor, callback))
        }

        override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
            consumers.removeIf { it.callback == callback }
        }
    }
}