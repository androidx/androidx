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

import android.content.Context
import android.os.Build
import androidx.core.util.Consumer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.TestConsumer
import androidx.window.WindowTestUtils
import androidx.window.WindowTestUtils.Companion.assumeAtLeastVendorApiLevel
import androidx.window.layout.adapter.WindowBackend
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
public class WindowInfoTrackerImplTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val testScope = TestScope(UnconfinedTestDispatcher())

    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

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
    public fun testWindowLayoutFeatures_contextAsListener(): Unit = testScope.runTest {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@runTest
        }
        assumeAtLeastVendorApiLevel(2)
        val fakeBackend = FakeWindowBackend()
        val repo = WindowInfoTrackerImpl(WindowMetricsCalculatorCompat, fakeBackend)
        val collector = TestConsumer<WindowLayoutInfo>()

        val windowContext =
            WindowTestUtils.createOverlayWindowContext()
        testScope.launch(Job()) {
            repo.windowLayoutInfo(windowContext).collect(collector::accept)
        }
        fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))
        collector.assertValue(WindowLayoutInfo(emptyList()))
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
            collector.assertValues(
                WindowLayoutInfo(emptyList()),
                WindowLayoutInfo(emptyList())
            )
        }
    }

    @Test
    public fun testWindowLayoutFeatures_multicastingWithContext(): Unit = testScope.runTest {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@runTest
        }
        assumeAtLeastVendorApiLevel(2)
        val windowMetricsCalculator = WindowMetricsCalculatorCompat
        val fakeBackend = FakeWindowBackend()
        val repo = WindowInfoTrackerImpl(
            windowMetricsCalculator,
            fakeBackend
        )
        val collector = TestConsumer<WindowLayoutInfo>()
        val job = Job()

        val windowContext = WindowTestUtils.createOverlayWindowContext()

        launch(job) {
            repo.windowLayoutInfo(windowContext).collect(collector::accept)
        }
        launch(job) {
            repo.windowLayoutInfo(windowContext).collect(collector::accept)
        }

        fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))
        collector.assertValues(
            WindowLayoutInfo(emptyList()),
            WindowLayoutInfo(emptyList())
        )
    }

    private class FakeWindowBackend : WindowBackend {

        private class CallbackHolder(
            val executor: Executor,
            val callback: Consumer<WindowLayoutInfo>
        ) : Consumer<WindowLayoutInfo> {

            override fun accept(value: WindowLayoutInfo) {
                executor.execute { callback.accept(value) }
            }
        }

        private val consumers = mutableListOf<CallbackHolder>()

        fun triggerSignal(info: WindowLayoutInfo) {
            consumers.forEach { consumer -> consumer.accept(info) }
        }

        override fun registerLayoutChangeCallback(
            context: Context,
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
