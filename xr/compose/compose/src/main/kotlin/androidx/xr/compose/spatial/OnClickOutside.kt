/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.spatial

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CLICK_OUTSIDE_DEBOUNCE_MILLISECONDS = 100L

/** A modifier for handling click or touch events that happen outside of the target area. */
internal fun Modifier.onClickOutside(enabled: Boolean, onClickOutside: () -> Unit) =
    this then OutsideClickNodeElement(enabled, onClickOutside)

@SuppressLint("ModifierNodeInspectableProperties")
private class OutsideClickNodeElement(var enabled: Boolean, var onClickOutside: () -> Unit) :
    ModifierNodeElement<OutsideClickNode>() {
    override fun create(): OutsideClickNode {
        return OutsideClickNode(enabled, onClickOutside)
    }

    override fun update(node: OutsideClickNode) {
        node.enabled = enabled
        node.onClickOutside = onClickOutside
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutsideClickNodeElement

        if (enabled != other.enabled) return false
        if (onClickOutside !== other.onClickOutside) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onClickOutside.hashCode()
        return result
    }
}

private class OutsideClickNode(var enabled: Boolean, var onClickOutside: () -> Unit) :
    Modifier.Node(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {

    private var inputCaptureView: InputCaptureView? = null
    private var job: Job? = null

    override fun onAttach() {
        onObservedReadsChanged()
        inputCaptureView?.show()
    }

    override fun onDetach() {
        inputCaptureView?.hide()
    }

    override fun onObservedReadsChanged() {
        observeReads {
            if (inputCaptureView == null) {
                inputCaptureView = InputCaptureView(currentValueOf(LocalView), ::onGlobalInput)
            } else {
                inputCaptureView?.targetView = currentValueOf(LocalView)
            }
        }
    }

    private fun onGlobalInput() {
        if (enabled && (job == null || job?.isCompleted == true)) {
            job =
                coroutineScope.launch {
                    // Wait a short time for the onPointerEvent to possibly cancel this operation.
                    delay(CLICK_OUTSIDE_DEBOUNCE_MILLISECONDS)
                    onClickOutside()
                }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        job?.cancel()
        // Prevent onClickOutside from being called for a short time.
        job = coroutineScope.launch { delay(CLICK_OUTSIDE_DEBOUNCE_MILLISECONDS) }
    }

    override fun onCancelPointerInput() {
        // We don't need to do anything here.
    }
}

/**
 * A View that captures global touch input events. These events could come from anywhere, even
 * outside of all of the application windows.
 *
 * This default View constructor is used by tooling (as per a warning in Android Studio). In
 * practice, use the constructor that takes a targetView and onOutsideInput.
 */
private class InputCaptureView private constructor(context: Context) : View(context) {
    constructor(targetView: View, onInput: () -> Unit) : this(targetView.context) {
        this.targetView = targetView
        this.onInput = onInput
    }

    init {
        // Assign the layout parameters for this view. See documentation for more details:
        // https://developer.android.com/reference/android/view/View#setLayoutParams(android.view.ViewGroup.LayoutParams)
        layoutParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            }
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    var targetView: View? = null
        set(value) {
            if (field != value && value != null) {
                updateLayoutParams<WindowManager.LayoutParams> {
                    // Get the Window token from the parent view
                    token = value.applicationWindowToken
                }
                if (isAttachedToWindow) {
                    windowManager.updateViewLayout(this, layoutParams)
                }
            }
            field = value
        }

    private var onInput: () -> Unit = {}

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || targetView == null) {
            return super.onTouchEvent(event)
        }
        onInput.invoke()
        return true
    }

    fun hide() {
        windowManager.removeView(this)
    }

    fun show() {
        windowManager.addView(this, layoutParams)
    }
}
