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

import androidx.annotation.MainThread;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Manages access to car hardware specific info such as model, energy, and speed info.
 */
@RequiresCarApi(3)
@MainThread
public interface CarInfo {
    /**
     * Fetch the {@link Model} information about the car hardware.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void fetchModel(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Model> listener);

    /**
     * Reguest the {@link EnergyProfile} information about the car hardware.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void fetchEnergyProfile(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyProfile> listener);

    /**
     * Request the {@link ExteriorDimensions} information about the vehicle. This will only return
     * meaningful data on select Automotive vehicles running SDK 30 and above.
     *
     * <p>This requires the {@code android.car.permission.CAR_INFO} permission.
     *
     * <p>See
     * <a href="https://developer.android.com/reference/android/car/VehiclePropertyIds#INFO_EXTERIOR_DIMENSIONS">VehiclePropertyIds#INFO_EXTERIOR_DIMENSIONS</a>
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    @RequiresCarApi(7)
    default void fetchExteriorDimensions(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<ExteriorDimensions> listener) {
    }

    /**
     * Setup an ongoing listener to receive {@link TollCard} information from the car hardware.
     *
     * <p>If the listener was added previously then it won't be added again.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addTollListener(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<TollCard> listener);

    /**
     * Remove an ongoing listener for {@link TollCard} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeTollListener(@NonNull OnCarDataAvailableListener<TollCard> listener);

    /**
     * Setup an ongoing listener to receive {@link EnergyLevel} information from the car hardware.
     *
     * <p>If the listener was added previously then it won't be added.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addEnergyLevelListener(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener);

    /**
     * Remove an ongoing listener for {@link EnergyLevel} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeEnergyLevelListener(@NonNull OnCarDataAvailableListener<EnergyLevel> listener);

    /**
     * Setup an ongoing listener to receive {@link Speed} information from the car hardware.
     *
     * <p>If the listener was added previously then it won't be added.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addSpeedListener(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Speed> listener);

    /**
     * Remove an ongoing listener for {@link Speed} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeSpeedListener(@NonNull OnCarDataAvailableListener<Speed> listener);

    /**
     * Setup an ongoing listener to receive {@link Mileage} information from the car hardware.
     *
     * <p>If the listener was added previously then it won't be added.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addMileageListener(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Mileage> listener);

    /**
     * Remove an ongoing listener for {@link Mileage} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeMileageListener(@NonNull OnCarDataAvailableListener<Mileage> listener);

    /**
     * Setup an ongoing listener to receive {@link EvStatus} information from the car hardware.
     *
     * <p>If the listener was added previously then it won't be added.
     *
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    @ExperimentalCarApi
    void addEvStatusListener(/* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EvStatus> listener);

    /**
     * Remove an ongoing listener for {@link EvStatus} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    @ExperimentalCarApi
    void removeEvStatusListener(@NonNull OnCarDataAvailableListener<EvStatus> listener);
}
