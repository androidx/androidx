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

package androidx.privacysandbox.ui.provider

import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Wrapper class to perform check on client version before delegating call to
 * [SandboxedUiAdapter.SessionClient]
 */
internal class LocalSessionClient(
    private val clientVersion: Int,
    private val client: SandboxedUiAdapter.SessionClient,
) : SandboxedUiAdapter.SessionClient {

    override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
        client.onSessionOpened(session)
    }

    override fun onResizeRequested(width: Int, height: Int) {
        client.onResizeRequested(width, height)
    }

    override fun onSessionError(throwable: Throwable) {
        client.onSessionError(throwable)
    }
}
