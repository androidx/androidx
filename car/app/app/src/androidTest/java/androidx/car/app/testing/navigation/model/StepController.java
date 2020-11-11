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
import androidx.annotation.Nullable;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.testing.model.ControllerUtil;
import androidx.car.app.testing.model.LaneController;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller that allows testing of a {@link Step}.
 *
 * <p>This controller allows retrieving the following fields:
 *
 * <ul>
 *   <li>The {@link Maneuver} set via {@link Step.Builder#setManeuver}.
 *   <li>The {@link Lane}s added via {@link Step.Builder#addLane}.
 *   <li>The cue set via {@link Step#builder} or {@link Step.Builder#setCue}.
 *   <li>The road set via {@link Step.Builder#setRoad}.
 * </ul>
 */
public class StepController {
    private final Step mStep;

    /** Creates a {@link StepController} to control a {@link Step} for testing. */
    @NonNull
    public static StepController of(@NonNull Step step) {
        return new StepController(requireNonNull(step));
    }

    /**
     * Retrieves the {@link Maneuver} that is set in the {@link Step} that is being controlled, or
     * {@code null} if none is present.
     */
    @Nullable
    public Maneuver getManeuver() {
        return (Maneuver) ControllerUtil.getFieldOrThrow(mStep, "maneuver");
    }

    /**
     * Returns a list of {@link LaneController}s, each containing a {@link Lane} added via {@link
     * Step.Builder#addLane}.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<LaneController> getLanes() {
        List<Lane> lanes = (List<Lane>) ControllerUtil.getFieldOrThrow(mStep, "lanes");
        List<LaneController> toReturn = new ArrayList<>();

        if (lanes != null) {
            for (Lane lane : lanes) {
                toReturn.add(LaneController.of(lane));
            }
        }
        return toReturn;
    }

    /**
     * Retrieves the cue set in the {@link Step} being controlled.
     *
     * <p>The values returned are the {@link CharSequence#toString} for the cue provided.
     */
    @NonNull
    public String getCue() {
        return requireNonNull(ControllerUtil.getFieldOrThrow(mStep, "cue")).toString();
    }

    /**
     * Retrieves the road set in the {@link Step} being controlled, or {@code null} if none is
     * present.
     *
     * <p>The values returned are the {@link CharSequence#toString} for the road provided.
     */
    @Nullable
    public String getRoad() {
        Object road = ControllerUtil.getFieldOrThrow(mStep, "road");
        return road == null ? null : road.toString();
    }

    /** Retrieves the {@link Step} that this controller is controlling. */
    @NonNull
    public Step get() {
        return mStep;
    }

    private StepController(Step step) {
        this.mStep = step;
    }
}
