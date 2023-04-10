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
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt

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

    override fun update(node: ExcludeFromSystemGestureNode): ExcludeFromSystemGestureNode =
        node.also {
            it.exclusion = exclusion
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
    var exclusion: ((LayoutCoordinates) -> Rect)?
) : Modifier.Node(), GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {
    var rect: android.graphics.Rect? = null

    private val view: View
        get() = currentValueOf(LocalView)

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val newRect = if (exclusion == null) {
            val boundsInRoot = coordinates.boundsInRoot()
            android.graphics.Rect(
                boundsInRoot.left.roundToInt(),
                boundsInRoot.top.roundToInt(),
                boundsInRoot.right.roundToInt(),
                boundsInRoot.bottom.roundToInt()
            )
        } else {
            calcBounds(coordinates, exclusion!!.invoke(coordinates))
        }
        replaceRect(newRect)
    }

    override fun onDetach() {
        super.onDetach()
        replaceRect(null) // On Node detach, reset
    }

    fun replaceRect(newRect: android.graphics.Rect?) {
        val rects = mutableVectorOf<android.graphics.Rect>()
        rects.addAll(view.systemGestureExclusionRects)

        rect?.let { rects.remove(it) }

        if (newRect?.isEmpty == false) {
            rects += newRect
        }
        view.systemGestureExclusionRects = rects.asMutableList()
        rect = newRect
    }

    private fun calcBounds(
        layoutCoordinates: LayoutCoordinates,
        rect: Rect
    ): android.graphics.Rect {
        val root = findRoot(layoutCoordinates)
        val topLeft = root.localPositionOf(layoutCoordinates, rect.topLeft)
        val topRight = root.localPositionOf(layoutCoordinates, rect.topRight)
        val bottomLeft = root.localPositionOf(layoutCoordinates, rect.bottomLeft)
        val bottomRight = root.localPositionOf(layoutCoordinates, rect.bottomRight)

        val left = minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x)
        val top = minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y)
        val right = maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x)
        val bottom = maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y)

        return android.graphics.Rect(
            left.roundToInt(),
            top.roundToInt(),
            right.roundToInt(),
            bottom.roundToInt()
        )
    }

    private fun findRoot(layoutCoordinates: LayoutCoordinates): LayoutCoordinates {
        var coordinates = layoutCoordinates
        var parent = layoutCoordinates.parentLayoutCoordinates
        while (parent != null) {
            coordinates = parent
            parent = coordinates.parentLayoutCoordinates
        }
        return coordinates
    }
}
