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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi

/**
 * Represents the horizontal order of panes in a pane scaffold. An implementation of this interface
 * is supposed to represent an 1-to-1 mapping between all the possible pane roles supported by the
 * associated pane scaffold, and those panes' index in the order. For example,
 * [ThreePaneScaffoldHorizontalOrder] represents an order of three panes supported by the three pane
 * scaffold implementations like [ListDetailPaneScaffold] and [SupportingPaneScaffold].
 *
 * @see ThreePaneScaffoldHorizontalOrder
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldHorizontalOrder<Role : PaneScaffoldRole> {
    /** The number of panes in the order. */
    val size: Int

    /** Returns the index of the given role in the order. */
    fun indexOf(role: Role): Int

    /** Performs the given [action] for each pane in the order. */
    fun forEach(action: (Role) -> Unit)

    /** Performs the given [action] for each pane in the order, with its index. */
    fun forEachIndexed(action: (Int, Role) -> Unit)

    /** Performs the given [action] for each pane in the order, with its index, in reverse order. */
    fun forEachIndexedReversed(action: (Int, Role) -> Unit)
}
