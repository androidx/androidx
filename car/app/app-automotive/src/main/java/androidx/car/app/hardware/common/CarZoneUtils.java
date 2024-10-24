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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;

import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Car zone utility methods. */
@ExperimentalCarApi
public final class CarZoneUtils {

    /**
     * Area types determine how {@link CarZone}s are converted to and from platform area ids.
     *
     */
    @RestrictTo(LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            AreaType.SEAT,
            AreaType.NONE,
    })
    public @interface AreaType {
        int SEAT = 1;
        int NONE = 2;
    }

    private CarZoneUtils() {}

    /**
     * Converts {@code areaId}, which is a bitmask of car areas, into a list of car zones.
     * Each object in the return list corresponds to an area in {@code areaId}.
     */
    public static @NonNull ImmutableSet<CarZone> convertAreaIdToCarZones(
            @AreaType int areaType, int areaId) {
        return getZoneAreaIdConverter(areaType).convertAreaIdToCarZones(areaId);
    }

    /**
     * Gets an object of the converter classes based on the area type. Only Seat area
     * type is supported yet.
     */
    public static @NonNull CarZoneAreaIdConverter getZoneAreaIdConverter(
            @AreaType int areaType) {
        switch (areaType) {
            //TODO(b/241144091): Add support for other types of areas.
            case AreaType.NONE:
                return new GlobalCarZoneAreaIdConverter();
            case AreaType.SEAT:
                return new SeatCarZoneAreaIdConverter();
            default:
                throw new IllegalArgumentException("Unsupported areaType: " + areaType);
        }
    }
}
