/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.slidingpanelayout.widget

import android.app.UiAutomation
import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = 19)
class SlidingPaneLayoutAccessibilityTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(
        TestActivity::class.java
    )

    private val timeout = 5000L

    private lateinit var uiAutomation: UiAutomation
    private lateinit var slidingPaneLayout: SlidingPaneLayout

    private lateinit var listPane: TextView
    private lateinit var detailPane: TextView

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        uiAutomation = instrumentation.uiAutomation

        val activity = mActivityTestRule.activity
        mActivityTestRule.runOnUiThread {
            slidingPaneLayout = activity.findViewById(R.id.sliding_pane_layout) as SlidingPaneLayout
            listPane = activity.findViewById(R.id.list_pane) as TextView
            detailPane = activity.findViewById(R.id.detail_pane) as TextView
            // On KitKat, some delegate methods aren't called for non-important views
            ViewCompat.setImportantForAccessibility(
                slidingPaneLayout, View.IMPORTANT_FOR_ACCESSIBILITY_YES)
        }
    }

    @Test
    fun testPaneOpening() {
        ViewCompat.setAccessibilityPaneTitle(detailPane, "Detail Pane")
        ViewCompat.setAccessibilityPaneTitle(listPane, "List Pane")
        mActivityTestRule.runOnUiThread {
            detailPane.visibility = View.INVISIBLE
            detailPane.viewTreeObserver.dispatchOnGlobalLayout()
        }
        uiAutomation.executeAndWaitForEvent(
            {
                try {
                    mActivityTestRule.runOnUiThread {
                        slidingPaneLayout.openPane()
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            },
            { event ->
                val isWindowStateChanged =
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                val isPaneTitle: Int = (event.contentChangeTypes
                    and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED)
                (isWindowStateChanged && isPaneTitle != 0)
            }, timeout
        )
    }

    @Test
    fun testPaneClosing() {
        ViewCompat.setAccessibilityPaneTitle(detailPane, "Detail Pane")
        ViewCompat.setAccessibilityPaneTitle(listPane, "List Pane")
        mActivityTestRule.runOnUiThread {
            detailPane.visibility = View.INVISIBLE
            detailPane.viewTreeObserver.dispatchOnGlobalLayout()
            slidingPaneLayout.openPane()
            detailPane.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT < 28) {
                detailPane.viewTreeObserver.dispatchOnGlobalLayout()
            }
        }
        uiAutomation.executeAndWaitForEvent(
            {
                try {
                    mActivityTestRule.runOnUiThread {
                        slidingPaneLayout.closePane()
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            },
            { event ->
                val isWindowStateChanged =
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                val isPaneTitle: Int = (event.contentChangeTypes
                    and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED)
                (isWindowStateChanged && isPaneTitle != 0)
            }, timeout
        )
    }
}
