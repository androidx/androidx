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

package androidx.xr.compose.subspace.node

import androidx.compose.runtime.CompositionLocalMap
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.OpaqueEntity
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceModifier

/**
 * Represents a Composable Subspace node in the Compose hierarchy.
 *
 * This interface is inspired by [androidx.compose.ui.node.ComposeUiNode].
 */
@PublishedApi
internal interface ComposeSubspaceNode {

    /** The [SubspaceMeasurePolicy] used to define the measure and layout behavior of this node. */
    var measurePolicy: SubspaceMeasurePolicy

    /** The [SubspaceModifier] applied to this node. */
    var modifier: SubspaceModifier

    /** The optional [CoreEntity] associated with this node. */
    var entity: OpaqueEntity?

    /** A snapshot of the current composition local map when this node is created. */
    var compositionLocalMap: CompositionLocalMap

    companion object {
        /**
         * Constructor function for creating a new [ComposeSubspaceNode].
         *
         * @return an instance of a [ComposeSubspaceNode].
         */
        val Constructor: () -> ComposeSubspaceNode = SubspaceLayoutNode.Constructor

        /**
         * Sets the [SubspaceMeasurePolicy] for the given [ComposeSubspaceNode].
         *
         * @param measurePolicy the [SubspaceMeasurePolicy] to be applied.
         */
        val SetMeasurePolicy: ComposeSubspaceNode.(SubspaceMeasurePolicy) -> Unit = {
            this.measurePolicy = it
        }

        /**
         * Sets the [CoreEntity] for the given [ComposeSubspaceNode].
         *
         * @param entity the [CoreEntity] to be associated, or null.
         */
        val SetCoreEntity: ComposeSubspaceNode.(OpaqueEntity?) -> Unit = { this.entity = it }

        /**
         * Sets the [SubspaceModifier] for the given [ComposeSubspaceNode].
         *
         * @param modifier the [SubspaceModifier] to be applied.
         */
        val SetModifier: ComposeSubspaceNode.(SubspaceModifier) -> Unit = { this.modifier = it }

        /** Sets a snapshot of the current composition local map when this node is created. */
        val SetCompositionLocalMap: ComposeSubspaceNode.(CompositionLocalMap) -> Unit = {
            this.compositionLocalMap = it
        }
    }
}
