/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.sdkproviderutils

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory

class PlayerViewabilityHandler {

    private class SessionObserverFactoryImpl(val playerViewProvider: PlayerViewProvider) :
        SessionObserverFactory {

        override fun create(): SessionObserver {
            return SessionObserverImpl(playerViewProvider)
        }

        private inner class SessionObserverImpl(val playerViewProvider: PlayerViewProvider) :
            SessionObserver {
            lateinit var view: View
            var isPlayerVisible = false

            override fun onSessionOpened(sessionObserverContext: SessionObserverContext) {
                Log.i(TAG, "onSessionOpened $sessionObserverContext")
                view = checkNotNull(sessionObserverContext.view)
            }

            override fun onUiContainerChanged(uiContainerInfo: Bundle) {
                val sandboxedSdkViewUiInfo = SandboxedSdkViewUiInfo.fromBundle(uiContainerInfo)
                Log.i(TAG, "onUiContainerChanged $sandboxedSdkViewUiInfo")

                val updatedVisibility = !sandboxedSdkViewUiInfo.onScreenGeometry.isEmpty
                if (updatedVisibility != isPlayerVisible) {
                    Log.i(
                        TAG,
                        "Video player previous visibility $isPlayerVisible, updated visibility $updatedVisibility"
                    )
                    isPlayerVisible = updatedVisibility
                    if (isPlayerVisible) {
                        playerViewProvider.onPlayerVisible(view.id)
                    } else {
                        playerViewProvider.onPlayerInvisible(view.id)
                    }
                }
            }

            override fun onSessionClosed() {
                Log.i(TAG, "session closed")
                playerViewProvider.onSessionClosed()
            }
        }
    }

    companion object {

        private val TAG = PlayerViewabilityHandler::class.simpleName

        fun addObserverFactoryToAdapter(
            adapter: SandboxedUiAdapter,
            playerViewProvider: PlayerViewProvider
        ) {
            return adapter.addObserverFactory(SessionObserverFactoryImpl(playerViewProvider))
        }
    }
}
