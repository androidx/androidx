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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class LargePayloadSupportTest {
    private val sizeInBytes = 2 * 1024 * 1024 // 2MB

    private val byteArray: ByteArray = Random.nextBytes(sizeInBytes)

    companion object {
        private const val TIMEOUT_SECONDS = 1L
    }

    @Test
    fun largePayloadSupport_encodeDecode_success() {
        val bundle = Bundle()
        bundle.putString("key", "value")
        bundle.putByteArray("large_data", byteArray)

        val encodedBundle = LargePayloadSupport.encodeBundleToPfd(bundle)
        assertThat(encodedBundle).isNotNull()

        val decodedBundle = LargePayloadSupport.decodeBundleFromPfd(encodedBundle!!)
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle?.getString("key")).isEqualTo("value")
        assertThat(decodedBundle?.getByteArray("large_data")?.size).isEqualTo(sizeInBytes)
    }

    @Test
    fun largePayloadSupport_encodeDecode_emptyBundle_success() {
        val bundle = Bundle()

        val encodedBundle = LargePayloadSupport.encodeBundleToPfd(bundle)
        assertThat(encodedBundle).isNotNull()

        val decodedBundle = LargePayloadSupport.decodeBundleFromPfd(encodedBundle!!)
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle!!.isEmpty).isTrue()
    }

    @Test
    fun largePayloadSupport_ipc_success() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, LargePayloadSupportRemoteService::class.java)

        val remoteMessenger = suspendCancellableCoroutine { continuation ->
            val connection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        continuation.resumeWith(Result.success(Messenger(service)))
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {}
                }
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            continuation.invokeOnCancellation { context.unbindService(connection) }
        }

        val replyDeferred = CompletableDeferred<Bundle>()
        val replyHandler =
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    replyDeferred.complete(msg.data)
                }
            }

        val replyMessenger = Messenger(replyHandler)

        val bundle = Bundle()
        bundle.putString("key", "ipc_value")
        bundle.putByteArray("large_data", byteArray)
        val encodedBundle = LargePayloadSupport.encodeBundleToPfd(bundle)

        val msg = Message.obtain(null, LargePayloadSupportRemoteService.MSG_PROCESS_JUMBO)
        msg.replyTo = replyMessenger
        msg.data = encodedBundle

        remoteMessenger.send(msg)

        val receivedData = withTimeout(TIMEOUT_SECONDS.seconds) { replyDeferred.await() }
        val receivedKey = receivedData.getString("key")
        val receivedChecksum = receivedData.getLong("large_data_checksum")

        assertThat(receivedKey).isEqualTo("ipc_value")

        val expectedCrc = java.util.zip.CRC32().apply { update(byteArray) }.value
        assertThat(receivedChecksum).isEqualTo(expectedCrc)
    }
}
