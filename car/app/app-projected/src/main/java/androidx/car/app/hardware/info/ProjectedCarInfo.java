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
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.ICarHardwareResultTypes;
import androidx.car.app.hardware.common.CarHardwareHostDispatcher;
import androidx.car.app.hardware.common.CarResultStub;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;

import java.util.concurrent.Executor;

/**
 * Manages access to vehicle specific info communication with a car app host.
 *
 */
@RestrictTo(LIBRARY)
public class ProjectedCarInfo implements CarInfo {

    private final CarResultStub<Model> mModelCarResultStub;
    private final CarResultStub<EnergyProfile> mEnergyProfileCarResultStub;
    private final CarResultStub<TollCard> mTollCarResultStub;
    private final CarResultStub<EnergyLevel> mEnergyLevelCarResultStub;
    private final CarResultStub<Speed> mSpeedCarResultStub;
    private final CarResultStub<Mileage> mMileageCarResultStub;
    private final CarResultStub<EvStatus> mEvStatusCarResultStub;

    // TODO(b/216177515): Remove this annotation once EvStatus is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
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
        mEvStatusCarResultStub = new CarResultStub<>(ICarHardwareResultTypes.TYPE_INFO_EV_STATUS,
                null, false, new EvStatus.Builder().build(), hostDispatcher);
    }

    @Override
    public void fetchModel(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Model> listener) {
        mModelCarResultStub.addListener(executor, listener);
    }

    @Override
    public void fetchEnergyProfile(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyProfile> listener) {
        mEnergyProfileCarResultStub.addListener(executor, listener);
    }

    // Exterior dimensions are not available in AAP without an update to the GAL protocol. As such
    // this method returns a default ExteriorDimensions that returns UNKNOWN (effectively null).
    @Override
    public void fetchExteriorDimensions(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<ExteriorDimensions> listener) {
        // TODO - b/325540913 Implement fetching exterior dimensions in AAP, including updating GAL
        executor.execute(() -> listener.onCarDataAvailable(new ExteriorDimensions()));
    }

    @Override
    public void addTollListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<TollCard> listener) {
        mTollCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeTollListener(@NonNull OnCarDataAvailableListener<TollCard> listener) {
        mTollCarResultStub.removeListener(listener);
    }

    @Override
    public void addEnergyLevelListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        mEnergyLevelCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeEnergyLevelListener(
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        mEnergyLevelCarResultStub.removeListener(listener);
    }

    @Override
    public void addSpeedListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Speed> listener) {
        mSpeedCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeSpeedListener(@NonNull OnCarDataAvailableListener<Speed> listener) {
        mSpeedCarResultStub.removeListener(listener);
    }

    @Override
    public void addMileageListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Mileage> listener) {
        mMileageCarResultStub.addListener(executor, listener);
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataAvailableListener<Mileage> listener) {
        mMileageCarResultStub.removeListener(listener);
    }

    // TODO(b/216177515): Remove this annotation once EvStatus is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
    @Override
    public void addEvStatusListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EvStatus> listener) {
        mEvStatusCarResultStub.addListener(executor, listener);
    }

    // TODO(b/216177515): Remove this annotation once EvStatus is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
    @Override
    public void removeEvStatusListener(@NonNull OnCarDataAvailableListener<EvStatus> listener) {
        mEvStatusCarResultStub.removeListener(listener);
    }

}
