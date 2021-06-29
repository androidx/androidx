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

import static android.car.VehiclePropertyIds.DISTANCE_DISPLAY_UNITS;
import static android.car.VehiclePropertyIds.EV_BATTERY_LEVEL;
import static android.car.VehiclePropertyIds.FUEL_LEVEL;
import static android.car.VehiclePropertyIds.FUEL_LEVEL_LOW;
import static android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE;
import static android.car.VehiclePropertyIds.INFO_FUEL_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_FUEL_TYPE;
import static android.car.VehiclePropertyIds.INFO_MAKE;
import static android.car.VehiclePropertyIds.INFO_MODEL;
import static android.car.VehiclePropertyIds.INFO_MODEL_YEAR;
import static android.car.VehiclePropertyIds.PERF_ODOMETER;
import static android.car.VehiclePropertyIds.RANGE_REMAINING;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.car.VehiclePropertyIds;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.GetPropertyRequest;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.hardware.common.PropertyUtils;
import androidx.car.app.utils.LogTags;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Manages access to vehicle specific info, for example, energy info, model info.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class AutomotiveCarInfo implements CarInfo {
    @VisibleForTesting
    static final List<Integer> ENERGY_LEVEL_REQUEST =
            Arrays.asList(EV_BATTERY_LEVEL, FUEL_LEVEL,
                    FUEL_LEVEL_LOW, RANGE_REMAINING,
                    DISTANCE_DISPLAY_UNITS);
    @VisibleForTesting
    static final float DEFAULT_SAMPLE_RATE = 5f;

    /*
     * ELECTRONIC_TOLL_COLLECTION_CARD_STATUS in VehiclePropertyIds. The property is added after
     * Android Q.
     */
    public static final int TOLL_CARD_STATUS_ID = 289410874;

    // VEHICLE_SPEED_DISPLAY_UNIT in VehiclePropertyIds. The property is added after Android Q.
    // TODO(192383417) check the compile SDK
    public static final int SPEED_DISPLAY_UNIT_ID = 289408516;

    private static final float UNKNOWN_CAPACITY = Float.NEGATIVE_INFINITY;
    private static final List<Integer> MILEAGE_REQUEST =
            Arrays.asList(PERF_ODOMETER, DISTANCE_DISPLAY_UNITS);
    private static final List<Integer> TOLL_REQUEST =
            Collections.singletonList(TOLL_CARD_STATUS_ID);
    private static final List<Integer> SPEED_REQUEST =
            Arrays.asList(VehiclePropertyIds.PERF_VEHICLE_SPEED,
                    VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY, SPEED_DISPLAY_UNIT_ID);
    private final Map<OnCarDataAvailableListener<?>, OnCarPropertyResponseListener> mListenerMap =
            new HashMap<>();
    private final PropertyManager mPropertyManager;

    /**
     * AutomotiveCarInfo class constructor initializing PropertyWorkManager object.
     *
     * @throws NullPointerException if {@code manager} is null
     */
    public AutomotiveCarInfo(@NonNull PropertyManager manager) {
        mPropertyManager = requireNonNull(manager);
    }

    @Override
    public void fetchModel(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Model> listener) {
        // Prepare request GetPropertyRequest
        List<GetPropertyRequest> request = new ArrayList<>();

        // Add "make", "model", "year" of the vehicle to the requests.
        request.add(GetPropertyRequest.create(INFO_MAKE));
        request.add(GetPropertyRequest.create(INFO_MODEL));
        request.add(GetPropertyRequest.create(INFO_MODEL_YEAR));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(request, executor);
        populateModelData(executor, listener, future);
    }

    @Override
    public void fetchEnergyProfile(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyProfile> listener) {
        // Prepare request GetPropertyRequest
        List<GetPropertyRequest> request = new ArrayList<>();

        // Add "evConnector" and "fuel" type of the vehicle to the requests.
        request.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        request.add(GetPropertyRequest.create(INFO_FUEL_TYPE));

        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(request, executor);
        populateEnergyProfileData(executor, listener, future);
    }

    @Override
    public void addTollListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<TollCard> listener) {
        TollListener tollListener = new TollListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(TOLL_REQUEST, DEFAULT_SAMPLE_RATE,
                tollListener, executor);
        mListenerMap.put(listener, tollListener);
    }

    @Override
    public void removeTollListener(@NonNull OnCarDataAvailableListener<TollCard> listener) {
        removeListenerImpl(listener);
    }

    @Override
    public void addEnergyLevelListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        getCapacitiesThenEnergyLevel(executor, listener);
    }

    @Override
    public void removeEnergyLevelListener(
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        removeListenerImpl(listener);
    }

    @Override
    public void addSpeedListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Speed> listener) {
        SpeedListener speedListener = new SpeedListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(SPEED_REQUEST, DEFAULT_SAMPLE_RATE,
                speedListener, executor);
        mListenerMap.put(listener, speedListener);
    }

    @Override
    public void removeSpeedListener(@NonNull OnCarDataAvailableListener<Speed> listener) {
        removeListenerImpl(listener);
    }

    @Override
    public void addMileageListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Mileage> listener) {
        MileageListener mileageListener = new MileageListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(MILEAGE_REQUEST, DEFAULT_SAMPLE_RATE,
                mileageListener, executor);
        mListenerMap.put(listener, mileageListener);
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataAvailableListener<Mileage> listener) {
        removeListenerImpl(listener);
    }

    static <T> CarValue<T> getCarValue(CarPropertyResponse<?> response, @Nullable T value) {
        long timestampMillis = response.getTimestampMillis();
        int status = response.getStatus();
        return new CarValue<>(value, timestampMillis, status);
    }

    @VisibleForTesting
    void getCapacitiesThenEnergyLevel(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        // Prepare request GetPropertyRequest for battery and fuel capacities.
        List<GetPropertyRequest> request = new ArrayList<>();

        // Add "evConnector" and "fuel" type of the vehicle to the requests.
        request.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        request.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));

        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(request, executor);

        future.addListener(() -> {
            try {
                float evBatteryCapacity = UNKNOWN_CAPACITY;
                float fuelCapacity = UNKNOWN_CAPACITY;
                List<CarPropertyResponse<?>> result = future.get();
                for (CarPropertyResponse<?> value : result) {
                    if (value.getValue() == null) {
                        Log.w(LogTags.TAG_CAR_HARDWARE,
                                "Failed to retrieve CarPropertyResponse value for property id "
                                        + value.getPropertyId());
                        continue;
                    }
                    if (value.getPropertyId() == INFO_EV_BATTERY_CAPACITY) {
                        evBatteryCapacity = (Float) value.getValue();
                    }
                    if (value.getPropertyId() == INFO_FUEL_CAPACITY) {
                        fuelCapacity = (Float) value.getValue();
                    }
                }
                EnergyLevelListener energyLevelListener = new EnergyLevelListener(listener,
                        executor, evBatteryCapacity, fuelCapacity);
                mPropertyManager.submitRegisterListenerRequest(ENERGY_LEVEL_REQUEST,
                        DEFAULT_SAMPLE_RATE, energyLevelListener, executor);
                mListenerMap.put(listener, energyLevelListener);
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Level", e);
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Level", e);
                Thread.currentThread().interrupt();
            }
        }, executor);
    }

    private void populateModelData(@NonNull Executor executor,
            OnCarDataAvailableListener<Model> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            try {
                List<CarPropertyResponse<?>> result = future.get();
                CarValue<String> makeValue = CarValue.UNIMPLEMENTED_STRING;
                CarValue<Integer> yearValue = CarValue.UNIMPLEMENTED_INTEGER;
                CarValue<String> modelValue = CarValue.UNIMPLEMENTED_STRING;
                for (CarPropertyResponse<?> value : result) {
                    if (value.getValue() == null) {
                        Log.w(LogTags.TAG_CAR_HARDWARE,
                                "Failed to retrieve CarPropertyResponse value for property id "
                                        + value.getPropertyId());
                        continue;
                    }
                    if (value.getPropertyId() == INFO_MAKE) {
                        makeValue = getCarValue(value, (String) value.getValue());
                    }
                    if (value.getPropertyId() == INFO_MODEL) {
                        modelValue = getCarValue(value, (String) value.getValue());
                    }
                    if (value.getPropertyId() == INFO_MODEL_YEAR) {
                        yearValue = getCarValue(value, (Integer) value.getValue());
                    }
                }
                Model model = new Model.Builder().setName(makeValue)
                        .setManufacturer(modelValue)
                        .setYear(yearValue)
                        .build();
                listener.onCarDataAvailable(model);
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Model", e);
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Model", e);
                Thread.currentThread().interrupt();
            }
        }, executor);
    }

    private void populateEnergyProfileData(@NonNull Executor executor,
            OnCarDataAvailableListener<EnergyProfile> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            try {
                List<CarPropertyResponse<?>> result = future.get();
                CarValue<List<Integer>> evConnector = CarValue.UNIMPLEMENTED_INTEGER_LIST;
                CarValue<List<Integer>> fuel = CarValue.UNIMPLEMENTED_INTEGER_LIST;
                for (CarPropertyResponse<?> value : result) {
                    if (value.getValue() == null) {
                        Log.w(LogTags.TAG_CAR_HARDWARE,
                                "Failed to retrieve CarPropertyResponse value for property id"
                                        + value.getPropertyId());
                        continue;
                    }
                    if (value.getPropertyId() == INFO_EV_CONNECTOR_TYPE) {
                        Integer[] evConnectorsInVehicle = (Integer[]) value.getValue();
                        List<Integer> evConnectorsInCarValue = new ArrayList<>();
                        for (Integer connectorType : evConnectorsInVehicle) {
                            evConnectorsInCarValue.add(
                                    PropertyUtils.covertEvConnectorType(connectorType));
                        }
                        evConnector = getCarValue(value, evConnectorsInCarValue);
                    }
                    if (value.getPropertyId() == INFO_FUEL_TYPE) {
                        fuel = getCarValue(value, Arrays.stream((Integer[]) requireNonNull(
                                value.getValue())).collect(Collectors.toList()));
                    }
                }
                EnergyProfile energyProfile = new EnergyProfile.Builder().setEvConnectorTypes(
                        evConnector)
                        .setFuelTypes(fuel)
                        .build();
                listener.onCarDataAvailable(energyProfile);
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Profile", e);
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Profile", e);
                Thread.currentThread().interrupt();
            }
        }, executor);
    }

    private void removeListenerImpl(OnCarDataAvailableListener<?> listener) {
        OnCarPropertyResponseListener responseListener = mListenerMap.remove(listener);
        if (responseListener != null) {
            mPropertyManager.submitUnregisterListenerRequest(responseListener);
        } else {
            throw new IllegalArgumentException("Listener is not registered yet");
        }
    }

    private static class TollListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<TollCard> mTollOnCarDataListener;
        private final Executor mExecutor;

        TollListener(OnCarDataAvailableListener<TollCard> listener, Executor executor) {
            mTollOnCarDataListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            if (carPropertyResponses.size() != 1
                    || carPropertyResponses.get(0).getPropertyId() != TOLL_CARD_STATUS_ID) {
                Log.e(LogTags.TAG_CAR_HARDWARE, "Invalid response callback in TollListener.");
                return;
            }
            mExecutor.execute(() -> {
                CarPropertyResponse<?> response = carPropertyResponses.get(0);
                CarValue<Integer> tollValue = getCarValue(response, (Integer) response.getValue());
                TollCard toll = new TollCard.Builder().setCardState(tollValue).build();
                mTollOnCarDataListener.onCarDataAvailable(toll);
            });
        }
    }

    private static class SpeedListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<Speed> mSpeedOnCarDataListener;
        private final Executor mExecutor;

        SpeedListener(OnCarDataAvailableListener<Speed> listener, Executor executor) {
            mSpeedOnCarDataListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            if (carPropertyResponses.size() != 3) {
                Log.e(LogTags.TAG_CAR_HARDWARE, "Invalid response callback in SpeedListener.");
                return;
            }
            mExecutor.execute(() -> {
                CarValue<Float> rawSpeedValue = CarValue.UNIMPLEMENTED_FLOAT;
                CarValue<Float> displaySpeedValue = CarValue.UNIMPLEMENTED_FLOAT;
                CarValue<Integer> displayUnitValue = CarValue.UNIMPLEMENTED_INTEGER;
                for (CarPropertyResponse<?> response : carPropertyResponses) {
                    switch (response.getPropertyId()) {
                        case VehiclePropertyIds.PERF_VEHICLE_SPEED:
                            rawSpeedValue = getCarValue(response, (Float) response.getValue());
                            break;
                        case VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY:
                            displaySpeedValue = getCarValue(response, (Float) response.getValue());
                            break;
                        case SPEED_DISPLAY_UNIT_ID:
                            Integer speedUnit = null;
                            if (response.getValue() != null) {
                                speedUnit = PropertyUtils.covertSpeedUnit(
                                        (Integer) response.getValue());
                            }
                            displayUnitValue = getCarValue(response, speedUnit);
                            break;
                        default:
                            Log.e(LogTags.TAG_CAR_HARDWARE,
                                    "Invalid response callback in SpeedListener.");
                    }
                }
                Speed speed = new Speed.Builder().setRawSpeedMetersPerSecond(rawSpeedValue)
                        .setDisplaySpeedMetersPerSecond(displaySpeedValue)
                        .setSpeedDisplayUnit(displayUnitValue).build();
                mSpeedOnCarDataListener.onCarDataAvailable(speed);
            });
        }
    }

    /**
     * Mileage listener to get distance display unit and odometer updates by
     * {@link CarPropertyResponse}.
     */
    @VisibleForTesting
    static class MileageListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<Mileage> mMileageOnCarDataAvailableListener;
        private final Executor mExecutor;

        MileageListener(OnCarDataAvailableListener<Mileage> listener, Executor executor) {
            mMileageOnCarDataAvailableListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            if (carPropertyResponses.size() == 2) {
                mExecutor.execute(() -> {
                    CarValue<Float> odometerValue = CarValue.UNIMPLEMENTED_FLOAT;
                    CarValue<Integer> distanceDisplayUnitValue = CarValue.UNIMPLEMENTED_INTEGER;
                    for (CarPropertyResponse<?> response : carPropertyResponses) {
                        if (response.getValue() == null) {
                            Log.w(LogTags.TAG_CAR_HARDWARE,
                                    "Failed to retrieve CarPropertyResponse value for property id "
                                            + response.getPropertyId());
                            continue;
                        }
                        switch (response.getPropertyId()) {
                            case PERF_ODOMETER:
                                odometerValue = new CarValue<>(
                                        (Float) response.getValue(),
                                        response.getTimestampMillis(), response.getStatus());
                                break;
                            case DISTANCE_DISPLAY_UNITS:
                                Integer displayUnit = null;
                                if (response.getValue() != null) {
                                    displayUnit = PropertyUtils.covertDistanceUnit(
                                            (Integer) response.getValue());
                                }
                                distanceDisplayUnitValue = new CarValue<>(displayUnit,
                                        response.getTimestampMillis(), response.getStatus());
                                break;
                            default:
                                Log.e(LogTags.TAG_CAR_HARDWARE,
                                        "Invalid response callback in MileageListener");
                        }
                    }
                    Mileage mileage =
                            new Mileage.Builder().setOdometerMeters(
                                    odometerValue).setDistanceDisplayUnit(
                                    distanceDisplayUnitValue).build();
                    mMileageOnCarDataAvailableListener.onCarDataAvailable(mileage);
                });
            }
        }
    }

    /**
     * EnergyLevel listener to get battery, energy updates by {@link CarPropertyResponse}.
     */
    @VisibleForTesting
    static class EnergyLevelListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<EnergyLevel>
                mEnergyLevelOnCarDataAvailableListener;
        private final Executor mExecutor;
        private float mEvBatteryCapacity;
        private float mFuelCapacity;

        EnergyLevelListener(OnCarDataAvailableListener<EnergyLevel> listener, Executor executor,
                float evBatteryCapacity, float fuelCapacity) {
            mEnergyLevelOnCarDataAvailableListener = listener;
            mExecutor = executor;
            mEvBatteryCapacity = evBatteryCapacity;
            mFuelCapacity = fuelCapacity;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            if (carPropertyResponses.size() == 5) {
                mExecutor.execute(() -> {
                    CarValue<Float> batteryPercentValue = CarValue.UNIMPLEMENTED_FLOAT;
                    CarValue<Float> fuelPercentValue = CarValue.UNIMPLEMENTED_FLOAT;
                    CarValue<Boolean> energyIsLowValue = CarValue.UNIMPLEMENTED_BOOLEAN;
                    CarValue<Float> rangeRemainingValue = CarValue.UNIMPLEMENTED_FLOAT;
                    CarValue<Integer> distanceDisplayUnitValue =
                            CarValue.UNIMPLEMENTED_INTEGER;
                    for (CarPropertyResponse<?> response : carPropertyResponses) {
                        if (response.getValue() == null) {
                            Log.w(LogTags.TAG_CAR_HARDWARE,
                                    "Failed to retrieve CarPropertyResponse value for property id "
                                            + response.getPropertyId());
                            continue;
                        }
                        switch (response.getPropertyId()) {
                            case EV_BATTERY_LEVEL:
                                if (mEvBatteryCapacity != Float.NEGATIVE_INFINITY) {
                                    batteryPercentValue = new CarValue<>(
                                            (Float) response.getValue() / mEvBatteryCapacity,
                                            response.getTimestampMillis(), response.getStatus());
                                }
                                break;
                            case FUEL_LEVEL:
                                if (mFuelCapacity != Float.NEGATIVE_INFINITY) {
                                    fuelPercentValue = new CarValue<>(
                                            (Float) response.getValue() / mFuelCapacity,
                                            response.getTimestampMillis(), response.getStatus());
                                }
                                break;
                            case FUEL_LEVEL_LOW:
                                energyIsLowValue = new CarValue<>(
                                        (Boolean) response.getValue(),
                                        response.getTimestampMillis(), response.getStatus());
                                break;
                            case RANGE_REMAINING:
                                rangeRemainingValue = new CarValue<>(
                                        (Float) response.getValue(),
                                        response.getTimestampMillis(), response.getStatus());
                                break;
                            case DISTANCE_DISPLAY_UNITS:
                                distanceDisplayUnitValue = new CarValue<>(
                                        (Integer) response.getValue(),
                                        response.getTimestampMillis(), response.getStatus());
                                break;
                            default:
                                Log.e(LogTags.TAG_CAR_HARDWARE,
                                        "Invalid response callback in EnergyLevelListener");
                        }
                    }
                    EnergyLevel energyLevel =
                            new EnergyLevel.Builder().setBatteryPercent(
                                    batteryPercentValue).setFuelPercent(
                                    fuelPercentValue).setEnergyIsLow(
                                    energyIsLowValue).setRangeRemainingMeters(
                                    rangeRemainingValue).setDistanceDisplayUnit(
                                    distanceDisplayUnitValue).build();
                    mEnergyLevelOnCarDataAvailableListener.onCarDataAvailable(energyLevel);
                });
            }
        }
    }
}
