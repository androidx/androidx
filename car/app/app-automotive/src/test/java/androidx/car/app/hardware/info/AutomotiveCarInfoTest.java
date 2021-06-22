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

import static android.car.VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE;
import static android.car.VehiclePropertyIds.INFO_FUEL_TYPE;

import static androidx.car.app.hardware.common.CarValue.STATUS_SUCCESS;
import static androidx.car.app.hardware.info.EnergyProfile.EVCONNECTOR_TYPE_CHADEMO;
import static androidx.car.app.hardware.info.EnergyProfile.FUEL_TYPE_UNLEADED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.property.CarPropertyManager;

import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.OnCarDataListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.shadows.car.ShadowCar;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = Config.NONE,
        shadows = {ShadowCar.class}
)
@DoNotInstrument
public class AutomotiveCarInfoTest {
    @Mock
    ListenableFuture<List<CarPropertyResponse<?>>> mListenableCarPropertyResponse;
    private List<CarPropertyResponse<?>> mResponse = new ArrayList<>();
    private CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private AtomicReference<Model> mLoadedResult = new AtomicReference<>();
    private Executor mExecutor = Executors.newSingleThreadExecutor();
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
    }

    @After
    public void tearDown() {
        mResponse = new ArrayList<>();
    }

    @Test
    public void getModel_verifyResponse() throws InterruptedException {
        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.create(VehiclePropertyIds.INFO_MAKE,
                STATUS_SUCCESS, 1, "Speedy "
                        + "Model"));
        mResponse.add(CarPropertyResponse.create(VehiclePropertyIds.INFO_MODEL,
                STATUS_SUCCESS, 2, "Toy "
                        + "Vehicle"));
        mResponse.add(CarPropertyResponse.create(VehiclePropertyIds.INFO_MODEL_YEAR,
                STATUS_SUCCESS, 3, 2020));
        mListenableCarPropertyResponse = Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(any(), any())).thenReturn(
                mListenableCarPropertyResponse);
        OnCarDataListener<Model> listener = (data) -> {
            mLoadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.getModel(Executors.newFixedThreadPool(1),
                listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(any(), any());
        mCountDownLatch.await();
        Model mModel = mLoadedResult.get();
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
        mListenableCarPropertyResponse = Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(any(), any())).thenReturn(
                mListenableCarPropertyResponse);
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OnCarDataListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            countDownLatch.countDown();
        };
        mAutomotiveCarInfo.getEnergyProfile(Executors.newFixedThreadPool(1),
                listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(any(), any());
        countDownLatch.await();
        EnergyProfile energyProfile = loadedResult.get();
        List<Integer> evConnector = new ArrayList<Integer>();
        evConnector.add(EVCONNECTOR_TYPE_CHADEMO);
        List<Integer> fuel = new ArrayList<Integer>();
        fuel.add(FUEL_TYPE_UNLEADED);
        assertThat(energyProfile.getEvConnectorTypes().getValue()).isEqualTo(
                evConnector);
        assertThat(energyProfile.getFuelTypes().getValue()).isEqualTo(fuel);
    }
}
