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
import androidx.constraintlayout.core.state.ConstraintReference

@LayoutScopeMarker
@Stable
class HorizontalChainScope internal constructor(internal val id: Any) {
    internal val tasks = mutableListOf<(State) -> Unit>()

    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference(SolverState.PARENT)

    /**
     * The start anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val start: VerticalAnchorable = ChainVerticalAnchorable(tasks, id, -2)

    /**
     * The left anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteLeft: VerticalAnchorable = ChainVerticalAnchorable(tasks, id, 0)

    /**
     * The end anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val end: VerticalAnchorable = ChainVerticalAnchorable(tasks, id, -1)

    /**
     * The right anchor of the chain - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteRight: VerticalAnchorable = ChainVerticalAnchorable(tasks, id, 1)
}

@LayoutScopeMarker
@Stable
class VerticalChainScope internal constructor(internal val id: Any) {
    internal val tasks = mutableListOf<(State) -> Unit>()

    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference(SolverState.PARENT)

    /**
     * The top anchor of the chain - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val top: HorizontalAnchorable = ChainHorizontalAnchorable(tasks, id, 0)

    /**
     * The bottom anchor of the chain - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val bottom: HorizontalAnchorable = ChainHorizontalAnchorable(tasks, id, 1)
}

private class ChainVerticalAnchorable constructor(
    tasks: MutableList<(State) -> Unit>,
    private val id: Any,
    index: Int
) : BaseVerticalAnchorable(tasks, index) {
    override fun getConstraintReference(state: State): ConstraintReference =
        state.helper(id, androidx.constraintlayout.core.state.State.Helper.HORIZONTAL_CHAIN)
}

private class ChainHorizontalAnchorable constructor(
    tasks: MutableList<(State) -> Unit>,
    private val id: Any,
    index: Int
) : BaseHorizontalAnchorable(tasks, index) {
    override fun getConstraintReference(state: State): ConstraintReference =
        state.helper(id, androidx.constraintlayout.core.state.State.Helper.VERTICAL_CHAIN)
}