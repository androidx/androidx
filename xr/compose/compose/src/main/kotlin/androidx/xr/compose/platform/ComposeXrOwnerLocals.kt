/*
 * Copyright 2025 The Android Open Source Project
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

import android.app.Activity
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.R
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.scene

/**
 * Provides a the current [ComposeXrOwnerLocals], a storage container for singletons tied to the
 * current [Activity].
 */
internal val LocalComposeXrOwners: CompositionLocal<ComposeXrOwnerLocals?> =
    compositionLocalWithComputedDefaultOf {
        LocalContext.currentValue.getActivity().getOrCreateXrOwnerLocals()
    }

/**
 * A storage container for singletons tied to the current [Activity].
 *
 * This will be stored within the decorView of the main window.
 */
internal class ComposeXrOwnerLocals(
    val session: Session?,
    val spatialConfiguration: SpatialConfiguration,
    val spatialCapabilities: SpatialCapabilities,
    val coreMainPanelEntity: CoreMainPanelEntity,
    val subspaceRootNode: Entity,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComposeXrOwnerLocals

        if (session != other.session) return false
        if (spatialConfiguration != other.spatialConfiguration) return false
        if (spatialCapabilities != other.spatialCapabilities) return false
        if (coreMainPanelEntity != other.coreMainPanelEntity) return false
        if (subspaceRootNode != other.subspaceRootNode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = session?.hashCode() ?: 0
        result = 31 * result + spatialConfiguration.hashCode()
        result = 31 * result + spatialCapabilities.hashCode()
        result = 31 * result + coreMainPanelEntity.hashCode()
        result = 31 * result + subspaceRootNode.hashCode()
        return result
    }
}

internal fun Activity.getOrCreateXrOwnerLocals(): ComposeXrOwnerLocals? =
    window.decorView.getTag(R.id.compose_xr_owner_locals) as? ComposeXrOwnerLocals
        ?: createXrOwnerLocals()

private fun Activity.createXrOwnerLocals(): ComposeXrOwnerLocals? {
    if (!SpatialConfiguration.hasXrSpatialFeature(this)) {
        return null
    }

    val session = (Session.create(this) as? SessionCreateSuccess)?.session ?: return null

    return ComposeXrOwnerLocals(
            session = session,
            spatialConfiguration = SessionSpatialConfiguration(session),
            spatialCapabilities = SessionSpatialCapabilities(session),
            coreMainPanelEntity = CoreMainPanelEntity(session),
            subspaceRootNode =
                GroupEntity.create(session, "SubspaceRootContainer").apply {
                    session.scene.setKeyEntity(this)
                },
        )
        .also { window.decorView.setTag(R.id.compose_xr_owner_locals, it) }
}
