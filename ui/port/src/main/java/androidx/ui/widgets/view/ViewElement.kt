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

import android.view.View
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.framework.Element
import androidx.ui.widgets.framework.LeafRenderObjectElement

/**
 * RenderObjectElement used to attach and detach a traditional View to the ViewHost
 * within the Crane Widget Hierarchy
 */
class ViewElement<T>(widget: ViewWidget<T>) : LeafRenderObjectElement(widget) where T : View {

    override fun forgetChild(child: Element) {
        TODO("njawad/Migration figure out how to handle forgetChild appropriately")
    }

    override fun performRebuild() {
        TODO("njawad/Migration figure out how to handle performRebuild appropriately")
    }

    override fun insertChildRenderObject(child: RenderObject?, slot: Any?) {
        val viewHost = obtainViewHost(this)
        viewHost.addView((child as ViewRenderObject).view)
    }

    override fun moveChildRenderObject(child: RenderObject?, slot: Any?) {
        TODO("njawad/Migration figure out how to handle moveChildRenderObject appropriately")
    }

    override fun removeChildRenderObject(child: RenderObject?) {
        val viewHost = obtainViewHost(this)
        viewHost.removeView((child as ViewRenderObject).view)
    }
}