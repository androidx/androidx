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
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.viewtree.getParentOrViewTreeDisjointParent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.compose.R
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private object ComposeXrOwnerLocalsConstants {
    const val SUBSPACE_ROOT_CONTAINER_NAME = "SubspaceRootContainer"
}

/**
 * A [CompositionLocal] that provides a [ComposeXrOwnerLocals] instance for the current
 * [ComponentActivity].
 *
 * This is the primary entry point for accessing the [Session], [SpatialConfiguration],
 * [SpatialCapabilities], and [CoreMainPanelEntity] instances associated with the current activity.
 *
 * Any activity-scoped state should probably be stored within the [ComposeXrOwnerLocals] instance.
 */
internal val LocalComposeXrOwners: CompositionLocal<ComposeXrOwnerLocals> =
    compositionLocalWithViewCachedDefaultOf(R.id.compose_xr_owner_locals) {
        ComposeXrOwnerLocals().also { locals ->
            LocalContext.currentValue.getComponentActivity()?.let { activity ->
                activity.lifecycleScope.launch { locals.initialize(activity) }
            }
        }
    }

/**
 * A storage container for singletons tied to the current [Activity].
 *
 * This will be stored within the decorView of the main window.
 */
internal class ComposeXrOwnerLocals : DefaultLifecycleObserver {

    var session: Session? by mutableStateOf(null)
        private set

    var spatialConfiguration: SpatialConfiguration? by mutableStateOf(null)
        private set

    var spatialCapabilities: SpatialCapabilities? by mutableStateOf(null)
        private set

    var coreMainPanelEntity: CoreMainPanelEntity? by mutableStateOf(null)
        private set

    var subspaceRootNode: Entity? by mutableStateOf(null)
        private set

    val dialogManager: DialogManager = DefaultDialogManager()

    private var isInitialized = false

    override fun onDestroy(owner: LifecycleOwner) {
        session?.scene?.clearSpaceChangedListener()
        owner.lifecycle.removeObserver(this)
    }

    internal suspend fun initialize(activity: ComponentActivity) {
        if (isInitialized || !SpatialConfiguration.hasXrSpatialFeature(activity)) return

        val createdSession = activity.getOrCreateSession()?.also { session = it } ?: return
        val subspaceRootNode =
            Entity.create(
                    session = createdSession,
                    name = ComposeXrOwnerLocalsConstants.SUBSPACE_ROOT_CONTAINER_NAME,
                    parent = createdSession.scene.activitySpace,
                )
                .also { subspaceRootNode = it }

        // If the main panel is implicitly hosted (not explicitly placed in a Subspace via
        // SpatialMainPanel), we parent it to the SubspaceRootNode so it still receives
        // recommended pose and scale updates from the system.
        activity.contentView.post {
            if (createdSession.scene.mainPanelEntity.parent == null) {
                createdSession.scene.mainPanelEntity.parent = subspaceRootNode
            }
        }

        spatialConfiguration =
            SessionSpatialConfiguration(
                session = createdSession,
                subspaceRootNode = subspaceRootNode,
            )
        spatialCapabilities = SessionSpatialCapabilities(createdSession)
        coreMainPanelEntity = CoreMainPanelEntity(createdSession)

        activity.lifecycle.addObserver(this)
        isInitialized = true
    }
}

internal suspend fun ComponentActivity.getOrCreateSession(): Session? =
    sessionCreationMutex.withLock {
        getOrCreateCachedValue(R.id.compose_xr_session) {
            try {
                withContext(sessionFactoryDispatcher) { sessionFactory.invoke() }
            } catch (_: Throwable) {
                // If we fail to create the session then there is nothing that we can do and the app
                // should fall back to non-XR behavior.
                null
            }
        }
    }

private val ComponentActivity.sessionCreationMutex: Mutex
    get() = getOrCreateCachedValue(R.id.compose_xr_session_mutex) { Mutex() }

private val ComponentActivity.sessionFactoryDispatcher: CoroutineDispatcher
    get() =
        contentView.getTag(R.id.compose_xr_session_factory_dispatcher) as? CoroutineDispatcher
            ?: Dispatchers.Main.immediate

private val ComponentActivity.sessionFactory: suspend () -> Session?
    get() =
        getOrCreateCachedValue(R.id.compose_xr_session_factory) {
            { (Session.create(context = this) as? SessionCreateSuccess)?.session }
        }

/**
 * Retrieves a cached value from the [ComponentActivity]'s contentView tag, or creates it using the
 * provided [factory] if it is not present, registering a lifecycle observer to clear it on
 * destruction.
 */
private inline fun <reified T : Any?> ComponentActivity.getOrCreateCachedValue(
    tagId: Int,
    factory: () -> T,
): T {
    val cachedValue = contentView.getTag(tagId) as? T
    if (cachedValue != null) return cachedValue

    val createdValue = factory() ?: return null as T

    contentView.setTag(tagId, createdValue)
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                contentView.setTag(tagId, null)
                owner.lifecycle.removeObserver(this)
            }
        }
    )
    return createdValue
}

/**
 * Creates a [ProvidableCompositionLocal] that dynamically caches its value in the Activity's
 * decorView or falls back to the disjoint root View.
 *
 * The [factory] has full access to [CompositionLocalAccessorScope], allowing the custom builder
 * block to query other composition locals natively at the call site.
 */
private inline fun <reified T : Any?> compositionLocalWithViewCachedDefaultOf(
    tagId: Int,
    crossinline factory: CompositionLocalAccessorScope.() -> T,
): ProvidableCompositionLocal<T> = compositionLocalWithComputedDefaultOf {
    val view = LocalView.currentValue
    val activity = LocalContext.currentValue.getComponentActivity()

    // Prioritize the main Activity's decorView as a fast-path optimization to avoid tree traversal.
    // Since findViewTreeRoot uses getParentOrViewTreeDisjointParent, both paths successfully
    // resolve to the Activity's decorView from disjoint windows (like Dialogs and Popups). This
    // fallback primarily handles previews and activityless tests.
    val rootView = activity?.contentView ?: view.findViewTreeRoot()

    val cachedValue = rootView.getTag(tagId) as? T
    if (cachedValue != null) return@compositionLocalWithComputedDefaultOf cachedValue

    val createdValue = factory()
    rootView.setTag(tagId, createdValue)

    activity
        ?.lifecycle
        ?.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    rootView.setTag(tagId, null)
                    owner.lifecycle.removeObserver(this)
                }
            }
        )

    createdValue
}

/**
 * Finds the root View of the current View tree, traversing up through disjoint parent boundaries
 * (like Dialogs and Popups) utilizing getParentOrViewTreeDisjointParent.
 *
 * @return The root View of the current View tree.
 */
private tailrec fun View.findViewTreeRoot(): View {
    val parent = getParentOrViewTreeDisjointParent()
    return if (parent is View) parent.findViewTreeRoot() else this
}
