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

package androidx.privacysandbox.ui.provider.impl

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.core.util.Supplier
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Placeholder client passed to [SandboxedUiAdapter.openSession] before actual client will be
 * created.
 *
 * If adapter uses background threads, calling openSession() earlier could improve latency by
 * scheduling provider tasks earlier and creating actual client while waiting for results.
 *
 * Using [DeferredObjectHolder] to create actual client in background.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class DeferredSessionClient(
    private val objectHolder: ObjectHolder<SandboxedUiAdapter.SessionClient>
) : SandboxedUiAdapter.SessionClient {

    override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
        objectHolder.demandObject().onSessionOpened(session)
    }

    override fun onSessionError(throwable: Throwable) {
        objectHolder.demandObject().onSessionError(throwable)
    }

    override fun onResizeRequested(width: Int, height: Int) {
        objectHolder.demandObject().onResizeRequested(width, height)
    }

    fun preloadClient() = objectHolder.preloadObject()

    companion object {
        private const val TAG = "DeferredSessionClient"

        fun <T : SandboxedUiAdapter.SessionClient> create(
            clientFactory: Supplier<T>,
            clientInit: Consumer<T>,
            errorHandler: Consumer<Throwable>
        ): DeferredSessionClient {
            return DeferredSessionClient(
                DeferredObjectHolder(
                    objectFactory = clientFactory,
                    objectInit = clientInit,
                    errorHandler = {
                        Log.e(TAG, "Exception during actual client initialization", it)
                        errorHandler.accept(it)
                    },
                    FailClient
                )
            )
        }

        private object FailClient : SandboxedUiAdapter.SessionClient {
            override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
                Log.w(TAG, "Auto-closing session on actual client initialization error")
                session.close()
            }

            override fun onSessionError(throwable: Throwable) {}

            override fun onResizeRequested(width: Int, height: Int) {}
        }
    }
}
