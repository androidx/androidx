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

import android.view.View
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt

internal abstract class RectListNode(
    open var rect: ((LayoutCoordinates) -> Rect)?
) : Modifier.Node(), GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {
    private var androidRect: android.graphics.Rect? = null

    protected val view: View
        get() = currentValueOf(LocalView)

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val newRect = if (rect == null) {
            val boundsInRoot = coordinates.boundsInRoot()
            android.graphics.Rect(
                boundsInRoot.left.roundToInt(),
                boundsInRoot.top.roundToInt(),
                boundsInRoot.right.roundToInt(),
                boundsInRoot.bottom.roundToInt()
            )
        } else {
            calcBounds(coordinates, rect!!.invoke(coordinates))
        }
        replaceRect(newRect)
    }

    override fun onDetach() {
        super.onDetach()
        replaceRect(null) // On Node detach, reset
    }

    abstract fun currentRects(): MutableVector<android.graphics.Rect>
    abstract fun updateRects(rects: MutableVector<android.graphics.Rect>)

    private fun replaceRect(newRect: android.graphics.Rect?) {
        val rects = currentRects()

        androidRect?.let { rects.remove(it) }

        if (newRect?.isEmpty == false) {
            rects += newRect
        }

        updateRects(rects)
        androidRect = newRect
    }

    private fun calcBounds(
        layoutCoordinates: LayoutCoordinates,
        rect: Rect
    ): android.graphics.Rect {
        val root = layoutCoordinates.findRootCoordinates()
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
}
