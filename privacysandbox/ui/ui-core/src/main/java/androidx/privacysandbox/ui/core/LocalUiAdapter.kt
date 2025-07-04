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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import java.util.concurrent.Executor

/**
 * [LocalUiAdapter] is loaded via reflection to establish session by client with provider.
 * openLocalSession function accepts clientVersion and params needed to create a session. It
 * internally calls openSession() on SandboxedUiAdapter. openLocalSession function signature should
 * not be changed to maintain backward compatibility. To introduce new param or modify existing
 * param an overloaded function should be created.
 */
// TODO(b/414583128): Make LocalUiAdapter.openLocalSession and ISandboxedUiAdapter.openRemoteSession
// API symmetrical
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface LocalUiAdapter {
    public fun openLocalSession(
        clientVersion: Int,
        context: Context,
        sessionData: SessionData,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SessionClient,
    )
}
