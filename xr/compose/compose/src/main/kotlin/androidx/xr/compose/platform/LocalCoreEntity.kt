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

package androidx.xr.compose.platform

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.viewtree.getParentOrViewTreeDisjointParent
import androidx.xr.compose.R
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.compose.subspace.layout.OpaqueEntity
import androidx.xr.compose.subspace.node.SubspaceNodeApplier

/**
 * A CompositionLocal that holds the current [OpaqueEntity] acting as the parent for any containing
 * composed UI.
 */
@PublishedApi
internal val LocalOpaqueEntity: ProvidableCompositionLocal<OpaqueEntity?> = compositionLocalOf {
    null
}

internal val LocalCoreMainPanelEntity: CompositionLocal<CoreMainPanelEntity?> =
    compositionLocalWithComputedDefaultOf {
        LocalComposeXrOwners.currentValue?.coreMainPanelEntity
            ?: LocalSession.currentValue?.let { CoreMainPanelEntity(it) }
    }

/**
 * Finds the nearest [CoreEntity] in the view hierarchy.
 *
 * This function traverses up the view hierarchy starting from the receiver [View], checking each
 * ancestor for a [CoreEntity] stored in its tag with the ID [R.id.compose_xr_local_view_entity].
 * The traversal uses [getParentOrViewTreeDisjointParent] to correctly navigate across different
 * view trees.
 *
 * @return The first [CoreEntity] found in an ancestor's tag, or `null` if the root of the view
 *   hierarchy is reached without finding one.
 */
internal fun View.findViewEntity(): CoreEntity? {
    var current: View? = this
    while (current != null) {
        val entity = current.getTag(R.id.compose_xr_local_view_entity)
        if (entity is CoreEntity) {
            return entity
        }
        current = current.getParentOrViewTreeDisjointParent() as? View
    }
    // No OpaqueEntity found in this branch of the hierarchy
    return null
}

/**
 * Determines the parent [CoreEntity] for spatial composable positioning.
 * 1. When the element is placed inside a Subspace composition the parent is pulled from
 *    [LocalOpaqueEntity]
 * 2. If the element is part of a standard 2D Compose composition that has a registered view-backed
 *    entity in [LocalView]
 * 3. If the element is not inside an active Subspace or a 2D view with a backing entity, it
 *    defaults to the application's root entity, typically the [LocalCoreMainPanelEntity]. This
 *    occurs when the element is the root spatial content, ex: `setContent { Orbiter(...) }`.
 */
@Composable
internal fun findNearestParentEntity(): CoreEntity? {
    val viewEntity = LocalView.current.findViewEntity()
    val opaqueEntity = LocalOpaqueEntity.current
    val coreMainPanelEntity = LocalCoreMainPanelEntity.current
    val isSubspace = currentComposer.applier is SubspaceNodeApplier

    val parentEntity: CoreEntity? =
        remember(viewEntity, opaqueEntity, coreMainPanelEntity, isSubspace) {
            (when {
                isSubspace -> opaqueEntity
                viewEntity != null -> viewEntity
                else -> coreMainPanelEntity
            })
                as CoreEntity?
        }

    return parentEntity
}
