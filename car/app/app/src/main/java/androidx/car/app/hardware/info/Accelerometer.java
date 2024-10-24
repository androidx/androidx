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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** Information about car specific accelerometers available from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
@KeepFields
public final class Accelerometer {
    private final @NonNull CarValue<List<Float>> mForces;

    /**
     * Returns the raw accelerometer force data from the car sensor.
     *
     * <p>Individual values can be {@code Float.Nan} if not reported. The array values consist of:
     * <ul>
     *     <li>[0]: Acceleration including gravity on the x-axis
     *     <li>[1]: Acceleration including gravity on the y-axis
     *     <li>[2]: Acceleration including gravity on the z-axis
     * </ul>
     */
    public @NonNull CarValue<List<Float>> getForces() {
        return mForces;
    }

    @Override
    public @NonNull String toString() {
        return "[ forces: " + mForces + " ]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mForces);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Accelerometer)) {
            return false;
        }
        Accelerometer otherAccelerometer = (Accelerometer) other;

        return Objects.equals(mForces, otherAccelerometer.mForces);
    }

    /**
     * Creates an {@link Accelerometer} with the given raw data.
     *
     * @throws NullPointerException if {@code forces} is {@code null}
     */
    public Accelerometer(@NonNull CarValue<List<Float>> forces) {
        mForces = requireNonNull(forces);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Accelerometer() {
        mForces = CarValue.UNKNOWN_FLOAT_LIST;
    }
}
