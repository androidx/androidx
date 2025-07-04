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
 * calling application supports the call icon extension.
 */
@ExperimentalAppActions
public interface CallIconExtensionRemote {
    /**
     * Whether or not the call icon extension is supported by the calling application.
     *
     * If `true`, then updates about icon in the call will be notified. If `false`, then the remote
     * doesn't support this extension and icons will not be notified to the caller nor will
     * associated actions receive state updates.
     *
     * Note: Must not be queried until after [CallExtensionScope.onConnected] is called.
     */
    public val isSupported: Boolean
}
