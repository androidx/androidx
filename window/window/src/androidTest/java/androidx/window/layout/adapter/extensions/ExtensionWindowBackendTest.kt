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

package androidx.window.layout.adapter.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.TestConsumer
import androidx.window.WindowTestUtils
import androidx.window.WindowTestUtils.Companion.assumeAtLeastVendorApiLevel
import androidx.window.WindowTestUtils.Companion.assumeBeforeVendorApiLevel
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.core.util.function.Consumer as OEMConsumer
import androidx.window.extensions.layout.FoldingFeature as OEMFoldingFeature
import androidx.window.extensions.layout.FoldingFeature.STATE_FLAT
import androidx.window.extensions.layout.FoldingFeature.TYPE_HINGE
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculatorCompat
import androidx.window.layout.adapter.extensions.ExtensionsWindowLayoutInfoAdapter.translate
import java.util.function.Consumer as JavaConsumer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExtensionWindowBackendTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val consumerAdapter = ConsumerAdapter(
        ExtensionWindowBackendTest::class.java.classLoader!!
    )

    @Before
    fun setUp() {
        assumeTrue("Must be at least API 24", Build.VERSION_CODES.N <= Build.VERSION.SDK_INT)
    }

    @Test
    public fun testExtensionWindowBackend_delegatesToWindowLayoutComponent() {
        assumeAtLeastVendorApiLevel(1)
        val component = RequestTrackingWindowComponent()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            assertTrue("Expected call with Activity: $activity", component.hasAddCall(activity))
        }
    }

    @Test
    public fun testExtensionWindowBackend_delegatesToWindowLayoutComponentWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = RequestTrackingWindowComponent()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        val windowContext = WindowTestUtils.createOverlayWindowContext()
        val windowContextConsumer = TestConsumer<WindowLayoutInfo>()

        backend.registerLayoutChangeCallback(windowContext, Runnable::run, windowContextConsumer)
        assertTrue(
            "Expected call with Context: $windowContext",
            component.hasAddCall(windowContext)
        )
    }

    /**
     * After {@link WindowExtensions#VENDOR_API_LEVEL_2} registerLayoutChangeCallback calls
     * addWindowLayoutInfoListener(context) instead.
     * {@link testExtensionWindowBackend_registerAtMostOnceWithContext} verifies the same behavior.
     */
    @Suppress("Deprecation")
    @Test
    public fun testExtensionWindowBackend_registerAtMostOnce() {
        assumeBeforeVendorApiLevel(2)
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())

            val consumerCaptor = argumentCaptor<JavaConsumer<OEMWindowLayoutInfo>>()
            verify(component).addWindowLayoutInfoListener(eq(activity), consumerCaptor.capture())
        }
    }

    @Test
    public fun testExtensionWindowBackend_registerAtMostOnceWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        val windowContext = WindowTestUtils.createOverlayWindowContext()
        val windowContextConsumer = TestConsumer<WindowLayoutInfo>()

        val consumerCaptor = argumentCaptor<OEMConsumer<OEMWindowLayoutInfo>>()

        backend.registerLayoutChangeCallback(windowContext, Runnable::run, windowContextConsumer)
        backend.registerLayoutChangeCallback(windowContext, Runnable::run, mock())
        verify(component).addWindowLayoutInfoListener(
            eq(windowContext),
            consumerCaptor.capture(),
        )

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())
            verify(component).addWindowLayoutInfoListener(
                eq(activity as Context),
                consumerCaptor.capture()
            )
        }
    }

    @Ignore // b/260647675, b/260648288
    @Suppress("NewApi", "Deprecation") // java.util.function.Consumer was added in API 24 (N)
    @Test
    public fun testExtensionWindowBackend_translateValues() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val component = mock<WindowLayoutComponent>()
        whenever(component.addWindowLayoutInfoListener(
            any(),
            any<JavaConsumer<OEMWindowLayoutInfo>>())
        ).thenAnswer { invocation ->
            val consumer = invocation.getArgument(1) as JavaConsumer<OEMWindowLayoutInfo>
            consumer.accept(OEMWindowLayoutInfo(emptyList()))
        }
        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            consumer.assertValue(WindowLayoutInfo(emptyList()))
        }
    }

    @Test
    public fun testExtensionWindowBackend_translateValuesWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = FakeWindowComponent()
        val windowContext = WindowTestUtils.createOverlayWindowContext()
        val windowContextConsumer = TestConsumer<WindowLayoutInfo>()
        val windowLayoutInfoFromContext = newTestOEMWindowLayoutInfo(windowContext)

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)
        backend.registerLayoutChangeCallback(windowContext, Runnable::run, windowContextConsumer)
        component.emit(windowLayoutInfoFromContext)
        windowContextConsumer.assertValue(
                translate(
                    windowContext,
                    windowLayoutInfoFromContext
                )
        )

        val consumer = TestConsumer<WindowLayoutInfo>()
        activityScenario.scenario.onActivity { activity ->
            val windowLayoutInfoFromActivity = newTestOEMWindowLayoutInfo(activity)
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            component.emit(newTestOEMWindowLayoutInfo(activity))
            consumer.assertValues(listOf(translate(activity, windowLayoutInfoFromActivity)))
        }
    }

    @Suppress("NewApi", "Deprecation") // java.util.function.Consumer was added in API 24 (N)
    @Test
    fun testExtensionWindowBackend_infoReplayedForAdditionalListener() {
        assumeBeforeVendorApiLevel(2)
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val component = mock<WindowLayoutComponent>()
        whenever(component.addWindowLayoutInfoListener(
            any(),
            any<JavaConsumer<OEMWindowLayoutInfo>>())
        ).thenAnswer { invocation ->
            val consumer = invocation.getArgument(1) as JavaConsumer<OEMWindowLayoutInfo>
            consumer.accept(OEMWindowLayoutInfo(emptyList()))
        }
        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            consumer.assertValue(WindowLayoutInfo(emptyList()))
        }
    }

    @Test
    public fun testExtensionWindowBackend_infoReplayedForAdditionalListenerWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = mock<WindowLayoutComponent>()
        whenever(component.addWindowLayoutInfoListener(
            any(),
            any<OEMConsumer<OEMWindowLayoutInfo>>())
        ).thenAnswer { invocation ->
            val consumer = invocation.getArgument(1) as OEMConsumer<OEMWindowLayoutInfo>
            consumer.accept(OEMWindowLayoutInfo(emptyList()))
        }
        whenever(component.addWindowLayoutInfoListener(
            any(),
            any<OEMConsumer<OEMWindowLayoutInfo>>())
        ).thenAnswer { invocation ->
            val consumer = invocation.getArgument(1) as OEMConsumer<OEMWindowLayoutInfo>
            consumer.accept(OEMWindowLayoutInfo(emptyList()))
        }
        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            consumer.assertValue(WindowLayoutInfo(emptyList()))
        }

        val windowContext = WindowTestUtils.createOverlayWindowContext()
        val windowContextConsumer = TestConsumer<WindowLayoutInfo>()
        backend.registerLayoutChangeCallback(windowContext, Runnable::run, windowContextConsumer)
        backend.registerLayoutChangeCallback(windowContext, Runnable::run, mock())
        windowContextConsumer.assertValue(WindowLayoutInfo(emptyList()))
    }

    @Suppress("Deprecation")
    @Test
    fun testExtensionWindowBackend_removeMatchingCallback() {
        assumeBeforeVendorApiLevel(2)
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.unregisterLayoutChangeCallback(consumer)

            val consumerCaptor = argumentCaptor<JavaConsumer<OEMWindowLayoutInfo>>()
            verify(component).addWindowLayoutInfoListener(eq(activity), consumerCaptor.capture())
            verify(component).removeWindowLayoutInfoListener(consumerCaptor.firstValue)
            assertFalse(backend.hasRegisteredListeners())
        }
    }

    @Suppress("Deprecation")
    @Test
    fun testExtensionWindowBackend_removesMultipleCallback() {
        assumeBeforeVendorApiLevel(2)
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            val consumer2 = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer2)
            backend.unregisterLayoutChangeCallback(consumer)
            backend.unregisterLayoutChangeCallback(consumer2)

            val consumerCaptor = argumentCaptor<JavaConsumer<OEMWindowLayoutInfo>>()
            verify(component).addWindowLayoutInfoListener(eq(activity), consumerCaptor.capture())
            verify(component).removeWindowLayoutInfoListener(consumerCaptor.firstValue)
            assertFalse(backend.hasRegisteredListeners())
        }
    }

    /**
     * Verifies context and consumer registration can be registered with using either
     * addWindowLayoutInfoListener(context) or addWindowLayoutInfoListener(activity),
     * but all registration are cleaned up by  removeWindowLayoutInfoListener().
     * Note: addWindowLayoutInfoListener(context) is added in
     * {@link WindowExtensions#VENDOR_API_LEVEL_2}.
     */
    @Test
    fun testExtensionWindowBackend_removeMatchingCallbackWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // createWindowContext is available after R.
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.unregisterLayoutChangeCallback(consumer)
            val windowContext = WindowTestUtils.createOverlayWindowContext()
            val windowContextConsumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(
                windowContext,
                Runnable::run,
                windowContextConsumer
            )
            backend.unregisterLayoutChangeCallback(windowContextConsumer)

            val consumerCaptor = argumentCaptor<OEMConsumer<OEMWindowLayoutInfo>>()
            verify(component).addWindowLayoutInfoListener(
                eq(activity as Context),
                consumerCaptor.capture()
            )
            verify(component).removeWindowLayoutInfoListener(
                consumerCaptor.firstValue
            )

            verify(component).addWindowLayoutInfoListener(
                eq(windowContext),
                consumerCaptor.capture()
            )
            verify(component).removeWindowLayoutInfoListener(
                consumerCaptor.lastValue
            )
        }
    }

    @Test
    fun testExtensionWindowBackend_removeMultipleCallbackWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // createWindowContext is available after R.
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            val consumer2 = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer2)
            backend.unregisterLayoutChangeCallback(consumer)
            backend.unregisterLayoutChangeCallback(consumer2)
            val windowContext = WindowTestUtils.createOverlayWindowContext()
            val windowContextConsumer = TestConsumer<WindowLayoutInfo>()
            val windowContextConsumer2 = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(
                windowContext,
                Runnable::run,
                windowContextConsumer
            )
            backend.registerLayoutChangeCallback(
                windowContext,
                Runnable::run,
                windowContextConsumer2
            )
            backend.unregisterLayoutChangeCallback(windowContextConsumer)
            backend.unregisterLayoutChangeCallback(windowContextConsumer2)

            val consumerCaptor = argumentCaptor<OEMConsumer<OEMWindowLayoutInfo>>()
            verify(component).addWindowLayoutInfoListener(
                eq(activity as Context),
                consumerCaptor.capture()
            )
            verify(component).removeWindowLayoutInfoListener(
                consumerCaptor.firstValue
            )

            verify(component).addWindowLayoutInfoListener(
                eq(windowContext),
                consumerCaptor.capture()
            )
            verify(component).removeWindowLayoutInfoListener(
                consumerCaptor.lastValue
            )
            assertFalse(backend.hasRegisteredListeners())
        }
    }

    @Suppress("Deprecation")
    @Test
    fun testExtensionWindowBackend_reRegisterCallback() {
        assumeBeforeVendorApiLevel(2)
        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

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

    /**
     * Verifies that a [WindowLayoutInfo] is published to the consumer upon each registration.
     * Note: addWindowLayoutInfoListener(context) is added in
     * {@link WindowExtensions#VENDOR_API_LEVEL_2}
     */
    @Test
    fun testExtensionWindowBackend_reRegisterCallbackWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = mock<WindowLayoutComponent>()

        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        val windowContext = WindowTestUtils.createOverlayWindowContext()
        val windowContextConsumer = TestConsumer<WindowLayoutInfo>()

        backend.registerLayoutChangeCallback(windowContext, Runnable::run, windowContextConsumer)
        backend.unregisterLayoutChangeCallback(windowContextConsumer)
        backend.registerLayoutChangeCallback(windowContext, Runnable::run, windowContextConsumer)

        val consumerCaptor = argumentCaptor<OEMConsumer<OEMWindowLayoutInfo>>()
        verify(component, times(2)).addWindowLayoutInfoListener(
            eq(windowContext),
            consumerCaptor.capture()
        )
        verify(component).removeWindowLayoutInfoListener(
            consumerCaptor.firstValue
        )

        activityScenario.scenario.onActivity { activity ->
            val consumer = TestConsumer<WindowLayoutInfo>()
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.unregisterLayoutChangeCallback(consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)

            verify(component, times(2))
                .addWindowLayoutInfoListener(
                    eq(activity as Context),
                    consumerCaptor.capture()
                )
            verify(component).removeWindowLayoutInfoListener(
                consumerCaptor.firstValue
            )
        }
    }

    @Test
    public fun testRegisterLayoutChangeCallback_clearListeners() {
        assumeBeforeVendorApiLevel(2)
        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

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

    /**
     * Verifies that both [Activity] and [UiContext] can be independently registered as listeners to
     * [WindowLayoutInfo].
     * Note: addWindowLayoutInfoListener(context) is added in
     * {@link WindowExtensions#VENDOR_API_LEVEL_2}
     */
    @Test
    public fun testRegisterLayoutChangeCallback_clearListenersWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

            // Check registering the layout change callback
            val firstConsumer = mock<Consumer<WindowLayoutInfo>>()
            val secondConsumer = mock<Consumer<WindowLayoutInfo>>()
            val thirdConsumer = mock<Consumer<WindowLayoutInfo>>()
            val windowContext = WindowTestUtils.createOverlayWindowContext()

            backend.registerLayoutChangeCallback(activity, Runnable::run, firstConsumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, secondConsumer)
            backend.registerLayoutChangeCallback(windowContext, Runnable::run, thirdConsumer)

            assertEquals(
                "Expected one registration for same Activity",
                2 /* expected */,
                component.oemConsumers.size
            )
            // Check unregistering the layout change callback
            backend.unregisterLayoutChangeCallback(firstConsumer)
            backend.unregisterLayoutChangeCallback(secondConsumer)
            backend.unregisterLayoutChangeCallback(thirdConsumer)
            assertTrue("Expected all listeners to be removed", component.oemConsumers.isEmpty())
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    public fun testLayoutChangeCallback_emitNewValue() {
        assumeBeforeVendorApiLevel(2)
        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

            // Check that callbacks from the extension are propagated correctly
            val consumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
            val windowLayoutInfo = newTestOEMWindowLayoutInfo(activity)

            component.emit(windowLayoutInfo)
            verify(consumer).accept(translate(activity, windowLayoutInfo))
        }
    }

    @Test
    public fun testExtensionWindowBackend_emitNewValueWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = FakeWindowComponent()
        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)

        // Check that callbacks from the extension are propagated for WindowContext.
        val consumer = mock<Consumer<WindowLayoutInfo>>()
        val windowContext = WindowTestUtils.createOverlayWindowContext()
        backend.registerLayoutChangeCallback(
            windowContext, Runnable::run, consumer
        )
        val windowLayoutInfo = newTestOEMWindowLayoutInfo(windowContext)

        component.emit(windowLayoutInfo)
        verify(consumer).accept(translate(windowContext, windowLayoutInfo))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    public fun testWindowLayoutInfo_updatesOnSubsequentRegistration() {
        assumeAtLeastVendorApiLevel(1)
        activityScenario.scenario.onActivity { activity ->
            val component = FakeWindowComponent()
            val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)
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

    @Test
    public fun testWindowLayoutInfo_updatesOnSubsequentRegistrationWithContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        assumeAtLeastVendorApiLevel(2)

        val component = FakeWindowComponent()
        val backend = ExtensionWindowBackend.newInstance(component, consumerAdapter)
        val consumer = TestConsumer<WindowLayoutInfo>()
        val windowContext = WindowTestUtils.createOverlayWindowContext()

        val oemWindowLayoutInfo = newTestOEMWindowLayoutInfo(windowContext)

        val expected = listOf(
            translate(windowContext, oemWindowLayoutInfo),
            translate(windowContext, oemWindowLayoutInfo)
        )

        backend.registerLayoutChangeCallback(windowContext, Runnable::run, consumer)
        component.emit(newTestOEMWindowLayoutInfo(windowContext))
        backend.unregisterLayoutChangeCallback(consumer)

        backend.registerLayoutChangeCallback(windowContext, Runnable::run, consumer)
        component.emit(newTestOEMWindowLayoutInfo(windowContext))
        backend.unregisterLayoutChangeCallback(consumer)
        consumer.assertValues(expected)
    }

    internal companion object {
        private fun newTestOEMWindowLayoutInfo(activity: Activity): OEMWindowLayoutInfo {
            val bounds = WindowMetricsCalculatorCompat.computeCurrentWindowMetrics(activity).bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val feature = OEMFoldingFeature(featureBounds, TYPE_HINGE, STATE_FLAT)
            val displayFeatures = listOf(feature)
            return OEMWindowLayoutInfo(displayFeatures)
        }

        /**
         * Creates an empty OEMWindowLayoutInfo. Note that before R context needs to be an
         * [Activity]. After R Context can be an [Activity] or a [UiContext] created with
         * [Context#createWindowContext] or [InputMethodService].
         */
        @RequiresApi(Build.VERSION_CODES.R)
        private fun newTestOEMWindowLayoutInfo(@UiContext context: Context): OEMWindowLayoutInfo {
            val bounds = WindowMetricsCalculatorCompat.computeCurrentWindowMetrics(context).bounds
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

        override fun addWindowLayoutInfoListener(
            context: Context,
            consumer: OEMConsumer<OEMWindowLayoutInfo>,
        ) {
            records.add(AddCall(context))
        }

        override fun removeWindowLayoutInfoListener(consumer: JavaConsumer<OEMWindowLayoutInfo>) {
        }

        class AddCall(val context: Context)

        fun hasAddCall(context: Context): Boolean {
            return records.any { addRecord -> addRecord.context == context }
        }
    }

    private class FakeWindowComponent : WindowLayoutComponent {

        val consumers = mutableListOf<JavaConsumer<OEMWindowLayoutInfo>>()
        val oemConsumers = mutableListOf<OEMConsumer<OEMWindowLayoutInfo>>()

        override fun addWindowLayoutInfoListener(
            activity: Activity,
            consumer: JavaConsumer<OEMWindowLayoutInfo>
        ) {
            consumers.add(consumer)
        }

        override fun addWindowLayoutInfoListener(
            context: Context,
            consumer: OEMConsumer<OEMWindowLayoutInfo>
        ) {
            oemConsumers.add(consumer)
        }

        override fun removeWindowLayoutInfoListener(consumer: JavaConsumer<OEMWindowLayoutInfo>) {
            consumers.remove(consumer)
        }

        override fun removeWindowLayoutInfoListener(
            consumer: OEMConsumer<OEMWindowLayoutInfo>
        ) {
            oemConsumers.remove(consumer)
        }

        @SuppressLint("NewApi")
        fun emit(info: OEMWindowLayoutInfo) {
            if (ExtensionsUtil.safeVendorApiLevel < 2) {
                consumers.forEach { it.accept(info) }
            } else {
                oemConsumers.forEach { it.accept(info) }
            }
        }
    }
}
