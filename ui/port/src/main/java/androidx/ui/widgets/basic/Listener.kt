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
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.rendering.proxybox.PointerCancelEventListener
import androidx.ui.rendering.proxybox.PointerDownEventListener
import androidx.ui.rendering.proxybox.PointerMoveEventListener
import androidx.ui.rendering.proxybox.PointerUpEventListener
import androidx.ui.rendering.proxybox.RenderPointerListener
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

// TODO(Migration/shepshapard): Porting tests requires widget testing infastructure.
/**
 * A widget that calls callbacks in response to pointer events.
 *
 * Rather than listening for raw pointer events, consider listening for
 * higher-level gestures using [GestureDetector].
 *
 * ## Layout behavior
 *
 * _See [BoxConstraints] for an introduction to box layout models._
 *
 * If it has a child, this widget defers to the child for sizing behavior. If
 * it does not have a child, it grows to fit the parent instead.
 *
 * The [behavior] argument defaults to [HitTestBehavior.deferToChild].
 */
class Listener(
    key: Key? = null,
    /** Called when a pointer comes into contact with the screen at this object. */
    private val onPointerDown: PointerDownEventListener? = null,
    /** Called when a pointer that triggered an [onPointerDown] changes position. */
    private val onPointerMove: PointerMoveEventListener? = null,
    /** Called when a pointer that triggered an [onPointerDown] is no longer in contact with the screen. */
    private val onPointerUp: PointerUpEventListener? = null,
    /** Called when the input from a pointer that triggered an [onPointerDown] is no longer directed towards this receiver. */
    private val onPointerCancel: PointerCancelEventListener? = null,
    /** How to behave during hit testing. */
    val behavior: HitTestBehavior = HitTestBehavior.DEFER_TO_CHILD,
    child: Widget? = null
) : SingleChildRenderObjectWidget(
    key = key,
    child = child
) {

    override fun createRenderObject(context: BuildContext): RenderPointerListener {
        return RenderPointerListener(
            onPointerDown = onPointerDown,
            onPointerMove = onPointerMove,
            onPointerUp = onPointerUp,
            onPointerCancel = onPointerCancel,
            behavior = behavior
        )
    }

    override fun updateRenderObject(
        context: BuildContext,
        renderObject: RenderObject
    ) {
        (renderObject as RenderPointerListener).let {
            it.onPointerDown = onPointerDown
            it.onPointerMove = onPointerMove
            it.onPointerUp = onPointerUp
            it.onPointerCancel = onPointerCancel
            it.behavior = behavior
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        val listeners: MutableList<String> = mutableListOf()
        if (onPointerDown != null) listeners.add("down")
        if (onPointerMove != null) listeners.add("move")
        if (onPointerUp != null) listeners.add("up")
        if (onPointerCancel != null) listeners.add("cancel")
        properties.add(IterableProperty("listeners", listeners, ifEmpty = "<none>"))
        properties.add(EnumProperty("behavior", behavior))
    }
}