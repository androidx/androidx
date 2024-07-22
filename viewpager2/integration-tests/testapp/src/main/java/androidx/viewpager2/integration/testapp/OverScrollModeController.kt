/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.viewpager2.widget.ViewPager2

/**
 * It configures a spinner to show overScrollModes and sets the overScrollMode of a ViewPager2 when
 * an overScrollMode is selected.
 */
class OverScrollModeController(private val viewPager: ViewPager2, private val spinner: Spinner) {
    fun setUp() {
        val overScrollMode = viewPager.overScrollMode
        val adapter =
            ArrayAdapter(
                spinner.context,
                android.R.layout.simple_spinner_item,
                arrayOf(ALWAYS, IF_CONTENT_SCROLLS, NEVER)
            )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val initialPosition = adapter.getPosition(overScrollModeToString(overScrollMode))
        if (initialPosition >= 0) {
            spinner.setSelection(initialPosition)
        }

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewPager.overScrollMode =
                        stringToOverScrollMode(parent.selectedItem.toString())
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
    }

    private fun overScrollModeToString(overScrollMode: Int): String {
        return when (overScrollMode) {
            View.OVER_SCROLL_ALWAYS -> ALWAYS
            View.OVER_SCROLL_IF_CONTENT_SCROLLS -> IF_CONTENT_SCROLLS
            View.OVER_SCROLL_NEVER -> NEVER
            else -> throw IllegalArgumentException("OverScrollMode $overScrollMode doesn't exist")
        }
    }

    internal fun stringToOverScrollMode(string: String): Int {
        return when (string) {
            ALWAYS -> View.OVER_SCROLL_ALWAYS
            IF_CONTENT_SCROLLS -> View.OVER_SCROLL_IF_CONTENT_SCROLLS
            NEVER -> View.OVER_SCROLL_NEVER
            else -> throw IllegalArgumentException("OverScrollMode $string doesn't exist")
        }
    }

    companion object {
        private const val ALWAYS = "always"
        private const val IF_CONTENT_SCROLLS = "ifContentScrolls"
        private const val NEVER = "never"
    }
}
