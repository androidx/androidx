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

package androidx.car.app.testing.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.LaneDirection;

import java.util.List;

/**
 * A controller that allows testing of a {@link Lane}.
 *
 * <p>This controller allows retrieving the {@link LaneDirection}s added via {@link
 * Lane.Builder#addDirection}.
 */
public class LaneController {
    private final Lane mLane;

    /** Creates a {@link LaneController} to control a {@link Lane} for testing. */
    @NonNull
    public static LaneController of(@NonNull Lane lane) {
        return new LaneController(requireNonNull(lane));
    }

    /** Returns all of the {@link LaneDirection}s added to the {@link Lane} being controlled. */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<LaneDirection> getDirections() {
        return (List<LaneDirection>) requireNonNull(
                ControllerUtil.getFieldOrThrow(mLane, "directions"));
    }

    /** Retrieves the {@link Lane} that this controller is controlling. */
    @NonNull
    public Lane get() {
        return mLane;
    }

    private LaneController(Lane lane) {
        this.mLane = lane;
    }
}
