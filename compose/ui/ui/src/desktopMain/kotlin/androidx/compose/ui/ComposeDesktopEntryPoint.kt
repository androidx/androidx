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

package androidx.compose.ui

import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.ui.semantics.SemanticsOwner

/**
 * The interface for classes that are an entry point for using Compose on the desktop.
 */
@ComposeToolingApi
interface ComposeDesktopEntryPoint {
    /**
     * Returns the [SemanticsOwner]s corresponding to the roots of the semantics trees in this
     * [ComposeDesktopEntryPoint].
     *
     * This is backed by Snapshot state, so reading this property in a restartable function (e.g., a
     * composable function) will cause the function to restart when the set of semantics owners
     * changes.
     */
    val semanticsOwners: Collection<SemanticsOwner>
}