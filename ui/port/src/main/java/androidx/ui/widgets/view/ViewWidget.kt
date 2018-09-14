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
import androidx.ui.foundation.Key
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.LeafRenderObjectWidget

fun <T : View> createViewWidget(key: Key, viewCreator: (context: Context) -> T): ViewWidget<T> {
    return object : ViewWidget<T>(key) {
        override fun createView(context: Context): T {
            return viewCreator(context)
        }
    }
}

/**
 * Widget used to insert a traditional View within the Crane Widget hierarchy
 * Subclasses are to implement the createView method used by the underlying
 * element to create an instance of this View and attach it to the traditional View
 * hieararchy
 */
abstract class ViewWidget<T>(key: Key) : LeafRenderObjectWidget(key) where T : View {

    lateinit var view: View

    abstract fun createView(context: Context): T

    override fun createElement(): ViewElement<T> {
        return ViewElement<T>(this)
    }

    final override fun createRenderObject(context: BuildContext): ViewRenderObject? {
        val viewHost = obtainViewHost(context)
        view = createView(viewHost.getContext())
        return ViewRenderObject(view)
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) {
        (renderObject as ViewRenderObject).view = view
    }
}