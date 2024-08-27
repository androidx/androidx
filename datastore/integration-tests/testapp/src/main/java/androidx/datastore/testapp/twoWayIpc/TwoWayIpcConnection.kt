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

package androidx.datastore.testapp.twoWayIpc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout

/** A [ServiceConnection] implementation that talks to an instance of [TwoWayIpcService]. */
class TwoWayIpcConnection(
    private val context: Context,
    private val klass: Class<out TwoWayIpcService>,
) : ServiceConnection {
    private val connectionEstablished = CompletableDeferred<Messenger>()

    private suspend fun <T> withConnectionTimeout(block: suspend () -> T): T {
        return withTimeout(TIMEOUT) { block() }
    }

    suspend fun connect() {
        val intent = Intent(context, klass)
        withConnectionTimeout {
            val serviceExists: Boolean = context.bindService(intent, this, Context.BIND_AUTO_CREATE)

            if (!serviceExists) {
                val targetPackage: String = intent.component!!.packageName
                val targetService: String = intent.component!!.className

                try {
                    context.packageManager.getPackageInfo(targetPackage, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    throw IllegalStateException("Package not installed [$targetPackage]", e)
                }
                throw IllegalStateException(
                    "Package installed but service not found [$targetService]"
                )
            }
            connectionEstablished.await()
        }
    }

    suspend fun disconnect() {
        sendMessage(Message.obtain().also { it.what = TwoWayIpcService.MSG_DESTROY_SUBJECTS })
        context.unbindService(this)
    }

    private suspend fun sendMessage(message: Message): Message = withConnectionTimeout {
        val response = CompletableDeferred<Message>()
        message.replyTo =
            Messenger(
                object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        if (msg.what == TwoWayIpcService.MSG_CREATE_SUBJECT) {
                            val stacktrace =
                                msg.data.getString("ipc_stacktrace") ?: "missing stacktrace"
                            response.completeExceptionally(
                                AssertionError("Exception in remote process: $stacktrace")
                            )
                        } else {
                            response.complete(Message.obtain().also { it.copyFrom(msg) })
                        }
                    }
                }
            )
        connectionEstablished.await().send(message)
        response.await()
    }

    override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
        connectionEstablished.complete(Messenger(binder))
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        // this is called only if the service crashes
    }

    internal suspend fun createSubject(
        hostExecutionScope: CoroutineScope,
    ): TwoWayIpcSubject {
        val hostSubject = TwoWayIpcSubject(datastoreScope = hostExecutionScope)
        val message = Message.obtain()
        message.what = TwoWayIpcService.MSG_CREATE_SUBJECT
        message.data.putParcelable("messenger", hostSubject.bus.incomingMessenger)
        val response = sendMessage(message)

        @Suppress("DEPRECATION")
        val outgoingMessenger = response.data.getParcelable<Messenger>("messenger")
        checkNotNull(outgoingMessenger) { "didn't receive an outgoing messenger" }
        hostSubject.bus.setOutgoingMessenger(outgoingMessenger)
        return hostSubject
    }

    companion object {
        val TIMEOUT = 5.seconds
    }
}
