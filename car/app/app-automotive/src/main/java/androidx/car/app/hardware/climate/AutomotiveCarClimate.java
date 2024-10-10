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

import static android.car.VehiclePropertyIds.HVAC_AC_ON;
import static android.car.VehiclePropertyIds.HVAC_AUTO_ON;
import static android.car.VehiclePropertyIds.HVAC_AUTO_RECIRC_ON;
import static android.car.VehiclePropertyIds.HVAC_DEFROSTER;
import static android.car.VehiclePropertyIds.HVAC_DUAL_ON;
import static android.car.VehiclePropertyIds.HVAC_FAN_DIRECTION;
import static android.car.VehiclePropertyIds.HVAC_FAN_SPEED;
import static android.car.VehiclePropertyIds.HVAC_MAX_AC_ON;
import static android.car.VehiclePropertyIds.HVAC_MAX_DEFROST_ON;
import static android.car.VehiclePropertyIds.HVAC_POWER_ON;
import static android.car.VehiclePropertyIds.HVAC_RECIRC_ON;
import static android.car.VehiclePropertyIds.HVAC_SEAT_TEMPERATURE;
import static android.car.VehiclePropertyIds.HVAC_SEAT_VENTILATION;
import static android.car.VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT;
import static android.car.VehiclePropertyIds.HVAC_TEMPERATURE_SET;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
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
import static androidx.car.app.hardware.common.CarValueUtils.getCarValue;
import static androidx.car.app.hardware.common.PropertyUtils.getMinMaxProfileFloatMap;
import static androidx.car.app.hardware.common.PropertyUtils.getMinMaxProfileIntegerMap;

import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarPropertyProfile;
import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.CarSetOperationStatusCallback;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.CarZone;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.utils.LogTags;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Manages access to car climate system such as cabin temperatures, fan speeds and fan directions.
 *
 */
@RestrictTo(LIBRARY)
@ExperimentalCarApi
public class AutomotiveCarClimate implements CarClimate {

    @VisibleForTesting
    static final float DEFAULT_SAMPLE_RATE_HZ = 5f;
    public static final int HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID = 320865556;

    // TODO(b/240347704): replace FEATURE_HVAC_ELECTRIC_DEFROSTER value with
    //  HVAC_ELECTRIC_DEFROSTER_ON if it becomes available.
    static ImmutableBiMap<Integer, Integer> sFeatureToPropertyId =
            new ImmutableBiMap.Builder<Integer,
                    Integer>()
                    .put(FEATURE_HVAC_POWER, HVAC_POWER_ON)
                    .put(FEATURE_HVAC_AC, HVAC_AC_ON)
                    .put(FEATURE_HVAC_MAX_AC, HVAC_MAX_AC_ON)
                    .put(FEATURE_CABIN_TEMPERATURE, HVAC_TEMPERATURE_SET)
                    .put(FEATURE_FAN_SPEED, HVAC_FAN_SPEED)
                    .put(FEATURE_FAN_DIRECTION, HVAC_FAN_DIRECTION)
                    .put(FEATURE_SEAT_TEMPERATURE_LEVEL, HVAC_SEAT_TEMPERATURE)
                    .put(FEATURE_SEAT_VENTILATION_LEVEL, HVAC_SEAT_VENTILATION)
                    .put(FEATURE_STEERING_WHEEL_HEAT, HVAC_STEERING_WHEEL_HEAT)
                    .put(FEATURE_HVAC_RECIRCULATION, HVAC_RECIRC_ON)
                    .put(FEATURE_HVAC_AUTO_RECIRCULATION, HVAC_AUTO_RECIRC_ON)
                    .put(FEATURE_HVAC_AUTO_MODE, HVAC_AUTO_ON)
                    .put(FEATURE_HVAC_DUAL_MODE, HVAC_DUAL_ON)
                    .put(FEATURE_HVAC_DEFROSTER, HVAC_DEFROSTER)
                    .put(FEATURE_HVAC_MAX_DEFROSTER, HVAC_MAX_DEFROST_ON)
                    .put(FEATURE_HVAC_ELECTRIC_DEFROSTER, HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID)
                    .buildOrThrow();

    private final Map<CarClimateStateCallback, OnCarPropertyResponseListener> mListenerMap =
            new HashMap<>();

    private final PropertyManager mPropertyManager;

