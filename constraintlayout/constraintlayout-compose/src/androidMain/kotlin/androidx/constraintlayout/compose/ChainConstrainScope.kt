/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Stable
import androidx.constraintlayout.core.parser.CLObject

@LayoutScopeMarker
@Stable
class HorizontalChainScope internal constructor(
    internal val id: Any,
    containerObject: CLObject
) {
    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference("parent")

    /**
     * The start anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val start: VerticalAnchorable = ChainVerticalAnchorable(containerObject, -2)

    /**
     * The left anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteLeft: VerticalAnchorable = ChainVerticalAnchorable(containerObject, 0)

    /**
     * The end anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val end: VerticalAnchorable = ChainVerticalAnchorable(containerObject, -1)

    /**
     * The right anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteRight: VerticalAnchorable = ChainVerticalAnchorable(containerObject, 1)
}

@LayoutScopeMarker
@Stable
class VerticalChainScope internal constructor(
    internal val id: Any,
    containerObject: CLObject
) {
    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference("parent")

    /**
     * The top anchor of the chain - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val top: HorizontalAnchorable = ChainHorizontalAnchorable(containerObject, 0)

    /**
     * The bottom anchor of the chain - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val bottom: HorizontalAnchorable = ChainHorizontalAnchorable(containerObject, 1)
}

private class ChainVerticalAnchorable constructor(
    containerObject: CLObject,
    index: Int
) : BaseVerticalAnchorable(containerObject, index)

private class ChainHorizontalAnchorable constructor(
    containerObject: CLObject,
    index: Int
) : BaseHorizontalAnchorable(containerObject, index)