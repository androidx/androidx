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

package androidx.ui.widgets.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.ui.Type
import androidx.ui.foundation.Key
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.InheritedWidget
import androidx.ui.widgets.framework.Widget

fun obtainViewHost(context: BuildContext): ViewHost {
    return context.inheritFromWidgetOfExactType(Type(ViewHost::class.java)) as ViewHost
}

/**
 * Inherited widget used to expose the Crane root View that is used for
 * inserting traditional Views within the Crane Widget hierarchy
 */
class ViewHost(private val container: ViewGroup, key: Key, child: Widget) :
        InheritedWidget(key, child) {

    fun getContext(): Context = container.context

    override fun updateShouldNotify(oldWidget: InheritedWidget): Boolean {
        // TODO (Migration/njawad figure out how to compare ViewHost instances?
        // For now always update for correctness
        return true
    }

    fun addView(view: View): Boolean {
        if (!container.hasView(view)) {
            container.addView(view)
            return true
        } else {
            return false
        }
    }

    fun childCount(): Int = container.childCount

    fun removeView(view: View): Boolean {
        if (container.hasView(view)) {
            container.removeView(view)
            return true
        } else {
            return false
        }
    }

    private fun ViewGroup.hasView(view: View): Boolean = indexOfChild(view) != -1
}