/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.appcompat.widget

import android.provider.Settings
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = 29)
class AppCompatAttributeTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule =
        androidx.test.rule.ActivityTestRule(AppCompatActivity::class.java, true, false)

    @Before
    fun setup() {
        getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings put global $DEBUG_VIEW_ATTRIBUTES $TEST_PACKAGE")
        assumeDebugViewAttributes(TEST_PACKAGE)
        activityRule.launchActivity(null)
    }

    @After
    fun tearDown() {
        getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings delete global $DEBUG_VIEW_ATTRIBUTES")
        assumeDebugViewAttributes(null)
    }

    @Test
    fun testAppCompatImageViewAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as ViewGroup
        val imageView = root.findViewById<ImageView>(R.id.image_view)
        assertTrue(imageView.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            imageView.attributeSourceResourceMap[androidx.appcompat.R.attr.srcCompat],
        )
        assertEquals(
            R.layout.view_attribute_layout,
            imageView.attributeSourceResourceMap[androidx.appcompat.R.attr.backgroundTint],
        )
        assertEquals(
            R.layout.view_attribute_layout,
            imageView.attributeSourceResourceMap[androidx.appcompat.R.attr.backgroundTintMode],
        )
    }

    @Test
    fun testAppCompatCheckBoxAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as ViewGroup
        val checkBox = root.findViewById<CheckBox>(R.id.check_box)
        assertTrue(checkBox.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            checkBox.attributeSourceResourceMap[androidx.appcompat.R.attr.buttonTint],
        )
    }

    @Test
    fun testAppCompatSeekBarAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as ViewGroup
        val seekBar = root.findViewById<SeekBar>(R.id.seek_bar)
        assertTrue(seekBar.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            seekBar.attributeSourceResourceMap[androidx.appcompat.R.attr.tickMarkTint],
        )
    }

    @Test
    fun testAppCompatTextViewAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as ViewGroup
        val textView = root.findViewById<TextView>(R.id.text_view)
        assertTrue(textView.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            textView.attributeSourceResourceMap[androidx.appcompat.R.attr.autoSizeTextType],
        )
    }

    @Test
    fun testSwitchCompatAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as ViewGroup
        val switchCompat = root.findViewById<SwitchCompat>(R.id.switch_compat)
        assertTrue(switchCompat.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            switchCompat.attributeSourceResourceMap[androidx.appcompat.R.attr.thumbTint],
        )
    }

    @Test
    fun testToolbarAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as ViewGroup
        val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
        assertTrue(toolbar.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            toolbar.attributeSourceResourceMap[androidx.appcompat.R.attr.titleMargin],
        )
    }

    @Test
    fun testLinearLayoutCompatAttributes() {
        val root =
            activityRule.activity.layoutInflater.inflate(R.layout.view_attribute_layout, null)
                as LinearLayoutCompat
        assertTrue(root.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            root.attributeSourceResourceMap[androidx.appcompat.R.attr.showDividers],
        )
    }

    private companion object {
        const val SETTINGS_TIMEOUT = 5000 // 5 seconds
        const val DEBUG_VIEW_ATTRIBUTES = "debug_view_attributes_application_package"
        const val TEST_PACKAGE = "androidx.appcompat.test"

        fun busyWait(timeout: Int, predicate: () -> Boolean): Boolean {
            val deadline = System.currentTimeMillis() + timeout

            do {
                if (predicate()) {
                    return true
                }
                Thread.sleep(50)
            } while (System.currentTimeMillis() < deadline)

            return false
        }

        fun assumeDebugViewAttributes(expected: String?) {
            val timeout = SETTINGS_TIMEOUT / 1000F
            val contentResolver = getInstrumentation().targetContext.contentResolver
            assumeTrue(
                "Assumed $DEBUG_VIEW_ATTRIBUTES would be $expected within $timeout seconds",
                busyWait(SETTINGS_TIMEOUT) {
                    Settings.Global.getString(contentResolver, DEBUG_VIEW_ATTRIBUTES) == expected
                },
            )
        }
    }
}
