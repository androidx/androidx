/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.mediarouter.media;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.media.MediaRoute2Info;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO: Remove SuppressLInt
@SuppressLint("NewApi")
@RequiresApi(api = Build.VERSION_CODES.R)
class MediaRouter2Utils {
    private MediaRouter2Utils() {}

    public static MediaRoute2Info toFwkMediaRoute2Info(MediaRouteDescriptor descriptor) {
        return new MediaRoute2Info.Builder(descriptor.getId(), descriptor.getName())
                .setDescription(descriptor.getDescription())
                .setConnectionState(descriptor.getConnectionState())
                .setVolumeHandling(descriptor.getVolumeHandling())
                .setVolume(descriptor.getVolume())
                .setVolumeMax(descriptor.getVolumeMax())
                .addFeatures(getFeaturesFromIntentFilters(descriptor.getControlFilters()))
                .setIconUri(descriptor.getIconUri())
                .setExtras(descriptor.getExtras())
                //TODO: set device type (for SystemUI to display proper icon?)
                //.setDeviceType(deviceType)
                //TODO: set client package name
                //.setClientPackageName(clientMap.get(device.getDeviceId()))
                .build();
    }

    static Collection<String> getFeaturesFromIntentFilters(List<IntentFilter> controlFilters) {
        Set<String> features = new HashSet<String>();
        for (IntentFilter filter : controlFilters) {
            int count = filter.countCategories();
            for (int i = 0; i < count; i++) {
                features.add(filter.getCategory(i));
            }
        }
        return features;
    }
}
