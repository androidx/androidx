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

import static android.car.VehiclePropertyIds.DISTANCE_DISPLAY_UNITS;
import static android.car.VehiclePropertyIds.EV_BATTERY_LEVEL;
import static android.car.VehiclePropertyIds.FUEL_LEVEL;
import static android.car.VehiclePropertyIds.FUEL_LEVEL_LOW;
import static android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE;
import static android.car.VehiclePropertyIds.INFO_FUEL_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_FUEL_TYPE;
import static android.car.VehiclePropertyIds.INFO_MAKE;
import static android.car.VehiclePropertyIds.INFO_MODEL;
import static android.car.VehiclePropertyIds.INFO_MODEL_YEAR;
import static android.car.VehiclePropertyIds.PERF_ODOMETER;
import static android.car.VehiclePropertyIds.RANGE_REMAINING;

import static androidx.car.app.hardware.common.CarValue.STATUS_SUCCESS;
import static androidx.car.app.hardware.info.AutomotiveCarInfo.DEFAULT_SAMPLE_RATE;
import static androidx.car.app.hardware.info.EnergyProfile.EVCONNECTOR_TYPE_CHADEMO;
import static androidx.car.app.hardware.info.EnergyProfile.FUEL_TYPE_UNLEADED;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.hardware.property.CarPropertyManager;

import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.OnCarDataListener;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.shadows.car.ShadowCar;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = Config.NONE,
        shadows = {ShadowCar.class}
)
@DoNotInstrument
public class AutomotiveCarInfoTest {
    private List<CarPropertyResponse<?>> mResponse;
    private CountDownLatch mCountDownLatch;
    private Executor mExecutor = directExecutor();
    private AutomotiveCarInfo mAutomotiveCarInfo;
    @Mock
    private Car mCarMock;
    @Mock
    private CarPropertyManager mCarPropertyManagerMock;
    @Mock
    private PropertyManager mPropertyManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCar.setCar(mCarMock);
        when(mCarMock.getCarManager(anyString())).thenReturn(mCarPropertyManagerMock);
        mAutomotiveCarInfo = new AutomotiveCarInfo(mPropertyManager);
        mCountDownLatch = new CountDownLatch(1);
        mResponse = new ArrayList<>();
    }

    @Test
    public void getModel_verifyResponse() throws InterruptedException {
        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.create(INFO_MAKE,
                STATUS_SUCCESS, 1, "Speedy "
                        + "Model"));
        mResponse.add(CarPropertyResponse.create(INFO_MODEL,
                STATUS_SUCCESS, 2, "Toy "
                        + "Vehicle"));
        mResponse.add(CarPropertyResponse.create(INFO_MODEL_YEAR,
                STATUS_SUCCESS, 3, 2020));
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(any(), any())).thenReturn(
                listenableCarPropertyResponse);
        AtomicReference<Model> loadedResult = new AtomicReference<>();
        OnCarDataListener<Model> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchModel(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(any(), any());
        mCountDownLatch.await();
        Model mModel = loadedResult.get();
        assertThat(mModel.getName().getValue()).isEqualTo("Speedy Model");
        assertThat(mModel.getManufacturer().getValue()).isEqualTo("Toy Vehicle");
        assertThat(mModel.getYear().getValue()).isEqualTo(2020);
        assertThat(mModel.getName().getTimestampMillis()).isEqualTo(1);
        assertThat(mModel.getManufacturer().getTimestampMillis()).isEqualTo(2);
        assertThat(mModel.getYear().getTimestampMillis()).isEqualTo(3);
    }

    @Test
    public void getEnergyProfile_verifyResponse() throws InterruptedException {
        // Add "evConnector" and "fuel" type of the vehicle to the requests.
        mResponse.add(CarPropertyResponse.create(INFO_EV_CONNECTOR_TYPE,
                STATUS_SUCCESS, 1, new int[]{EVCONNECTOR_TYPE_CHADEMO}));
        mResponse.add(CarPropertyResponse.create(INFO_FUEL_TYPE,
                STATUS_SUCCESS, 2, new int[]{FUEL_TYPE_UNLEADED}));
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(any(), any())).thenReturn(
                listenableCarPropertyResponse);
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        OnCarDataListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchEnergyProfile(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(any(), any());
        mCountDownLatch.await();
        EnergyProfile energyProfile = loadedResult.get();
        List<Integer> evConnector = new ArrayList<Integer>();
        evConnector.add(EVCONNECTOR_TYPE_CHADEMO);
        List<Integer> fuel = new ArrayList<Integer>();
        fuel.add(FUEL_TYPE_UNLEADED);
        assertThat(energyProfile.getEvConnectorTypes().getValue()).isEqualTo(
                evConnector);
        assertThat(energyProfile.getFuelTypes().getValue()).isEqualTo(fuel);
    }

    @Test
    public void getMileage_verifyResponse() throws InterruptedException {
        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(any(), eq(DEFAULT_SAMPLE_RATE),
                captor.capture(), any());

        mResponse.add(CarPropertyResponse.create(PERF_ODOMETER, STATUS_SUCCESS, 1, 1f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS, STATUS_SUCCESS, 2, 2));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        Mileage mileage = loadedResult.get();
        assertThat(mileage.getOdometerMeters().getValue()).isEqualTo(1f);
        assertThat(mileage.getDistanceDisplayUnit().getValue()).isEqualTo(2);
    }

    @Test
    public void getEnergyLevel_verifyResponse() throws InterruptedException {
        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);

        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.create(INFO_EV_BATTERY_CAPACITY,
                STATUS_SUCCESS, 1, 2f));
        capacities.add(CarPropertyResponse.create(INFO_FUEL_CAPACITY,
                STATUS_SUCCESS, 1, 3f));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(any(), any())).thenReturn(future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(any(), any());
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(any(),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), any());

        mResponse.add(CarPropertyResponse.create(EV_BATTERY_LEVEL,
                STATUS_SUCCESS, 1, 4f));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL,
                STATUS_SUCCESS, 1, 6f));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL_LOW,
                STATUS_SUCCESS, 1, true));
        mResponse.add(CarPropertyResponse.create(RANGE_REMAINING,
                STATUS_SUCCESS, 1, 5f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS,
                STATUS_SUCCESS, 1, 7));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EnergyLevel energyLevel = loadedResult.get();
        assertThat(energyLevel.getBatteryPercent().getValue()).isEqualTo(
                2f);
        assertThat(energyLevel.getFuelPercent().getValue()).isEqualTo(
                2f);
        assertThat(energyLevel.getEnergyIsLow().getValue()).isEqualTo(
                true);
        assertThat(energyLevel.getRangeRemainingMeters().getValue()).isEqualTo(
                5f);
        assertThat(energyLevel.getDistanceDisplayUnit().getValue()).isEqualTo(7);
    }
}
