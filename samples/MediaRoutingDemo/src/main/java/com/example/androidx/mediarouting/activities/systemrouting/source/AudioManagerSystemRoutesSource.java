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
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link AudioManager}. */
@RequiresApi(Build.VERSION_CODES.M)
public final class AudioManagerSystemRoutesSource implements SystemRoutesSource {

    @NonNull
    private final AudioManager mAudioManager;

    /** Returns a new instance. */
    @NonNull
    public static AudioManagerSystemRoutesSource create(@NonNull Context context) {
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        return new AudioManagerSystemRoutesSource(audioManager);
    }

    AudioManagerSystemRoutesSource(@NonNull AudioManager audioManager) {
        mAudioManager = audioManager;
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchRoutes() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (AudioDeviceInfo audioDeviceInfo :
                mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            SystemRouteItem.Builder builder = new SystemRouteItem.Builder(
                    String.valueOf(audioDeviceInfo.getId()),
                    SystemRouteItem.ROUTE_SOURCE_AUDIO_MANAGER)
                    .setName(audioDeviceInfo.getProductName().toString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setAddress(Api28Impl.getAddress(audioDeviceInfo));
            }

            out.add(builder.build());
        }

        return out;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static String getAddress(AudioDeviceInfo audioDeviceInfo) {
            return audioDeviceInfo.getAddress();
        }

    }
}
