/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.workaround;

import android.media.MediaMuxer;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.NegativeLatLongSavesIncorrectlyQuirk;

/**
 * Workaround to correct negative geo location in the saved video metadata.
 *
 * <p>See {@link NegativeLatLongSavesIncorrectlyQuirk} and b/232327925 for the reason.
 * {@link #adjustGeoLocation} should be applied to the geo location before setting to
 * MediaMuxer#setLocation.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CorrectNegativeLatLongForMediaMuxer {

    private CorrectNegativeLatLongForMediaMuxer() {
    }

    /**
     * Adjusts the geo location for setting to {@link MediaMuxer#setLocation(float, float)}.
     *
     * <p>In the source code of MediaMuxer#setLocation:
     * <pre>{@code
     *     int latitudex10000  = (int) (latitude * 10000 + 0.5);
     *     int longitudex10000 = (int) (longitude * 10000 + 0.5);
     * }</pre>
     * For negative latitude and longitude, "minus 0.5" should be applied instead of "plus 0.5" for
     * rounding. This method does nothing for positive value and "minus 1" for negative value.
     * The geo location should apply this adjustment before setting to MediaMuxer#setLocation, which
     * results in the effect of "minus 0.5" for rounding on negative latitude and longitude in
     * the above source code.
     *
     * @return a pair of {@link Double}. The first element of the pair is the adjusted latitude.
     * The second element of the pair is the adjusted longitude.
     */
    @NonNull
    public static Pair<Double, Double> adjustGeoLocation(double latitude, double longitude) {
        if (DeviceQuirks.get(NegativeLatLongSavesIncorrectlyQuirk.class) != null) {
            latitude = adjustInternal(latitude);
            longitude = adjustInternal(longitude);
        }
        return Pair.create(latitude, longitude);
    }

    private static double adjustInternal(double value) {
        return value >= 0 ? value : (value * 10000.0 - 1.0) / 10000.0;
    }
}
