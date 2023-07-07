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

package androidx.glance.wear.tiles.preview

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.glance.wear.tiles.preview.test.R
import androidx.test.filters.MediumTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class GlanceTileServiceViewAdapterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    private lateinit var glanceTileServiceViewAdapter: GlanceTileServiceViewAdapter

    @Before
    fun setup() {
        glanceTileServiceViewAdapter =
            activityTestRule.activity.findViewById(R.id.glance_tile_service_view_adapter)
    }

    private fun initAndInflate(
        className: String,
        methodName: String,
    ) {
        activityTestRule.runOnUiThread {
            glanceTileServiceViewAdapter.init(className, methodName)
            glanceTileServiceViewAdapter.requestLayout()
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
    fun testFirstGlancePreview() {
        initAndInflate(
            "androidx.glance.wear.tiles.preview.FirstGlancePreviewKt",
            "FirstGlancePreview")

        activityTestRule.runOnUiThread {
            val rootComposable = glanceTileServiceViewAdapter.getChildAt(0) as ViewGroup
            val linearLayoutColumn = rootComposable.getChildOfType<LinearLayout>()
            Assert.assertNotNull(viewNotFoundMsg("LinearLayout", "Column"), linearLayoutColumn)

            val frameLayout = linearLayoutColumn!!.getChildOfType<FrameLayout>()
            val textView = frameLayout!!.getChildOfType<TextView>()
            Assert.assertNotNull(viewNotFoundMsg("TextView", "Text"), textView)

            val linearLayoutRow = linearLayoutColumn.getChildOfType<LinearLayout>()
            Assert.assertNotNull(viewNotFoundMsg("LinearLayout", "Row"), linearLayoutRow)

            val button1 =
                linearLayoutRow!!.getChildOfType<FrameLayout>()!!.getChildOfType<TextView>()
            val button2 =
                linearLayoutRow.getChildOfType<FrameLayout>(1)!!.getChildOfType<TextView>()
            Assert.assertNotNull(viewNotFoundMsg("TextView", "Button"), button1)
            Assert.assertEquals("Button 1", button1!!.text.toString())
            Assert.assertNotNull(viewNotFoundMsg("TextView", "Button"), button2)
            Assert.assertEquals("Button 2", button2!!.text.toString())
        }
    }

    companion object {
        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.glance_tile_service_adapter_test)
            }
        }
    }
}
