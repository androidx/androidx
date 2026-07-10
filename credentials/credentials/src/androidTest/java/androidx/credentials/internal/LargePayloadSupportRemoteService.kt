/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.internal

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import java.util.zip.CRC32

/**
 * A remote service used for testing [LargePayloadSupport] inter-process communication (IPC).
 *
 * This service receives a [Message] containing a [Bundle] with a [android.os.ParcelFileDescriptor]
 * extra, extracts its contents, calculates a CRC32 checksum of the byte array payload to verify
 * data integrity, and replies back to the sender with the extracted string and the checksum.
 */
class LargePayloadSupportRemoteService : Service() {

    private val handler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_PROCESS_JUMBO -> {
                        val data = msg.data
                        data.classLoader = LargePayloadSupport::class.java.classLoader
                        val unparceled = LargePayloadSupport.decodeBundleFromPfd(data)
                        val replyMsg = Message.obtain()
                        val replyData = Bundle()
                        val replyKey = unparceled?.getString("key") ?: "null"
                        val largeData = unparceled?.getByteArray("large_data")
                        val checksum = largeData?.let { CRC32().apply { update(it) }.value } ?: -1L
                        replyData.putString("key", replyKey)
                        replyData.putLong("large_data_checksum", checksum)
                        replyMsg.data = replyData
                        msg.replyTo?.send(replyMsg)
                    }
                    else -> super.handleMessage(msg)
                }
            }
        }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder? = messenger.binder

    companion object {
        /** Message code indicating a request to process a [LargePayloadSupport] encoded payload. */
        const val MSG_PROCESS_JUMBO = 1
    }
}
