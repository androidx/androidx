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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.window.TestActivity
import androidx.window.WindowSdkExtensions
import androidx.window.WindowTestUtils
import androidx.window.WindowTestUtils.Companion.assumeAtLeastWindowExtensionVersion
import androidx.window.WindowTestUtils.Companion.assumeBeforeWindowExtensionVersion
import androidx.window.core.Bounds
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import androidx.window.layout.adapter.WindowBackend
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import java.util.concurrent.Executor
import kotlin.test.AfterTest
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WindowInfoTrackerImplTest {

    @get:Rule val activityScenario = activityScenarioRule<TestActivity>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val windowSdkExtensions = WindowSdkExtensions.getInstance()
    private val windowMetricsCalculator = WindowMetricsCalculatorCompat()
    private val fakeBackend = FakeWindowBackend()
    private val tracker =
        WindowInfoTrackerImpl(windowMetricsCalculator, fakeBackend, windowSdkExtensions)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testWindowLayoutInfo_activityAsListener() =
        testScope.runTest {
            activityScenario.scenario.onActivity { testActivity ->
                Dispatchers.setMain(testDispatcher) // Needed for flowOn(Dispatchers.Main).
                val collector = mutableListOf<WindowLayoutInfo>()
                val job = Job()

                testScope.launch(job) { tracker.windowLayoutInfo(testActivity).toList(collector) }
                fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))

                assertThat(collector).containsExactly(WindowLayoutInfo(emptyList()))
                job.cancel()
                assertThat(fakeBackend.consumers).isEmpty()
            }
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun testWindowLayoutInfo_contextAsListener() =
        testScope.runTest {
            assume().that(Build.VERSION.SDK_INT).isAtLeast(Build.VERSION_CODES.R)
            assumeAtLeastWindowExtensionVersion(2)
            Dispatchers.setMain(testDispatcher) // Needed for flowOn(Dispatchers.Main).
            val collector = mutableListOf<WindowLayoutInfo>()
            val windowContext = WindowTestUtils.createOverlayWindowContext()
            val job = Job()

            testScope.launch(job) { tracker.windowLayoutInfo(windowContext).toList(collector) }
            fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))

            assertThat(collector).containsExactly(WindowLayoutInfo(emptyList()))
            job.cancel()
            assertThat(fakeBackend.consumers).isEmpty()
        }

    @Test
    fun testWindowLayoutInfo_multicastingWithActivity() =
        testScope.runTest {
            activityScenario.scenario.onActivity { testActivity ->
                Dispatchers.setMain(testDispatcher) // Needed for flowOn(Dispatchers.Main).
                val collector = mutableListOf<WindowLayoutInfo>()
                val job = Job()

                launch(job) { tracker.windowLayoutInfo(testActivity).toList(collector) }
                launch(job) { tracker.windowLayoutInfo(testActivity).toList(collector) }
                fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))

                assertThat(collector)
                    .containsExactly(WindowLayoutInfo(emptyList()), WindowLayoutInfo(emptyList()))
            }
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun testWindowLayoutInfo_multicastingWithContext() =
        testScope.runTest {
            assume().that(Build.VERSION.SDK_INT).isAtLeast(Build.VERSION_CODES.R)
            assumeAtLeastWindowExtensionVersion(2)
            Dispatchers.setMain(testDispatcher) // Needed for flowOn(Dispatchers.Main).
            val collector = mutableListOf<WindowLayoutInfo>()
            val windowContext = WindowTestUtils.createOverlayWindowContext()
            val job = Job()

            launch(job) { tracker.windowLayoutInfo(windowContext).toList(collector) }
            launch(job) { tracker.windowLayoutInfo(windowContext).toList(collector) }
            fakeBackend.triggerSignal(WindowLayoutInfo(emptyList()))

            assertThat(collector)
                .containsExactly(WindowLayoutInfo(emptyList()), WindowLayoutInfo(emptyList()))
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun testWindowLayoutInfo_nonUiContext_throwsError() =
        testScope.runTest {
            assume().that(Build.VERSION.SDK_INT).isAtLeast(Build.VERSION_CODES.R)
            assumeAtLeastWindowExtensionVersion(2)
            Dispatchers.setMain(testDispatcher) // Needed for flowOn(Dispatchers.Main).
            val context: Context = ApplicationProvider.getApplicationContext()
            val tracker = WindowInfoTracker.getOrCreate(context)

            testScope.launch(Job()) {
                assertFailsWith<IllegalArgumentException> {
                    tracker.windowLayoutInfo(context).collect {}
                }
            }
        }

    @Test
    fun testSupportedWindowPostures_throwsBeforeApi6() {
        assumeBeforeWindowExtensionVersion(6)
        activityScenario.scenario.onActivity { _ ->
            assertFailsWith<UnsupportedOperationException> { tracker.supportedPostures }
        }
    }

    @Test
    fun testSupportedWindowPostures_reportsFeatures() {
        assumeAtLeastWindowExtensionVersion(6)
        activityScenario.scenario.onActivity { _ ->
            val fakeBackend =
                FakeWindowBackend(supportedPostures = listOf(SupportedPosture.TABLETOP))
            val tracker =
                WindowInfoTrackerImpl(windowMetricsCalculator, fakeBackend, windowSdkExtensions)

            assertThat(tracker.supportedPostures).containsExactly(SupportedPosture.TABLETOP)
        }
    }

    @Test
    fun testGetCurrentWindowLayoutInfo_throwsBeforeApi9() {
        assumeBeforeWindowExtensionVersion(9)
        activityScenario.scenario.onActivity { testActivity ->
            assertFailsWith<UnsupportedOperationException> {
                tracker.getCurrentWindowLayoutInfo(testActivity)
            }
        }
    }

    @Test
    fun testGetCurrentWindowLayoutInfo_nonUiContext_throwsError() {
        assumeAtLeastWindowExtensionVersion(9)
        val context: Context = ApplicationProvider.getApplicationContext()
        val tracker = WindowInfoTracker.getOrCreate(context)

        assertFailsWith<IllegalArgumentException> { tracker.getCurrentWindowLayoutInfo(context) }
    }

    @Test
    fun testGetCurrentWindowLayoutInfo_activityAsContext() {
        assumeAtLeastWindowExtensionVersion(9)
        activityScenario.scenario.onActivity { testActivity ->
            val displayFeature: DisplayFeature =
                HardwareFoldingFeature(Bounds(0, 0, 100, 200), HINGE, FLAT)
            val currentWindowLayoutInfo = WindowLayoutInfo(listOf(displayFeature))
            val fakeBackend = FakeWindowBackend(currentWindowLayoutInfo = currentWindowLayoutInfo)
            val tracker =
                WindowInfoTrackerImpl(windowMetricsCalculator, fakeBackend, windowSdkExtensions)

            assertThat(tracker.getCurrentWindowLayoutInfo(testActivity))
                .isEqualTo(currentWindowLayoutInfo)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun testGetCurrentWindowLayoutInfo_overlayWindowAsContext() {
        assume().that(Build.VERSION.SDK_INT).isAtLeast(Build.VERSION_CODES.R)
        assumeAtLeastWindowExtensionVersion(9)
        val displayFeature: DisplayFeature =
            HardwareFoldingFeature(Bounds(0, 0, 100, 200), HINGE, FLAT)
        val currentWindowLayoutInfo = WindowLayoutInfo(listOf(displayFeature))
        val fakeBackend = FakeWindowBackend(currentWindowLayoutInfo = currentWindowLayoutInfo)
        val tracker =
            WindowInfoTrackerImpl(windowMetricsCalculator, fakeBackend, windowSdkExtensions)
        val windowContext = WindowTestUtils.createOverlayWindowContext()

        assertThat(tracker.getCurrentWindowLayoutInfo(windowContext))
            .isEqualTo(currentWindowLayoutInfo)
    }

    private class FakeWindowBackend(
        override val supportedPostures: List<SupportedPosture> = emptyList(),
        val currentWindowLayoutInfo: WindowLayoutInfo = WindowLayoutInfo(emptyList()),
    ) : WindowBackend {
        val consumers = mutableMapOf<Consumer<WindowLayoutInfo>, Executor>()

        fun triggerSignal(info: WindowLayoutInfo) {
            consumers.forEach { (callback, executor) -> executor.execute { callback.accept(info) } }
        }

        override fun registerLayoutChangeCallback(
            context: Context,
            executor: Executor,
            callback: Consumer<WindowLayoutInfo>,
        ) {
            consumers[callback] = executor
        }

        override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
            consumers.remove(callback)
        }

        override fun getCurrentWindowLayoutInfo(context: Context) = currentWindowLayoutInfo
    }
}
