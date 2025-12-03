/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import org.w3c.dom.HTMLElement

internal class WebInteropViewHolder<T : HTMLElement>(
    factory: () -> T,
    interopContainer: InteropContainer,
    compositeKeyHashCode: CompositeKeyHashCode,
) : WebInteropElementHolder<T>(
    factory,
    interopContainer,
    compositeKeyHashCode
) {
    init {
        group.htmlElement.appendChild((interopView as HTMLElement).apply { style.apply {
            width = "100%"
            height = "100%"
        }})

        platformModifier = Modifier
    }

    override var userComponentRect: String
        get() = interopView.style.cssText
        set(value) {
            interopView.style.cssText = value
        }

    override fun insertInteropView(root: InteropViewGroup, index: Int) {
        val referenceNode = root.htmlElement.children.item(index)
        if (referenceNode != null) {
            root.htmlElement.insertBefore(group.htmlElement, referenceNode)
        } else {
            root.htmlElement.appendChild(group.htmlElement)
        }
        super.insertInteropView(root, index)
    }


    override fun removeInteropView(root: InteropViewGroup) {
        root.htmlElement.removeChild(group.htmlElement)
        super.removeInteropView(root)
    }

    override fun dispatchToView(pointerEvent: PointerEvent) {}
}
