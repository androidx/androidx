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

package androidx.core.telecom.test.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals

class TestMuteStateReceiver : BroadcastReceiver() {
    private val isMutedChannel: Channel<Boolean> = Channel(Channel.UNLIMITED)

    companion object {
        private val TAG: String = TestMuteStateReceiver::class.java.simpleName.toString()
    }

    suspend fun waitForGlobalMuteState(isMuted: Boolean, id: String = "") {
        Log.i(TAG, "waitForGlobalMuteState: v=[$isMuted], id=[$id]")
        val result =
            withTimeoutOrNull(5000) {
                isMutedChannel
                    .receiveAsFlow()
                    .filter { state ->
                        Log.i(TAG, "it=[$state], isMuted=[$isMuted]")
                        state == isMuted
                    }
                    .firstOrNull()
            }
        Log.i(TAG, "asserting id=[$id], result=$result")
        assertEquals("Global Mute State {$id} never reached the expected state", isMuted, result)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (AudioManager.ACTION_MICROPHONE_MUTE_CHANGED == intent.action) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val isMicGloballyMuted = audioManager.isMicrophoneMute
            Log.i(TAG, "onReceive: isMicGloballyMuted=[${isMicGloballyMuted}]")
            isMutedChannel.trySend(isMicGloballyMuted)
        }
    }
}
