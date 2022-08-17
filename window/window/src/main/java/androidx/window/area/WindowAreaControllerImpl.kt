/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.SpecificationComputer
import androidx.window.extensions.area.WindowAreaComponent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.concurrent.Executor
import java.util.function.Consumer

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

    private lateinit var rearDisplaySessionConsumer: Consumer<Int>
    private var currentStatus: WindowAreaStatus? = null

    override fun rearDisplayStatus(): Flow<WindowAreaStatus> {
        return flow {
            val channel = Channel<WindowAreaStatus>(
                capacity = BUFFER_CAPACITY,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            val listener = Consumer<Int> { status ->
                currentStatus = WindowAreaStatus.translate(status)
                channel.trySend(currentStatus ?: WindowAreaStatus.UNSUPPORTED)
            }
            val loader = WindowAreaControllerImpl::class.java.classLoader
            if (loader == null) {
                channel.trySend(WindowAreaStatus.UNSUPPORTED)
            } else {
                val consumerAdapter = ConsumerAdapter(loader)
                val subscription = consumerAdapter.createSubscriptionNoActivity(
                    windowAreaComponent,
                    Int::class,
                    "addRearDisplayStatusListener",
                    "removeRearDisplayStatusListener",
                ) { value ->
                    listener.accept(value)
                }
                try {
                    for (item in channel) {
                        emit(item)
                    }
                } finally {
                    subscription.dispose()
                }
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
            throw WindowAreaController.REAR_DISPLAY_ERROR
        }
        rearDisplaySessionConsumer =
            RearDisplaySessionConsumer(windowAreaSessionCallback, windowAreaComponent)
        val loader = WindowAreaControllerImpl::class.java.classLoader
        loader?.let {
            val consumerAdapter = ConsumerAdapter(it)
            consumerAdapter.createConsumer(
                windowAreaComponent,
                Int::class,
                "startRearDisplaySession",
                activity
            ) { value ->
                rearDisplaySessionConsumer.accept(value)
            }
        }
    }

    internal class RearDisplaySessionConsumer(
        private val appCallback: WindowAreaSessionCallback,
        private val extensionsComponent: WindowAreaComponent
    ) : Consumer<Int> {

        private var session: WindowAreaSession? = null

        override fun accept(sessionStatus: Int) {
            when (sessionStatus) {
                WindowAreaComponent.SESSION_STATE_ACTIVE -> onSessionStarted()
                WindowAreaComponent.SESSION_STATE_INACTIVE -> onSessionFinished()
                else -> {
                    if (BuildConfig.verificationMode ==
                            SpecificationComputer.VerificationMode.STRICT) {
                        Log.d(TAG, "Received an unknown session status value: $sessionStatus")
                    }
                    onSessionFinished()
                }
            }
        }

        private fun onSessionStarted() {
            session = RearDisplaySessionImpl(extensionsComponent)
            session?.let { appCallback.onSessionStarted(it) }
        }

        private fun onSessionFinished() {
            session = null
            appCallback.onSessionEnded()
        }
    }

    internal companion object {
        private val TAG = WindowAreaControllerImpl::class.simpleName
        /*
        Chosen as 10 for a base default value. We shouldn't be receiving
        many changes to window area status so this is enough capacity
        to not end up blocking.
         */
        private const val BUFFER_CAPACITY = 10
    }
}
