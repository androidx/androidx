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
import static androidx.car.app.activity.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.GetPropertyRequest;
import androidx.car.app.hardware.common.OnCarDataListener;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final float UNKNOWN_CAPACITY = Float.NEGATIVE_INFINITY;
    private static final List<Integer> MILEAGE_REQUEST =
            Arrays.asList(PERF_ODOMETER, DISTANCE_DISPLAY_UNITS);
    private final Map<OnCarDataListener<?>, OnCarPropertyResponseListener> mListenerMap =
            new HashMap<>();
    private PropertyManager mPropertyManager;

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
            @NonNull OnCarDataListener<Model> listener) {
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
            @NonNull OnCarDataListener<EnergyProfile> listener) {
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
            @NonNull OnCarDataListener<TollCard> listener) {

    }

    @Override
    public void removeTollListener(@NonNull OnCarDataListener<TollCard> listener) {
    }

    @Override
    public void addEnergyLevelListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<EnergyLevel> listener) {
        getCapacitiesThenEnergyLevel(executor, listener);
    }

    @Override
    public void removeEnergyLevelListener(@NonNull OnCarDataListener<EnergyLevel> listener) {
        removeListenerImpl(listener);
    }

    @Override
    public void addSpeedListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Speed> listener) {
    }

    @Override
    public void removeSpeedListener(@NonNull OnCarDataListener<Speed> listener) {
    }

    @Override
    public void addMileageListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Mileage> listener) {
        MileageListener mileageListener = new MileageListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(MILEAGE_REQUEST, DEFAULT_SAMPLE_RATE,
                mileageListener, executor);
        mListenerMap.put(listener, mileageListener);
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataListener<Mileage> listener) {
        removeListenerImpl(listener);
    }

    <T> CarValue<T> getCarValue(CarPropertyResponse<?> response, @Nullable T value) {
        long timestampMillis = response.getTimestampMillis();
        int status = response.getStatus();
        return new CarValue<>(value, timestampMillis, status);
    }

    @VisibleForTesting
    void getCapacitiesThenEnergyLevel(@NonNull Executor executor,
            @NonNull OnCarDataListener<EnergyLevel> listener) {
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
                        Log.w(TAG, "Failed to retrieve CarPropertyResponse value for property id "
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
                // TODO(b/191084385): Match exception style in {@link CarValue}.
                Log.e(TAG, "Failed to get CarPropertyResponse for Energy Level", e);
            } catch (InterruptedException e) {
                // TODO(b/191084385): Match exception style in {@link CarValue}.
                Log.e(TAG, "Failed to get CarPropertyResponse for Energy Level", e);
                Thread.currentThread().interrupt();
            }
        }, executor);
    }

    private void populateModelData(@NonNull Executor executor, OnCarDataListener<Model> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            try {
                List<CarPropertyResponse<?>> result = future.get();
                CarValue<String> makeValue = CarValue.UNIMPLEMENTED_STRING;
                CarValue<Integer> yearValue = CarValue.UNIMPLEMENTED_INTEGER;
                CarValue<String> modelValue = CarValue.UNIMPLEMENTED_STRING;
                for (CarPropertyResponse<?> value : result) {
                    if (value.getValue() == null) {
                        Log.w(TAG, "Failed to retrieve CarPropertyResponse value for property id "
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
                listener.onCarData(model);
            } catch (ExecutionException e) {
                // TODO(b/191084385): Match exception style in {@link CarValue}.
                Log.e(TAG, "Failed to get CarPropertyResponse for Model", e);
            } catch (InterruptedException e) {
                // TODO(b/191084385): Match exception style in {@link CarValue}.
                Log.e(TAG, "Failed to get CarPropertyResponse for Model", e);
                Thread.currentThread().interrupt();
            }
        }, executor);
    }

    private void populateEnergyProfileData(@NonNull Executor executor,
            OnCarDataListener<EnergyProfile> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            try {
                List<CarPropertyResponse<?>> result = future.get();
                CarValue<List<Integer>> evConnector = CarValue.UNIMPLEMENTED_INTEGER_LIST;
                CarValue<List<Integer>> fuel = CarValue.UNIMPLEMENTED_INTEGER_LIST;
                for (CarPropertyResponse<?> value : result) {
                    if (value.getValue() == null) {
                        Log.w(TAG, "Failed to retrieve CarPropertyResponse value for property id "
                                + value.getPropertyId());
                        continue;
                    }
                    if (value.getPropertyId() == INFO_EV_CONNECTOR_TYPE) {
                        evConnector = getCarValue(value, Arrays.stream((int[]) requireNonNull(
                                value.getValue()))
                                .boxed().collect(Collectors.toList()));
                    }
                    if (value.getPropertyId() == INFO_FUEL_TYPE) {
                        fuel = getCarValue(value, Arrays.stream((int[]) requireNonNull(
                                value.getValue()))
                                .boxed().collect(Collectors.toList()));
                    }
                }
                EnergyProfile energyProfile = new EnergyProfile.Builder().setEvConnectorTypes(
                        evConnector)
                        .setFuelTypes(fuel)
                        .build();
                listener.onCarData(energyProfile);
            } catch (ExecutionException e) {
                // TODO(b/191084385): Match exception style in {@link CarValue}.
                Log.e(TAG, "Failed to get CarPropertyResponse for Energy Profile", e);
            } catch (InterruptedException e) {
                // TODO(b/191084385): Match exception style in {@link CarValue}.
                Log.e(TAG, "Failed to get CarPropertyResponse for Energy Profile", e);
                Thread.currentThread().interrupt();
            }
        }, executor);
    }

    private void removeListenerImpl(OnCarDataListener<?> listener) {
        OnCarPropertyResponseListener responseListener = mListenerMap.remove(listener);
        if (responseListener != null) {
            mPropertyManager.submitUnregisterListenerRequest(responseListener);
        } else {
            throw new IllegalArgumentException("Listener is not registered yet");
        }
    }

    /**
     * Mileage listener to get distance display unit and odometer updates by
     * {@link CarPropertyResponse}.
     */
    @VisibleForTesting
    static class MileageListener implements OnCarPropertyResponseListener {
        private final OnCarDataListener<Mileage> mMileageOnCarDataListener;
        private final Executor mExecutor;

        MileageListener(OnCarDataListener<Mileage> listener, Executor executor) {
            mMileageOnCarDataListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            if (carPropertyResponses.size() == 2) {
                mExecutor.execute(() -> {
                    CarValue<Float> odometerValue = CarValue.UNIMPLEMENTED_FLOAT;
                    CarValue<Integer> distanceDisplayUnitValue =
                            CarValue.UNIMPLEMENTED_INTEGER;
                    for (CarPropertyResponse<?> response : carPropertyResponses) {
                        if (response.getValue() == null) {
                            Log.w(TAG,
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
                                distanceDisplayUnitValue = new CarValue<>(
                                        (Integer) response.getValue(),
                                        response.getTimestampMillis(), response.getStatus());
                                break;
                            default:
                                Log.e(TAG, "Invalid response callback in MileageListener");
                        }
                    }
                    Mileage mileage =
                            new Mileage.Builder().setOdometerMeters(
                                    odometerValue).setDistanceDisplayUnit(
                                    distanceDisplayUnitValue).build();
                    mMileageOnCarDataListener.onCarData(mileage);
                });
            }
        }
    }

    /**
     * EnergyLevel listener to get battery, energy updates by {@link CarPropertyResponse}.
     */
    @VisibleForTesting
    static class EnergyLevelListener implements OnCarPropertyResponseListener {
        private final OnCarDataListener<EnergyLevel> mEnergyLevelOnCarDataListener;
        private final Executor mExecutor;
        private float mEvBatteryCapacity;
        private float mFuelCapacity;

        EnergyLevelListener(OnCarDataListener<EnergyLevel> listener, Executor executor,
                float evBatteryCapacity, float fuelCapacity) {
            mEnergyLevelOnCarDataListener = listener;
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
                            Log.w(TAG,
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
                                Log.e(TAG, "Invalid response callback in EnergyLevelListener");
                        }
                    }
                    EnergyLevel energyLevel =
                            new EnergyLevel.Builder().setBatteryPercent(
                                    batteryPercentValue).setFuelPercent(
                                    fuelPercentValue).setEnergyIsLow(
                                    energyIsLowValue).setRangeRemainingMeters(
                                    rangeRemainingValue).setDistanceDisplayUnit(
                                    distanceDisplayUnitValue).build();
                    mEnergyLevelOnCarDataListener.onCarData(energyLevel);
                });
            }
        }
    }
}
