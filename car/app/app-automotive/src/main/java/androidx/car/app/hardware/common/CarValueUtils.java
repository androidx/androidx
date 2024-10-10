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

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Utility functions to work with {@link androidx.car.app.hardware.info.AutomotiveCarInfo} and
 * {@link androidx.car.app.hardware.climate.AutomotiveCarClimate}
 *
 */
@RestrictTo(LIBRARY)
public final class CarValueUtils {
    /**
     * Gets a {@link androidx.car.app.hardware.common.CarValue} object from
     * {@link androidx.car.app.hardware.common.CarPropertyResponse}
     */
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static <T> @NonNull CarValue<T> getCarValue(@NonNull CarPropertyResponse<?> response,
            @Nullable T value) {
        long timestampMillis = response.getTimestampMillis();
        int status = response.getStatus();
        List<CarZone> zones =  response.getCarZones();
        return new CarValue<>(value, timestampMillis, status, zones);
    }

    /**
     * Builds {@link CarValue} from an existing {@link CarPropertyResponse}.
     */
    @OptIn(markerClass = ExperimentalCarApi.class)
    @SuppressWarnings("unchecked")
    public static <T> @NonNull CarValue<T> getCarValue(@NonNull CarPropertyResponse<?> response) {
        return new CarValue<>((T) response.getValue(), response.getTimestampMillis(),
                response.getStatus());
    }

    private CarValueUtils() {
    }
}
