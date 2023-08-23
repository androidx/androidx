/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Mark the layout rectangle as preferring to stay clear of floating windows.
 *
 * @see View.setPreferKeepClearRects
 */
fun Modifier.preferKeepClear() =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        this
    } else {
        this then preferKeepClearT(null)
    }

/**
 * Mark a rectangle within the local layout coordinates preferring to stay clear of floating
 * windows.
 * After layout, [keepClearRect] is called to determine the [Rect] to mark as keep clear.
 *
 * The [LayoutCoordinates] of the [Modifier]'s location in the layout is passed as passed as
 * [keepClearRect]'s parameter.
 *
 * @see View.setPreferKeepClearRects
 */
fun Modifier.preferKeepClear(keepClearRect: (LayoutCoordinates) -> Rect) =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        this
    } else {
        this then preferKeepClearT(keepClearRect)
    }

@Suppress("NOTHING_TO_INLINE", "ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private inline fun preferKeepClearT(
    noinline rect: ((LayoutCoordinates) -> Rect)?
): Modifier = PreferKeepClearElement(rect)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class PreferKeepClearElement(
    val clearRect: ((LayoutCoordinates) -> Rect)?
) : ModifierNodeElement<PreferKeepClearNode>() {
    override fun create(): PreferKeepClearNode {
        return PreferKeepClearNode(clearRect)
    }

    override fun update(node: PreferKeepClearNode) {
        node.rect = clearRect
    }

    override fun hashCode(): Int {
        return clearRect.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PreferKeepClearNode) return false
        return clearRect == other.rect
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "preferKeepClearBounds"
        if (clearRect != null) {
            properties["clearRect"] = clearRect
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class PreferKeepClearNode(
    rect: ((LayoutCoordinates) -> Rect)?
) : RectListNode(rect) {
    override fun currentRects(): MutableVector<android.graphics.Rect> {
        val rects = mutableVectorOf<android.graphics.Rect>()
        rects.addAll(view.preferKeepClearRects)
        return rects
    }

    override fun updateRects(rects: MutableVector<android.graphics.Rect>) {
        view.preferKeepClearRects = rects.asMutableList()
    }
}
