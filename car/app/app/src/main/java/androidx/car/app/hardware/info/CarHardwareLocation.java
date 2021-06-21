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
package androidx.car.app.hardware.info;

import static java.util.Objects.requireNonNull;

import android.location.Location;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;

import java.util.Objects;

/** Information about car specific car location available from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
public final class CarHardwareLocation {

    // Not private because needed in builder.
    static final CarValue<Location> UNIMPLEMENTED_LOCATION = new CarValue<>(null, 0,
            CarValue.STATUS_UNAVAILABLE);

    @Keep
    @NonNull
    private final CarValue<Location> mLocation;

    /** Returns the raw location data from the car sensor. */
    @NonNull
    public CarValue<Location> getLocation() {
        return mLocation;
    }

    @Override
    @NonNull
    public String toString() {
        return "[ location: " + mLocation + " ]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLocation);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarHardwareLocation)) {
            return false;
        }
        CarHardwareLocation otherCarHardwareLocation = (CarHardwareLocation) other;

        return Objects.equals(mLocation, otherCarHardwareLocation.mLocation);
    }

    /**
     * Creates an {@link CarHardwareLocation} with the given raw data.
     *
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public CarHardwareLocation(@NonNull CarValue<Location> location) {
        mLocation = requireNonNull(location);
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarHardwareLocation() {
        mLocation = UNIMPLEMENTED_LOCATION;
    }
}
