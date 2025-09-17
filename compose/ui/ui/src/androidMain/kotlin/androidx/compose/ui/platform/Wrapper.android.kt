/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.platform

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.CompositionServiceKey
import androidx.compose.runtime.CompositionServices
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.R
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.LifecycleRetainScopeOwner.FrameEndScheduler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.Collections
import java.util.WeakHashMap

internal actual fun createApplier(container: LayoutNode): AbstractApplier<LayoutNode> =
    UiApplier(container)

/**
 * Composes the given composable into the given view.
 *
 * The new composition can be logically "linked" to an existing one, by providing a [parent]. This
 * will ensure that invalidations and CompositionLocals will flow through the two compositions as if
 * they were not separate.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to be
 * able to save and restore the values used within the composition. See [View.setId].
 *
 * @param parent The [Recomposer] or parent composition reference.
 * @param content Composable that will be the content of the view.
 */
internal fun AbstractComposeView.setContent(
    parent: CompositionContext,
    content: @Composable () -> Unit,
): Composition {
    GlobalSnapshotManager.ensureStarted()
    val composeView =
        if (childCount > 0) {
            getChildAt(0) as? AndroidComposeView
        } else {
            removeAllViews()
            null
        }
            ?: AndroidComposeView(context, parent.effectCoroutineContext).also {
                addView(it.view, DefaultLayoutParams)
            }
    return doSetContent(composeView, parent, content)
}

private fun doSetContent(
    owner: AndroidComposeView,
    parent: CompositionContext,
    content: @Composable () -> Unit,
): Composition {
    if (isDebugInspectorInfoEnabled && owner.getTag(R.id.inspection_slot_table_set) == null) {
        owner.setTag(
            R.id.inspection_slot_table_set,
            Collections.newSetFromMap(WeakHashMap<CompositionData, Boolean>()),
        )
    }

    val wrapped =
        owner.view.getTag(R.id.wrapped_composition_tag) as? WrappedComposition
            ?: WrappedComposition(owner, Composition(UiApplier(owner.root), parent)).also {
                owner.view.setTag(R.id.wrapped_composition_tag, it)
            }
    wrapped.setContent(content)

    // When the CoroutineContext between the owner and parent doesn't match, we need to reset it
    // to this new parent's CoroutineContext, because the previous CoroutineContext was cancelled.
    // This usually happens when the owner (AndroidComposeView) wasn't completely torn down during a
    // config change. That expected scenario occurs when the manifest's configChanges includes
    // 'screenLayout' and the user selects a pop-up view for the app.
    if (owner.coroutineContext != parent.effectCoroutineContext) {
        owner.coroutineContext = parent.effectCoroutineContext
    }

    owner.frameEndScheduler = FrameEndScheduler(parent::scheduleFrameEndCallback)
    return wrapped
}

private class WrappedComposition(val owner: AndroidComposeView, val original: Composition) :
    Composition, LifecycleEventObserver, CompositionServices {

    private var disposed = false
    private var addedToLifecycle: Lifecycle? = null
    private var lastContent: @Composable () -> Unit = {}

    override fun setContent(content: @Composable () -> Unit) {
        owner.setOnViewTreeOwnersAvailable {
            if (!disposed) {
                val lifecycle = it.lifecycleOwner.lifecycle
                lastContent = content
                if (addedToLifecycle == null) {
                    addedToLifecycle = lifecycle
                    // this will call ON_CREATE synchronously if we already created
                    lifecycle.addObserver(this)
                } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    original.setContent {
                        @Suppress("UNCHECKED_CAST")
                        val inspectionTable =
                            owner.getTag(R.id.inspection_slot_table_set)
                                as? MutableSet<CompositionData>
                                ?: (owner.parent as? View)?.getTag(R.id.inspection_slot_table_set)
                                    as? MutableSet<CompositionData>
                        if (inspectionTable != null) {
                            inspectionTable.add(currentComposer.compositionData)
                            currentComposer.collectParameterInformation()
                        }

                        // TODO(mnuzen): Combine the two boundsUpdatesLoop() into one LaunchedEffect
                        LaunchedEffect(owner) { owner.boundsUpdatesAccessibilityEventLoop() }
                        LaunchedEffect(owner) { owner.boundsUpdatesContentCaptureEventLoop() }

                        CompositionLocalProvider(LocalInspectionTables provides inspectionTable) {
                            ProvideAndroidCompositionLocals(owner, content)
                        }
                    }
                }
            }
        }
    }

    override fun dispose() {
        if (!disposed) {
            disposed = true
            owner.view.setTag(R.id.wrapped_composition_tag, null)
            addedToLifecycle?.removeObserver(this)
        }
        original.dispose()
    }

    override val hasInvalidations
        get() = original.hasInvalidations

    override val isDisposed: Boolean
        get() = original.isDisposed

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            dispose()
        } else if (event == Lifecycle.Event.ON_CREATE) {
            if (!disposed) {
                setContent(lastContent)
            }
        }
    }

    override fun <T> getCompositionService(key: CompositionServiceKey<T>): T? =
        (original as? CompositionServices)?.getCompositionService(key)
}

private val DefaultLayoutParams =
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
