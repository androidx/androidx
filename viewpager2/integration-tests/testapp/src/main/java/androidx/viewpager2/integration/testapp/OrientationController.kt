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

package androidx.viewpager2.integration.testapp

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.viewpager2.widget.ViewPager2

/**
 * It configures a spinner to show orientations and sets the orientation of a ViewPager2
 * when an orientation is selected.
 */
class OrientationController(private val viewPager: ViewPager2, private val spinner: Spinner) {
    fun setUp() {
        val orientation = viewPager.orientation
        val adapter = ArrayAdapter(
            spinner.context, android.R.layout.simple_spinner_item,
            arrayOf(HORIZONTAL, VERTICAL)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val initialPosition = adapter.getPosition(orientationToString(orientation))
        if (initialPosition >= 0) {
            spinner.setSelection(initialPosition)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewPager.orientation = stringToOrientation(parent.selectedItem.toString())
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }
    }

    private fun orientationToString(orientation: Int): String {
        return when (orientation) {
            ViewPager2.ORIENTATION_HORIZONTAL -> HORIZONTAL
            ViewPager2.ORIENTATION_VERTICAL -> VERTICAL
            else -> throw IllegalArgumentException("Orientation $orientation doesn't exist")
        }
    }

    internal fun stringToOrientation(string: String): Int {
        return when (string) {
            HORIZONTAL -> ViewPager2.ORIENTATION_HORIZONTAL
            VERTICAL -> ViewPager2.ORIENTATION_VERTICAL
            else -> throw IllegalArgumentException("Orientation $string doesn't exist")
        }
    }

    companion object {
        private const val HORIZONTAL = "horizontal"
        private const val VERTICAL = "vertical"
    }
}
