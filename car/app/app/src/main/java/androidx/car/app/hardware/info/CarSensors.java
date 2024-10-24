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
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Manages access to androidx.car.app.hardware specific sensors such as compass, accelerometer,
 * gyroscope and location.
 *
 * <p>For Android Automotive OS, the sensor APIs in this class are currently not implemented, and
 * they will return {@link CarValue#STATUS_UNIMPLEMENTED} by default. To get these values for
 * Android Automotive OS, use
 * <a href="https://developer.android.com/reference/android/hardware/SensorManager">
 * SensorManager</a> and
 * <a href="https://developer.android.com/reference/android/location/LocationManager">
 * LocationManager</a> instead.
 */
//TODO(b/220203294): Implement the sensor apis and remove the related comment above.
@RequiresCarApi(3)
@MainThread
public interface CarSensors {

    /**
     * Defines the possible update rates that properties, sensors, and actions can be requested
     * with.
     *
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
     * Car hardware property, sensor, or action should be fetched at a rate consistent with drawing
     * UI to a screen.
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
     * <p>If the {@code listener} was added previously then previous rate is updated with the new
     * rate. Use {@link #UPDATE_RATE_NORMAL} as a good default {@code rate} in most cases.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addAccelerometerListener(@UpdateRate int rate,
            /* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Accelerometer> listener);

    /**
     * Remove an ongoing listener for {@link Accelerometer} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeAccelerometerListener(@NonNull OnCarDataAvailableListener<Accelerometer> listener);

    /**
     * Setup an ongoing listener to receive {@link Gyroscope} data from the car hardware.
     *
     * <p>If the {@code listener} was added previously then previous rate is updated with the new
     * rate. Use {@link #UPDATE_RATE_NORMAL} as a good default {@code rate} in most cases.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addGyroscopeListener(@UpdateRate int rate,
            /* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Gyroscope> listener);

    /**
     * Remove an ongoing listener for {@link Gyroscope} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeGyroscopeListener(@NonNull OnCarDataAvailableListener<Gyroscope> listener);

    /**
     * Setup an ongoing listener to receive {@link Compass} data from the car hardware.
     *
     * <p>If the {@code listener} was added previously then previous rate is updated with the new
     * rate. Use {@link #UPDATE_RATE_NORMAL} as a good default {@code rate} in most cases.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addCompassListener(@UpdateRate int rate,
            /* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Compass> listener);

    /**
     * Remove an ongoing listener for {@link Compass} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeCompassListener(@NonNull OnCarDataAvailableListener<Compass> listener);

    /**
     * Setup an ongoing listener to receive {@link CarHardwareLocation} data from the car hardware.
     *
     * <p>If the {@code listener} was added previously then previous rate is updated with the new
     * rate. Use {@link #UPDATE_RATE_NORMAL} as a good default {@code rate} in most cases.
     *
     * @param rate     the maximum rate at which the data will be returned through the provided
     *                 listener
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener that will be invoked when data is available
     */
    void addCarHardwareLocationListener(@UpdateRate int rate,
            /* @CallbackExecutor */ @NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<CarHardwareLocation> listener);

    /**
     * Remove an ongoing listener for {@link CarHardwareLocation} information.
     *
     * <p>If the listener is not currently added, then nothing will be removed.
     *
     * @param listener the listener to remove
     */
    void removeCarHardwareLocationListener(
            @NonNull OnCarDataAvailableListener<CarHardwareLocation> listener);
}
