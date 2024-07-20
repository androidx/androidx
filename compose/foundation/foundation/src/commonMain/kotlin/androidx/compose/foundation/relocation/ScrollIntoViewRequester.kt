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

@file:JvmMultifileClass
@file:JvmName("ScrollIntoView")

package androidx.compose.foundation.relocation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.toSize
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Bring this node into bounds by making all the scrollable parents scroll appropriately.
 *
 * This method will not return until this request is satisfied or a newer request interrupts it. If
 * this call is interrupted by a newer call, this method will throw a
 * [CancellationException][kotlinx.coroutines.CancellationException].
 *
 * @param rect The rectangle (In local coordinates) that should be brought into view. If you don't
 *   specify the coordinates, the coordinates of the
 *   [Modifier.bringIntoViewRequester()][bringIntoViewRequester] associated with this
 *   [BringIntoViewRequester] will be used.
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 */
suspend fun DelegatableNode.scrollIntoView(rect: Rect? = null) {
    if (!node.isAttached) return
    val layoutCoordinates = requireLayoutCoordinates()
    val parent = findBringIntoViewParent() ?: return
    parent.scrollIntoView(layoutCoordinates, rect)
}

internal suspend fun BringIntoViewParent.scrollIntoView(
    layoutCoordinates: LayoutCoordinates,
    rect: Rect? = null
) {
    if (!layoutCoordinates.isAttached) return
    bringChildIntoView(layoutCoordinates) {
        // If the rect is not specified, use a rectangle representing the entire composable.
        // If the coordinates are detached when this call is made, we don't bother even
        // submitting the request, but if the coordinates become detached while the request
        // is being handled we just return a null Rect.
        rect ?: layoutCoordinates.takeIf { it.isAttached }?.size?.toSize()?.toRect()
    }
}
