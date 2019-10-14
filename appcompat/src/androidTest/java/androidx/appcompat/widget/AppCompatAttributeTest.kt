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

import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.test.R
import androidx.appcompat.app.AppCompatActivity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = 29)
class AppCompatAttributeTest {
    @get:Rule
    val activityRule = ActivityTestRule(AppCompatActivity::class.java, true, false)

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "settings put global debug_view_attributes_application_package " +
                    "androidx.appcompat.test"
        )
        activityRule.launchActivity(null)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "settings delete global debug_view_attributes_application_package"
        )
    }

    @Test
    fun testAppCompatImageViewAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as ViewGroup
        val imageView = root.findViewById<ImageView>(R.id.image_view)
        assertTrue(imageView.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            imageView.attributeSourceResourceMap[R.attr.srcCompat]
        )
        assertEquals(
            R.layout.view_attribute_layout,
            imageView.attributeSourceResourceMap[R.attr.backgroundTint]
        )
        assertEquals(
            R.layout.view_attribute_layout,
            imageView.attributeSourceResourceMap[R.attr.backgroundTintMode]
        )
    }

    @Test
    fun testAppCompatCheckBoxAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as ViewGroup
        val checkBox = root.findViewById<CheckBox>(R.id.check_box)
        assertTrue(checkBox.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            checkBox.attributeSourceResourceMap[R.attr.buttonTint]
        )
    }

    @Test
    fun testAppCompatSeekBarAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as ViewGroup
        val seekBar = root.findViewById<SeekBar>(R.id.seek_bar)
        assertTrue(seekBar.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            seekBar.attributeSourceResourceMap[R.attr.tickMarkTint]
        )
    }

    @Test
    fun testAppCompatTextViewAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as ViewGroup
        val textView = root.findViewById<TextView>(R.id.text_view)
        assertTrue(textView.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            textView.attributeSourceResourceMap[R.attr.autoSizeTextType]
        )
    }

    @Test
    fun testSwitchCompatAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as ViewGroup
        val switchCompat = root.findViewById<SwitchCompat>(R.id.switch_compat)
        assertTrue(switchCompat.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            switchCompat.attributeSourceResourceMap[R.attr.thumbTint]
        )
    }

    @Test
    fun testToolbarAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as ViewGroup
        val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
        assertTrue(toolbar.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            toolbar.attributeSourceResourceMap[R.attr.titleMargin]
        )
    }

    @Test
    fun testLinearLayoutCompatAttributes() {
        val root = activityRule.activity.layoutInflater.inflate(
            R.layout.view_attribute_layout,
            null
        ) as LinearLayoutCompat
        assertTrue(root.attributeSourceResourceMap.isNotEmpty())
        assertEquals(
            R.layout.view_attribute_layout,
            root.attributeSourceResourceMap[R.attr.showDividers]
        )
    }
}