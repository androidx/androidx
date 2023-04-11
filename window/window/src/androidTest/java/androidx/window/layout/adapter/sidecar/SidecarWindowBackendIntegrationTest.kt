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

package androidx.window.layout.adapter.sidecar

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.TestActivity
import androidx.window.TestActivity.Companion.resetResumeCounter
import androidx.window.TestActivity.Companion.waitForOnResume
import androidx.window.TestConfigChangeHandlingActivity
import androidx.window.WindowTestBase
import androidx.window.core.Version
import androidx.window.extensions.layout.FoldingFeature as ExtensionFoldingFeature
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.adapter.sidecar.ExtensionInterfaceCompat.ExtensionCallbackInterface
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import java.util.HashSet

/** Tests for the extension implementation on the device.  */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class SidecarWindowBackendIntegrationTest : WindowTestBase() {

    private lateinit var context: Context

    private val configHandlingActivityTestRule =
        ActivityScenarioRule(TestConfigChangeHandlingActivity::class.java)

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    public fun testExtensionInterface() {
        assumeExtensionV10_V01()
        val extension = SidecarWindowBackend.initAndVerifyExtension(context)
        assertTrue(extension!!.validateExtensionInterface())
    }

    @Test
    public fun testDisplayFeatureDataClass() {
        assumeExtensionV10_V01()
        val rect = Rect(0, 100, 100, 100)
        val type = 1
        val state = 1
        val displayFeature =
            ExtensionFoldingFeature(
                rect,
                type,
                state
            )
        assertEquals(rect, displayFeature.bounds)
    }

    @Test
    public fun testWindowLayoutCallback() {
        assumeExtensionV10_V01()
        val extension = SidecarWindowBackend.initAndVerifyExtension(context)
        val callbackInterface = mock<ExtensionCallbackInterface>()
        extension!!.setExtensionCallback(callbackInterface)
        activityTestRule.scenario.onActivity { activity ->
            extension.onWindowLayoutChangeListenerAdded(activity)
            assertTrue("Layout must happen after launch", activity.waitForLayout())
            verify(callbackInterface, atLeastOnce()).onWindowLayoutChanged(
                any(),
                argThat(WindowLayoutInfoValidator(activity))
            )
        }
    }

    @Test
    public fun testRegisterWindowLayoutChangeListener() {
        assumeExtensionV10_V01()
        val extension = SidecarWindowBackend.initAndVerifyExtension(context)
        activityTestRule.scenario.onActivity { activity ->
            extension!!.onWindowLayoutChangeListenerAdded(activity)
            extension.onWindowLayoutChangeListenerRemoved(activity)
        }
    }

    @Test
    public fun testWindowLayoutUpdatesOnConfigChange() {
        assumeExtensionV10_V01()
        val extension = SidecarWindowBackend.initAndVerifyExtension(context)
        val callbackInterface = mock<ExtensionCallbackInterface>()

        extension!!.setExtensionCallback(callbackInterface)
        val scenario = ActivityScenario.launch(TestConfigChangeHandlingActivity::class.java)
        scenario.onActivity { activity ->
            extension.onWindowLayoutChangeListenerAdded(activity)
            activity.resetLayoutCounter()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity.waitForLayout()
            val activityOrientation = activity.resources.configuration.orientation
            if (activityOrientation != Configuration.ORIENTATION_PORTRAIT) {
                // Orientation change did not occur on this device config. Skipping the test.
                return@onActivity
            }
            activity.resetLayoutCounter()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val layoutHappened = activity.waitForLayout()
            if (activity.resources.configuration.orientation
                != Configuration.ORIENTATION_LANDSCAPE
            ) {
                // Orientation change did not occur on this device config. Skipping the test.
                return@onActivity
            }
            assertTrue("Layout must happen after orientation change", layoutHappened)
            if (!isSidecar) {
                verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(
                        any(),
                        argThat(DistinctWindowLayoutInfoMatcher())
                    )
            } else {
                verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(any(), any())
            }
        }
    }

    @Ignore // b/277591676
    @Test
    public fun testWindowLayoutUpdatesOnRecreate() {
        assumeExtensionV10_V01()
        val extension = SidecarWindowBackend.initAndVerifyExtension(context)
        val callbackInterface = mock<ExtensionCallbackInterface>()
        extension!!.setExtensionCallback(callbackInterface)
        activityTestRule.scenario.onActivity { activity ->
            extension.onWindowLayoutChangeListenerAdded(activity)
            activity.resetLayoutCounter()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity.waitForLayout()
            if (activity.resources.configuration.orientation
                != Configuration.ORIENTATION_PORTRAIT
            ) {
                // Orientation change did not occur on this device config. Skipping the test.
                return@onActivity
            }
            resetResumeCounter()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            waitForOnResume()
            activity.waitForLayout()
            if (activity.resources.configuration.orientation
                != Configuration.ORIENTATION_LANDSCAPE
            ) {
                // Orientation change did not occur on this device config. Skipping the test.
                return@onActivity
            }
            if (!isSidecar) {
                verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(
                        any(),
                        argThat(DistinctWindowLayoutInfoMatcher())
                    )
            } else {
                verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(any(), any())
            }
        }
    }

    @Test
    public fun testVersionSupport() {
        // Only versions 1.0 and 0.1 are supported for now
        val version = SidecarCompat.sidecarVersion
        if (version != null) {
            val validVersion = Version.VERSION_0_1 == version || Version.VERSION_1_0 == version
            assertTrue("Version must be either 1.0 or 0.1", validVersion)
        }
    }

    private fun assumeExtensionV10_V01() {
        Assume.assumeTrue(
            Version.VERSION_0_1 == SidecarCompat.sidecarVersion
        )
    }

    private val isSidecar: Boolean
        get() = SidecarCompat.sidecarVersion != null

    /**
     * An argument matcher that ensures the arguments used to call are distinct.  The only exception
     * is to allow the first value to trigger twice in case the initial value is pushed and then
     * replayed.
     */
    private class DistinctWindowLayoutInfoMatcher : ArgumentMatcher<WindowLayoutInfo> {
        private val mWindowLayoutInfos: MutableSet<WindowLayoutInfo> = HashSet()
        override fun matches(windowLayoutInfo: WindowLayoutInfo): Boolean {
            return when {
                mWindowLayoutInfos.size == 1 &&
                    mWindowLayoutInfos.contains(windowLayoutInfo) -> {
                    // First element is emitted twice so it is allowed
                    true
                }
                mWindowLayoutInfos.contains(windowLayoutInfo) -> {
                    false
                }
                windowLayoutInfo.displayFeatures.isEmpty() -> {
                    true
                }
                else -> {
                    mWindowLayoutInfos.add(windowLayoutInfo)
                    true
                }
            }
        }
    }

    /**
     * An argument matcher to ensure each [WindowLayoutInfo] is valid.
     */
    private class WindowLayoutInfoValidator(private val mActivity: TestActivity) :
        ArgumentMatcher<WindowLayoutInfo> {
        override fun matches(windowLayoutInfo: WindowLayoutInfo): Boolean {
            if (windowLayoutInfo.displayFeatures.isEmpty()) {
                return true
            }
            for (displayFeature in windowLayoutInfo.displayFeatures) {
                if (!isValid(mActivity, displayFeature)) {
                    return false
                }
            }
            return true
        }
    }

    private companion object {
        private fun isValid(activity: TestActivity, displayFeature: DisplayFeature): Boolean {
            if (displayFeature as? FoldingFeature == null) {
                return false
            }
            val featureRect: Rect = displayFeature.bounds
            val windowMetrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
            if (
                featureRect.height() == 0 && featureRect.width() == 0 || featureRect.left < 0 ||
                featureRect.top < 0
            ) {
                return false
            }
            if (featureRect.right < 1 || featureRect.right > windowMetrics.bounds.width()) {
                return false
            }
            return if (
                featureRect.bottom < 1 || featureRect.bottom > windowMetrics.bounds.height()
            ) {
                false
            } else true
        }
    }
}
