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
import android.widget.TextView
import androidx.wear.tiles.tooling.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        className: String,
    ) {
        activityTestRule.runOnUiThread {
            tileServiceViewAdapter.init(className)
            tileServiceViewAdapter.requestLayout()
        }
    }

    @Test
    fun testTileServiceViewAdapter() {
        initAndInflate("androidx.wear.tiles.tooling.TestTileService")

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