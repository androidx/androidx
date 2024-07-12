/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.CoroutineScope

/**
 * Coordinates focus for any composables in [content] and determines which composable will get
 * focus. [HierarchicalFocusCoordinator]s can be nested, and form a tree, with an implicit root.
 * Focus-requiring components (i.e. components using [ActiveFocusListener] or
 * [ActiveFocusRequester]) should only be in the leaf [HierarchicalFocusCoordinator]s, and there
 * should be at most one per [HierarchicalFocusCoordinator]. For [HierarchicalFocusCoordinator]
 * elements sharing a parent (or at the top level, sharing the implicit root parent), only one
 * should have focus enabled. The selected [HierarchicalFocusCoordinator] is the one that has focus
 * enabled for itself and all ancestors, it will pass focus to its focus-requiring component if it
 * has one, or call FocusManager#clearFocus() otherwise. If no [HierarchicalFocusCoordinator] is
 * selected, there will be no change on the focus state.
 *
 * Example usage:
 *
 * @sample androidx.wear.compose.foundation.samples.HierarchicalFocusCoordinatorSample
 * @param requiresFocus a function should return true when the [content] subtree of the composition
 *   is active and may requires the focus (and false when it's not). For example, a pager can
 *   enclose each page's content with a call to [HierarchicalFocusCoordinator], marking only the
 *   current page as requiring focus.
 * @param content The content of this component.
 */
@Composable
public fun HierarchicalFocusCoordinator(
    requiresFocus: () -> Boolean,
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current
    FocusComposableImpl(
        requiresFocus,
        onFocusChanged = { if (it) focusManager.clearFocus() },
        content = content
    )
}

/**
 * Use as part of a focus-requiring component to register a callback to be notified when the focus
 * state changes.
 *
 * @param onFocusChanged callback to be invoked when the focus state changes, the parameter is the
 *   new state (if true, we are becoming active and should request focus).
 */
@Composable
public fun ActiveFocusListener(onFocusChanged: CoroutineScope.(Boolean) -> Unit) {
    FocusComposableImpl(focusEnabled = { true }, onFocusChanged = onFocusChanged, content = {})
}

@Deprecated(
    "Renamed ActiveFocusListener, use that instead",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("ActiveFocusListener(onFocusChanged)")
)
@Composable
@ExperimentalWearFoundationApi
public fun OnFocusChange(onFocusChanged: CoroutineScope.(Boolean) -> Unit) =
    ActiveFocusListener(onFocusChanged)

/**
 * Use as part of a focus-requiring component to register a callback to automatically request focus
 * when this component is active. Note that this may call requestFocus in the provided
 * FocusRequester, so that focusRequester should be used in a .focusRequester modifier on a
 * Composable that is part of the composition.
 *
 * @param focusRequester The associated [FocusRequester] to request focus on.
 */
@Composable
public fun ActiveFocusRequester(focusRequester: FocusRequester) {
    ActiveFocusListener { if (it) focusRequester.requestFocus() }
}

@Deprecated(
    "Renamed ActiveFocusRequester, use that instead",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("ActiveFocusRequester(focusRequester)")
)
@Composable
@ExperimentalWearFoundationApi
public fun RequestFocusWhenActive(focusRequester: FocusRequester) =
    ActiveFocusRequester(focusRequester)

/**
 * Creates, remembers and returns a new [FocusRequester], that will have .requestFocus called when
 * the enclosing [HierarchicalFocusCoordinator] becomes active. Note that the location you call this
 * is important, in particular, which [HierarchicalFocusCoordinator] is enclosing it. Also, this may
 * call requestFocus in the returned FocusRequester, so that focusRequester should be used in a
 * .focusRequester modifier on a Composable that is part of the composition.
 */
@Composable
public fun rememberActiveFocusRequester() =
    remember { FocusRequester() }.also { ActiveFocusRequester(it) }

/**
 * Implements a node in the Focus control tree (either a [HierarchicalFocusCoordinator] or
 * [ActiveFocusListener]). Each [FocusComposableImpl] maps to a [FocusNode] in our internal
 * representation, this is used to:
 * 1) Check that our parent is focused (or we have no explicit parent), to see if we can be focused.
 * 2) See if we have children. If not, we are a leaf node and will forward focus status updates to
 *    the onFocusChanged callback.
 */
@Composable
internal fun FocusComposableImpl(
    focusEnabled: () -> Boolean,
    onFocusChanged: CoroutineScope.(Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val updatedFocusEnabled by rememberUpdatedState(focusEnabled)
    val parent by rememberUpdatedState(LocalFocusNodeParent.current)

    // Node in our internal tree representation of the FocusComposableImpl
    val node = remember {
        FocusNode(
            focused = derivedStateOf { (parent?.focused?.value ?: true) && updatedFocusEnabled() }
        )
    }

    // Attach our node to our parent's (and remove if we leave the composition).
    parent?.let {
        DisposableEffect(it) {
            it.children.add(node)

            onDispose { it.children.remove(node) }
        }
    }

    CompositionLocalProvider(LocalFocusNodeParent provides node, content = content)

    // If we are a leaf node, forward events to the onFocusChanged callback
    LaunchedEffect(node.focused.value) {
        if (node.children.isEmpty()) {
            onFocusChanged(node.focused.value)
        }
    }
}

// Internal class used to represent a node in our tree of focus-aware components.
internal class FocusNode(
    val focused: State<Boolean>,
    var children: SnapshotStateList<FocusNode> = mutableStateListOf()
)

// Composition Local used to keep a tree of focus-aware nodes (either controller nodes or
// focus requesting nodes).
// Nodes will register into their parent (unless they are the top ones) when they enter the
// composition and are removed when they leave it.
internal val LocalFocusNodeParent = compositionLocalOf<FocusNode?> { null }
