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

package androidx.window.layout

import android.app.Activity
import androidx.core.util.Consumer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.TestConsumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Job
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
public class WindowInfoTrackerImplTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    public fun testWindowLayoutFeatures(): Unit = testScope.runTest {
        activityScenario.scenario.onActivity { testActivity ->
            val windowMetricsCalculator = WindowMetricsCalculatorCompat
            val fakeBackend = FakeWindowBackend()
            val repo = WindowInfoTrackerImpl(
                windowMetricsCalculator,
                fakeBackend
            )
            val collector = TestConsumer<WindowLayoutInfo>()
            testScope.launch(Job()) {
                repo.windowLayoutInfo(testActivity).collect(collector::accept)
            }
            fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))
            collector.assertValue(WindowLayoutInfo(emptyList()))
        }
    }

    @Test
    public fun testWindowLayoutFeatures_multicasting(): Unit = testScope.runTest {
        activityScenario.scenario.onActivity { testActivity ->
            val windowMetricsCalculator = WindowMetricsCalculatorCompat
            val fakeBackend = FakeWindowBackend()
            val repo = WindowInfoTrackerImpl(
                windowMetricsCalculator,
                fakeBackend
            )
            val collector = TestConsumer<WindowLayoutInfo>()
            val job = Job()
            launch(job) {
                repo.windowLayoutInfo(testActivity).collect(collector::accept)
            }
            launch(job) {
                repo.windowLayoutInfo(testActivity).collect(collector::accept)
            }
            fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))
            collector.assertValues(WindowLayoutInfo(emptyList()), WindowLayoutInfo(emptyList()))
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
            consumers.remove(callback)
        }
    }
}
