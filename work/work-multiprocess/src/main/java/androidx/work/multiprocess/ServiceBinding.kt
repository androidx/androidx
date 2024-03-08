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
package androidx.work.multiprocess

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import androidx.concurrent.futures.SuspendToFutureAdapter.launchFuture
import androidx.core.util.Function
import androidx.work.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal fun <T : IInterface?> bindToService(
    context: Context,
    intent: Intent,
    asInterface: Function<IBinder?, T>,
    loggingTag: String
): Session<T> {
    Logger.get().debug(loggingTag, "Binding via $intent")

    val session = Session(loggingTag, asInterface)
    try {
        val bound = context.bindService(intent, session, Context.BIND_AUTO_CREATE)
        if (!bound) {
            session.resolveClosedConnection(RuntimeException("Unable to bind to service"))
        }
    } catch (throwable: Throwable) {
        session.resolveClosedConnection(throwable)
    }
    return session
}

internal class Session<T : IInterface?>(
    private val logTag: String,
    private val asInterface: Function<IBinder?, T>
) : ServiceConnection {

    sealed class State {
        object Created : State()
        class Connected(val iBinder: IBinder) : State()
        class Disconnected(val throwable: Throwable) : State()
    }

    private val stateFlow = MutableStateFlow<State>(State.Created)

    val connectedFuture = launchFuture<T>(Dispatchers.Unconfined) {
        val state = stateFlow.first { it != State.Created }
        if (state is State.Connected) {
            asInterface.apply(state.iBinder)
        } else {
            // we can go straight to disconnected state if we failed to bind
            throw (state as State.Disconnected).throwable
        }
    }

    val disconnectedFuture = launchFuture<Unit> {
        stateFlow.first { it is State.Disconnected }
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        stateFlow.value = State.Connected(iBinder)
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        Logger.get().debug(logTag, "Service disconnected")
        resolveClosedConnection(RuntimeException("Service disconnected"))
    }

    override fun onBindingDied(name: ComponentName) {
        onBindingDied()
    }

    /**
     * Clean-up client when a binding dies.
     */
    fun onBindingDied() {
        Logger.get().debug(logTag, "Binding died")
        resolveClosedConnection(RuntimeException("Binding died"))
    }

    override fun onNullBinding(name: ComponentName) {
        Logger.get().error(logTag, "Unable to bind to service")
        resolveClosedConnection(RuntimeException("Cannot bind to service $name"))
    }

    fun resolveClosedConnection(throwable: Throwable) {
        stateFlow.value = State.Disconnected(throwable)
    }
}
