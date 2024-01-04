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

package androidx.wear.tiles.tooling

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.wear.tiles.tooling.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val TEST_TILE_PREVIEWS_FILE = "androidx.wear.tiles.tooling.TestTilePreviewsKt"

class TileServiceViewAdapterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    private lateinit var tileServiceViewAdapter: TileServiceViewAdapter

    @Before
    fun setup() {
        tileServiceViewAdapter =
            activityTestRule.activity.findViewById(R.id.tile_service_view_adapter)
    }

    private fun initAndInflate(
        methodFqn: String,
    ) {
        activityTestRule.runOnUiThread {
            tileServiceViewAdapter.init(methodFqn)
            tileServiceViewAdapter.requestLayout()
        }
    }

    @Test
    fun testTilePreview() {
        initAndInflate("$TEST_TILE_PREVIEWS_FILE.TilePreview")

        activityTestRule.runOnUiThread {
            val textView =
                (tileServiceViewAdapter.getChildAt(0) as ViewGroup)
                    .getChildAt(0) as TextView
            assertNotNull(textView)
            assertEquals("Hello world!", textView.text.toString())
        }
    }

    @Test
    fun testTileLayoutPreview() {
        initAndInflate("$TEST_TILE_PREVIEWS_FILE.TileLayoutPreview")

        activityTestRule.runOnUiThread {
            val textView =
                (tileServiceViewAdapter.getChildAt(0) as ViewGroup)
                    .getChildAt(0) as TextView
            assertNotNull(textView)
            assertEquals("Hello world!", textView.text.toString())
        }
    }

    @Test
    fun testTileLayoutElementPreview() {
        initAndInflate("$TEST_TILE_PREVIEWS_FILE.TileLayoutElementPreview")

        activityTestRule.runOnUiThread {
            val textView =
                ((tileServiceViewAdapter.getChildAt(0) as ViewGroup)
                    .getChildAt(0) as FrameLayout).getChildAt(0) as TextView
            assertNotNull(textView)
            assertEquals("Hello world!", textView.text.toString())
        }
    }

    @Test
    fun testTilePreviewDeclaredWithPrivateMethod() {
        initAndInflate("$TEST_TILE_PREVIEWS_FILE.TilePreviewWithPrivateVisibility")

        activityTestRule.runOnUiThread {
            val textView =
                (tileServiceViewAdapter.getChildAt(0) as ViewGroup)
                    .getChildAt(0) as TextView
            assertNotNull(textView)
            assertEquals("Hello world!", textView.text.toString())
        }
    }

    @Test
    fun testTilePreviewThatHasSharedFunctionName() {
        initAndInflate("$TEST_TILE_PREVIEWS_FILE.duplicateFunctionName")

        activityTestRule.runOnUiThread {
            val textView =
                (tileServiceViewAdapter.getChildAt(0) as ViewGroup)
                    .getChildAt(0) as TextView
            assertNotNull(textView)
            assertEquals("Hello world!", textView.text.toString())
        }
    }

    companion object {
        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.tile_service_adapter_test)
            }
        }
    }
}
