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

import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2

/**
 * Populates a [Spinner] with a set of [ViewPager2.PageTransformer]s.
 * Propagates user selection to the [ViewPager2].
 */
class PageTransformerController(private val viewPager: ViewPager2, private val spinner: Spinner) {
    fun setUp() {
        val transformers = listOf(
            "None" to ViewPager2.PageTransformer { _, _ -> /* no op */ },
            "Margin 50px" to MarginPageTransformer(50),
            "Margin 32dp" to MarginPageTransformer(32.dpToPx)
        )

        val cancelTranslationsTransformer = ViewPager2.PageTransformer { page, _ ->
            page.translationX = 0f
            page.translationY = 0f
        }

        spinner.adapter = ArrayAdapter(
            spinner.context, android.R.layout.simple_spinner_item,
            transformers.map { it.first }.toList()
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = transformers.first { it.first == parent.selectedItem }.second
                viewPager.setPageTransformer(CompositePageTransformer().also {
                    it.addTransformer(cancelTranslationsTransformer)
                    it.addTransformer(selected)
                })
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }
    }

    private val (Int).dpToPx: Int
        get() = TypedValue.applyDimension(
            COMPLEX_UNIT_DIP,
            this.toFloat(),
            viewPager.resources.displayMetrics
        ).toInt()
}
