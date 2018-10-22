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

package androidx.ui.widgets.binding

import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.RenderObjectWithChildMixin
import androidx.ui.scheduler.binding.SchedulerBinding
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.BuildOwner
import androidx.ui.widgets.framework.RenderObjectWidget
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.framework.key.GlobalObjectKey

/**
 * A bridge from a [RenderObject] to an [Element] tree.
 *
 * The given container is the [RenderObject] that the [Element] tree should be
 * inserted into. It must be a [RenderObject] that implements the
 * [RenderObjectWithChildMixin] protocol. The type argument `T` is the kind of
 * [RenderObject] that the container expects as its child.
 *
 * Used by [runApp] to bootstrap applications.
 *
 * Ctor comment:
 * Creates a bridge from a [RenderObject] to an [Element] tree.
 *
 * Used by [WidgetsBinding] to attach the root widget to the [RenderView].
 */
class RenderObjectToWidgetAdapter<T : RenderObject>(
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget,
    /** The [RenderObject] that is the parent of the [Element] created by this widget. */
    val container: RenderObjectWithChildMixin<T>,
    /** A short description of this widget used by debugging aids. */
    val debugShortDescription: String?
// TODO(Migration/Filip): Added artificial generic argument below
) : RenderObjectWidget(key = GlobalObjectKey<State<StatefulWidget>>(container)) {

    override fun createElement(): RenderObjectToWidgetElement<T> =
            RenderObjectToWidgetElement<T>(this)

    override fun createRenderObject(context: BuildContext): RenderObjectWithChildMixin<T> =
            container

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) { }

    /**
     * Inflate this widget and actually set the resulting [RenderObject] as the
     * child of [container].
     *
     * If `element` is null, this function will create a new element. Otherwise,
     * the given element will have an update scheduled to switch to this widget.
     *
     * Used by [runApp] to bootstrap applications.
     */
    fun attachToRenderTree(
        owner: BuildOwner,
        // TODO(Migration/Andrey): Crane tmp solution for providing bindings inside widgets
        schedulerBinding: SchedulerBinding,
        element: RenderObjectToWidgetElement<T>?
    ): RenderObjectToWidgetElement<T> {
        var elem = element

        if (elem == null) {
            owner.lockState {
                elem = createElement()
                assert(elem != null)
                elem!!.assignOwner(owner)
                // TODO(Migration/Andrey): Crane tmp solution for providing bindings inside widgets
                elem!!.assignSchedulerBinding(schedulerBinding)
            }
            owner.buildScope(elem!!, {
                elem!!.mount(null, null)
            })
        } else {
            elem!!._newWidget = this
            elem!!.markNeedsBuild()
        }
        return elem!!
    }

    override fun toStringShort(): String = debugShortDescription ?: super.toStringShort()
}