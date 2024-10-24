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

package androidx.car.app.hardware.climate;

import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_CABIN_TEMPERATURE;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_FAN_DIRECTION;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_FAN_SPEED;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AC;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AUTO_MODE;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AUTO_RECIRCULATION;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_DEFROSTER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_DUAL_MODE;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_ELECTRIC_DEFROSTER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_MAX_AC;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_MAX_DEFROSTER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_POWER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_RECIRCULATION;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_SEAT_TEMPERATURE_LEVEL;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_SEAT_VENTILATION_LEVEL;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_STEERING_WHEEL_HEAT;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Allows specification of a request for registering the climate state events using string
 * flags and car zones.
 *
 * <p> Applications need to use {@link Builder} to create a
 * {@link RegisterClimateStateRequest}.
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
public final class RegisterClimateStateRequest {

    @ClimateProfileRequest.ClimateProfileFeature
    static final Set<Integer> ALL_FEATURES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(FEATURE_HVAC_POWER,
                    FEATURE_HVAC_AC,
                    FEATURE_HVAC_MAX_AC,
                    FEATURE_CABIN_TEMPERATURE,
                    FEATURE_FAN_SPEED,
                    FEATURE_FAN_DIRECTION,
                    FEATURE_SEAT_TEMPERATURE_LEVEL,
                    FEATURE_SEAT_VENTILATION_LEVEL,
                    FEATURE_STEERING_WHEEL_HEAT,
                    FEATURE_HVAC_RECIRCULATION,
                    FEATURE_HVAC_AUTO_RECIRCULATION,
                    FEATURE_HVAC_AUTO_MODE,
                    FEATURE_HVAC_DUAL_MODE,
                    FEATURE_HVAC_DEFROSTER,
                    FEATURE_HVAC_MAX_DEFROSTER,
                    FEATURE_HVAC_ELECTRIC_DEFROSTER))
    );

    @ClimateProfileRequest.ClimateProfileFeature
    private final List<CarClimateFeature> mRequestFeatures;

    /** Returns a list of CarClimateFeatures which are included in this request. */
    @ClimateProfileRequest.ClimateProfileFeature
    public @NonNull List<CarClimateFeature> getClimateRegisterFeatures() {
        return mRequestFeatures;
    }

    RegisterClimateStateRequest(Builder builder) {
        if (builder.mRegisterAllFeatures) {
            mRequestFeatures = Collections.unmodifiableList(constructAllFeatures());
        } else {
            mRequestFeatures = Collections.unmodifiableList(builder.mFeatures);
        }
    }

    @Override
    public @NonNull String toString() {
        return "RegisterClimateStateRequest{"
                + "mRequestFeatures=" + mRequestFeatures
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RegisterClimateStateRequest that = (RegisterClimateStateRequest) o;
        return Objects.equals(mRequestFeatures, that.mRequestFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRequestFeatures);
    }

    @ClimateProfileRequest.ClimateProfileFeature
    private List<CarClimateFeature> constructAllFeatures() {
        List<CarClimateFeature> features = new ArrayList<>(ALL_FEATURES.size());
        for (int flag : ALL_FEATURES) {
            features.add(new CarClimateFeature.Builder(flag).build());
        }
        return features;
    }

    /** A builder of {@link RegisterClimateStateRequest}.*/
    public static final class Builder {
        final boolean mRegisterAllFeatures;
        List<CarClimateFeature> mFeatures;

        /**
         * Creates an instance of {@link Builder}.
         *
         * @param registerAllFeatures   a boolean value used to register all climate features in
         *                              all zones
         */
        public Builder(boolean registerAllFeatures) {
            mRegisterAllFeatures = registerAllFeatures;
            mFeatures = new ArrayList<>();
        }

        /**
         * Adds CarClimateFeatures in ClimateProfileRequest.ClimateProfileFeature.
         *
         * @param features                  indicate which features the application is
         *                                  interested in
         * @throws IllegalArgumentException if the feature flag is not one of
         *                                  ClimateProfileFeature
         */
        public @NonNull Builder addClimateRegisterFeatures(
                CarClimateFeature @NonNull ... features) {
            for (CarClimateFeature feature : features) {
                if (!ALL_FEATURES.contains(feature.getFeature())) {
                    throw new IllegalArgumentException("Invalid flag for registering climate "
                            + "request: " + feature.getFeature());
                }
                mFeatures.add(feature);
            }
            return this;
        }

        /**
         * Constructs the {@link RegisterClimateStateRequest} defined by this builder.
         */
        public @NonNull RegisterClimateStateRequest build() {
            return new RegisterClimateStateRequest(this);
        }
    }
}
