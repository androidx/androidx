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

package androidx.ui.rendering.proxybox

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.hit_test.HitTestEntry
import androidx.ui.rendering.box.RenderBox

/**
 * Signature for listening to [PointerDownEvent] events.
 *
 * Used by [Listener] and [RenderPointerListener].
 */
typealias PointerDownEventListener = (event: PointerDownEvent) -> Unit

/**
 * Signature for listening to [PointerMoveEvent] events.
 *
 * Used by [Listener] and [RenderPointerListener].
 */
typealias PointerMoveEventListener = (event: PointerMoveEvent) -> Unit

/**
 * Signature for listening to [PointerUpEvent] events.
 *
 * Used by [Listener] and [RenderPointerListener].
 */
typealias PointerUpEventListener = (event: PointerUpEvent) -> Unit

/**
 * Signature for listening to [PointerCancelEvent] events.
 *
 * Used by [Listener] and [RenderPointerListener].
 */
typealias PointerCancelEventListener = (event: PointerCancelEvent) -> Unit

/**
 * Calls callbacks in response to pointer events.
 *
 * If it has a child, defers to the child for sizing behavior.
 *
 * If it does not have a child, grows to fit the parent-provided constraints.
 *
 * The [behavior] argument defaults to [HitTestBehavior.deferToChild].
 */
open class RenderPointerListener(
    /** Called when a pointer comes into contact with the screen at this object. */
    var onPointerDown: PointerDownEventListener? = null,
    /** Called when a pointer that triggered an [onPointerDown] changes position. */
    var onPointerMove: PointerMoveEventListener? = null,
    /**
     * Called when a pointer that triggered an [onPointerDown] is no longer in
     * contact with the screen.
     */
    var onPointerUp: PointerUpEventListener? = null,
    /**
     * Called when the input from a pointer that triggered an [onPointerDown] is
     * no longer directed towards this receiver.
     */
    var onPointerCancel: PointerCancelEventListener? = null,
    behavior: HitTestBehavior = HitTestBehavior.DEFER_TO_CHILD,
    child: RenderBox? = null
) : RenderProxyBoxWithHitTestBehavior(
    behavior = behavior, child = child
) {

    override fun performResize() {
        this.size = constraints!!.biggest
    }

    override fun handleEvent(event: PointerEvent, entry: HitTestEntry) {
        assert(debugHandleEvent(event, entry))
        when (event) {
            is PointerDownEvent -> onPointerDown?.invoke(event)
            is PointerMoveEvent -> onPointerMove?.invoke(event)
            is PointerUpEvent -> onPointerUp?.invoke(event)
            is PointerCancelEvent -> onPointerCancel?.invoke(event)
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        val listeners: MutableList<String> = mutableListOf()

        if (onPointerDown != null) listeners.add("down")
        if (onPointerMove != null) listeners.add("move")
        if (onPointerUp != null) listeners.add("up")
        if (onPointerCancel != null) listeners.add("cancel")
        if (listeners.isEmpty()) listeners.add("<none>")

        properties.add(IterableProperty("listeners", listeners))
        // TODO(shepshapard): add raw listeners to the diagnostics data.
    }
}
