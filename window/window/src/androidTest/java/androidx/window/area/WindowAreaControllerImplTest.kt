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
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer as AndroidXConsumer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.window.TestActivity
import androidx.window.WindowTestUtils.Companion.assumeAtLeastWindowExtensionVersion
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_PRESENT_ON_AREA
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNAVAILABLE
import androidx.window.area.WindowAreaControllerImpl.Companion.REAR_DISPLAY_WINDOW_AREA_TOKEN
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
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
     * Tests that we can get a list of [WindowArea] objects with a type of
     * [WindowArea.Type.TYPE_REAR_FACING]. Verifies that updating the status of features on device
     * returns an updated [WindowArea] list.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRearFacingWindowAreaList(): Unit =
        testScope.runTest {
            assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            activityScenario.scenario.onActivity {
                val extensionComponent = FakeWindowAreaComponent()
                val controller = WindowAreaControllerImpl(windowAreaComponent = extensionComponent)
                extensionComponent.currentRearDisplayStatus = STATUS_UNAVAILABLE
                extensionComponent.currentRearDisplayPresentationStatus = STATUS_UNAVAILABLE
                val collector = TestWindowAreaListConsumer()
                controller.addWindowAreasListener(Runnable::run, collector)

                val capabilityMap = HashMap<WindowAreaCapability.Operation, WindowAreaCapability>()
                val rearDisplayCapability =
                    WindowAreaCapability(OPERATION_TRANSFER_TO_AREA, WINDOW_AREA_STATUS_UNAVAILABLE)
                val rearDisplayPresentationCapability =
                    WindowAreaCapability(OPERATION_PRESENT_ON_AREA, WINDOW_AREA_STATUS_UNAVAILABLE)
                capabilityMap[OPERATION_TRANSFER_TO_AREA] = rearDisplayCapability
                capabilityMap[OPERATION_PRESENT_ON_AREA] = rearDisplayPresentationCapability

                var expectedAreaInfo =
                    WindowArea(
                        windowMetrics =
                            WindowMetricsCalculator.fromDisplayMetrics(
                                extensionComponent.rearDisplayMetrics
                            ),
                        type = WindowArea.Type.TYPE_REAR_FACING,
                        token = REAR_DISPLAY_WINDOW_AREA_TOKEN,
                        capabilityMap = capabilityMap,
                    )

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])

                extensionComponent.updateRearDisplayStatusListeners(STATUS_AVAILABLE)

                val updatedRearDisplayCapability =
                    WindowAreaCapability(OPERATION_TRANSFER_TO_AREA, WINDOW_AREA_STATUS_AVAILABLE)
                capabilityMap[OPERATION_TRANSFER_TO_AREA] = updatedRearDisplayCapability

                expectedAreaInfo =
                    WindowArea(
                        windowMetrics =
                            WindowMetricsCalculator.fromDisplayMetrics(
                                extensionComponent.rearDisplayMetrics
                            ),
                        type = WindowArea.Type.TYPE_REAR_FACING,
                        token = REAR_DISPLAY_WINDOW_AREA_TOKEN,
                        capabilityMap = capabilityMap,
                    )

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])

                // Update the presentation capability status and verify that only one window area
                // info is still returned
                extensionComponent.updateRearDisplayPresentationStatusListeners(STATUS_AVAILABLE)

                val updatedRearDisplayPresentationCapability =
                    WindowAreaCapability(OPERATION_PRESENT_ON_AREA, WINDOW_AREA_STATUS_AVAILABLE)
                capabilityMap[OPERATION_PRESENT_ON_AREA] = updatedRearDisplayPresentationCapability

                expectedAreaInfo =
                    WindowArea(
                        windowMetrics =
                            WindowMetricsCalculator.fromDisplayMetrics(
                                extensionComponent.rearDisplayMetrics
                            ),
                        type = WindowArea.Type.TYPE_REAR_FACING,
                        token = REAR_DISPLAY_WINDOW_AREA_TOKEN,
                        capabilityMap = capabilityMap,
                    )

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])
            }
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun testWindowAreaListNullComponent(): Unit =
        testScope.runTest {
            activityScenario.scenario.onActivity {
                val controller = EmptyWindowAreaControllerImpl()
                val collector = TestWindowAreaListConsumer()
                controller.addWindowAreasListener(Runnable::run, collector)
                assertEquals(collector.values.size, 1)
                assertEquals(listOf(), collector.values[0])
            }
        }

    /**
     * Tests the [WindowAreaController.transferToWindowArea] flow. Tests the flow through
     * WindowAreaControllerImpl with a fake extension. This fake extension changes the orientation
     * of the activity to landscape to simulate a configuration change that would occur when moving
     * to the rear facing window area and then returns it back to portrait when it's disabled.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransferToRearFacingWindowArea(): Unit =
        testScope.runTest {
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)
            extensions.currentRearDisplayStatus = STATUS_AVAILABLE
            val collector = TestWindowAreaListConsumer()
            controller.addWindowAreasListener(Runnable::run, collector)
            val windowArea: WindowArea? = collector.values.last().firstOrNull()

            assertNotNull(windowArea)
            assertEquals(
                WINDOW_AREA_STATUS_AVAILABLE,
                windowArea.getCapability(OPERATION_TRANSFER_TO_AREA).status,
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

                controller.transferToWindowArea(windowArea.token, testActivity)
                testActivity.waitForLayout()
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                )
            }
            val activeWindowArea: WindowArea? = collector.values.last().firstOrNull()

            assertNotNull(activeWindowArea)
            assertEquals(
                WINDOW_AREA_STATUS_ACTIVE,
                activeWindowArea.getCapability(OPERATION_TRANSFER_TO_AREA).status,
            )

            activityScenario.scenario.onActivity { testActivity ->
                testActivity.resetLayoutCounter()
                controller.transferToWindowArea(null, testActivity)
                testActivity.waitForLayout()
                assert(
                    testActivity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                )
            }
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransferRearDisplayReturnsError_statusUnavailable() {
        testTransferRearDisplayReturnsError(STATUS_UNAVAILABLE)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransferRearDisplayReturnsError_statusActive() {
        testTransferRearDisplayReturnsError(STATUS_ACTIVE)
    }

    /**
     * Base test method to provide a specific [WindowAreaComponent.WindowAreaStatus] that should
     * return an IllegalStateException when trying to move to a rear display with a status that is
     * not [WindowAreaComponent.STATUS_ACTIVE].
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun testTransferRearDisplayReturnsError(
        initialState: @WindowAreaComponent.WindowAreaStatus Int
    ) =
        testScope.runTest {
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)
            extensions.currentRearDisplayStatus = initialState
            val collector = TestWindowAreaListConsumer()
            controller.addWindowAreasListener(Runnable::run, collector)
            val windowArea: WindowArea? = collector.values.last().firstOrNull()

            assertNotNull(windowArea)
            assertEquals(
                windowArea.getCapability(OPERATION_TRANSFER_TO_AREA).status,
                WindowAreaAdapter.translate(initialState),
            )

            activityScenario.scenario.onActivity { testActivity ->
                assertFailsWith<IllegalStateException> {
                    controller.transferToWindowArea(windowArea.token, testActivity)
                }
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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testPresentRearDisplayArea(): Unit =
        testScope.runTest {
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)

            extensions.updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            extensions.updateRearDisplayPresentationStatusListeners(STATUS_AVAILABLE)
            val collector = TestWindowAreaListConsumer()
            controller.addWindowAreasListener(Runnable::run, collector)
            val windowArea: WindowArea? = collector.values.last().firstOrNull()

            assertNotNull(windowArea)
            assertTrue {
                windowArea.getCapability(OPERATION_PRESENT_ON_AREA).status ==
                    WINDOW_AREA_STATUS_AVAILABLE
            }

            val callback = TestWindowAreaPresentationSessionCallback()
            activityScenario.scenario.onActivity { testActivity ->
                controller.presentContentOnWindowArea(
                    windowArea.token,
                    testActivity,
                    Runnable::run,
                    callback,
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRearDisplayPresentationModeSessionEndedError(): Unit =
        testScope.runTest {
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val extensionComponent = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensionComponent)

            extensionComponent.updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            extensionComponent.updateRearDisplayPresentationStatusListeners(STATUS_UNAVAILABLE)
            val collector = TestWindowAreaListConsumer()
            controller.addWindowAreasListener(Runnable::run, collector)
            val windowArea: WindowArea? = collector.values.last().firstOrNull()

            assertNotNull(windowArea)
            assertTrue {
                windowArea.getCapability(OPERATION_PRESENT_ON_AREA).status ==
                    WINDOW_AREA_STATUS_UNAVAILABLE
            }

            val callback = TestWindowAreaPresentationSessionCallback()
            activityScenario.scenario.onActivity { testActivity ->
                controller.presentContentOnWindowArea(
                    windowArea.token,
                    testActivity,
                    Runnable::run,
                    callback,
                )
                assert(!callback.sessionActive)
                assert(callback.sessionError != null)
                assert(callback.sessionError is IllegalStateException)
            }
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testPresentContentWithNewControllerThrowsException(): Unit =
        testScope.runTest {
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val extensions = FakeWindowAreaComponent()
            val controller = WindowAreaControllerImpl(windowAreaComponent = extensions)

            extensions.updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            extensions.updateRearDisplayPresentationStatusListeners(STATUS_AVAILABLE)

            val collector = TestWindowAreaListConsumer()
            controller.addWindowAreasListener(Runnable::run, collector)
            val windowArea: WindowArea? = collector.values.last().firstOrNull()

            assertNotNull(windowArea)
            assertEquals(
                WINDOW_AREA_STATUS_AVAILABLE,
                windowArea.getCapability(OPERATION_PRESENT_ON_AREA).status,
            )

            // Create a new controller to start the presentation.
            val controller2 = WindowAreaControllerImpl(windowAreaComponent = extensions)

            val callback = TestWindowAreaPresentationSessionCallback()
            activityScenario.scenario.onActivity { testActivity ->
                controller2.presentContentOnWindowArea(
                    windowArea.token,
                    testActivity,
                    Runnable::run,
                    callback,
                )

                assert(!callback.sessionActive)
                assert(callback.sessionError != null)
                assert(callback.sessionError is IllegalArgumentException)
            }
        }

    /**
     * Tests that we can get a list of [WindowArea] objects with a type of
     * [WindowArea.Type.TYPE_REAR_FACING]. Verifies that updating the status of features on device
     * returns an updated [WindowArea] list.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRearFacingWindowAreaList_getRearDisplayMetricsClassCastException(): Unit =
        testScope.runTest {
            assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            activityScenario.scenario.onActivity {
                val extensionComponent = FakeWindowAreaComponentClassCastException()
                val controller = WindowAreaControllerImpl(windowAreaComponent = extensionComponent)
                extensionComponent.currentRearDisplayStatus = STATUS_UNAVAILABLE
                extensionComponent.currentRearDisplayPresentationStatus = STATUS_UNAVAILABLE
                val collector = TestWindowAreaListConsumer()
                controller.addWindowAreasListener(Runnable::run, collector)

                val capabilityMap = HashMap<WindowAreaCapability.Operation, WindowAreaCapability>()
                val rearDisplayCapability =
                    WindowAreaCapability(OPERATION_TRANSFER_TO_AREA, WINDOW_AREA_STATUS_UNAVAILABLE)
                val rearDisplayPresentationCapability =
                    WindowAreaCapability(OPERATION_PRESENT_ON_AREA, WINDOW_AREA_STATUS_UNAVAILABLE)
                capabilityMap[OPERATION_TRANSFER_TO_AREA] = rearDisplayCapability
                capabilityMap[OPERATION_PRESENT_ON_AREA] = rearDisplayPresentationCapability

                val expectedAreaInfo =
                    WindowArea(
                        windowMetrics =
                            WindowMetricsCalculator.fromDisplayMetrics(DisplayMetrics()),
                        type = WindowArea.Type.TYPE_REAR_FACING,
                        token = REAR_DISPLAY_WINDOW_AREA_TOKEN,
                        capabilityMap = capabilityMap,
                    )

                assertEquals(listOf(expectedAreaInfo), collector.values[collector.values.size - 1])
            }
        }

    @RequiresApi(Build.VERSION_CODES.N)
    private class TestWindowAreaListConsumer : AndroidXConsumer<List<WindowArea>> {

        val values: MutableList<List<WindowArea>> = mutableListOf()

        override fun accept(value: List<WindowArea>) {
            values.add(value)
        }
    }

    private open class FakeWindowAreaComponent : WindowAreaComponent {
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
            rearDisplaySessionConsumer: Consumer<Int>,
        ) {
            if (currentRearDisplayStatus != STATUS_AVAILABLE) {
                rearDisplaySessionConsumer.accept(SESSION_STATE_INACTIVE)
            }
            updateRearDisplayStatusListeners(STATUS_ACTIVE)
            testActivity = activity
            this.rearDisplaySessionConsumer = rearDisplaySessionConsumer
            testActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            rearDisplaySessionConsumer.accept(SESSION_STATE_ACTIVE)
        }

        override fun endRearDisplaySession() {
            updateRearDisplayStatusListeners(STATUS_AVAILABLE)
            testActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            rearDisplaySessionConsumer?.accept(SESSION_STATE_INACTIVE)
        }

        override fun startRearDisplayPresentationSession(
            activity: Activity,
            consumer: Consumer<Int>,
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
                rearDisplayPresentationSessionConsumer!!,
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

    /** Specific extensions fake to simulate [ClassCastException] to test library fix. */
    private class FakeWindowAreaComponentClassCastException : FakeWindowAreaComponent() {
        override fun getRearDisplayMetrics(): DisplayMetrics {
            throw ClassCastException()
        }
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
        private val sessionConsumer: Consumer<Int>,
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
}
