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

@file:JvmMultifileClass
@file:JvmName("BringIntoViewRequesterKt")

package androidx.compose.foundation.relocation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatableNode
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Platform-specific "root" of the [BringIntoViewParent] chain to call into when there are no
 * [BringIntoViewParent]s above a node.
 */
internal expect fun DelegatableNode.defaultBringIntoViewParent(): BringIntoViewParent

/**
 * A node that can respond to [bringChildIntoView] requests from its children by scrolling its
 * content.
 */
internal fun interface BringIntoViewParent {
    /**
     * Scrolls this node's content so that [boundsProvider] will be in visible bounds. Must ensure
     * that the request is propagated up to the parent node.
     *
     * This method will not return until this request has been satisfied or interrupted by a
     * newer request.
     *
     * @param childCoordinates The [LayoutCoordinates] of the child node making the request. This
     * parent can use these [LayoutCoordinates] to translate [boundsProvider] into its own
     * coordinates.
     * @param boundsProvider A function returning the rectangle to bring into view, relative to
     * [childCoordinates]. The function may return a different value over time, if the bounds of the
     * request change while the request is being processed. If the rectangle cannot be calculated,
     * e.g. because [childCoordinates] is not attached, return null.
     */
    suspend fun bringChildIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?)
}
