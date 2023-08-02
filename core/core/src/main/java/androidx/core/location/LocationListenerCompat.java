/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.location;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * A version of {@link LocationListener} suitable for use on all API levels.
 */
public interface LocationListenerCompat extends LocationListener {

    @Override
    default void onStatusChanged(@NonNull String provider, int status, @Nullable Bundle extras) {}

    @Override
    default void onProviderEnabled(@NonNull String provider) {}

    @Override
    default void onProviderDisabled(@NonNull String provider) {}

    @Override
    default void onLocationChanged(@NonNull List<Location> locations) {
        final int size = locations.size();
        for (int i = 0; i < size; i++) {
            onLocationChanged(locations.get(i));
        }
    }

    @Override
    default void onFlushComplete(int requestCode) {}
}
