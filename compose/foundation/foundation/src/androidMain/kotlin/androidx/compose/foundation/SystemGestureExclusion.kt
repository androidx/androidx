/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
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
 * Excludes the layout rectangle from the system gesture.
 *
 * @see View.setSystemGestureExclusionRects
 */
fun Modifier.systemGestureExclusion() =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        this
    } else {
        this then excludeFromSystemGestureQ(null)
    }

/**
 * Excludes a rectangle within the local layout coordinates from the system gesture.
 * After layout, [exclusion] is called to determine the [Rect] to exclude from the system
 * gesture area.
 *
 * The [LayoutCoordinates] of the [Modifier]'s location in the layout is passed as passed as
 * [exclusion]'s parameter.
 *
 * @see View.setSystemGestureExclusionRects
 */
fun Modifier.systemGestureExclusion(exclusion: (LayoutCoordinates) -> Rect) =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        this
    } else {
        this then excludeFromSystemGestureQ(exclusion)
    }

@Suppress("NOTHING_TO_INLINE", "ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@RequiresApi(Build.VERSION_CODES.Q)
private inline fun excludeFromSystemGestureQ(
    noinline exclusion: ((LayoutCoordinates) -> Rect)?
): Modifier = ExcludeFromSystemGestureElement(exclusion)

@RequiresApi(Build.VERSION_CODES.Q)
private class ExcludeFromSystemGestureElement(
    val exclusion: ((LayoutCoordinates) -> Rect)?
) : ModifierNodeElement<ExcludeFromSystemGestureNode>() {
    @SuppressLint("NewApi")
    override fun create(): ExcludeFromSystemGestureNode {
        return ExcludeFromSystemGestureNode(exclusion)
    }

    override fun update(node: ExcludeFromSystemGestureNode) {
        node.rect = exclusion
    }

    override fun hashCode(): Int {
        return exclusion.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ExcludeFromSystemGestureElement) return false
        return exclusion == other.exclusion
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "systemGestureExclusion"
        if (exclusion != null) {
            properties["exclusion"] = exclusion
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private class ExcludeFromSystemGestureNode(
    rect: ((LayoutCoordinates) -> Rect)?
) : RectListNode(rect) {
    override fun currentRects(): MutableVector<android.graphics.Rect> {
        val rects = mutableVectorOf<android.graphics.Rect>()
        rects.addAll(view.systemGestureExclusionRects)
        return rects
    }
    override fun updateRects(rects: MutableVector<android.graphics.Rect>) {
        view.systemGestureExclusionRects = rects.asMutableList()
    }
}
