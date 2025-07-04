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

package androidx.privacysandbox.ui.core

import androidx.annotation.RestrictTo
import androidx.privacysandbox.ui.core.SharedUiAdapter.SessionClient
import java.util.concurrent.Executor

/**
 * [LocalSharedUiAdapter] is loaded via reflection to establish session by client with provider.
 * openLocalSession function accepts clientVersion and params needed to create a session. It
 * internally calls openSession() on [ISharedUiAdapter]. openLocalSession function signature should
 * not be changed to maintain backward compatibility. For new params or modifying existing params
 * new overloaded function should be created.
 */
// TODO(b/414583128): Make LocalSharedUiAdapter.openLocalSession and
// ISharedUiAdapter.openRemoteSession
// API symmetrical
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
public interface LocalSharedUiAdapter {
    public fun openLocalSession(clientVersion: Int, clientExecutor: Executor, client: SessionClient)
}