    public AutomotiveCarClimate(@NonNull PropertyManager manager) {
        mPropertyManager = requireNonNull(manager);
    }

    @Override
    public void registerClimateStateCallback(@NonNull Executor executor,
            @NonNull RegisterClimateStateRequest request,
            @NonNull CarClimateStateCallback callback) {
        Map<Integer, List<CarZone>> propertyIdsWithCarZones = new HashMap<>();
        for (CarClimateFeature feature : request.getClimateRegisterFeatures()) {
            int propertyId = requireNonNull(sFeatureToPropertyId.get(feature.getFeature()));
            propertyIdsWithCarZones.put(propertyId, feature.getCarZones());
        }
        PropertyListener listener = new PropertyListener(callback, executor);
        mPropertyManager.submitRegisterListenerRequest(propertyIdsWithCarZones,
                DEFAULT_SAMPLE_RATE_HZ,
                listener,
                executor);
        mListenerMap.put(callback, listener);
    }

    @Override
    public void unregisterClimateStateCallback(@NonNull CarClimateStateCallback callback) {
        OnCarPropertyResponseListener responseListener = mListenerMap.remove(callback);
        if (responseListener != null) {
            mPropertyManager.submitUnregisterListenerRequest(responseListener);
        }
    }

    @Override
    public void fetchClimateProfile(@NonNull Executor executor,
            @NonNull ClimateProfileRequest request,
            @NonNull CarClimateProfileCallback callback) {
        if (request.getClimateProfileFeatures().isEmpty()) {
            Log.e(LogTags.TAG_CAR_HARDWARE,
                    "ClimateProfileRequest does not contain features.");
            return;
        }
        List<Integer> propertyIds = new ArrayList<>();
        for (CarClimateFeature feature : request.getClimateProfileFeatures()) {
            propertyIds.add(requireNonNull(sFeatureToPropertyId.get(
                    feature.getFeature())));
        }
        ListenableFuture<List<CarPropertyProfile<?>>> future =
                mPropertyManager.fetchSupportedZonesResponse(
                        propertyIds, executor);
        populateData(executor, callback, future);
    }

    @Override
    public <E> void setClimateState(@NonNull Executor executor,
            @NonNull ClimateStateRequest<E> request,
            @NonNull CarSetOperationStatusCallback callback) {

    }

    private static class PropertyListener implements OnCarPropertyResponseListener {
        private final Executor mExecutor;
        private final CarClimateStateCallback mCarClimateStateCallback;

        PropertyListener(CarClimateStateCallback callback, Executor executor) {
            mCarClimateStateCallback = callback;
            mExecutor = executor;
        }

