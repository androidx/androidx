/*
 * Copyright 2025 The Android Open Source Project
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

import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.os.LocaleList
import android.view.View.AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SlidingPaneLayoutFindViewByAccessibilityIdTraversalTest {

    @After
    fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    fun testAccessibilityService_canFindChildren() {
        // View#findViewByAccessibilityIdTraversal is a hidden method and we can't directly test it.
        // In this test, we exploit that UiDevice is an Accessibility service and its findObject
        // used the findViewByAccessibilityIdTraversal on API 28 and under.
        // And if UiDevice can find the children, then other A11y services like Talkback will work
        // as intended.
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val layout =
                activity.layoutInflater.inflate(
                    R.layout.user_resizeable_slidingpanelayout,
                    null,
                    false,
                )
            container.addView(layout, ViewGroup.LayoutParams(300, 500))

            val spl = layout.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            spl.isUserResizingEnabled = true
            spl.isOverlappingEnabled = false

            activity.setContentView(container)
        }

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val layout = withActivity { findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout) }

            assertThat(layout.isUserResizable).isTrue()
            assertThat(device.findObject(By.text("List"))).isNotNull()
            assertThat(device.findObject(By.text("Detail"))).isNotNull()
        }
    }

    // We have SdkSuppress because ViewGroup#dispatchProvideAutofillStructure is available only
    // after API 26.
    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testAutofill_canFindChildren() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val layout =
                activity.layoutInflater.inflate(
                    R.layout.user_resizeable_slidingpanelayout,
                    null,
                    false,
                )
            container.addView(
                layout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )

            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val slidingPaneLayout = withActivity {
                findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            }
            val viewStructure = TestViewStructure()
            slidingPaneLayout.dispatchProvideAutofillStructure(
                viewStructure,
                AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS,
            )
            // Ensure that the children is visible to Autofill service.
            assertThat(viewStructure.getChildCount()).isEqualTo(2)
        }
    }

    // SdkSuppress because dispatchProvideStructure is introduced in API 23.
    @Test
    fun testDispatchProvideStructure_createViewStructureForChildrenView() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val layout =
                activity.layoutInflater.inflate(
                    R.layout.user_resizeable_slidingpanelayout,
                    null,
                    false,
                ) as SlidingPaneLayout

            container.addView(
                layout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )

            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val slidingPaneLayout = withActivity {
                findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            }
            val viewStructure = TestViewStructure()
            slidingPaneLayout.dispatchProvideStructure(viewStructure)
            assertThat(viewStructure.getChildCount()).isEqualTo(2)
        }
    }

    @RequiresApi(23)
    private class TestViewStructure : ViewStructure() {
        private var childMap = mutableMapOf<Int, TestViewStructure>()

        override fun setId(id: Int, packageName: String?, typeName: String?, entryName: String?) {}

        override fun setDimens(
            left: Int,
            top: Int,
            scrollX: Int,
            scrollY: Int,
            width: Int,
            height: Int,
        ) {}

        override fun setTransformation(matrix: Matrix?) {}

        override fun setElevation(elevation: Float) {}

        override fun setAlpha(alpha: Float) {}

        override fun setVisibility(visibility: Int) {}

        override fun setEnabled(state: Boolean) {}

        override fun setClickable(state: Boolean) {}

        override fun setLongClickable(state: Boolean) {}

        override fun setContextClickable(state: Boolean) {}

        override fun setFocusable(state: Boolean) {}

        override fun setFocused(state: Boolean) {}

        override fun setAccessibilityFocused(state: Boolean) {}

        override fun setCheckable(state: Boolean) {}

        override fun setChecked(state: Boolean) {}

        override fun setSelected(state: Boolean) {}

        override fun setActivated(state: Boolean) {}

        override fun setOpaque(opaque: Boolean) {}

        override fun setClassName(className: String?) {}

        override fun setContentDescription(contentDescription: CharSequence?) {}

        override fun setText(text: CharSequence?) {}

        override fun setText(text: CharSequence?, selectionStart: Int, selectionEnd: Int) {}

        override fun setTextStyle(size: Float, fgColor: Int, bgColor: Int, style: Int) {}

        override fun setTextLines(charOffsets: IntArray?, baselines: IntArray?) {}

        override fun setHint(hint: CharSequence?) {}

        override fun getText(): CharSequence? = null

        override fun getTextSelectionStart(): Int = 0

        override fun getTextSelectionEnd(): Int = 0

        override fun getHint(): CharSequence? = null

        override fun getExtras(): Bundle? = null

        override fun hasExtras(): Boolean = false

        override fun setChildCount(num: Int) {}

        override fun addChildCount(num: Int): Int = childMap.size

        override fun getChildCount(): Int = childMap.size

        override fun newChild(index: Int): ViewStructure? {
            if (!childMap.contains(index)) {
                childMap[index] = TestViewStructure()
            }
            return childMap[index]
        }

        override fun asyncNewChild(index: Int): ViewStructure? {
            return newChild(index)
        }

        override fun getAutofillId(): AutofillId? {
            return null
        }

        override fun setAutofillId(id: AutofillId) {}

        override fun setAutofillId(parentId: AutofillId, virtualId: Int) {}

        override fun setAutofillType(type: Int) {}

        override fun setAutofillHints(hint: Array<out String?>?) {}

        override fun setAutofillValue(value: AutofillValue?) {}

        override fun setAutofillOptions(options: Array<out CharSequence?>?) {}

        override fun setInputType(inputType: Int) {}

        override fun setDataIsSensitive(sensitive: Boolean) {}

        override fun asyncCommit() {}

        override fun setWebDomain(domain: String?) {}

        override fun setLocaleList(localeList: LocaleList?) {}

        override fun newHtmlInfoBuilder(tagName: String): HtmlInfo.Builder? = null

        override fun setHtmlInfo(htmlInfo: HtmlInfo) {}

        // Override the hidden ViewStructure#getTempRect method which is called in
        // View#populateVirtualStructure().
        fun getTempRect(): Rect {
            return Rect()
        }
    }
}
