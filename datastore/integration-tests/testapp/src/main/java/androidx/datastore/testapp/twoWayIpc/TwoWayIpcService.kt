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

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Another service of the same type, that runs in another separate process.
 *
 * @see TwoWayIpcService
 */
class TwoWayIpcService2 : TwoWayIpcService()

/**
 * An Android [android.app.Service] implementation that can create and maintain multiple
 * [TwoWayIpcSubject] instances.
 *
 * It properly scopes those subjects and destroys their scopes when the Service is destroyed,
 * allowing tests to properly maintain resources.
 *
 * @see androidx.datastore.testapp.multiprocess.MultiProcessTestRule
 */
open class TwoWayIpcService : LifecycleService() {
    private val subjects = mutableListOf<TwoWayIpcSubject>()
    private val jobForSubjects = Job()
    private val scopeForSubjects = CoroutineScope(jobForSubjects + Dispatchers.IO)
    private val messenger: Messenger =
        Messenger(
            Handler(Looper.getMainLooper()) { incoming ->
                // make a copy to prevent recycling
                when (incoming.what) {
                    MSG_CREATE_SUBJECT -> {
                        val subject = TwoWayIpcSubject(scopeForSubjects).also { subjects.add(it) }

                        @Suppress("DEPRECATION")
                        val messenger = incoming.data.getParcelable<Messenger>("messenger")
                        checkNotNull(messenger) { "missing messenger" }
                        subject.bus.setOutgoingMessenger(messenger)
                        val response =
                            Message.obtain().also {
                                it.data.putParcelable("messenger", subject.bus.incomingMessenger)
                            }
                        incoming.replyTo.send(response)
                    }
                    MSG_DESTROY_SUBJECTS -> {
                        val incomingCopy = Message.obtain().also { it.copyFrom(incoming) }
                        lifecycleScope.launch {
                            IpcLogger.log("destroying subjects")
                            try {
                                jobForSubjects.cancelAndJoin()
                                IpcLogger.log("destroyed subjects")
                            } finally {
                                incomingCopy.replyTo.send(
                                    Message.obtain().also { it.data.putBoolean("closed", true) }
                                )
                            }
                        }
                    }
                    else -> error("unknown message type ${incoming.what}")
                }
                true
            }
        )

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return messenger.binder
    }

    companion object {
        const val MSG_CREATE_SUBJECT = 500
        const val MSG_DESTROY_SUBJECTS = 501
    }
}
