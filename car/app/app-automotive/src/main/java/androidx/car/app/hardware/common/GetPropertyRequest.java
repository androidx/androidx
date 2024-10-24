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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Container class for information about getting property values.
 *
 * <P>submits this request to {@link PropertyManager} for getting property values.
 *
 */
@AutoValue
@RestrictTo(LIBRARY)
public abstract class GetPropertyRequest {
    /**
     * Creates a request with {@link CarZone#CAR_ZONE_GLOBAL}.
     *
     * @param propertyId    one of the values in {@link android.car.VehiclePropertyIds}
     */
    public static @NonNull GetPropertyRequest create(int propertyId) {
        return builder().setPropertyId(propertyId).build();
    }

    /** Returns one of the values in {@link android.car.VehiclePropertyIds}. */
    public abstract int getPropertyId();

    /** Returns a list of {@link CarZone}s associated with this request.  */
    public abstract @NonNull ImmutableList<CarZone> getCarZones();

    /** Get a {@link Builder} for creating the {@link GetPropertyRequest}. */
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static @NonNull Builder builder() {
        return new AutoValue_GetPropertyRequest.Builder()
                .setCarZones(Collections.singletonList(CarZone.CAR_ZONE_GLOBAL));
    }

    /** A builder for the {@link GetPropertyRequest}. */
    @AutoValue.Builder
    public abstract static class Builder {
        /** Sets a property ID for the {@link GetPropertyRequest}. */
        public abstract @NonNull Builder setPropertyId(int propertyId);

        /** Sets a list of {@link CarZone}s for the {@link GetPropertyRequest}. */
        public abstract @NonNull Builder setCarZones(@NonNull List<CarZone> carZones);

        /** Creates an instance of {@link GetPropertyRequest}. */
        public abstract @NonNull GetPropertyRequest build();
    }

}
