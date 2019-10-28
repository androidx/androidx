/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the mechanism enforcing that page width/height are 100%/100%.
 * This assumption is used in a number of places in code.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PageFillTest : BaseTest() {
    @Test
    fun test_pageFillEnforced_width() {
        test_pageFillEnforced(LayoutParams(MATCH_PARENT, 50))
    }

    @Test
    fun test_pageFillEnforced_height() {
        test_pageFillEnforced(LayoutParams(50, MATCH_PARENT))
    }

    @Test
    fun test_pageFillEnforced_both() {
        test_pageFillEnforced(LayoutParams(50, 50))
    }

    private fun test_pageFillEnforced(layoutParams: LayoutParams) {
        val fixedViewSizeAdapter = object : RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return object : ViewHolder(View(parent.context).apply {
                    this.layoutParams = layoutParams
                }) {}
            }

            override fun getItemCount(): Int = 1
            override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
        }

        setUpTest(ORIENTATION_HORIZONTAL).apply {
            runOnUiThreadSync {
                viewPager.adapter = fixedViewSizeAdapter
                try {
                    viewPager.measure(0, 0)
                    fail("Expected exception was not thrown")
                } catch (e: IllegalStateException) {
                    assertThat(e.message, containsString(
                            "Pages must fill the whole ViewPager2 (use match_parent)"))
                }
            }
        }
    }
}
