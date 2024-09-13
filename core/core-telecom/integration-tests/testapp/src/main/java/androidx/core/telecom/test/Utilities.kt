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

package androidx.core.telecom.test

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager.AudioRecordingCallback
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.util.Preconditions

@RequiresApi(34)
class Utilities {
    companion object {
        const val APP_SCHEME = "MyCustomScheme"
        const val ALL_CALL_CAPABILITIES =
            (CallAttributesCompat.SUPPORTS_SET_INACTIVE or
                CallAttributesCompat.SUPPORTS_STREAM or
                CallAttributesCompat.SUPPORTS_TRANSFER)

        // outgoing attributes constants
        const val OUTGOING_NAME = "Darth Maul"
        val OUTGOING_URI: Uri = Uri.parse("tel:6506958985")

        // incoming attributes constants
        const val INCOMING_NAME = "Sundar Pichai"
        val INCOMING_URI: Uri = Uri.parse("tel:6506958985")

        // Audio recording config constants
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val RECORD_AUDIO_REQUEST_CODE = 200

        // Create AudioRecord
        fun createAudioRecord(context: Context, mainActivity: CallingMainActivity): AudioRecord {
            if (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    mainActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_REQUEST_CODE
                )
            }
            val channelMask =
                if (CHANNEL_COUNT == 1) AudioFormat.CHANNEL_IN_MONO
                else AudioFormat.CHANNEL_IN_STEREO
            var minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelMask, AUDIO_FORMAT)
            Preconditions.checkState(minBufferSize > 0)
            minBufferSize *= 2

            val audioFormatObj =
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(channelMask)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            val audioRecordBuilder: AudioRecord.Builder = AudioRecord.Builder()
            audioRecordBuilder.setAudioSource(AUDIO_SOURCE)
            audioRecordBuilder.setAudioFormat(audioFormatObj)
            audioRecordBuilder.setBufferSizeInBytes(minBufferSize)
            return audioRecordBuilder.build()
        }
    }

    // AudioRecordingCallback implementation
    class TelecomAudioRecordingCallback(private var mAudioRecord: AudioRecord) :
        AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            for (config in configs) {
                if (config.clientAudioSessionId == mAudioRecord.audioSessionId) {
                    Log.i(
                        CallingMainActivity::class.simpleName,
                        String.format(
                            "onRecordingConfigChanged: random: isClientSilenced=[%b], config=[%s]",
                            config.isClientSilenced,
                            config
                        )
                    )
                    break
                }
            }
        }
    }
}
