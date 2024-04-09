/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget.preview

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.preview.test.R
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class GlanceAppWidgetViewAdapterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    private lateinit var glanceAppWidgetViewAdapter: GlanceAppWidgetViewAdapter

    @Before
    fun setup() {
        glanceAppWidgetViewAdapter =
            activityTestRule
                .activity
                .window
                .decorView
                .findViewInHierarchy(GlanceAppWidgetViewAdapter::class.java)!!
    }

    /**
     * [AppWidgetHostView] does not support view ID, therefore we need to perform our own traversal
     * to find the [GlanceAppWidgetViewAdapter].
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : View> View.findViewInHierarchy(viewClass: Class<T>): T? {
        if (viewClass.isInstance(this)) {
            return this as T
        }
        if (this is ViewGroup) {
            (0 until childCount).forEach {
                val res = getChildAt(it).findViewInHierarchy(viewClass)
                if (res != null) {
                    return res
                }
            }
        }
        return null
    }

    private fun initAndInflate(
        className: String,
        methodName: String,
        size: DpSize,
    ) {
        activityTestRule.runOnUiThread {
            glanceAppWidgetViewAdapter.init(className, methodName, size)
            glanceAppWidgetViewAdapter.requestLayout()
        }
    }

    private inline fun <reified T> ViewGroup.getChildOfType(count: Int = 0): T? {
        var i = 0
        (0 until childCount).forEach {
            val child = getChildAt(it)
            if (child is T) {
                if (i == count) {
                    return child
                }
                i += 1
            }
        }
        return null
    }

    private fun viewNotFoundMsg(viewTypeName: String, composableName: String) =
        "Could not find the $viewTypeName View matching $composableName"

    @Test
    fun glanceAppWidgetPreview_unspecifiedSize() {
        initAndInflate(
            className = "androidx.glance.appwidget.preview.GlanceAppWidgetPreviewsKt",
            methodName = "FirstGlancePreview",
            size = DpSize.Unspecified
        )

        activityTestRule.runOnUiThread {
            val rootComposable = glanceAppWidgetViewAdapter.getChildAt(0) as ViewGroup
            val linearLayoutColumn = rootComposable.getChildOfType<LinearLayout>()
            assertNotNull(linearLayoutColumn, viewNotFoundMsg("LinearLayout", "Column"))
            val textView = linearLayoutColumn.getChildOfType<TextView>()
            assertNotNull(textView, viewNotFoundMsg("TextView", "Text"))
            assertThat(textView.text.toString())
                .isEqualTo("First Glance widget, LocalSize = Unspecified")
            val linearLayoutRow = linearLayoutColumn.getChildOfType<LinearLayout>()
            assertNotNull(linearLayoutRow, viewNotFoundMsg("LinearLayout", "Row"))
            // Backport button are implemented using FrameLayout and depending on the API version
            // Button might be wrapped in the RelativeLayout.
            val button1 = linearLayoutRow.getChildOfType<Button>()
                ?: linearLayoutRow.getChildOfType<RelativeLayout>()!!.getChildOfType<FrameLayout>()
            val button2 = linearLayoutRow.getChildOfType<Button>(1)
                ?: linearLayoutRow.getChildOfType<RelativeLayout>(1)!!.getChildOfType<FrameLayout>()
            assertNotNull(button1, viewNotFoundMsg("FrameLayout", "Button"))
            assertNotNull(button2, viewNotFoundMsg("FrameLayout", "Button"))
        }
    }

    @Test
    fun glanceAppWidgetPreview_withSize() {
        initAndInflate(
            className = "androidx.glance.appwidget.preview.GlanceAppWidgetPreviewsKt",
            methodName = "FirstGlancePreview",
            size = DpSize(Dp(123.0f), Dp(456.0f))
        )

        activityTestRule.runOnUiThread {
            val rootComposable = glanceAppWidgetViewAdapter.getChildAt(0) as ViewGroup
            val linearLayoutColumn = rootComposable.getChildOfType<LinearLayout>()
            assertNotNull(linearLayoutColumn, viewNotFoundMsg("LinearLayout", "Column"))
            val textView = linearLayoutColumn.getChildOfType<TextView>()
            assertNotNull(textView, viewNotFoundMsg("TextView", "Text"))
            assertThat(textView.text.toString())
                .isEqualTo("First Glance widget, LocalSize = 123.0.dp x 456.0.dp")
            val linearLayoutRow = linearLayoutColumn.getChildOfType<LinearLayout>()
            assertNotNull(linearLayoutRow, viewNotFoundMsg("LinearLayout", "Row"))
            // Backport button are implemented using FrameLayout and depending on the API version
            // Button might be wrapped in the RelativeLayout.
            val button1 = linearLayoutRow.getChildOfType<Button>()
                ?: linearLayoutRow.getChildOfType<RelativeLayout>()!!.getChildOfType<FrameLayout>()
            val button2 = linearLayoutRow.getChildOfType<Button>(1)
                ?: linearLayoutRow.getChildOfType<RelativeLayout>(1)!!.getChildOfType<FrameLayout>()
            assertNotNull(button1, viewNotFoundMsg("FrameLayout", "Button"))
            assertNotNull(button2, viewNotFoundMsg("FrameLayout", "Button"))
        }
    }

    companion object {
        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.glance_appwidget_adapter_test)
            }
        }
    }
}
