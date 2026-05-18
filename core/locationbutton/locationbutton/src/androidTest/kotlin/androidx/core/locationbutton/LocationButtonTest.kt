/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.core.locationbutton

import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.os.Build
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.core.locationbutton.testing.TestLocationButtonProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [LocationButton]. */
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 24)
public class LocationButtonTest {

    private val isRemoteRenderingSupported =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule public val activityRule = ActivityScenarioRule(LocationButtonTestActivity::class.java)

    @Test
    public fun testLocationButtonIsRendered() {
        val provider = TestLocationButtonProvider.create()
        lateinit var button: LocationButton

        activityRule.scenario.onActivity { activity ->
            button =
                LocationButton(activity).apply {
                    setLocationButtonProviderForTesting(provider)
                    activity.setContentView(this)
                }
        }

        instrumentation.waitForIdleSync()

        assertThat(button).isNotNull()
        assertThat(button.isRemoteSessionActive).isEqualTo(isRemoteRenderingSupported)
        assertThat(button.isSurfaceViewVisible).isEqualTo(isRemoteRenderingSupported)
        assertThat(button.isLocalButtonVisible).isEqualTo(!isRemoteRenderingSupported)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    public fun testLocationButtonRequest() {
        var capturedRequest: LocationButtonRequest? = null
        val provider =
            object :
                TestLocationButtonProvider(
                    InstrumentationRegistry.getInstrumentation().targetContext
                ) {
                override fun onSessionRequestReceived(
                    request: LocationButtonRequest,
                    session: LocationButtonSession,
                ) {
                    capturedRequest = request
                }
            }
        var button: LocationButton? = null

        activityRule.scenario.onActivity { activity ->
            button =
                LocationButton(activity).apply {
                    setLocationButtonProviderForTesting(provider)

                    setCornerRadius(12f)
                    setPressedCornerRadius(8f)
                    setTextColor(android.graphics.Color.RED)
                    setIconTint(android.graphics.Color.BLUE)
                    setStrokeColor(android.graphics.Color.BLACK)
                    setStrokeWidth(4)
                    setTextType(LocationButton.TEXT_TYPE_USE_PRECISE_LOCATION)

                    activity.setContentView(this)
                }
        }

        instrumentation.waitForIdleSync()

        assertThat(button!!.isRemoteSessionActive).isTrue()
        assertThat(capturedRequest).isNotNull()

        val request = capturedRequest!!
        assertThat(request.cornerRadius).isEqualTo(12f)
        assertThat(request.pressedCornerRadius).isEqualTo(8f)
        assertThat(request.textColor).isEqualTo(android.graphics.Color.RED)
        assertThat(request.iconTint).isEqualTo(android.graphics.Color.BLUE)
        assertThat(request.strokeColor).isEqualTo(android.graphics.Color.BLACK)
        assertThat(request.strokeWidth).isEqualTo(4)
        assertThat(request.textType).isEqualTo(LocationButton.TEXT_TYPE_USE_PRECISE_LOCATION)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    public fun testLocationButtonSessionClosedOnDetach() {
        val provider = TestLocationButtonProvider.create()
        lateinit var button: LocationButton
        var parent: FrameLayout? = null

        activityRule.scenario.onActivity { activity ->
            button =
                LocationButton(activity).apply { setLocationButtonProviderForTesting(provider) }

            // Wrap the button inside a parent container to remove it easily
            parent =
                FrameLayout(activity).apply {
                    addView(button)
                    activity.setContentView(this)
                }
        }

        // Wait for UI to settle and remote session to open
        instrumentation.waitForIdleSync()

        assertThat(button.isRemoteSessionActive).isTrue()

        // Detach the view from the layout on the Main thread
        activityRule.scenario.onActivity { _ -> parent!!.removeView(button) }

        // Wait for the detach callbacks to be processed
        instrumentation.waitForIdleSync()

        // Assert that the remote session has been closed
        assertThat(button.isRemoteSessionActive).isFalse()
    }

    @Test
    public fun testLocationButtonThrowsExceptionOnNonActivityContext() {
        activityRule.scenario.onActivity { activity ->
            // Use a ContextThemeWrapper wrapping the Application Context to provide the
            // required theme attributes so the view initializes successfully without crashing
            // in the constructor with an UnsupportedOperationException.
            val themedContext =
                androidx.appcompat.view.ContextThemeWrapper(
                    activity.applicationContext,
                    androidx.appcompat.R.style.Theme_AppCompat,
                )
            val button = LocationButton(themedContext)

            val exception =
                assertThrows(IllegalStateException::class.java) {
                    button.checkActivityContextForTesting()
                }
            assertThat(exception.message)
                .contains("LocationButton must be hosted within an Activity context")
        }
    }

    @Test
    public fun testLocationButtonThrowsExceptionOnAddView() {
        activityRule.scenario.onActivity { activity ->
            val button = LocationButton(activity)
            val child = android.view.View(activity)

            val exception =
                assertThrows(UnsupportedOperationException::class.java) { button.addView(child) }

            assertThat(exception.message).contains("Cannot add views to LocationButton")
        }
    }

    @Test
    public fun testLocationButtonMeasureAppliesConstraints() {
        activityRule.scenario.onActivity { activity ->
            val button = LocationButton(activity)

            // Apply large padding to test clamping (Max 8dp)
            val maxPaddingPx = (8 * activity.resources.displayMetrics.density).toInt()
            button.setPadding(100, 100, 100, 100)

            // Request EXACTLY 500dp width and EXACTLY 300dp height
            val density = activity.resources.displayMetrics.density
            val widthSpec =
                MeasureSpec.makeMeasureSpec((500 * density).toInt(), MeasureSpec.EXACTLY)
            val heightSpec =
                MeasureSpec.makeMeasureSpec((300 * density).toInt(), MeasureSpec.EXACTLY)

            button.measure(widthSpec, heightSpec)

            // Assert height is capped at 136dp
            val maxHeightPx = (136 * density).toInt()
            assertThat(button.measuredHeight).isEqualTo(maxHeightPx)
        }
    }

    @Test
    public fun testLocationButtonMeasureWrapContent() {
        activityRule.scenario.onActivity { activity ->
            val button = LocationButton(activity)

            // Set to wrap_content
            val wrapSpec = MeasureSpec.makeMeasureSpec(1000, MeasureSpec.AT_MOST)
            button.measure(wrapSpec, wrapSpec)

            // Ensure width and height are resolved to at least the minimum 48dp bounds
            val minSizePx = (48 * activity.resources.displayMetrics.density).toInt()
            assertThat(button.measuredWidth).isAtLeast(minSizePx)
            assertThat(button.measuredHeight).isAtLeast(minSizePx)
        }
    }

    @Test
    public fun testLocationButtonPropertySetters() {
        activityRule.scenario.onActivity { activity ->
            val button = LocationButton(activity)

            button.setCornerRadius(12f)
            button.setPressedCornerRadius(8f)
            button.setTextColor(android.graphics.Color.RED)
            button.setIconTint(android.graphics.Color.BLUE)
            button.setStrokeColor(android.graphics.Color.BLACK)
            button.setStrokeWidth(4)
            button.setTextType(LocationButton.TEXT_TYPE_USE_PRECISE_LOCATION)
            button.setCompositionOrder(1)

            assertThat(button).isNotNull()
        }
    }

    @Test
    public fun testLocationButtonListener() {
        activityRule.scenario.onActivity { activity ->
            val button = LocationButton(activity)

            button.setLocationButtonListener(
                object : LocationButtonListener {
                    override fun onPermissionResult(isGranted: Boolean) {
                        // no-op for registration verification
                    }

                    override fun onRequestPermissions() {}
                }
            )

            // Test clear listener
            button.setLocationButtonListener(null)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    public fun testLocationButtonListenerExecution() {
        var capturedSession: LocationButtonSession? = null
        val provider =
            object :
                TestLocationButtonProvider(
                    InstrumentationRegistry.getInstrumentation().targetContext
                ) {
                override fun onSessionRequestReceived(
                    request: LocationButtonRequest,
                    session: LocationButtonSession,
                ) {
                    capturedSession = session
                }
            }
        var button: LocationButton? = null
        var permissionGranted = false

        activityRule.scenario.onActivity { activity ->
            button =
                LocationButton(activity).apply {
                    setLocationButtonProviderForTesting(provider)
                    setLocationButtonListener(
                        object : LocationButtonListener {
                            override fun onPermissionResult(isGranted: Boolean) {
                                permissionGranted = isGranted
                            }

                            override fun onRequestPermissions() {}
                        }
                    )
                    activity.setContentView(this)
                }
        }

        instrumentation.waitForIdleSync()

        assertThat(button!!.isRemoteSessionActive).isTrue()
        assertThat(capturedSession).isNotNull()

        // Simulate permission result from system via provider helper
        activityRule.scenario.onActivity { _ ->
            provider.notifyPermissionResult(capturedSession!!, true)
        }

        instrumentation.waitForIdleSync()

        assertThat(permissionGranted).isTrue()
    }
}
