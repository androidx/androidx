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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarSetOperationStatusCallback;

import java.util.concurrent.Executor;

/**
 * Manages access to car climate system such as cabin temperatures, fan speeds and fan directions.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
@ExperimentalCarApi
public class AutomotiveCarClimate implements CarClimate {

    public AutomotiveCarClimate() {
    }

    @Override
    public void registerClimateStateCallback(@NonNull Executor executor,
            @NonNull RegisterClimateStateRequest request,
            @NonNull CarClimateStateCallback callback) {

    }

    @Override
    public void unregisterClimateStateCallback(@NonNull CarClimateStateCallback callback) {

    }

    @Override
    public void fetchClimateProfile(@NonNull Executor executor,
            @NonNull ClimateProfileRequest request,
            @NonNull CarClimateProfileCallback callback) {

    }

    @Override
    public <E> void setClimateState(@NonNull Executor executor,
            @NonNull ClimateStateRequest<E> request,
            @NonNull CarSetOperationStatusCallback callback) {

    }
}
