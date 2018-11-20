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

package androidx.ui.gestures2

import androidx.ui.foundation.Key
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

class PointerListenerWidget(
    key: Key? = null,
    private val pointerListener: (event: PointerEvent2, pass: PointerEventPass) -> PointerEvent2,
    val behavior: HitTestBehavior = HitTestBehavior.TRANSLUCENT,
    child: Widget? = null
) : SingleChildRenderObjectWidget(
    key = key,
    child = child
) {

    override fun createRenderObject(context: BuildContext): PointerListenerRenderBox {
        return PointerListenerRenderBox(
            behavior = behavior,
            pointerListener = pointerListener
        )
    }

    override fun updateRenderObject(
        context: BuildContext,
        renderObject: RenderObject
    ) {
        (renderObject as PointerListenerRenderBox).let {
            it.behavior = behavior
            it.pointerListener = pointerListener
        }
    }
}