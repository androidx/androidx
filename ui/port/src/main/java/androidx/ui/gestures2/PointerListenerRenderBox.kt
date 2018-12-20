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

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.hit_test.HitTestEntry
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.rendering.proxybox.RenderProxyBoxWithHitTestBehavior

/**
 * Calls callbacks in response to pointer events.
 *
 * If it has a child, defers to the child for sizing behavior.
 *
 * If it does not have a child, grows to fit the parent-provided constraints.
 *
 * The [behavior] argument defaults to [HitTestBehavior.deferToChild].
 */
open class PointerListenerRenderBox(
    behavior: HitTestBehavior = HitTestBehavior.TRANSLUCENT,
    child: RenderBox? = null,
    var pointerListener: (event: PointerEvent2, pass: PointerEventPass) -> PointerEvent2
) : RenderProxyBoxWithHitTestBehavior(
    behavior = behavior, child = child
) {

    override fun performResize() {
        this.size = constraints!!.biggest
    }

    override fun handleEvent(event: PointerEvent, entry: HitTestEntry) {
        throw NotImplementedError("This is not implemented for Gestures2 (Woodpecker)")
    }

    override fun handleEvent(event: PointerEvent2, entry: HitTestEntry, pass: PointerEventPass) =
        pointerListener(event, pass)

    override fun hitTestChildren(result: HitTestResult, position: Offset): Boolean {
            return child?.hitTest(result, position = position) ?: false
    }
}
