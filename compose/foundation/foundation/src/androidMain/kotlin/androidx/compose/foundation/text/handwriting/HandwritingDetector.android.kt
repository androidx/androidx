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

package androidx.compose.foundation.text.handwriting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.input.internal.ComposeInputMethodManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TouchBoundsExpansion
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * Configures an element to act as a handwriting detector which detects stylus handwriting and
 * delegates handling of the recognised text to another element.
 *
 * Stylus movement on the element will start a handwriting session, and trigger the [callback]. The
 * [callback] implementation is expected to show and focus a text input field with a
 * [handwritingHandler] modifier which can handle the recognized text from the handwriting session.
 *
 * A common use case is a component which looks like a text input field but does not actually
 * support text input itself, and clicking on this fake text input field causes a real text input
 * field to be shown. To support handwriting initiation in this case, this modifier can be applied
 * to the fake text input field to configure it as a detector, and a [handwritingHandler] modifier
 * can be applied to the real text input field. The [callback] implementation is typically the same
 * as the `onClick` implementation for the fake text field's [clickable] modifier, which shows and
 * focuses the real text input field.
 *
 * This function returns a no-op modifier on API levels below Android U (34) as stylus handwriting
 * is not supported.
 *
 * @param callback a callback which will be triggered when stylus handwriting is detected
 * @sample androidx.compose.foundation.samples.HandwritingDetectorSample
 */
fun Modifier.handwritingDetector(callback: () -> Unit) =
    if (isStylusHandwritingSupported) {
        then(HandwritingDetectorElement(callback))
    } else {
        this
    }

private class HandwritingDetectorElement(private val callback: () -> Unit) :
    ModifierNodeElement<HandwritingDetectorNode>() {
    override fun create() = HandwritingDetectorNode(callback)

    override fun update(node: HandwritingDetectorNode) {
        node.callback = callback
    }

    override fun hashCode() = 31 * callback.hashCode()

    override fun equals(other: Any?) =
        (this === other) or ((other is HandwritingDetectorElement) && callback === other.callback)

    override fun InspectorInfo.inspectableProperties() {
        name = "handwritingDetector"
        properties["callback"] = callback
    }
}

private class HandwritingDetectorNode(var callback: () -> Unit) :
    DelegatingNode(), PointerInputModifierNode {
    private val composeImm by
        lazy(LazyThreadSafetyMode.NONE) { ComposeInputMethodManager(requireView()) }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }

    val pointerInputNode =
        delegate(
            StylusHandwritingNode {
                callback()
                composeImm.prepareStylusHandwritingDelegation()
            }
        )

    override val touchBoundsExpansion: TouchBoundsExpansion
        get() = pointerInputNode.touchBoundsExpansion
}
