/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.service

import androidx.annotation.GuardedBy
import javax.annotation.concurrent.ThreadSafe

/**
 * Cache for different types of supported UI (RemoteViews for general Android and TileLayout for
 * Wear).
 * <p>
 * When developers call our APIs to update UI, we don't immediately respect that and send it over
 * the service. Instead, we cache it here and wait for the GRPC impl to decide on an appropriate
 * time to return UI.
 */
@ThreadSafe
internal class UiCache {

    private val lock = Any()

    @GuardedBy("lock")
    var cachedRemoteViewsInternal: RemoteViewsInternal? = null
        get() {
            synchronized(lock) {
                return field
            }
        }
        private set

    @GuardedBy("lock")
    var cachedTileLayoutInternal: TileLayoutInternal? = null
        get() {
            synchronized(lock) {
                return field
            }
        }
        private set

    // Needs to be reset after the UiUpdate signal has been sent to assistant. When assistant receives
    // the signal, it should send rpc requests to fetch these cached UiResponse(s).
    @GuardedBy("lock")
    var hasUnreadUiResponse: Boolean = false
        get() {
            synchronized(lock) {
                return field
            }
        }
        private set

    /**
     * Caches a UiResponse for this particular {@link BaseExecutionSession}.
     */
    fun updateUiInternal(uiResponse: UiResponse) {
        synchronized(lock) {
            hasUnreadUiResponse = true
            if (uiResponse.remoteViewsInternal != null) {
                cachedRemoteViewsInternal = uiResponse.remoteViewsInternal
            }
            if (uiResponse.tileLayoutInternal != null) {
                cachedTileLayoutInternal = uiResponse.tileLayoutInternal
            }
        }
    }

    fun resetUnreadUiResponse() {
        synchronized(lock) {
            hasUnreadUiResponse = false
        }
    }
}
