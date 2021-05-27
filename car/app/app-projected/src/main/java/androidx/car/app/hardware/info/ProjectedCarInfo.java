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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.hardware.ICarHardwareResultTypes;
import androidx.car.app.hardware.common.CarHardwareHostDispatcher;
import androidx.car.app.hardware.common.CarResultStub;
import androidx.car.app.hardware.common.OnCarDataListener;

import java.util.concurrent.Executor;

/**
 * Manages access to vehicle specific info communication with a car app host.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ProjectedCarInfo implements CarInfo {

    private final CarResultStub<Model> mModelCarResultStub;

    public ProjectedCarInfo(@NonNull CarHardwareHostDispatcher hostDispatcher) {
        mModelCarResultStub = new CarResultStub<Model>(ICarHardwareResultTypes.TYPE_INFO_MODEL,
                null, /* isSingleShot= */ true, new Model.Builder().build(), hostDispatcher);
    }

    @Override
    public void getModel(@NonNull Executor executor,
            @NonNull OnCarDataListener<Model> listener) {
        mModelCarResultStub.addListener(executor, listener);
    }

    @Override
    public void getEnergyProfile(@NonNull Executor executor,
            @NonNull OnCarDataListener<EnergyProfile> listener) {
        // TODO(b/188144401): Implement calls to host
    }

    @Override
    public void addTollListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Toll> listener) {
        // TODO(b/188143193): Implement calls to host
    }

    @Override
    public void removeTollListener(@NonNull OnCarDataListener<Toll> listener) {
        // TODO(b/188143193): Implement calls to host
    }

    @Override
    public void addEnergyLevelListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<EnergyLevel> listener) {
        // TODO(b/188144402): Implement calls to host
    }

    @Override
    public void removeEnergyLevelListener(@NonNull OnCarDataListener<EnergyLevel> listener) {
        // TODO(b/188144402): Implement calls to host
    }

    @Override
    public void addSpeedListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Speed> listener) {
        // TODO(b/188143193): Implement calls to host
    }

    @Override
    public void removeSpeedListener(@NonNull OnCarDataListener<Speed> listener) {
        // TODO(b/188143193): Implement calls to host
    }

    @Override
    public void addMileageListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Mileage> listener) {
        // TODO(b/188143193): Implement calls to host
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataListener<Mileage> listener) {
        // TODO(b/188143193): Implement calls to host
    }

}
