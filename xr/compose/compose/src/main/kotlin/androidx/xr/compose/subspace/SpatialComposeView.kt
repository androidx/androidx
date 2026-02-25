/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.subspace

import android.content.Context
import android.view.View
import android.view.ViewParent
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.viewtree.setViewTreeDisjointParent
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Creates and initializes a new [ComposeView] configured for use within a spatial context.
 *
 * This function sets up the necessary ViewTree owners (Lifecycle, ViewModelStore,
 * SavedStateRegistry) by inheriting them from the [LocalView]. It also links the parent composition
 * context to ensure that CompositionLocal values are correctly propagated to the new view's
 * content.
 *
 * @param parentView The view from which to retrieve the ViewTree owners (Lifecycle, ViewModelStore,
 *   SavedStateRegistry). This is typically the view hosting the composition (e.g.,
 *   LocalView.current).
 * @param context The context used to create the [ComposeView].
 * @param compositionContext The parent [CompositionContext] to link the new view's composition to.
 *   This ensures that the new composition can access CompositionLocals from the parent.
 * @param localId A unique identifier for this view.
 * @return A new, detached [ComposeView] ready to be embedded in a PanelEntity.
 */
internal fun spatialComposeView(
    parentView: View,
    context: Context,
    compositionContext: CompositionContext,
    localId: Int,
): ComposeView =
    ComposeView(context).apply {
        id = View.generateViewId()

        setViewTreeLifecycleOwner(parentView.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(parentView.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(parentView.findViewTreeSavedStateRegistryOwner())
        setViewTreeDisjointParent(parentView as? ViewParent ?: parentView.parent)

        // Set the strategy to automatically dispose the composition
        // when the ComposeView is detached from the window.
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        setParentCompositionContext(compositionContext)

        // Set unique id for AbstractComposeView. This allows state restoration
        // for the state defined inside the SpatialElevation via rememberSaveable().
        setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "ComposeView:$localId")

        // Enable children to draw their shadow by not clipping them
        clipChildren = false
    }
