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

package androidx.core.telecom.extensions

import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Interface used to allow the remote surface (automotive, watch, etc...) to know if the connected
 * calling application supports the meeting summary extension
 */
@ExperimentalAppActions
public interface MeetingSummaryRemote {
    /**
     * Whether or not the meeting summary extension is supported by the calling application.
     *
     * If `true`, then updates about meeting summary in the call will be given. If `false`, then the
     * remote doesn't support this extension and updates about the meeting summary will not be
     * given.
     *
     * Note: Must not be queried until after [CallExtensionScope.onConnected] is called.
     */
    public val isSupported: Boolean
}
