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

package androidx.car.app.hardware.climate;

import androidx.annotation.MainThread;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarSetOperationStatusCallback;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Manages access to car climate system such as cabin temperatures, fan speeds and fan directions.
 */
@RequiresCarApi(5)
@MainThread
@ExperimentalCarApi
public interface CarClimate {
    /**
     * Registers an ongoing listener to receive car climate state events from the car hardware
     * such as when the driver side temperature is increased/decreased.
     *
     * <p> If the callback was registered previously then it won't be registered again.
     *
     * @param executor  the executor which will be used for invoking the listener
     * @param request   the RegisterClimateStateRequest indicates climate features
     *                  which the caller is interested in
     * @param callback  the CarClimateStateCallback that will be invoked when the
     *                  car climate is changed
     */
    void registerClimateStateCallback(@NonNull Executor executor,
            @NonNull RegisterClimateStateRequest request,
            @NonNull CarClimateStateCallback callback);

    /**
     * Unregisters the ongoing listener for receiving car climate state events.
     *
     * <p>If the callback is not currently registered, then this method call has no impact.
     *
     * @param callback the callback to unregister
     */
    void unregisterClimateStateCallback(@NonNull CarClimateStateCallback callback);

    /**
     * Fetches the climate profile information of the associated features specified in the
     * request, for example, HVAC features like Fan Speed, Cabin Temperature, etc.
     *
     * @param executor  the executor which will be used for invoking the listener
     * @param request   the ClimateProfileRequest indicates profile features which the
     *                  caller is interested in
     * @param callback  the CarClimateProfileCallback that will be
     *                  invoked when the data is available
     */
    void fetchClimateProfile(@NonNull Executor executor,
            @NonNull ClimateProfileRequest request,
            @NonNull CarClimateProfileCallback callback);

    /**
     * Sets values for the car climate system.
     *
     * @param executor  the executor which will be used for invoking the listener
     * @param request   the ClimateStateRequest indicates climate features which the
     *                  caller is setting value for
     * @param callback  the CarSetOperationStatusCallback that will be invoked when the
     *                  set operation succeeded or failed
     * @param <E>       the data type for ClimateStateRequest
     */
    <E> void setClimateState(@NonNull Executor executor,
            @NonNull ClimateStateRequest<E> request,
            @NonNull CarSetOperationStatusCallback callback);
}
