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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;

import java.util.List;
import java.util.Objects;

/** Information about car specific gyroscopes available from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
public final class Gyroscope {
    @Keep
    @NonNull
    private final CarValue<List<Float>> mRotations;

    /**
     * Returns the raw gyroscope data from the car sensor.
     *
     * <p>Individual values can be {@code Float.Nan} if not reported. The array values consist of:
     * <ul>
     *     <li>[0]: X component of rotation, in rad/s
     *     <li>[1]: Y component of rotation, in rad/s
     *     <li>[2]: Z component of rotation, in rad/s
     * </ul>
     */
    @NonNull
    public CarValue<List<Float>> getRotations() {
        return mRotations;
    }

    @Override
    @NonNull
    public String toString() {
        return "[ rotations: " + mRotations + " ]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRotations);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Gyroscope)) {
            return false;
        }
        Gyroscope otherGyroscope = (Gyroscope) other;

        return Objects.equals(mRotations, otherGyroscope.mRotations);
    }

    /**
     * Creates an {@link Gyroscope} with the given raw data.
     *
     * @throws NullPointerException if {@code rotations} is {@code null}
     */
    public Gyroscope(@NonNull CarValue<List<Float>> rotations) {
        mRotations = requireNonNull(rotations);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Gyroscope() {
        mRotations = CarValue.UNIMPLEMENTED_FLOAT_LIST;
    }
}
