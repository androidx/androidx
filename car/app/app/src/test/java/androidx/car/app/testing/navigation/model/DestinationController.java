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

package androidx.car.app.testing.navigation.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.car.app.model.LatLng;
import androidx.car.app.model.Place;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.testing.model.ControllerUtil;

/**
 * A controller that allows testing of a {@link Destination}.
 *
 * <p>This controller allows retrieving the following fields:
 *
 * <ul>
 *   <li>The name set via {@link Destination.Builder#setName}.
 *   <li>The address set via {@link Destination.Builder#setAddress}.
 * </ul>
 */
public class DestinationController {
    private final Destination mDestination;

    /** Creates a {@link DestinationController} to control a {@link Destination} for testing. */
    @NonNull
    public static DestinationController of(@NonNull Destination destination) {
        return new DestinationController(requireNonNull(destination));
    }

    /**
     * Retrieves the name that was set in the {@link Destination} being controlled via {@link
     * Destination.Builder#setName} or {@link Place#builder(LatLng)}.
     *
     * <p>The value returned is the {@link CharSequence#toString} for the name provided.
     */
    @NonNull
    public String getName() {
        return requireNonNull(ControllerUtil.getFieldOrThrow(mDestination, "name")).toString();
    }

    /**
     * Retrieves the address that was set in the {@link Destination} being controlled via {@link
     * Destination.Builder#setAddress}.
     *
     * <p>The value returned is the {@link CharSequence#toString} for the address provided.
     */
    @NonNull
    public String getAddress() {
        return requireNonNull(ControllerUtil.getFieldOrThrow(mDestination, "address")).toString();
    }

    /** Retrieves the {@link Destination} that this controller is controlling. */
    @NonNull
    public Destination get() {
        return mDestination;
    }

    private DestinationController(Destination destination) {
        this.mDestination = destination;
    }
}