        @Override
        @SuppressWarnings({"unchecked", "unsafe"})
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            mExecutor.execute(() -> {
                for (CarPropertyResponse<?> response : carPropertyResponses) {
                    Integer mFeature = sFeatureToPropertyId.inverse().get(response.getPropertyId());
                    if (mFeature == null) {
                        Log.e(LogTags.TAG_CAR_HARDWARE, "Feature not found for property Id "
                                + response.getPropertyId());
                        continue;
                    }
                    CarValue<?> mCarValue = getCarValue(response, response.getValue());
                    switch (mFeature) {
                        case FEATURE_HVAC_POWER:
                            mCarClimateStateCallback.onHvacPowerStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_AC:
                            mCarClimateStateCallback.onHvacAcStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_MAX_AC:
                            mCarClimateStateCallback.onHvacMaxAcModeStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_CABIN_TEMPERATURE:
                            mCarClimateStateCallback.onCabinTemperatureStateAvailable(
                                    (CarValue<Float>) mCarValue);
                            break;
                        case FEATURE_FAN_SPEED:
                            mCarClimateStateCallback.onFanSpeedLevelStateAvailable(
                                    (CarValue<Integer>) mCarValue);
                            break;
                        case FEATURE_FAN_DIRECTION:
                            mCarClimateStateCallback.onFanDirectionStateAvailable(
                                    (CarValue<Integer>) mCarValue);
                            break;
                        case FEATURE_SEAT_TEMPERATURE_LEVEL:
                            mCarClimateStateCallback.onSeatTemperatureLevelStateAvailable(
                                    (CarValue<Integer>) mCarValue);
                            break;
                        case FEATURE_SEAT_VENTILATION_LEVEL:
                            mCarClimateStateCallback.onSeatVentilationLevelStateAvailable(
                                    (CarValue<Integer>) mCarValue);
                            break;
                        case FEATURE_STEERING_WHEEL_HEAT:
                            mCarClimateStateCallback.onSteeringWheelHeatStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_RECIRCULATION:
                            mCarClimateStateCallback.onHvacRecirculationStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_AUTO_RECIRCULATION:
                            mCarClimateStateCallback.onHvacAutoRecirculationStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_AUTO_MODE:
                            mCarClimateStateCallback.onHvacAutoModeStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_DUAL_MODE:
                            mCarClimateStateCallback.onHvacDualModeStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_DEFROSTER:
                            mCarClimateStateCallback.onDefrosterStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_MAX_DEFROSTER:
                            mCarClimateStateCallback.onMaxDefrosterStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        case FEATURE_HVAC_ELECTRIC_DEFROSTER:
                            mCarClimateStateCallback.onElectricDefrosterStateAvailable(
                                    (CarValue<Boolean>) mCarValue);
                            break;
                        default:
                            Log.e(LogTags.TAG_CAR_HARDWARE,
                                    "Invalid response callback in PropertyListener with "
                                            + "feature value: " + mFeature);
                            break;
                    }
                }
            });
        }
    }

    private static void populateData(@NonNull Executor executor,
            @NonNull CarClimateProfileCallback onCarClimateProfileCallback,
            ListenableFuture<List<CarPropertyProfile<?>>> future) {
        future.addListener(() -> {
            List<CarPropertyProfile<?>> carPropertyProfiles;
            try {
                carPropertyProfiles = future.get();
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse due to error", e);
                return;
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse due to error", e);
                Thread.currentThread().interrupt();
                return;
            }

            // Extract all car zones corresponding to each feature.
            for (CarPropertyProfile<?> carPropertyProfile : carPropertyProfiles) {
                Integer feature = sFeatureToPropertyId.inverse().get(
                        carPropertyProfile.getPropertyId());
                if (feature == null) {
                    Log.e(LogTags.TAG_CAR_HARDWARE, "Feature not found for property Id "
                            + carPropertyProfile.getPropertyId());
                    continue;
                }

                switch (feature) {
                    case FEATURE_HVAC_POWER:
                        onCarClimateProfileCallback.onHvacPowerProfileAvailable(
                                new HvacPowerProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_CABIN_TEMPERATURE:
                        if ((carPropertyProfile.getCelsiusRange() == null
                                || carPropertyProfile.getFahrenheitRange() == null)
                                && (carPropertyProfile.getCarZoneSetsToMinMaxRange() == null)) {
                            Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch cabin "
                                    + "temperature value with profile: " + carPropertyProfile);
                            break;
                        }
                        CabinTemperatureProfile.Builder cabinTemperatureProfileBuilder =
                                new CabinTemperatureProfile.Builder();
                        if (carPropertyProfile.getCelsiusRange() != null) {
                            cabinTemperatureProfileBuilder.setSupportedMinMaxCelsiusRange(
                                    carPropertyProfile.getCelsiusRange());
                        }
                        if (carPropertyProfile.getFahrenheitRange() != null) {
                            cabinTemperatureProfileBuilder.setSupportedMinMaxFahrenheitRange(
                                    carPropertyProfile.getFahrenheitRange());
                        }
                        if (carPropertyProfile.getCarZoneSetsToMinMaxRange() != null) {
                            cabinTemperatureProfileBuilder
                                    .setCarZoneSetsToCabinCelsiusTemperatureRanges(
                                            getMinMaxProfileFloatMap(
                                                    carPropertyProfile
                                                            .getCarZoneSetsToMinMaxRange()));
                        }
                        if (carPropertyProfile.getCelsiusIncrement() != -1f) {
                            cabinTemperatureProfileBuilder.setCelsiusSupportedIncrement(
                                    carPropertyProfile.getCelsiusIncrement());
                        }
                        if (carPropertyProfile.getCelsiusIncrement() != -1f) {
                            cabinTemperatureProfileBuilder.setFahrenheitSupportedIncrement(
                                    carPropertyProfile.getFahrenheitIncrement());
                        }
                        onCarClimateProfileCallback.onCabinTemperatureProfileAvailable(
                                cabinTemperatureProfileBuilder.build());
                        break;
                    case FEATURE_FAN_SPEED:
                        if (carPropertyProfile.getCarZoneSetsToMinMaxRange() == null) {
                            Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch fan speed value"
                                    + " with profile: " + carPropertyProfile);
                            break;
                        }
                        onCarClimateProfileCallback.onFanSpeedLevelProfileAvailable(
                                new FanSpeedLevelProfile.Builder(getMinMaxProfileIntegerMap(
                                        carPropertyProfile.getCarZoneSetsToMinMaxRange()))
                                        .build()
                        );
                        break;
                    case FEATURE_FAN_DIRECTION:
                        if (carPropertyProfile.getHvacFanDirection() == null) {
                            Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch direction value"
                                    + " with profile: " + carPropertyProfile);
                            break;
                        }
                        onCarClimateProfileCallback.onFanDirectionProfileAvailable(
                                new FanDirectionProfile.Builder(
                                        carPropertyProfile.getHvacFanDirection())
                                        .build()
                        );
                        break;
                    case FEATURE_SEAT_TEMPERATURE_LEVEL:
                        if (carPropertyProfile.getCarZoneSetsToMinMaxRange() == null) {
                            Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch seat temperature"
                                    + " value with profile: " + carPropertyProfile);
                            break;
                        }
                        onCarClimateProfileCallback.onSeatTemperatureLevelProfileAvailable(
                                new SeatTemperatureProfile.Builder(getMinMaxProfileIntegerMap(
                                        carPropertyProfile.getCarZoneSetsToMinMaxRange()))
                                        .build()
                        );
                        break;
                    case FEATURE_SEAT_VENTILATION_LEVEL:
                        if (carPropertyProfile.getCarZoneSetsToMinMaxRange() == null) {
                            Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch seat ventilation"
                                    + " value with profile: " + carPropertyProfile);
                            break;
                        }
                        onCarClimateProfileCallback.onSeatVentilationLevelProfileAvailable(
                                new SeatVentilationProfile.Builder(getMinMaxProfileIntegerMap(
                                        carPropertyProfile.getCarZoneSetsToMinMaxRange()))
                                        .build()
                        );
                        break;
                    case FEATURE_STEERING_WHEEL_HEAT:
                        if (carPropertyProfile.getCarZoneSetsToMinMaxRange() == null) {
                            Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch steering wheel"
                                    + " heat value with profile: " + carPropertyProfile);
                            break;
                        }
                        onCarClimateProfileCallback.onSteeringWheelHeatProfileAvailable(
                                new SteeringWheelHeatProfile.Builder(getMinMaxProfileIntegerMap(
                                        carPropertyProfile.getCarZoneSetsToMinMaxRange()))
                                        .build()
                        );
                        break;
                    case FEATURE_HVAC_AC:
                        onCarClimateProfileCallback.onHvacAcProfileAvailable(
                                new HvacAcProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_MAX_AC:
                        onCarClimateProfileCallback.onHvacMaxAcModeProfileAvailable(
                                new HvacMaxAcModeProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_RECIRCULATION:
                        onCarClimateProfileCallback.onHvacRecirculationProfileAvailable(
                                new HvacRecirculationProfile.Builder(
                                        carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_AUTO_RECIRCULATION:
                        onCarClimateProfileCallback.onHvacAutoRecirculationProfileAvailable(
                                new HvacAutoRecirculationProfile.Builder(
                                        carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_AUTO_MODE:
                        onCarClimateProfileCallback.onHvacAutoModeProfileAvailable(
                                new HvacAutoModeProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_DUAL_MODE:
                        onCarClimateProfileCallback.onHvacDualModeProfileAvailable(
                                new HvacDualModeProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_DEFROSTER:
                        onCarClimateProfileCallback.onDefrosterProfileAvailable(
                                new DefrosterProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_MAX_DEFROSTER:
                        onCarClimateProfileCallback.onMaxDefrosterProfileAvailable(
                                new MaxDefrosterProfile.Builder(carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    case FEATURE_HVAC_ELECTRIC_DEFROSTER:
                        onCarClimateProfileCallback.onElectricDefrosterProfileAvailable(
                                new ElectricDefrosterProfile.Builder(
                                        carPropertyProfile.getCarZones())
                                        .build());
                        break;
                    default:
                        Log.e(LogTags.TAG_CAR_HARDWARE,
                                "Invalid response callback while populating data for "
                                        + "feature value: " + feature);
                        break;
                }
            }
        }, executor);
    }
}
