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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Keeping the file for backwards compatibility

@Deprecated(
    "Replaced by Modifier.requestFocusOnHierarchyActive(), use that instead",
    level = DeprecationLevel.WARNING, // TODO: b/369332589 - Make hidden in a follow up cl
)
@Composable
public fun rememberActiveFocusRequester(): FocusRequester =
    remember { FocusRequester() }
        .also { focusRequester ->
            Box(Modifier.hierarchicalOnFocusChanged { if (it) focusRequester.requestFocus() })
        }

@Deprecated(
    "Replaced by Modifier.hierarchicalFocusGroup(), use that instead",
    level = DeprecationLevel.WARNING, // TODO: b/369332589 - Make hidden in a follow up cl
)
@Composable
public fun HierarchicalFocusCoordinator(
    requiresFocus: () -> Boolean,
    content: @Composable () -> Unit,
) {
    Box(Modifier.hierarchicalFocusGroup(requiresFocus())) { content() }
}

@Deprecated(
    "Replaced by Modifier.requestFocusOnHierarchyActive(), or the new LocalScreenIsActive, use that instead",
    level = DeprecationLevel.WARNING, // TODO: b/369332589 - Make hidden in a follow up cl
)
@Composable
public fun ActiveFocusListener(onFocusChanged: CoroutineScope.(Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    Box(Modifier.hierarchicalOnFocusChanged { scope.launch { onFocusChanged(it) } })
}
