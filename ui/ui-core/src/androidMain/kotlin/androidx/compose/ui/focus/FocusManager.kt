/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.focus

// TODO(b/162206799): Delete this after Dev 16.
/**
 * An object for focusable object.
 *
 * Any component that will have input focus must implement this FocusNode.
 */
@Deprecated(
    message = "Please use Modifier.focus() instead",
    level = DeprecationLevel.ERROR
)
class FocusNode

// TODO(b/162206799): Delete this after Dev 16.
/**
 * A callback interface for focus transition
 *
 * The callback is called when the focused node has changed with the previously focused node and
 * currently focused node.
 */
@Deprecated(
    message = "Please use Modifier.focusObserver() instead",
    level = DeprecationLevel.ERROR
)
typealias FocusTransitionObserver = () -> Unit

// TODO(b/162206799): Delete this after Dev 16.
/**
 * Interface of manager of focused composable.
 *
 * Focus manager keeps tracking the input focused node and provides focus transitions.
 */
@Deprecated(
    message = "Please use Modifier.focus() instead",
    level = DeprecationLevel.ERROR
)
interface FocusManager