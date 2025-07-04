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

package androidx.privacysandbox.ui.client

import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionController
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject

/**
 * Wrapper class to perform check on provider version before delegating call to
 * [IRemoteSharedUiSessionController]
 */
internal class RemoteSharedUiSessionController(
    private val uiProviderVersion: Int,
    private val remoteSessionController: IRemoteSharedUiSessionController,
) : androidx.privacysandbox.ui.client.IRemoteSharedUiSessionController {
    override fun close() {
        tryToCallRemoteObject(remoteSessionController) { this.close() }
    }
}
