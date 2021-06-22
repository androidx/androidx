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

import static android.car.VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE;
import static android.car.VehiclePropertyIds.INFO_FUEL_TYPE;
import static android.car.VehiclePropertyIds.INFO_MAKE;
import static android.car.VehiclePropertyIds.INFO_MODEL;
import static android.car.VehiclePropertyIds.INFO_MODEL_YEAR;

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
import androidx.car.app.hardware.common.PropertyManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public void getModel(@NonNull Executor executor,
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
    public void getEnergyProfile(@NonNull Executor executor,
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
    }

    @Override
    public void removeEnergyLevelListener(@NonNull OnCarDataListener<EnergyLevel> listener) {
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
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataListener<Mileage> listener) {
    }

    <T> CarValue<T> getCarValue(CarPropertyResponse<?> response, @Nullable T value) {
        long timeStamp = response.getTimestampMillis();
        int status = response.getStatus();
        return new CarValue<>(value, timeStamp, status);
    }

    @VisibleForTesting
    void populateModelData(@NonNull Executor executor, OnCarDataListener<Model> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            try {
                List<CarPropertyResponse<?>> result = future.get();
                CarValue<String> makeValue = CarValue.UNIMPLEMENTED_STRING;
                CarValue<Integer> yearValue = CarValue.UNIMPLEMENTED_INTEGER;
                CarValue<String> modelValue = CarValue.UNIMPLEMENTED_STRING;
                for (CarPropertyResponse<?> value : result) {
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

    @VisibleForTesting
    void populateEnergyProfileData(@NonNull Executor executor,
            OnCarDataListener<EnergyProfile> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            try {
                List<CarPropertyResponse<?>> result = future.get();
                CarValue<List<Integer>> evConnector = CarValue.UNIMPLEMENTED_INTEGER_LIST;
                CarValue<List<Integer>> fuel = CarValue.UNIMPLEMENTED_INTEGER_LIST;
                for (CarPropertyResponse<?> value : result) {
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
}
