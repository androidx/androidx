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

import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.gestures.drag_details.DragUpdateDetails
import androidx.ui.gestures.drag_details.GestureDragUpdateCallback
import androidx.ui.gestures.long_press.GestureLongPressCallback
import androidx.ui.gestures.tap.GestureTapCallback
import androidx.ui.rendering.box.RenderBox
import androidx.ui.semantics.SemanticsAction
import androidx.ui.semantics.SemanticsConfiguration

// TODO(Migration/shepshapard): Porting of tests requires SemanticsConfiguration which is not yet
// ported.
/**
 * Listens for the specified gestures from the semantics server (e.g.
 * an accessibility tool).
 */
class RenderSemanticsGestureHandler(
    child: RenderBox? = null,
    onTap: GestureTapCallback? = null,
    onLongPress: GestureLongPressCallback? = null,
    onHorizontalDragUpdate: GestureDragUpdateCallback? = null,
    onVerticalDragUpdate: GestureDragUpdateCallback? = null,
    /**
     * The fraction of the dimension of this render box to use when
     * scrolling. For example, if this is 0.8 and the box is 200 pixels
     * wide, then when a left-scroll action is received from the
     * accessibility system, it will translate into a 160 pixel
     * leftwards drag.
     */
    val scrollFactor: Double = 0.8
) : RenderProxyBox(
    child
) {

    /**
     * If non-null, the set of actions to allow. Other actions will be omitted,
     * even if their callback is provided.
     *
     * For example, if [onTap] is non-null but [validActions] does not contain
     * [SemanticsAction.tap], then the semantic description of this node will
     * not claim to support taps.
     *
     * This is normally used to filter the actions made available by
     * [onHorizontalDragUpdate] and [onVerticalDragUpdate]. Normally, these make
     * both the right and left, or up and down, actions available. For example,
     * if [onHorizontalDragUpdate] is set but [validActions] only contains
     * [SemanticsAction.scrollLeft], then the [SemanticsAction.scrollRight]
     * action will be omitted.
     */
    var validActions: Set<SemanticsAction>? = null
        set(value) {
            if (value == field) return
            field = value
            markNeedsSemanticsUpdate()
        }

    /** Called when the user taps on the render object. */
    var onTap: GestureTapCallback? = onTap
        set(value) {
            if (value == field) return
            val handlerExistenceChanged = (field != null) != (value != null)
            field = value
            if (handlerExistenceChanged) markNeedsSemanticsUpdate()
        }

    /** Called when the user presses on the render object for a long period of time. */
    var onLongPress: GestureLongPressCallback? = onLongPress
        set(value) {
            if (value == field) return
            val handlerExistenceChanged = (field != null) != (value != null)
            field = value
            if (handlerExistenceChanged) markNeedsSemanticsUpdate()
        }

    /** Called when the user scrolls to the left or to the right. */
    var onHorizontalDragUpdate: GestureDragUpdateCallback? = onHorizontalDragUpdate
        set(value) {
            if (value == field) return
            val handlerExistenceChanged = (field != null) != (value != null)
            field = value
            if (handlerExistenceChanged) markNeedsSemanticsUpdate()
        }

    /** Called when the user scrolls up or down. */
    var onVerticalDragUpdate: GestureDragUpdateCallback? = onVerticalDragUpdate
        set(value) {
            if (value == field) return
            val handlerExistenceChanged = (field != null) != (value != null)
            field = value
            if (handlerExistenceChanged) markNeedsSemanticsUpdate()
        }

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        if (onTap != null && this.isValidAction(SemanticsAction.tap)) {
            config.onTap = onTap
        }
        if (onLongPress != null && this.isValidAction(SemanticsAction.longPress)) {
            config.onLongPress = onLongPress
        }
        if (onHorizontalDragUpdate != null) {
            if (this.isValidAction(SemanticsAction.scrollRight)) {
                config.onScrollRight = ::performSemanticScrollRight
            }
            if (this.isValidAction(SemanticsAction.scrollLeft)) {
                config.onScrollLeft = ::performSemanticScrollLeft
            }
        }
        if (onVerticalDragUpdate != null) {
            if (this.isValidAction(SemanticsAction.scrollUp)) {
                config.onScrollUp = ::performSemanticScrollUp
            }
            if (this.isValidAction(SemanticsAction.scrollDown)) {
                config.onScrollDown = ::performSemanticScrollDown
            }
        }
    }

    private fun isValidAction(action: SemanticsAction) = validActions?.contains(action) ?: true

    private fun performSemanticScrollLeft() {
        onHorizontalDragUpdate?.let {
            var primaryDelta = size.width * -scrollFactor
            it(
                DragUpdateDetails(
                    delta = Offset(dx = primaryDelta, dy = 0.0),
                    primaryDelta = primaryDelta,
                    globalPosition = this.localToGlobal(size.center(Offset.zero))
                )
            )
        }
    }

    private fun performSemanticScrollRight() {
        onHorizontalDragUpdate?.let {
            var primaryDelta = size.width * scrollFactor
            it(
                DragUpdateDetails(
                    delta = Offset(dx = primaryDelta, dy = 0.0),
                    primaryDelta = primaryDelta,
                    globalPosition = this.localToGlobal(size.center(Offset.zero))
                )
            )
        }
    }

    private fun performSemanticScrollUp() {
        onVerticalDragUpdate?.let {
            var primaryDelta = size.height * -scrollFactor
            it(
                DragUpdateDetails(
                    delta = Offset(dx = 0.0, dy = primaryDelta),
                    primaryDelta = primaryDelta,
                    globalPosition = this.localToGlobal(size.center(Offset.zero))
                )
            )
        }
    }

    private fun performSemanticScrollDown() {
        onVerticalDragUpdate?.let {
            var primaryDelta = size.height * scrollFactor
            it(
                DragUpdateDetails(
                    delta = Offset(dx = 0.0, dy = primaryDelta),
                    primaryDelta = primaryDelta,
                    globalPosition = this.localToGlobal(size.center(Offset.zero))
                )
            )
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        val gestures: MutableList<String> = mutableListOf()
        if (onTap != null) gestures.add("tap")
        if (onLongPress != null) gestures.add("long press")
        if (onHorizontalDragUpdate != null) gestures.add("horizontal scroll")
        if (onVerticalDragUpdate != null) gestures.add("vertical scroll")
        if (gestures.isEmpty()) gestures.add("<none>")
        properties.add(IterableProperty("gestures", gestures))
    }
}