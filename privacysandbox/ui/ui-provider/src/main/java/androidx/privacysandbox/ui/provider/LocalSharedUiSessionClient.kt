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

import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SharedUiAdapter

/**
 * Wrapper class to perform check on client version before delegating call to
 * [SharedUiAdapter.SessionClient]
 */
// TODO(b/414773324): Opt-in should not be needed for internal classes
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
internal class LocalSharedUiSessionClient(
    private val clientVersion: Int,
    private val sessionClient: SharedUiAdapter.SessionClient,
) : SharedUiAdapter.SessionClient {
    override fun onSessionOpened(session: SharedUiAdapter.Session) {
        sessionClient.onSessionOpened(session)
    }

    override fun onSessionError(throwable: Throwable) {
        sessionClient.onSessionError(throwable)
    }

    override fun toString() = "LocalSharedUiSessionClient[$sessionClient]"
}
