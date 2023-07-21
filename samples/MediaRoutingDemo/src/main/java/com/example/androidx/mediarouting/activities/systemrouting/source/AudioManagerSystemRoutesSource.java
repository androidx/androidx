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

package com.example.androidx.mediarouting.activities.systemrouting.source;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link AudioManager}. */
@RequiresApi(Build.VERSION_CODES.M)
public final class AudioManagerSystemRoutesSource extends SystemRoutesSource {

    @NonNull
    private final AudioManager mAudioManager;

    @NonNull
    private final AudioDeviceCallback mAudioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            for (AudioDeviceInfo audioDeviceInfo: addedDevices) {
                mOnRoutesChangedListener.onRouteAdded(createRouteItemFor(audioDeviceInfo));
            }
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            for (AudioDeviceInfo audioDeviceInfo: removedDevices) {
                mOnRoutesChangedListener.onRouteRemoved(createRouteItemFor(audioDeviceInfo));
            }
        }
    };

    /** Returns a new instance. */
    @NonNull
    public static AudioManagerSystemRoutesSource create(@NonNull Context context) {
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        return new AudioManagerSystemRoutesSource(audioManager);
    }

    AudioManagerSystemRoutesSource(@NonNull AudioManager audioManager) {
        mAudioManager = audioManager;
    }

    @Override
    public void start() {
        mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, /* handler= */ null);
    }

    @Override
    public void stop() {
        mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
    }

    @NonNull
    @Override
    public SystemRoutesSourceItem getSourceItem() {
        return new SystemRoutesSourceItem.Builder(SystemRoutesSourceItem.ROUTE_SOURCE_AUDIO_MANAGER)
                .build();
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchSourceRouteItems() {
        List<SystemRouteItem> out = new ArrayList<>();

        AudioDeviceInfo[] deviceInfos = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo audioDeviceInfo : deviceInfos) {
            out.add(createRouteItemFor(audioDeviceInfo));
        }

        return out;
    }

    @NonNull
    private static SystemRouteItem createRouteItemFor(@NonNull AudioDeviceInfo audioDeviceInfo) {
        SystemRouteItem.Builder builder = new SystemRouteItem.Builder(
                String.valueOf(audioDeviceInfo.getId()))
                .setName(audioDeviceInfo.getProductName().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setAddress(Api28Impl.getAddress(audioDeviceInfo));
        }

        return builder.build();
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static final class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static String getAddress(AudioDeviceInfo audioDeviceInfo) {
            return audioDeviceInfo.getAddress();
        }

    }
}
