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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Manages access to androidx.car.app.hardware specific sensors such as compass, accelerometer,
 * and gyroscope.
 */
@RequiresCarApi(3)
@MainThread
public interface CarSensors {

    /**
     * Defines the possible update rates that properties, sensors, and actions can be requested
     * with.
     *
     * @hide
     */
    @IntDef({
            UPDATE_RATE_NORMAL,
            UPDATE_RATE_UI,
            UPDATE_RATE_FASTEST,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    @interface UpdateRate {
    }

    /**
     * Car hardware info, sensor, or action should be fetched at its default rate.
     */
    @UpdateRate
    int UPDATE_RATE_NORMAL = 1;

    /**
     * Car hardware property, sensor, or action should be fetched at a rate consistent with
     * drawing UI to a screen.
     */
    @UpdateRate
    int UPDATE_RATE_UI = 2;

    /**
     * Car hardware property, sensor, or action should be fetched at its fastest possible rate.
     */
    @UpdateRate
    int UPDATE_RATE_FASTEST = 3;

    /**
     * Setup an ongoing listener to receive {@link Accelerometer} data from the car hardware.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener. You may use {@link #UPDATE_RATE_NORMAL} as a good default in most
     *                 cases
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available. If the listener
     *                 was added previously then previous rate is updated with the new rate.
     */
    void addAccelerometerListener(@UpdateRate int rate,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull OnCarDataAvailableListener<Accelerometer> listener);

    /**
     * Remove an ongoing listener for {@link Accelerometer} information.
     *
     * @param listener the listener to remove. If the listener is not currently added, then nothing
     *                 will be removed.
     */
    void removeAccelerometerListener(@NonNull OnCarDataAvailableListener<Accelerometer> listener);

    /**
     * Setup an ongoing listener to receive {@link Gyroscope} data from the car hardware.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener. You may use {@link #UPDATE_RATE_NORMAL} as a good default in most
     *                 cases
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available. If the listener
     *                 was added previously then previous rate is updated with the new rate.
     */
    void addGyroscopeListener(@UpdateRate int rate,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull OnCarDataAvailableListener<Gyroscope> listener);

    /**
     * Remove an ongoing listener for {@link Gyroscope} information.
     *
     * @param listener the listener to remove. If the listener is not currently added, then nothing
     *                 will be removed.
     */
    void removeGyroscopeListener(@NonNull OnCarDataAvailableListener<Gyroscope> listener);

    /**
     * Setup an ongoing listener to receive {@link Compass} data from the car hardware.
     *
     * <p>If the listener was added previously then previous rate is updated with the new rate.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener. You may use {@link #UPDATE_RATE_NORMAL} as a good default in most
     *                 cases
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available. If the listener
     *                 was added previously then previous rate is updated with the new rate.
     */
    void addCompassListener(@UpdateRate int rate,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull OnCarDataAvailableListener<Compass> listener);

    /**
     * Remove an ongoing listener for {@link Compass} information.
     *
     * @param listener the listener to remove. If the listener is not currently added, then nothing
     *                 will be removed.
     */
    void removeCompassListener(@NonNull OnCarDataAvailableListener<Compass> listener);

    /**
     * Setup an ongoing listener to receive {@link CarHardwareLocation} data from the car hardware.
     *
     * <p>If the listener was added previously then previous rate is updated with the new rate.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener. You may use {@link #UPDATE_RATE_NORMAL} as a good default in most
     *                 cases
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available. If the listener
     *                 was added previously then previous rate is updated with the new rate.
     */
    void addCarHardwareLocationListener(@UpdateRate int rate,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull OnCarDataAvailableListener<CarHardwareLocation> listener);

    /**
     * Remove an ongoing listener for {@link CarHardwareLocation} information.
     *
     * @param listener the listener to remove. If the listener is not currently added, then nothing
     *                 will be removed.
     */
    void removeCarHardwareLocationListener(
            @NonNull OnCarDataAvailableListener<CarHardwareLocation> listener);
}
