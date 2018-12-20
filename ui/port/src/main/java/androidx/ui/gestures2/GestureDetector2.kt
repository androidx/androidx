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
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget

data class MetaData(val touchSlop: Int)

class GestureDetector2(
    key: Key? = null,
    val metaData: MetaData,
    val child: Widget? = null,
    private val onTapDown: (() -> Unit)? = null,
    private val onTapUp: (() -> Unit)? = null,
    private val onTap: (() -> Unit)? = null,
    private val dragCallback: DragGestureRecognizerCallback? = null,
    private val scaleCallback: ScaleGestureRecognizerCallback? = null
) : StatefulWidget(
    key = key
) {
    override fun createState(): State<StatefulWidget> {
        return GestureDetector2State(
            this,
            metaData,
            onTapDown,
            onTapUp,
            onTap,
            dragCallback,
            scaleCallback
        ) as State<StatefulWidget>
    }
}

class GestureDetector2State(
    widget: GestureDetector2,
    metaData: MetaData,
    onTapDown: (() -> Unit)? = null,
    onTapUp: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    dragCallback: DragGestureRecognizerCallback? = null,
    scaleCallback: ScaleGestureRecognizerCallback? = null
) :
    State<GestureDetector2>(widget = widget) {

    private val gestureRecognizers: MutableList<GestureRecognizer2> = mutableListOf()

    init {

        if (scaleCallback != null) {
            val scaleGestureRecognizer2 =
                ScaleGestureRecognizer2(
                    metaData.touchSlop,
                    PointerEventPass.POST_UP,
                    scaleCallback
                )
            gestureRecognizers.add(scaleGestureRecognizer2)
        }

        if (dragCallback != null) {
            val dragGestureRecognizer2 =
                DragGestureRecognizer2(
                    metaData.touchSlop,
                    PointerEventPass.POST_UP,
                    dragCallback
                )
            gestureRecognizers.add(dragGestureRecognizer2)
        }

        if (onTapDown != null || onTapUp != null || onTap != null) {
            val tapGestureRecognizer = TapGestureRecognizer2(PointerEventPass.POST_UP)
            onTapDown?.let {
                tapGestureRecognizer.onTapDown = it
            }
            onTapUp?.let {
                tapGestureRecognizer.onTapUp = it
            }
            onTap?.let {
                tapGestureRecognizer.onTap = it
            }
            gestureRecognizers.add(tapGestureRecognizer)
        }
    }

    override fun build(context: BuildContext): Widget {
        return PointerListenerWidget(
            pointerListener = { pointerEvent, pass ->
                var event = pointerEvent
                gestureRecognizers.forEach {
                    event = it.handleEvent(event, pass)
                }
                return@PointerListenerWidget event
            },
            behavior = HitTestBehavior.TRANSLUCENT,
            child = widget.child
        )
    }
}