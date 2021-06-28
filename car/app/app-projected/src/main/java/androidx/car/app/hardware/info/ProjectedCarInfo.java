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
    private final CarResultStub<EnergyProfile> mEnergyProfileCarResultStub;
    private final CarResultStub<TollCard> mTollCarResultStub;
    private final CarResultStub<EnergyLevel> mEnergyLevelCarResultStub;
    private final CarResultStub<Speed> mSpeedCarResultStub;
    private final CarResultStub<Mileage> mMileageCarResultStub;

    public ProjectedCarInfo(@NonNull CarHardwareHostDispatcher hostDispatcher) {
        mModelCarResultStub = new CarResultStub<Model>(ICarHardwareResultTypes.TYPE_INFO_MODEL,
                null, /* isSingleShot= */ true, new Model.Builder().build(), hostDispatcher);
        mEnergyProfileCarResultStub =
                new CarResultStub<EnergyProfile>(ICarHardwareResultTypes.TYPE_INFO_ENERGY_PROFILE,
                        null, /* isSingleShot= */ true, new EnergyProfile.Builder().build(),
                        hostDispatcher);
        mTollCarResultStub = new CarResultStub<>(ICarHardwareResultTypes.TYPE_INFO_TOLL, null,
                /* isSingleShot= */ false, new TollCard.Builder().build(), hostDispatcher);
        mEnergyLevelCarResultStub =
                new CarResultStub<>(ICarHardwareResultTypes.TYPE_INFO_ENERGY_LEVEL,
                        null, /* isSingleShot= */ false, new EnergyLevel.Builder().build(),
                        hostDispatcher);
        mSpeedCarResultStub = new CarResultStub<>(ICarHardwareResultTypes.TYPE_INFO_SPEED,
                null, /* isSingleShot= */ false, new Speed.Builder().build(), hostDispatcher);
        mMileageCarResultStub = new CarResultStub<>(ICarHardwareResultTypes.TYPE_INFO_MILEAGE,
                null, /* isSingleShot= */ false, new Mileage.Builder().build(),
                hostDispatcher);
    }

    @Override
    public void fetchModel(@NonNull Executor executor,
            @NonNull OnCarDataListener<Model> listener) {
        mModelCarResultStub.addListener(executor, listener);
    }

    @Override
    public void fetchEnergyProfile(@NonNull Executor executor,
            @NonNull OnCarDataListener<EnergyProfile> listener) {
        mEnergyProfileCarResultStub.addListener(executor, listener);
    }

    @Override
    public void addTollListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<TollCard> listener) {
        mTollCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeTollListener(@NonNull OnCarDataListener<TollCard> listener) {
        mTollCarResultStub.removeListener(listener);
    }

    @Override
    public void addEnergyLevelListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<EnergyLevel> listener) {
        mEnergyLevelCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeEnergyLevelListener(@NonNull OnCarDataListener<EnergyLevel> listener) {
        mEnergyLevelCarResultStub.removeListener(listener);
    }

    @Override
    public void addSpeedListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Speed> listener) {
        mSpeedCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeSpeedListener(@NonNull OnCarDataListener<Speed> listener) {
        mSpeedCarResultStub.removeListener(listener);
    }

    @Override
    public void addMileageListener(@NonNull Executor executor,
            @NonNull OnCarDataListener<Mileage> listener) {
        mMileageCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataListener<Mileage> listener) {
        mMileageCarResultStub.removeListener(listener);
    }

}
