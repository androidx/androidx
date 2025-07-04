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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionServiceKey
import androidx.compose.runtime.CompositionServices
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.subspace.node.SubspaceNodeApplier

/**
 * A composition object that is tied to [owner]'s lifecycle and attachment state.
 *
 * See [androidx.compose.ui.platform.WrappedComposition]
 */
internal class WrappedComposition(
    private val owner: AndroidComposeSpatialElement,
    compositionContext: CompositionContext,
) : Composition, LifecycleEventObserver, CompositionServices {

    private val composition = Composition(SubspaceNodeApplier(owner), compositionContext)
    private var lastContent: @Composable () -> Unit = {}
    private var addedToLifecycle: Lifecycle? = null

    override val hasInvalidations
        get() = composition.hasInvalidations

    override val isDisposed: Boolean
        get() = composition.isDisposed

    override fun setContent(content: @Composable () -> Unit) {
        owner.setOnSubspaceAvailable {
            if (isDisposed) return@setOnSubspaceAvailable

            val lifecycle = it.lifecycle
            lastContent = content

            if (addedToLifecycle == null) {
                addedToLifecycle = lifecycle
                // this will call ON_CREATE synchronously if it is already past the created state.
                lifecycle.addObserver(this)
            } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                composition.setContent { ProvideCompositionLocals(owner, content) }
            }
        }

        owner.onDetachedFromSubspaceOnce { dispose() }
    }

    override fun dispose() {
        if (!isDisposed) {
            owner.wrappedComposition = null
            composition.dispose()
        }
    }

    override fun <T> getCompositionService(key: CompositionServiceKey<T>): T? =
        (composition as? CompositionServices)?.getCompositionService(key)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            dispose()
        } else if (event == Lifecycle.Event.ON_CREATE) {
            if (!isDisposed) {
                setContent(lastContent)
            }
        }
    }
}
