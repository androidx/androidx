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

package androidx.window.area

import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.TestConsumer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.WindowAreaComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalWindowApi::class)
class WindowAreaControllerImplTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public fun testRearDisplayStatus(): Unit = testScope.runTest {
        assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.N)
        activityScenario.scenario.onActivity {
            val extensionComponent = FakeWindowAreaComponent()
            val repo = WindowAreaControllerImpl(extensionComponent)
            val collector = TestConsumer<WindowAreaStatus>()
            extensionComponent
                .updateStatusListeners(WindowAreaComponent.STATUS_UNAVAILABLE)
            testScope.launch {
                repo.rearDisplayStatus().collect(collector::accept)
            }
            collector.assertValue(WindowAreaStatus.UNAVAILABLE)
            extensionComponent
                .updateStatusListeners(WindowAreaComponent.STATUS_AVAILABLE)
            collector.assertValues(
                WindowAreaStatus.UNAVAILABLE,
                WindowAreaStatus.AVAILABLE
            )
        }
    }

    @Test
    public fun testRearDisplayStatusNullComponent(): Unit = testScope.runTest {
        activityScenario.scenario.onActivity {
            val repo = EmptyWindowAreaControllerImpl()
            val collector = TestConsumer<WindowAreaStatus>()
            testScope.launch {
                repo.rearDisplayStatus().collect(collector::accept)
            }
            collector.assertValue(WindowAreaStatus.UNSUPPORTED)
        }
    }

    /**
     * Tests the rear display mode flow works as expected. Tests the flow
     * through WindowAreaControllerImpl with a fake extension. This fake extension
     * changes the orientation of the activity to landscape when rear display mode is enabled
     * and then returns it back to portrait when it's disabled.
     */
    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public fun testRearDisplayMode(): Unit = testScope.runTest {
        val extensions = FakeWindowAreaComponent()
        val repo = WindowAreaControllerImpl(extensions)
        extensions.currentStatus = WindowAreaComponent.STATUS_AVAILABLE
        val callback = TestWindowAreaSessionCallback()
        activityScenario.scenario.onActivity { testActivity ->
            testActivity.resetLayoutCounter()
            testActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            testActivity.waitForLayout()
        }

        activityScenario.scenario.onActivity { testActivity ->
            assert(testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            testActivity.resetLayoutCounter()
            repo.rearDisplayMode(testActivity, Runnable::run, callback)
        }

        activityScenario.scenario.onActivity { testActivity ->
            assert(testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            assert(callback.currentSession != null)
            testActivity.resetLayoutCounter()
            callback.endSession()
        }
        activityScenario.scenario.onActivity { testActivity ->
            assert(testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            assert(callback.currentSession == null)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public fun testRearDisplayModeReturnsError(): Unit = testScope.runTest {
        val extensionComponent = FakeWindowAreaComponent()
        extensionComponent.currentStatus = WindowAreaComponent.STATUS_UNAVAILABLE
        val repo = WindowAreaControllerImpl(extensionComponent)
        val callback = TestWindowAreaSessionCallback()
        activityScenario.scenario.onActivity { testActivity ->
            assertFailsWith(
                exceptionClass = UnsupportedOperationException::class,
                block = { repo.rearDisplayMode(testActivity, Runnable::run, callback) }
            )
        }
    }

    @Test
    public fun testRearDisplayModeNullComponent(): Unit = testScope.runTest {
        val repo = EmptyWindowAreaControllerImpl()
        val callback = TestWindowAreaSessionCallback()
        activityScenario.scenario.onActivity { testActivity ->
            assertFailsWith(
                exceptionClass = UnsupportedOperationException::class,
                block = { repo.rearDisplayMode(testActivity, Runnable::run, callback) }
            )
        }
    }

    private class FakeWindowAreaComponent : WindowAreaComponent {
        val statusListeners = mutableListOf<Consumer<Int>>()
        var currentStatus = WindowAreaComponent.STATUS_UNSUPPORTED
        var testActivity: Activity? = null
        var sessionConsumer: Consumer<Int>? = null

        @RequiresApi(Build.VERSION_CODES.N)
        override fun addRearDisplayStatusListener(consumer: Consumer<Int>) {
            statusListeners.add(consumer)
            consumer.accept(currentStatus)
        }

        override fun removeRearDisplayStatusListener(consumer: Consumer<Int>) {
            statusListeners.remove(consumer)
        }

        // Fake WindowAreaComponent will change the orientation of the activity to signal
        // entering rear display mode, as well as ending the session
        @RequiresApi(Build.VERSION_CODES.N)
        override fun startRearDisplaySession(
            activity: Activity,
            rearDisplaySessionConsumer: Consumer<Int>
        ) {
            if (currentStatus != WindowAreaComponent.STATUS_AVAILABLE) {
                throw WindowAreaController.REAR_DISPLAY_ERROR
            }
            testActivity = activity
            sessionConsumer = rearDisplaySessionConsumer
            testActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            rearDisplaySessionConsumer.accept(WindowAreaComponent.SESSION_STATE_ACTIVE)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun endRearDisplaySession() {
            testActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            sessionConsumer?.accept(WindowAreaComponent.SESSION_STATE_INACTIVE)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun updateStatusListeners(newStatus: Int) {
            currentStatus = newStatus
            for (consumer in statusListeners) {
                consumer.accept(currentStatus)
            }
        }
    }

    private class TestWindowAreaSessionCallback : WindowAreaSessionCallback {

        var currentSession: WindowAreaSession? = null
        var error: Throwable? = null

        override fun onSessionStarted(session: WindowAreaSession) {
            currentSession = session
        }

        override fun onSessionEnded() {
            currentSession = null
        }

        fun endSession() = currentSession?.close()
    }
}