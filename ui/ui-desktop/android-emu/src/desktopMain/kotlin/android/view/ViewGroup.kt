/*
 * Copyright 2020 The Android Open Source Project
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

package android.view

import android.content.Context

abstract class ViewGroup(context: Context) : View(context) {
    var clipChildren: Boolean = true
    var children = mutableListOf<View>()
    val childCount = children.count()

    fun getChildAt(i: Int) = children[i]

    fun removeAllViews() {
        children.clear()
    }

    fun addView(child: android.view.View, params: ViewGroup.LayoutParams?) {
        children.add(child)
    }

    class LayoutParams(width: Int, height: Int) {
        companion object {
            const val WRAP_CONTENT = 0
        }
    }
}
