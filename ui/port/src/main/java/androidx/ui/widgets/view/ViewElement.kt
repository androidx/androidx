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
import androidx.ui.widgets.framework.Element
import androidx.ui.widgets.framework.LeafRenderObjectElement

/**
 * RenderObjectElement used to attach and detach a traditional View to the ViewHost
 * within the Crane Widget Hierarchy
 */
class ViewElement<T>(widget: ViewWidget<T>) : LeafRenderObjectElement(widget) where T : View {

    // Cached ViewHost instance that is resolved in mount
    // note, ViewHost cannot be queried before usage as the inherited widget map is cleared
    // in Element#deactivate which is invoked before unmmount
    lateinit var viewHost: ViewHost

    override fun mount(parent: Element?, newSlot: Any?) {
        super.mount(parent, newSlot)
        viewHost = obtainViewHost(this)
        // Add the target View once this element has been active for the first time
        viewHost.addView((renderObject as ViewRenderObject).view)
    }

    override fun unmount() {
        viewHost.removeView((renderObject as ViewRenderObject).view)
        super.unmount()
    }
}