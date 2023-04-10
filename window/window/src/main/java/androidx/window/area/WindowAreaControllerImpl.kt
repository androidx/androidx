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

package androidx.window.area

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.window.core.BuildConfig
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.VerificationMode
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_INACTIVE
import androidx.window.extensions.core.util.function.Consumer
import java.util.concurrent.Executor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Implementation of WindowAreaController for devices
 * that do implement the WindowAreaComponent on device.
 *
 * Requires [Build.VERSION_CODES.N] due to the use of [Consumer].
 * Will not be created though on API levels lower than
 * [Build.VERSION_CODES.S] as that's the min level of support for
 * this functionality.
 */
@ExperimentalWindowApi
@RequiresApi(Build.VERSION_CODES.N)
internal class WindowAreaControllerImpl(
    private val windowAreaComponent: WindowAreaComponent
) : WindowAreaController {

    private var currentStatus: WindowAreaStatus? = null

    override fun rearDisplayStatus(): Flow<WindowAreaStatus> {
        return callbackFlow {
            val listener = Consumer<@WindowAreaComponent.WindowAreaStatus Int> { status ->
                currentStatus = WindowAreaAdapter.translate(status)
                channel.trySend(currentStatus ?: WindowAreaStatus.UNSUPPORTED)
            }
            windowAreaComponent.addRearDisplayStatusListener(listener)
            awaitClose {
                windowAreaComponent.removeRearDisplayStatusListener(listener)
            }
        }.distinctUntilChanged()
    }

    override fun rearDisplayMode(
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    ) {
        // If we already have a status value that is not [WindowAreaStatus.AVAILABLE]
        // we should throw an exception quick to indicate they tried to enable
        // RearDisplay mode when it was not available.
        if (currentStatus != null && currentStatus != WindowAreaStatus.AVAILABLE) {
            throw UnsupportedOperationException("Rear Display mode cannot be enabled currently")
        }
        val rearDisplaySessionConsumer =
            RearDisplaySessionConsumer(executor, windowAreaSessionCallback, windowAreaComponent)
        windowAreaComponent.startRearDisplaySession(activity, rearDisplaySessionConsumer)
    }

    internal class RearDisplaySessionConsumer(
        private val executor: Executor,
        private val appCallback: WindowAreaSessionCallback,
        private val extensionsComponent: WindowAreaComponent
    ) : Consumer<@WindowAreaComponent.WindowAreaSessionState Int> {

        private var session: WindowAreaSession? = null

        override fun accept(t: @WindowAreaComponent.WindowAreaSessionState Int) {
            when (t) {
                SESSION_STATE_ACTIVE -> onSessionStarted()
                SESSION_STATE_INACTIVE -> onSessionFinished()
                else -> {
                    if (BuildConfig.verificationMode == VerificationMode.STRICT) {
                        Log.d(TAG, "Received an unknown session status value: $t")
                    }
                    onSessionFinished()
                }
            }
        }

        private fun onSessionStarted() {
            session = RearDisplaySessionImpl(extensionsComponent)
            session?.let { executor.execute { appCallback.onSessionStarted(it) } }
        }

        private fun onSessionFinished() {
            session = null
            executor.execute { appCallback.onSessionEnded() }
        }
    }

    internal companion object {
        private val TAG = WindowAreaControllerImpl::class.simpleName
    }
}
