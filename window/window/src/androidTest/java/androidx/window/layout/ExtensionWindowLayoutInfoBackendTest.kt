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

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.os.Build
import androidx.core.util.Consumer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.TestConsumer
import androidx.window.core.ConsumerAdapter
import androidx.window.extensions.layout.FoldingFeature.STATE_FLAT
import androidx.window.extensions.layout.FoldingFeature.TYPE_HINGE
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.layout.ExtensionsWindowLayoutInfoAdapter.translate
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.window.extensions.layout.FoldingFeature as OEMFoldingFeature
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import java.util.function.Consumer as JavaConsumer
import com.nhaarman.mockitokotlin2.times

class ExtensionWindowLayoutInfoBackendTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val consumerAdapter = ConsumerAdapter(
        ExtensionWindowLayoutInfoBackendTest::class.java.classLoader!!
    )

    @Before
    fun setUp() {
        assumeTrue("Must be at least API 24", Build.VERSION_CODES.N <= Build.VERSION.SDK_INT)
    }

    @Test
    public fun testExtensionWindowBackend_delegatesToWindowLayoutComponent() {
        val component = RequestTrackingWindowComponent()

        val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            assertTrue("Expected call with Activity: $activity", component.hasAddCall(activity))
        }
    }

    @Test
    public fun testExtensionWindowBackend_registerAtMostOnce() {
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())

            verify(component).addWindowLayoutInfoListener(eq(activity), any())
        }
    }

    @SuppressLint("NewApi") // java.util.function.Consumer was added in API 24 (N)
    @Test
    public fun testExtensionWindowBackend_translateValues() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val component = mock<WindowLayoutComponent>()
        whenever(component.addWindowLayoutInfoListener(any(), any()))
            .thenAnswer { invocation ->
                val consumer = invocation.getArgument(1) as JavaConsumer<OEMWindowLayoutInfo>
                consumer.accept(OEMWindowLayoutInfo(emptyList()))
            }
        val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            consumer.assertValue(WindowLayoutInfo(emptyList()))
        }
    }

    @SuppressLint("NewApi") // java.util.function.Consumer was added in API 24 (N)
    @Test
    public fun testExtensionWindowBackend_infoReplayedForAdditionalListener() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val component = mock<WindowLayoutComponent>()
        whenever(component.addWindowLayoutInfoListener(any(), any()))
            .thenAnswer { invocation ->
                val consumer = invocation.getArgument(1) as JavaConsumer<OEMWindowLayoutInfo>
                consumer.accept(OEMWindowLayoutInfo(emptyList()))
            }
        val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            consumer.assertValue(WindowLayoutInfo(emptyList()))
        }
    }

    @Test
    public fun testExtensionWindowBackend_removeMatchingCallback() {
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.unregisterLayoutChangeCallback(consumer)

            val consumerCaptor = argumentCaptor<JavaConsumer<OEMWindowLayoutInfo>>()
            verify(component).addWindowLayoutInfoListener(eq(activity), consumerCaptor.capture())
            verify(component).removeWindowLayoutInfoListener(consumerCaptor.firstValue)
        }
    }

    @Test
    public fun testExtensionWindowBackend_reRegisterCallback() {
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.unregisterLayoutChangeCallback(consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            val consumerCaptor = argumentCaptor<JavaConsumer<OEMWindowLayoutInfo>>()
            verify(component, times(2)).addWindowLayoutInfoListener(
                eq(activity),
                consumerCaptor.capture()
            )
            verify(component).removeWindowLayoutInfoListener(consumerCaptor.firstValue)
        }
    }

    @Test
    public fun testRegisterLayoutChangeCallback_clearListeners() {
        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

            // Check registering the layout change callback
            val firstConsumer = mock<Consumer<WindowLayoutInfo>>()
            val secondConsumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(
                activity,
                { obj: Runnable -> obj.run() },
                firstConsumer
            )
            backend.registerLayoutChangeCallback(
                activity,
                { obj: Runnable -> obj.run() },
                secondConsumer
            )

            assertEquals("Expected one registration for same Activity", 1, component.consumers.size)
            // Check unregistering the layout change callback
            backend.unregisterLayoutChangeCallback(firstConsumer)
            backend.unregisterLayoutChangeCallback(secondConsumer)
            assertTrue("Expected all listeners to be removed", component.consumers.isEmpty())
        }
    }

    @Test
    public fun testLayoutChangeCallback_emitNewValue() {
        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)

            // Check that callbacks from the extension are propagated correctly
            val consumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
            val windowLayoutInfo = newTestOEMWindowLayoutInfo(activity)

            component.emit(windowLayoutInfo)
            verify(consumer).accept(translate(activity, windowLayoutInfo))
        }
    }

    @Test
    public fun testWindowLayoutInfo_updatesOnSubsequentRegistration() {
        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowLayoutInfoBackend(component, consumerAdapter)
            val consumer = TestConsumer<WindowLayoutInfo>()
            val oemWindowLayoutInfo = newTestOEMWindowLayoutInfo(activity)
            val expected = listOf(
                translate(activity, oemWindowLayoutInfo),
                translate(activity, oemWindowLayoutInfo)
            )

            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            component.emit(newTestOEMWindowLayoutInfo(activity))
            backend.unregisterLayoutChangeCallback(consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            component.emit(newTestOEMWindowLayoutInfo(activity))
            backend.unregisterLayoutChangeCallback(consumer)
            consumer.assertValues(expected)
        }
    }

    internal companion object {

        private fun newTestOEMWindowLayoutInfo(activity: Activity): OEMWindowLayoutInfo {
            val bounds = WindowMetricsCalculatorCompat.computeCurrentWindowMetrics(activity).bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val feature = OEMFoldingFeature(featureBounds, TYPE_HINGE, STATE_FLAT)
            val displayFeatures = listOf(feature)
            return OEMWindowLayoutInfo(displayFeatures)
        }
    }

    private class RequestTrackingWindowComponent : WindowLayoutComponent {

        val records = mutableListOf<AddCall>()

        override fun addWindowLayoutInfoListener(
            activity: Activity,
            consumer: JavaConsumer<OEMWindowLayoutInfo>
        ) {
            records.add(AddCall(activity))
        }

        override fun removeWindowLayoutInfoListener(consumer: JavaConsumer<OEMWindowLayoutInfo>) {
        }

        class AddCall(val activity: Activity)

        fun hasAddCall(activity: Activity): Boolean {
            return records.any { addRecord -> addRecord.activity == activity }
        }
    }

    private class FakeWindowComponent : WindowLayoutComponent {

        val consumers = mutableListOf<JavaConsumer<OEMWindowLayoutInfo>>()

        override fun addWindowLayoutInfoListener(
            activity: Activity,
            consumer: JavaConsumer<OEMWindowLayoutInfo>
        ) {
            consumers.add(consumer)
        }

        override fun removeWindowLayoutInfoListener(consumer: JavaConsumer<OEMWindowLayoutInfo>) {
            consumers.remove(consumer)
        }

        @SuppressLint("NewApi")
        fun emit(info: OEMWindowLayoutInfo) {
            consumers.forEach { it.accept(info) }
        }
    }
}
