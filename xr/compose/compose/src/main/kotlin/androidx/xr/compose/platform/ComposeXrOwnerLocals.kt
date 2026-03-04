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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.R
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.scene

/**
 * Provides a the current [ComposeXrOwnerLocals], a storage container for singletons tied to the
 * current [Activity].
 */
internal val LocalComposeXrOwners: CompositionLocal<ComposeXrOwnerLocals?> =
    compositionLocalWithComputedDefaultOf {
        LocalContext.currentValue.getActivity()?.getOrCreateXrOwnerLocals()
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
    val dialogManager: DialogManager,
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

@VisibleForTesting
internal fun Activity.getOrCreateXrOwnerLocals(): ComposeXrOwnerLocals? =
    getXrOwnerLocals() ?: createXrOwnerLocals()

private fun Activity.getXrOwnerLocals(): ComposeXrOwnerLocals? =
    contentView.getTag(R.id.compose_xr_owner_locals) as? ComposeXrOwnerLocals

private fun Activity.createXrOwnerLocals(): ComposeXrOwnerLocals? {
    // Don't try to create a session for non-XR configurations.
    if (!SpatialConfiguration.hasXrSpatialFeature(this)) return null

    val session = getOrCreateSession() ?: return null

    // When the owning lifecycle is destroyed, clear the cached  `ComposeXrOwnerLocals` from the
    // View's tag.
    if (this is LifecycleOwner) {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    contentView.setTag(R.id.compose_xr_owner_locals, null)
                    owner.lifecycle.removeObserver(this)
                }
            }
        )
    }

    return ComposeXrOwnerLocals(
            session = session,
            spatialConfiguration = SessionSpatialConfiguration(session),
            spatialCapabilities = SessionSpatialCapabilities(session),
            coreMainPanelEntity = CoreMainPanelEntity(session),
            subspaceRootNode =
                Entity.create(session, "SubspaceRootContainer").apply {
                    session.scene.keyEntity = this
                },
            dialogManager = DefaultDialogManager(),
        )
        .also { contentView.setTag(R.id.compose_xr_owner_locals, it) }
}

internal fun Activity.getOrCreateSession(): Session? {
    return getSession() ?: createSession()
}

private fun Activity.getSession(): Session? {
    return contentView.getTag(R.id.compose_xr_session) as? Session
}

private fun Activity.createSession(): Session? {
    return try {
        val session = getSessionFactory(this).invoke() ?: return null
        contentView.setTag(R.id.compose_xr_session, session)

        // When the owning lifecycle is destroyed, clear the cached `Session` from the View's tag.
        // This is critical to prevent crashes from using a stale `Session` after Activity
        // recreation as `Session` lifecycle is currently tied to the lifecycle of an activity so
        // that it forces a fresh `Session` instance to be created on next access.
        if (this is LifecycleOwner) {
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        contentView.setTag(R.id.compose_xr_session, null)
                        owner.lifecycle.removeObserver(this)
                    }
                }
            )
        }

        session
    } catch (_: Throwable) {
        // If we fail to create the session then there is nothing that we can do and the app should
        // fall back to non-XR behavior.
        null
    }
}

private fun Activity.getSessionFactory(activity: Activity): () -> Session? {
    @Suppress("UNCHECKED_CAST")
    return contentView.getTag(R.id.compose_xr_session_factory) as? () -> Session?
        ?: {
            (Session.create(activity) as? SessionCreateSuccess)?.session
        }
}
