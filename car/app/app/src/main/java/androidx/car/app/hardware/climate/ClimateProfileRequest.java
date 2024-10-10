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

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Allows specification of a request for fetching the climate profile information using integer
 * flags and car zones.
 *
 * <p>A climate profile comprises of certain features of a car such as HVAC power, fan direction,
 * etc of a car the information of which can be requested for particular car zones through this
 * class.
 *
 * <p> Applications need to use {@link Builder} to create a
 * {@link ClimateProfileRequest}.
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
public final class ClimateProfileRequest {
    /**
     * Possible feature flags can be used to create request.
     *
     */
    @IntDef({
            FEATURE_HVAC_POWER,
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
            FEATURE_HVAC_ELECTRIC_DEFROSTER,
            FEATURE_CAR_ZONE_MAPPING,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface ClimateProfileFeature {

    }

    /**
     * Adding this feature flag, the application will get the profile information of HVAC power
     * in the car by
     * {@link CarClimateProfileCallback#onHvacPowerProfileAvailable(HvacPowerProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_POWER = 1;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC AC
     * in the car by {@link CarClimateProfileCallback#onHvacAcProfileAvailable(HvacAcProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_AC = 2;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC MAX AC
     * in the car by
     * {@link CarClimateProfileCallback#onHvacMaxAcModeProfileAvailable(HvacMaxAcModeProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_MAX_AC = 3;

    /**
     * Adding this feature flag, the application will get the profile information of cabin
     * temperature in the car by
     * {@link CarClimateProfileCallback#onCabinTemperatureProfileAvailable(
     * CabinTemperatureProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_CABIN_TEMPERATURE = 4;

    /**
     * Adding this feature flag, the application will get the profile information of climate
     * fan speed level in the car by
     * {@link CarClimateProfileCallback#onFanSpeedLevelProfileAvailable(FanSpeedLevelProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_FAN_SPEED = 5;

    /**
     * Adding this feature flag, the application will get the profile information of climate fan
     * directions in the car by
     * {@link CarClimateProfileCallback#onFanDirectionProfileAvailable(FanDirectionProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_FAN_DIRECTION = 6;

    /**
     * Adding this feature flag, the application will get the profile information of seat
     * temperature levels in the car by
     * {@link CarClimateProfileCallback#onSeatTemperatureLevelProfileAvailable(
     * SeatTemperatureProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_SEAT_TEMPERATURE_LEVEL = 7;

    /**
     * Adding this feature flag, the application will get the profile information of seat
     * ventilation levels in the car by
     * {@link CarClimateProfileCallback#onSeatVentilationLevelProfileAvailable(
     * SeatVentilationProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_SEAT_VENTILATION_LEVEL = 8;

    /**
     * Adding this feature flag, the application will get the profile information of steering
     * wheel heat in the car by
     * {@link CarClimateProfileCallback#onSteeringWheelHeatProfileAvailable(
     * SteeringWheelHeatProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_STEERING_WHEEL_HEAT = 9;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC
     * recirculation mode in the car by
     * {@link CarClimateProfileCallback#onHvacRecirculationProfileAvailable(
     * HvacRecirculationProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_RECIRCULATION = 10;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC AUTO
     * recirculation mode in the car by
     * {@link CarClimateProfileCallback#onHvacAutoRecirculationProfileAvailable(
     * HvacAutoRecirculationProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_AUTO_RECIRCULATION = 11;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC AUTO mode
     * in the car by {@link CarClimateProfileCallback#onHvacAutoModeProfileAvailable(
     * HvacAutoModeProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_AUTO_MODE = 12;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC DUAL mode
     * in the car by {@link CarClimateProfileCallback#onHvacDualModeProfileAvailable(
     * HvacDualModeProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_DUAL_MODE = 13;

    /**
     * Adding this feature flag, the application will get the profile information of defroster
     * in the car by {@link CarClimateProfileCallback#onDefrosterProfileAvailable(
     * DefrosterProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_DEFROSTER = 14;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC MAX
     * defroster mode in the car by
     * {@link CarClimateProfileCallback#onMaxDefrosterProfileAvailable(MaxDefrosterProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_MAX_DEFROSTER = 15;

    /**
     * Adding this feature flag, the application will get the profile information of HVAC
     * electric defroster in the car by
     * {@link CarClimateProfileCallback#onElectricDefrosterProfileAvailable(
     * ElectricDefrosterProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_HVAC_ELECTRIC_DEFROSTER = 16;

    /**
     * Adding this feature flag, the application will get the profile information of car zone
     * mapping in the car by
     * {@link CarClimateProfileCallback#onCarZoneMappingInfoProfileAvailable(
     * CarZoneMappingInfoProfile)}.
     */
    @ClimateProfileFeature
    public static final int FEATURE_CAR_ZONE_MAPPING = 17;

    static final Set<Integer> ALL_FEATURES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    FEATURE_HVAC_POWER,
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
                    FEATURE_HVAC_ELECTRIC_DEFROSTER,
                    FEATURE_CAR_ZONE_MAPPING
            ))
    );

    private final List<CarClimateFeature> mRequestFeatures;

    ClimateProfileRequest(Builder builder) {
        if (builder.mAllProfiles) {
            mRequestFeatures = Collections.unmodifiableList(constructAllFeatures());
        } else {
            mRequestFeatures = Collections.unmodifiableList(builder.mFeatures);
        }
    }

    /** Returns a list of CarClimateFeatures which are included in this request. */
    public @NonNull List<CarClimateFeature> getClimateProfileFeatures() {
        return mRequestFeatures;
    }

    /** Returns a set of all possible CarClimateFeatures. */
    public @NonNull Set<Integer>  getAllClimateProfiles() {
        return ALL_FEATURES;
    }

    @Override
    public @NonNull String toString() {
        return "ClimateProfileRequest{"
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
        ClimateProfileRequest that = (ClimateProfileRequest) o;
        return Objects.equals(mRequestFeatures, that.mRequestFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRequestFeatures);
    }

    private List<CarClimateFeature> constructAllFeatures() {
        List<CarClimateFeature> features = new ArrayList<>(ALL_FEATURES.size());
        for (int flag : ALL_FEATURES) {
            features.add(new CarClimateFeature.Builder(flag).build());
        }
        return features;
    }

    /** A builder of {@link ClimateProfileRequest}. */
    public static final class Builder {
        boolean mAllProfiles = false;
        List<CarClimateFeature> mFeatures;

        /**
         * Creates an instance of {@link ClimateProfileRequest.Builder}.
         */
        public Builder() {
            mFeatures = new ArrayList<>();
        }

        /**
         * Adds all CarClimateFeatures in the request by enabling all profiles.
         */
        public @NonNull Builder setAllClimateProfiles() {
            mAllProfiles = true;
            return this;
        }

        /**
         * Adds the given CarClimateFeatures in the request.
         *
         * @param features CarClimateFeatures indicates the features
         *                 the application is interested in
         * @throws IllegalArgumentException if the feature flag is not one of
         *                                  ClimateProfileFeatures
         */
        public @NonNull Builder addClimateProfileFeatures(
                CarClimateFeature @NonNull ... features) {
            for (CarClimateFeature feature : features) {
                int flag = feature.getFeature();
                if (!ALL_FEATURES.contains(flag)) {
                    throw new IllegalArgumentException("Invalid flag for climate profile request: "
                            + flag);
                }
                if (mFeatures.contains(feature)) {
                    throw new IllegalArgumentException("Flag already registered in climate "
                            + "profile request: " + flag);
                }
                mFeatures.add(feature);
            }
            return this;
        }

        /**
         * Constructs the {@link ClimateProfileRequest} defined by this builder.
         */
        public @NonNull ClimateProfileRequest build() {
            return new ClimateProfileRequest(this);
        }
    }
}