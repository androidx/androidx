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

package androidx.core.telecom.internal

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.internal.utils.AudioManagerUtil.Companion.getAvailableAudioDevices
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.getEndpointsFromAudioDeviceInfo
import kotlinx.coroutines.channels.SendChannel

/**
 * This class is responsible for getting [AudioDeviceInfo]s from the [AudioManager] pre-call and
 * emitting them to the [EndpointStateHandler] as [androidx.core.telecom.CallEndpointCompat]s
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class AudioDeviceListener(
    val mContext: Context,
    private val mActionChannel: SendChannel<EndpointAction>,
    private val mUuidSessionId: Int,
) : AutoCloseable, AudioDeviceCallback() {
    val mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        // [AudioDeviceInfo] <-- AudioManager / platform
        val initialAudioDevices = getAvailableAudioDevices(mAudioManager)
        // [CallEndpoints]   <-- [AudioDeviceInfo]
        val initialEndpoints =
            getEndpointsFromAudioDeviceInfo(mContext, mUuidSessionId, initialAudioDevices)
        mAudioManager.registerAudioDeviceCallback(this, null /*handler*/)
        // Send the initial list of pre-call [CallEndpointCompat]s out to the client. They
        // will be emitted and cached in the Flow & only consumed once the client has
        // collected it.
        mActionChannel.trySend(EndpointAction.Add(initialEndpoints))
    }

    override fun close() {
        mAudioManager.unregisterAudioDeviceCallback(this)
    }

    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
        if (addedDevices != null) {
            val endpoints =
                getEndpointsFromAudioDeviceInfo(mContext, mUuidSessionId, addedDevices.toList())
            mActionChannel.trySend(EndpointAction.Add(endpoints.filterNotNull()))
        }
    }

    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
        if (removedDevices != null) {
            val endpoints =
                getEndpointsFromAudioDeviceInfo(mContext, mUuidSessionId, removedDevices.toList())
            mActionChannel.trySend(EndpointAction.Remove(endpoints.filterNotNull()))
        }
    }
}
