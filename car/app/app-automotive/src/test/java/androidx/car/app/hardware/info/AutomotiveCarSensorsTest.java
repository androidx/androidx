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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.Mockito.verify;

import android.location.Location;

import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AutomotiveCarSensorsTest {

    private AutomotiveCarSensors mAutomotiveCarSensors = new AutomotiveCarSensors();
    private Executor mExecutor = directExecutor();
    @Mock
    private OnCarDataAvailableListener<Accelerometer> mAccelerometerListener;
    @Mock
    private OnCarDataAvailableListener<Compass> mCompassListener;
    @Mock
    private OnCarDataAvailableListener<Gyroscope> mGyroscopeListener;
    @Mock
    private OnCarDataAvailableListener<CarHardwareLocation> mCarHardwareLocationListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getAccelerometer_unsupported() {
        ArgumentCaptor<Accelerometer> captor = ArgumentCaptor.forClass(Accelerometer.class);
        mAutomotiveCarSensors.addAccelerometerListener(CarSensors.UPDATE_RATE_NORMAL, mExecutor,
                mAccelerometerListener);
        verify(mAccelerometerListener).onCarDataAvailable(captor.capture());
        CarValue<List<Float>> forces = captor.getValue().getForces();
        assertThat(forces.getStatus()).isEqualTo(CarValue.STATUS_UNIMPLEMENTED);
        assertThat(forces.getValue()).isNull();
    }

    @Test
    public void getCompass_unsupported() {
        ArgumentCaptor<Compass> captor = ArgumentCaptor.forClass(Compass.class);
        mAutomotiveCarSensors.addCompassListener(CarSensors.UPDATE_RATE_NORMAL, mExecutor,
                mCompassListener);
        verify(mCompassListener).onCarDataAvailable(captor.capture());
        CarValue<List<Float>> orientations = captor.getValue().getOrientations();
        assertThat(orientations.getStatus()).isEqualTo(CarValue.STATUS_UNIMPLEMENTED);
        assertThat(orientations.getValue()).isNull();
    }

    @Test
    public void getGyroscope_unsupported() {
        ArgumentCaptor<Gyroscope> captor = ArgumentCaptor.forClass(Gyroscope.class);
        mAutomotiveCarSensors.addGyroscopeListener(CarSensors.UPDATE_RATE_NORMAL, mExecutor,
                mGyroscopeListener);
        verify(mGyroscopeListener).onCarDataAvailable(captor.capture());
        CarValue<List<Float>> rotations = captor.getValue().getRotations();
        assertThat(rotations.getStatus()).isEqualTo(CarValue.STATUS_UNIMPLEMENTED);
        assertThat(rotations.getValue()).isNull();
    }

    @Test
    public void getCarHardwareLocation_unsupported() {
        ArgumentCaptor<CarHardwareLocation> captor = ArgumentCaptor.forClass(
                CarHardwareLocation.class);
        mAutomotiveCarSensors.addCarHardwareLocationListener(CarSensors.UPDATE_RATE_NORMAL,
                mExecutor, mCarHardwareLocationListener);
        verify(mCarHardwareLocationListener).onCarDataAvailable(captor.capture());
        CarValue<Location> location = captor.getValue().getLocation();
        assertThat(location.getStatus()).isEqualTo(CarValue.STATUS_UNIMPLEMENTED);
        assertThat(location.getValue()).isNull();
    }
}
