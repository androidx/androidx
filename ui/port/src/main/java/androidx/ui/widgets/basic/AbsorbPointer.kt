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

package androidx.ui.widgets.basic

import androidx.ui.foundation.Key
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.RenderAbsorbPointer
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

// /// A widget that absorbs pointers during hit testing.
// ///
// /// When [absorbing] is true, this widget prevents its subtree from receiving
// /// pointer events by terminating hit testing at itself. It still consumes space
// /// during layout and paints its child as usual. It just prevents its children
// /// from being the target of located events, because it returns true from
// /// [RenderBox.hitTest].
// ///
// /// See also:
// ///
// ///  * [IgnorePointer], which also prevents its children from receiving pointer
// ///    events but is itself invisible to hit testing.
class AbsorbPointer(
    key: Key? = null,
    // /// Whether this widget absorbs pointers during hit testing.
    // ///
    // /// Regardless of whether this render object absorbs pointers during hit
    // /// testing, it will still consume space during layout and be visible during
    // /// painting.
    val absorbing: Boolean = true,
    child: Widget? = null
) : SingleChildRenderObjectWidget(
        key,
        child) {

    override fun createRenderObject(context: BuildContext): RenderObject {
        return RenderAbsorbPointer(absorbing = absorbing)
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        (renderObject as RenderAbsorbPointer).absorbing = absorbing
    }
}