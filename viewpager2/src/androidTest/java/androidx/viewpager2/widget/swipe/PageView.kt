/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.test.R

private const val PAGE_COLOR_EVEN = 0xFFFF0000.toInt()
private const val PAGE_COLOR_ODD = 0xFF0000FF.toInt()

object PageView {
    fun inflatePage(parent: ViewGroup): View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_test_layout, parent, false)

    fun findPageInActivity(activity: Activity): View = activity.findViewById(R.id.text_view)

    fun getPageText(page: View): String = (page as TextView).text.toString()

    fun setPageText(page: View, text: String) {
        (page as TextView).text = text
    }

    fun setPageColor(page: View, position: Int) {
        page.setBackgroundColor(if (position % 2 == 0) PAGE_COLOR_EVEN else PAGE_COLOR_ODD)
    }
}
