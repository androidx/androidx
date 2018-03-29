/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media;

import android.media.VolumeProvider;

import androidx.annotation.RequiresApi;

@RequiresApi(21)
class VolumeProviderCompatApi21 {
    public static Object createVolumeProvider(int volumeControl, int maxVolume, int currentVolume,
            final Delegate delegate) {
        return new VolumeProvider(volumeControl, maxVolume, currentVolume) {
            @Override
            public void onSetVolumeTo(int volume) {
                delegate.onSetVolumeTo(volume);
            }

            @Override
            public void onAdjustVolume(int direction) {
                delegate.onAdjustVolume(direction);
            }
        };
    }

    public static void setCurrentVolume(Object volumeProviderObj, int currentVolume) {
        ((VolumeProvider) volumeProviderObj).setCurrentVolume(currentVolume);
    }

    public interface Delegate {
        void onSetVolumeTo(int volume);
        void onAdjustVolume(int delta);
    }

    private VolumeProviderCompatApi21() {
    }
}
