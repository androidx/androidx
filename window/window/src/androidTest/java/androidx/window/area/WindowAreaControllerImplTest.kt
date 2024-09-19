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

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Binder
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.WindowTestUtils.Companion.assumeAtLeastVendorApiLevel
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_PRESENT_ON_AREA
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_ACTIVITY_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNAVAILABLE
import androidx.window.area.adapter.WindowAreaAdapter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.ExtensionWindowAreaPresentation
import androidx.window.extensions.area.ExtensionWindowAreaStatus
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_INACTIVE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_AVAILABLE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_UNAVAILABLE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_UNSUPPORTED
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.layout.WindowMetricsCalculator
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalWindowApi::class)
class WindowAreaControllerImplTest {

    @get:Rule
    val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val minVendorApiLevel = 3

    /**
     * Tests that we can get a list of [WindowAreaInfo] objects with a type of
     * [WindowAreaInfo.Type.TYPE_REAR_FACING]. Verifies that updating the status of features on
     * device returns an updated [WindowAreaInfo] list.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testRearFacingWindowAreaInfoList(): Unit =
        testScope.runTest {
            assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            activityScenario.scenario.onActivity {
                val extensionComponent = FakeWindowAreaComponent()
                val controller = WindowAreaControllerImpl(windowAreaComponent = extensionComponent)
                extensionComponent.currentRearDisplayStatus = STATUS_UNAVAILABLE
                extensionComponent.currentRearDisplayPresentationStatus = STATUS_UNAVAILABLE
                val collector = TestWindowAreaInfoListConsumer()
                testScope.launch(Job()) { controller.windowAreaInfos.collect(collector::accept) }

                val expectedAreaInfo =
                    WindowAreaInfo(
                        metrics =
                            WindowMetricsCalculator.fromDisplayMetrics(
                                extensionComponent.rearDisplayMetrics
                            ),
                        type = WindowAreaInfo.Type.TYPE_REAR_FACING,
                        token = Binder(REAR_FACING_BINDER_DESCRIPTION),
                        windowAreaComponent = extensionComponent
                    )
                val rearDisplayCapability =
                    WindowAreaCapability(
                        OPERATION_TRANSFER_ACTIVITY_TO_AREA,
                        WINDOW_AREA_STATUS_UNAVAILABLE
                    )
                val rearDisplayPresentationCapability =
                    WindowAreaCapability(OPERATION_PRESENT_ON_AREA, WINDOW_AREA_STATUS_UNAVAILABLE)
                expectedAreaInfo.capabilityMap[OPERATION_TRANSFER_ACTIVITY_TO_AREA] =
                    rearDisplayCapability
                expectedAreaInfo.capabilityMap[OPERATION_PRESENT_ON_AREA] =
                    rearDisplayPresentationCapability

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])

                extensionComponent.updateRearDisplayStatusListeners(STATUS_AVAILABLE)

                val updatedRearDisplayCapability =
                    WindowAreaCapability(
                        OPERATION_TRANSFER_ACTIVITY_TO_AREA,
                        WINDOW_AREA_STATUS_AVAILABLE
                    )
                expectedAreaInfo.capabilityMap[OPERATION_TRANSFER_ACTIVITY_TO_AREA] =
                    updatedRearDisplayCapability

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])

                // Update the presentation capability status and verify that only one window area
                // info is still returned
                extensionComponent.updateRearDisplayPresentationStatusListeners(STATUS_AVAILABLE)

                val updatedRearDisplayPresentationCapability =
                    WindowAreaCapability(OPERATION_PRESENT_ON_AREA, WINDOW_AREA_STATUS_AVAILABLE)
                expectedAreaInfo.capabilityMap[OPERATION_PRESENT_ON_AREA] =
                    updatedRearDisplayPresentationCapability

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])
            }
        }

    @Test
    fun testWindowAreaInfoListNullComponent(): Unit =
        testScope.runTest {
            activityScenario.scenario.onActivity {
                val controller = EmptyWindowAreaControllerImpl()
                val collector = TestWindowAreaInfoListConsumer()
                testScope.launch(Job()) { controller.windowAreaInfos.collect(collector::accept) }
                assertTrue(collector.values.size == 1)
                assertEquals(listOf(), collector.values[0])
            }
        }

    /**
     * Tests the transfer to rear facing window area flow. Tests the flow through
     * WindowAreaControllerImpl with a fake extension. This fake extension changes the orientation
     * of the activity to landscape to simulate a configuration change that would occur when
     * transferring to the rear facing window area and then returns it back to portrait when it's
     * disabled.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testTransferToRearFacingWindowArea(): Unit =
        testScope.runTest {
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)
            extensions.currentRearDisplayStatus = STATUS_AVAILABLE
            val callback = TestWindowAreaSessionCallback()
            val windowAreaInfo: WindowAreaInfo? =
                async {
                        return@async controller.windowAreaInfos.first().firstOrNull {
                            it.type == WindowAreaInfo.Type.TYPE_REAR_FACING
                        }
                    }
                    .await()

            assertNotNull(windowAreaInfo)
            assertEquals(
                windowAreaInfo.getCapability(OPERATION_TRANSFER_ACTIVITY_TO_AREA).status,
                WINDOW_AREA_STATUS_AVAILABLE
            )

            activityScenario.scenario.onActivity { testActivity ->
                testActivity.resetLayoutCounter()
                testActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                testActivity.waitForLayout()
            }

            activityScenario.scenario.onActivity { testActivity ->
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                )
                testActivity.resetLayoutCounter()
                controller.transferActivityToWindowArea(
                    windowAreaInfo.token,
                    testActivity,
                    Runnable::run,
                    callback
                )
            }

            activityScenario.scenario.onActivity { testActivity ->
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                )
                assert(callback.currentSession != null)
                testActivity.resetLayoutCounter()
                callback.endSession()
            }
            activityScenario.scenario.onActivity { testActivity ->
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                )
                assert(callback.currentSession == null)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testTransferRearDisplayReturnsError_statusUnavailable() {
        testTransferRearDisplayReturnsError(STATUS_UNAVAILABLE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testTransferRearDisplayReturnsError_statusActive() {
        testTransferRearDisplayReturnsError(STATUS_ACTIVE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun testTransferRearDisplayReturnsError(
        initialState: @WindowAreaComponent.WindowAreaStatus Int
    ) =
        testScope.runTest {
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)
            extensions.currentRearDisplayStatus = initialState
            val callback = TestWindowAreaSessionCallback()
            val windowAreaInfo: WindowAreaInfo? =
                async {
                        return@async controller.windowAreaInfos.first().firstOrNull {
                            it.type == WindowAreaInfo.Type.TYPE_REAR_FACING
                        }
                    }
                    .await()

            assertNotNull(windowAreaInfo)
            assertEquals(
                windowAreaInfo.getCapability(OPERATION_TRANSFER_ACTIVITY_TO_AREA).status,
                WindowAreaAdapter.translate(initialState)
            )

            activityScenario.scenario.onActivity { testActivity ->
                controller.transferActivityToWindowArea(
                    windowAreaInfo.token,
                    testActivity,
                    Runnable::run,
                    callback
                )
                assertNotNull(callback.error)
                assertNull(callback.currentSession)
            }
        }

    /**
     * Tests the presentation flow on to a rear facing display works as expected. The
     * [WindowAreaPresentationSessionCallback] provided to
     * [WindowAreaControllerImpl.presentContentOnWindowArea] should receive a
     * [WindowAreaSessionPresenter] when the session is active, and be notified that the [View]
     * provided through [WindowAreaSessionPresenter.setContentView] is visible when inflated.
     *
     * Tests the flow through WindowAreaControllerImpl with a fake extension component.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testPresentRearDisplayArea(): Unit =
        testScope.runTest {
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)

            extensions.updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            extensions.updateRearDisplayPresentationStatusListeners(STATUS_AVAILABLE)
            val windowAreaInfo: WindowAreaInfo? =
                async {
                        return@async controller.windowAreaInfos.first().firstOrNull {
                            it.type == WindowAreaInfo.Type.TYPE_REAR_FACING
                        }
                    }
                    .await()

            assertNotNull(windowAreaInfo)
            assertTrue {
                windowAreaInfo.getCapability(OPERATION_PRESENT_ON_AREA).status ==
                    WINDOW_AREA_STATUS_AVAILABLE
            }

            val callback = TestWindowAreaPresentationSessionCallback()
            activityScenario.scenario.onActivity { testActivity ->
                controller.presentContentOnWindowArea(
                    windowAreaInfo.token,
                    testActivity,
                    Runnable::run,
                    callback
                )
                assert(callback.sessionActive)
                assert(!callback.contentVisible)

                callback.presentation?.setContentView(TextView(testActivity))
                assert(callback.contentVisible)
                assert(callback.sessionActive)

                callback.presentation?.close()
                assert(!callback.contentVisible)
                assert(!callback.sessionActive)
            }
        }

    /**
     * Tests the presentation flow on to a rear facing display works as expected. Similar to
     * [testPresentRearDisplayArea], but starts the presentation with a new instance of
     * [WindowAreaControllerImpl].
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testPresentRearDisplayAreaWithNewController(): Unit =
        testScope.runTest {
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)

            extensions.updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            extensions.updateRearDisplayPresentationStatusListeners(STATUS_AVAILABLE)

            val windowAreaInfo =
                async {
                        return@async controller.windowAreaInfos.first().firstOrNull {
                            it.type == WindowAreaInfo.Type.TYPE_REAR_FACING
                        }
                    }
                    .await()

            assertNotNull(windowAreaInfo)
            assertTrue {
                windowAreaInfo.getCapability(OPERATION_PRESENT_ON_AREA).status ==
                    WINDOW_AREA_STATUS_AVAILABLE
            }

            // Create a new controller to start the presentation.
            val controller2 = WindowAreaControllerImpl(windowAreaComponent = extensions)

            val callback = TestWindowAreaPresentationSessionCallback()
            activityScenario.scenario.onActivity { testActivity ->
                controller2.presentContentOnWindowArea(
                    windowAreaInfo.token,
                    testActivity,
                    Runnable::run,
                    callback
                )
                assert(callback.sessionActive)
                assert(!callback.contentVisible)

                callback.presentation?.setContentView(TextView(testActivity))
                assert(callback.contentVisible)
                assert(callback.sessionActive)

                callback.presentation?.close()
                assert(!callback.contentVisible)
                assert(!callback.sessionActive)
            }
        }

    /**
     * Tests the presentation flow on to a rear facing display works as expected. Similar to
     * [testTransferToRearFacingWindowArea], but starts the presentation with a new instance of
     * [WindowAreaControllerImpl].
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testTransferToRearDisplayAreaWithNewController(): Unit =
        testScope.runTest {
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)
            extensions.currentRearDisplayStatus = STATUS_AVAILABLE
            val callback = TestWindowAreaSessionCallback()
            val windowAreaInfo =
                async {
                        return@async controller.windowAreaInfos.first().firstOrNull {
                            it.type == WindowAreaInfo.Type.TYPE_REAR_FACING
                        }
                    }
                    .await()

            assertNotNull(windowAreaInfo)
            assertEquals(
                windowAreaInfo.getCapability(OPERATION_TRANSFER_ACTIVITY_TO_AREA).status,
                WINDOW_AREA_STATUS_AVAILABLE
            )

            activityScenario.scenario.onActivity { testActivity ->
                testActivity.resetLayoutCounter()
                testActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                testActivity.waitForLayout()
            }

            // Create a new controller to start the transfer.
            val controller2 = WindowAreaControllerImpl(windowAreaComponent = extensions)

            activityScenario.scenario.onActivity { testActivity ->
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                )
                testActivity.resetLayoutCounter()
                controller2.transferActivityToWindowArea(
                    windowAreaInfo.token,
                    testActivity,
                    Runnable::run,
                    callback
                )
            }

            activityScenario.scenario.onActivity { testActivity ->
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                )
                assert(callback.currentSession != null)
                testActivity.resetLayoutCounter()
                callback.endSession()
            }
            activityScenario.scenario.onActivity { testActivity ->
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                )
                assert(callback.currentSession == null)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testRearDisplayPresentationModeSessionEndedError(): Unit =
        testScope.runTest {
            assumeAtLeastVendorApiLevel(minVendorApiLevel)
            val extensionComponent = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensionComponent)

            extensionComponent.updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            extensionComponent.updateRearDisplayPresentationStatusListeners(STATUS_UNAVAILABLE)
            val windowAreaInfo: WindowAreaInfo? =
                async {
                        return@async controller.windowAreaInfos.first().firstOrNull {
                            it.type == WindowAreaInfo.Type.TYPE_REAR_FACING
                        }
                    }
                    .await()

            assertNotNull(windowAreaInfo)
            assertTrue {
                windowAreaInfo.getCapability(OPERATION_PRESENT_ON_AREA).status ==
                    WINDOW_AREA_STATUS_UNAVAILABLE
            }

            val callback = TestWindowAreaPresentationSessionCallback()
            activityScenario.scenario.onActivity { testActivity ->
                controller.presentContentOnWindowArea(
                    windowAreaInfo.token,
                    testActivity,
                    Runnable::run,
                    callback
                )
                assert(!callback.sessionActive)
                assert(callback.sessionError != null)
                assert(callback.sessionError is IllegalStateException)
            }
        }

    private class TestWindowAreaInfoListConsumer : Consumer<List<WindowAreaInfo>> {

        val values: MutableList<List<WindowAreaInfo>> = mutableListOf()

        override fun accept(infos: List<WindowAreaInfo>) {
            values.add(infos)
        }
    }

    private class FakeWindowAreaComponent : WindowAreaComponent {
        val rearDisplayStatusListeners = mutableListOf<Consumer<Int>>()
        val rearDisplayPresentationStatusListeners =
            mutableListOf<Consumer<ExtensionWindowAreaStatus>>()
        var currentRearDisplayStatus = STATUS_UNSUPPORTED
        var currentRearDisplayPresentationStatus = STATUS_UNSUPPORTED

        var testActivity: Activity? = null
        var rearDisplaySessionConsumer: Consumer<Int>? = null
        var rearDisplayPresentationSessionConsumer: Consumer<Int>? = null

        override fun addRearDisplayStatusListener(consumer: Consumer<Int>) {
            rearDisplayStatusListeners.add(consumer)
            consumer.accept(currentRearDisplayStatus)
        }

        override fun removeRearDisplayStatusListener(consumer: Consumer<Int>) {
            rearDisplayStatusListeners.remove(consumer)
        }

        override fun addRearDisplayPresentationStatusListener(
            consumer: Consumer<ExtensionWindowAreaStatus>
        ) {
            rearDisplayPresentationStatusListeners.add(consumer)
            consumer.accept(TestExtensionWindowAreaStatus(currentRearDisplayPresentationStatus))
        }

        override fun removeRearDisplayPresentationStatusListener(
            consumer: Consumer<ExtensionWindowAreaStatus>
        ) {
            rearDisplayPresentationStatusListeners.remove(consumer)
        }

        // Fake WindowAreaComponent will change the orientation of the activity to signal
        // entering rear display mode, as well as ending the session
        override fun startRearDisplaySession(
            activity: Activity,
            rearDisplaySessionConsumer: Consumer<Int>
        ) {
            if (currentRearDisplayStatus != STATUS_AVAILABLE) {
                rearDisplaySessionConsumer.accept(SESSION_STATE_INACTIVE)
            }
            testActivity = activity
            this.rearDisplaySessionConsumer = rearDisplaySessionConsumer
            testActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            rearDisplaySessionConsumer.accept(SESSION_STATE_ACTIVE)
        }

        override fun endRearDisplaySession() {
            testActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            rearDisplaySessionConsumer?.accept(SESSION_STATE_INACTIVE)
        }

        override fun startRearDisplayPresentationSession(
            activity: Activity,
            consumer: Consumer<Int>
        ) {
            if (currentRearDisplayPresentationStatus != STATUS_AVAILABLE) {
                consumer.accept(SESSION_STATE_INACTIVE)
                return
            }
            testActivity = activity
            rearDisplayPresentationSessionConsumer = consumer
            consumer.accept(SESSION_STATE_ACTIVE)
        }

        override fun endRearDisplayPresentationSession() {
            rearDisplayPresentationSessionConsumer?.accept(SESSION_STATE_ACTIVE)
            rearDisplayPresentationSessionConsumer?.accept(SESSION_STATE_INACTIVE)
        }

        override fun getRearDisplayPresentation(): ExtensionWindowAreaPresentation {
            return TestExtensionWindowAreaPresentation(
                testActivity!!,
                rearDisplayPresentationSessionConsumer!!
            )
        }

        override fun getRearDisplayMetrics(): DisplayMetrics {
            return DisplayMetrics().apply {
                widthPixels = 1080
                heightPixels = 1080
                densityDpi = 240
            }
        }

        fun updateRearDisplayStatusListeners(newStatus: Int) {
            currentRearDisplayStatus = newStatus
            for (consumer in rearDisplayStatusListeners) {
                consumer.accept(currentRearDisplayStatus)
            }
        }

        fun updateRearDisplayPresentationStatusListeners(newStatus: Int) {
            currentRearDisplayPresentationStatus = newStatus
            for (consumer in rearDisplayPresentationStatusListeners) {
                consumer.accept(TestExtensionWindowAreaStatus(currentRearDisplayPresentationStatus))
            }
        }
    }

    private class TestWindowAreaSessionCallback : WindowAreaSessionCallback {
        var currentSession: WindowAreaSession? = null
        var error: Throwable? = null

        override fun onSessionStarted(session: WindowAreaSession) {
            currentSession = session
        }

        override fun onSessionEnded(t: Throwable?) {
            error = t
            currentSession = null
        }

        fun endSession() = currentSession?.close()
    }

    private class TestWindowAreaPresentationSessionCallback :
        WindowAreaPresentationSessionCallback {
        var sessionActive: Boolean = false
        var contentVisible: Boolean = false
        var presentation: WindowAreaSessionPresenter? = null
        var sessionError: Throwable? = null

        override fun onSessionStarted(session: WindowAreaSessionPresenter) {
            sessionActive = true
            presentation = session
        }

        override fun onSessionEnded(t: Throwable?) {
            presentation = null
            sessionActive = false
            sessionError = t
        }

        override fun onContainerVisibilityChanged(isVisible: Boolean) {
            contentVisible = isVisible
        }
    }

    private class TestExtensionWindowAreaStatus(private val status: Int) :
        ExtensionWindowAreaStatus {
        override fun getWindowAreaStatus(): Int {
            return status
        }

        override fun getWindowAreaDisplayMetrics(): DisplayMetrics {
            return DisplayMetrics().apply {
                widthPixels = 1080
                heightPixels = 1080
                densityDpi = 240
            }
        }
    }

    private class TestExtensionWindowAreaPresentation(
        private val activity: Activity,
        private val sessionConsumer: Consumer<Int>
    ) : ExtensionWindowAreaPresentation {
        override fun getPresentationContext(): Context {
            return activity
        }

        override fun setPresentationView(view: View) {
            sessionConsumer.accept(WindowAreaComponent.SESSION_STATE_CONTENT_VISIBLE)
        }

        override fun getWindow(): Window {
            return activity.window
        }
    }

    companion object {
        private const val REAR_FACING_BINDER_DESCRIPTION = "TEST_WINDOW_AREA_REAR_FACING"
    }
}
