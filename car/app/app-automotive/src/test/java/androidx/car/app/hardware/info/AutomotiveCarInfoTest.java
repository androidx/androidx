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
import static android.car.VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED;
import static android.car.VehiclePropertyIds.EV_CHARGE_PORT_OPEN;
import static android.car.VehiclePropertyIds.FUEL_LEVEL;
import static android.car.VehiclePropertyIds.FUEL_LEVEL_LOW;
import static android.car.VehiclePropertyIds.FUEL_VOLUME_DISPLAY_UNITS;
import static android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE;
import static android.car.VehiclePropertyIds.INFO_FUEL_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_FUEL_TYPE;
import static android.car.VehiclePropertyIds.INFO_MAKE;
import static android.car.VehiclePropertyIds.INFO_MODEL;
import static android.car.VehiclePropertyIds.INFO_MODEL_YEAR;
import static android.car.VehiclePropertyIds.PERF_ODOMETER;
import static android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED;
import static android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY;
import static android.car.VehiclePropertyIds.RANGE_REMAINING;

import static androidx.car.app.hardware.common.CarValue.STATUS_SUCCESS;
import static androidx.car.app.hardware.common.CarValue.STATUS_UNAVAILABLE;
import static androidx.car.app.hardware.info.AutomotiveCarInfo.DEFAULT_SAMPLE_RATE;
import static androidx.car.app.hardware.info.AutomotiveCarInfo.SPEED_DISPLAY_UNIT_ID;
import static androidx.car.app.hardware.info.AutomotiveCarInfo.TOLL_CARD_STATUS_ID;
import static androidx.car.app.hardware.info.EnergyProfile.EVCONNECTOR_TYPE_CHADEMO;
import static androidx.car.app.hardware.info.EnergyProfile.FUEL_TYPE_UNLEADED;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.hardware.property.CarPropertyManager;

import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.GetPropertyRequest;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
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
    private List<GetPropertyRequest> mGetPropertyRequests;
    private List<Integer> mPropertyIds;
    private List<CarPropertyResponse<?>> mResponse;
    private CountDownLatch mCountDownLatch;
    private final Executor mExecutor = directExecutor();
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
        mGetPropertyRequests = new ArrayList<>();
        mPropertyIds = new ArrayList<>();
        mResponse = new ArrayList<>();
    }

    @Test
    public void getModel_SuccessfulResponse() throws InterruptedException {
        // Add "INFO_MAKE", "INFO_MODEL" and "INFO_MODEL_YEAR" of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MAKE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL_YEAR));

        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.create(INFO_MAKE,
                STATUS_SUCCESS, 1, "Toy Vehicle"));
        mResponse.add(CarPropertyResponse.create(INFO_MODEL,
                STATUS_SUCCESS, 2, "Speedy Model"));
        mResponse.add(CarPropertyResponse.create(INFO_MODEL_YEAR,
                STATUS_SUCCESS, 3, 2020));
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        AtomicReference<Model> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Model> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchModel(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        mCountDownLatch.await();
        Model mModel = loadedResult.get();
        assertThat(mModel.getName().getValue()).isEqualTo("Speedy Model");
        assertThat(mModel.getManufacturer().getValue()).isEqualTo("Toy Vehicle");
        assertThat(mModel.getYear().getValue()).isEqualTo(2020);
        assertThat(mModel.getManufacturer().getTimestampMillis()).isEqualTo(1);
        assertThat(mModel.getName().getTimestampMillis()).isEqualTo(2);
        assertThat(mModel.getYear().getTimestampMillis()).isEqualTo(3);
    }

    @Test
    public void getModel_MoreResponsesThanRequestsFailure() throws InterruptedException {
        // Add "INFO_MAKE" and "INFO_MODEL" of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MAKE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL));

        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.create(INFO_MAKE,
                STATUS_SUCCESS, 1, "Toy Vehicle"));
        mResponse.add(CarPropertyResponse.create(INFO_MODEL,
                STATUS_SUCCESS, 2, "Speedy Model"));
        mResponse.add(CarPropertyResponse.create(INFO_MODEL_YEAR,
                STATUS_SUCCESS, 3, 2020));
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        OnCarDataAvailableListener<Model> listener = (data) -> {};

        // Given that the number of values in the response is more(3) than what was requested(2),
        // there should be null pointer exception.
        assertThrows(NullPointerException.class, () ->
                mAutomotiveCarInfo.fetchModel(mExecutor, listener));
    }

    @Test
    public void getEnergyProfile_SuccessfulResponse() throws InterruptedException {
        // chademo in car service
        int chademoInVehicle = 4;

        // Add "INFO_EV_CONNECTOR_TYPE" and "INFO_FUEL_TYPE" type of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_TYPE));

        // Add "evConnector" and "fuel" type of the vehicle to the response.
        mResponse.add(CarPropertyResponse.create(INFO_EV_CONNECTOR_TYPE,
                STATUS_SUCCESS, 1, new Integer[]{chademoInVehicle}));
        mResponse.add(CarPropertyResponse.create(INFO_FUEL_TYPE,
                STATUS_SUCCESS, 2, new Integer[]{FUEL_TYPE_UNLEADED}));
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchEnergyProfile(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
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
    public void getEnergyProfile_EmptyResponse() throws InterruptedException {
        // Add "INFO_EV_CONNECTOR_TYPE" and "INFO_FUEL_TYPE" type of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_TYPE));

        // Leave the response empty.
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchEnergyProfile(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        mCountDownLatch.await();
        EnergyProfile energyProfile = loadedResult.get();
        assertThat(energyProfile.getEvConnectorTypes().getValue()).isEqualTo(
                null);
        assertThat(energyProfile.getFuelTypes().getValue()).isEqualTo(null);
    }

    @Test
    public void getMileage_verifyResponse() throws InterruptedException {
        // VehicleUnit.METER in car service
        int meterUnit = 0x21;

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(PERF_ODOMETER);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);

        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(PERF_ODOMETER, STATUS_SUCCESS, 1, 1f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS, STATUS_SUCCESS, 2,
                meterUnit));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        Mileage mileage = loadedResult.get();
        assertThat(mileage.getOdometerMeters().getValue()).isEqualTo(1f);
        assertThat(mileage.getDistanceDisplayUnit().getValue()).isEqualTo(2);
    }

    @Test
    public void getMileage_multiplRequestsSameListener() throws InterruptedException {
        // VehicleUnit.METER in car service
        int meterUnit = 0x21;

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(PERF_ODOMETER);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);

        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);
        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager, times(2)).submitRegisterListenerRequest(
                eq(mPropertyIds), eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(PERF_ODOMETER, STATUS_SUCCESS, 1, 1f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS, STATUS_SUCCESS, 2,
                meterUnit));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        Mileage mileage = loadedResult.get();
        assertThat(mileage.getOdometerMeters().getValue()).isEqualTo(1f);
        assertThat(mileage.getDistanceDisplayUnit().getValue()).isEqualTo(2);
    }

    @Test
    public void getMileage_multipleRequestsDifferentListener() throws InterruptedException {
        // VehicleUnit.METER in car service
        int meterUnit = 0x21;

        CountDownLatch firstCountDownLatch = new CountDownLatch(1);
        CountDownLatch secondCountDownLatch = new CountDownLatch(1);
        Executor firstExecutor = directExecutor();
        Executor secondExecutor = directExecutor();

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(PERF_ODOMETER);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);

        AtomicReference<Mileage> loadedFirstResult = new AtomicReference<>();
        AtomicReference<Mileage> loadedSecondResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> firstListener = (data) -> {
            loadedFirstResult.set(data);
            firstCountDownLatch.countDown();
        };

        OnCarDataAvailableListener<Mileage> secondListener = (data) -> {
            loadedSecondResult.set(data);
            secondCountDownLatch.countDown();
        };

        // Send request for first listener.
        mAutomotiveCarInfo.addMileageListener(firstExecutor, firstListener);

        ArgumentCaptor<OnCarPropertyResponseListener> firstCaptor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);

        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(mPropertyIds), eq(DEFAULT_SAMPLE_RATE), firstCaptor.capture(),
                eq(firstExecutor));

        mResponse.add(CarPropertyResponse.create(PERF_ODOMETER, STATUS_SUCCESS, 1, 1f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS, STATUS_SUCCESS, 2,
                meterUnit));

        firstCaptor.getValue().onCarPropertyResponses(mResponse);

        // Send request for second listener.
        mAutomotiveCarInfo.addMileageListener(secondExecutor, secondListener);

        ArgumentCaptor<OnCarPropertyResponseListener> secondCaptor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);

        // Listener request would be submitted twice by now.
        verify(mPropertyManager, times(2)).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE),
                secondCaptor.capture(), eq(secondExecutor));
        secondCaptor.getValue().onCarPropertyResponses(mResponse);

        firstCountDownLatch.await();
        Mileage firstMileage = loadedFirstResult.get();
        assertThat(firstMileage.getOdometerMeters().getValue()).isEqualTo(1f);
        assertThat(firstMileage.getDistanceDisplayUnit().getValue()).isEqualTo(2);

        secondCountDownLatch.await();
        Mileage secondMileage = loadedSecondResult.get();
        assertThat(secondMileage.getOdometerMeters().getValue()).isEqualTo(1f);
        assertThat(secondMileage.getDistanceDisplayUnit().getValue()).isEqualTo(2);
    }

    @Test
    public void getMileage_readExceptionAfterUnregisteringListener() throws InterruptedException {
        // VehicleUnit.METER in car service
        int meterUnit = 0x21;

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(PERF_ODOMETER);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);

        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);
        mAutomotiveCarInfo.removeMileageListener(listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(PERF_ODOMETER, STATUS_SUCCESS, 1, 1f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS, STATUS_SUCCESS, 2,
                meterUnit));

        captor.getValue().onCarPropertyResponses(mResponse);
        assertThat(mCountDownLatch.getCount() != 0);

        Mileage mileage = loadedResult.get();
        assertThrows(NullPointerException.class, () ->
                mileage.getOdometerMeters().getValue());
        assertThrows(NullPointerException.class, () ->
                mileage.getDistanceDisplayUnit().getValue());
    }

    @Test
    public void getEvStatus_verifyResponse() throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list.
        mPropertyIds.add(EV_CHARGE_PORT_OPEN);
        mPropertyIds.add(EV_CHARGE_PORT_CONNECTED);

        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(EV_CHARGE_PORT_OPEN, STATUS_SUCCESS, 1, true));
        mResponse.add(
                CarPropertyResponse.create(EV_CHARGE_PORT_CONNECTED, STATUS_SUCCESS, 2, false));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EvStatus evStatus = loadedResult.get();
        assertThat(evStatus.getEvChargePortOpen().getValue()).isEqualTo(true);
        assertThat(evStatus.getEvChargePortConnected().getValue()).isEqualTo(false);
    }

    @Test
    public void getEvStatus_withInvalidResponse_verifyResponse() throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list.
        mPropertyIds.add(EV_CHARGE_PORT_OPEN);
        mPropertyIds.add(EV_CHARGE_PORT_CONNECTED);

        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(EV_CHARGE_PORT_OPEN, STATUS_SUCCESS, 1, true));
        mResponse.add(
                CarPropertyResponse.create(EV_CHARGE_PORT_CONNECTED, CarValue.STATUS_UNKNOWN, 1,
                        null));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EvStatus evStatus = loadedResult.get();
        assertThat(evStatus.getEvChargePortOpen().getValue()).isEqualTo(true);
        assertThat(evStatus.getEvChargePortConnected().getStatus()).isEqualTo(
                CarValue.STATUS_UNIMPLEMENTED);
    }

    @Test
    public void getEvStatus_readExceptionAfterUnregisteringListener() throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list.
        mPropertyIds.add(EV_CHARGE_PORT_OPEN);
        mPropertyIds.add(EV_CHARGE_PORT_CONNECTED);

        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);
        mAutomotiveCarInfo.removeEvStatusListener(listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(EV_CHARGE_PORT_OPEN, STATUS_SUCCESS, 1, true));
        mResponse.add(
                CarPropertyResponse.create(EV_CHARGE_PORT_CONNECTED, STATUS_SUCCESS, 2, false));

        captor.getValue().onCarPropertyResponses(mResponse);
        assertThat(mCountDownLatch.getCount() != 0);

        EvStatus evStatus = loadedResult.get();
        assertThrows(NullPointerException.class, () ->
                evStatus.getEvChargePortOpen().getValue());
        assertThrows(NullPointerException.class, () ->
                evStatus.getEvChargePortConnected().getValue());
    }

    @Config(minSdk = 31)
    @Test
    public void getTollCard_verifyResponseApi31() throws InterruptedException {
        // Create "TOLL_CARD_STATUS_ID" request property IDs list.
        mPropertyIds.add(TOLL_CARD_STATUS_ID);

        AtomicReference<TollCard> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<TollCard> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addTollListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(TOLL_CARD_STATUS_ID,
                STATUS_SUCCESS, 1, TollCard.TOLLCARD_STATE_VALID));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        TollCard tollCard = loadedResult.get();
        assertThat(tollCard.getCardState().getValue()).isEqualTo(TollCard.TOLLCARD_STATE_VALID);
    }

    @Config(maxSdk = 30)
    @Test
    public void getTollCard_verifyResponseApi30() {
        AtomicReference<TollCard> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<TollCard> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.addTollListener(mExecutor, listener);

        TollCard tollCard = loadedResult.get();
        assertThat(tollCard.getCardState().getStatus()).isEqualTo(CarValue.STATUS_UNIMPLEMENTED);
    }

    @Config(minSdk = 31)
    @Test
    public void getTollCard_readExceptionAfterUnregisteringListenerApi31() throws
            InterruptedException {
        // Create "TOLL_CARD_STATUS_ID" request property IDs list.
        mPropertyIds.add(TOLL_CARD_STATUS_ID);

        AtomicReference<TollCard> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<TollCard> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addTollListener(mExecutor, listener);
        mAutomotiveCarInfo.removeTollListener(listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(TOLL_CARD_STATUS_ID,
                STATUS_SUCCESS, 1, TollCard.TOLLCARD_STATE_VALID));

        captor.getValue().onCarPropertyResponses(mResponse);
        assertThat(mCountDownLatch.getCount() != 0);

        TollCard tollCard = loadedResult.get();
        assertThrows(NullPointerException.class, () ->
                tollCard.getCardState().getValue());
    }

    @Test
    public void getSpeed_verifyResponse() throws InterruptedException {
        // Create "PERF_VEHICLE_SPEED", "PERF_VEHICLE_SPEED_DISPLAY" and "SPEED_DISPLAY_UNIT_ID"
        // property IDs list.
        mPropertyIds.add(PERF_VEHICLE_SPEED);
        mPropertyIds.add(PERF_VEHICLE_SPEED_DISPLAY);
        mPropertyIds.add(SPEED_DISPLAY_UNIT_ID);

        float defaultSpeed = 20f;
        float defaultRawSpeed = 20.5f;

        // VehicleUnit.METER_PER_SEC in car service
        int metersPerSec = 0x01;

        AtomicReference<Speed> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Speed> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addSpeedListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(SPEED_DISPLAY_UNIT_ID, STATUS_SUCCESS, 1,
                metersPerSec));
        mResponse.add(CarPropertyResponse.create(PERF_VEHICLE_SPEED,
                STATUS_SUCCESS, 2, defaultRawSpeed));
        mResponse.add(CarPropertyResponse.create(PERF_VEHICLE_SPEED_DISPLAY,
                STATUS_SUCCESS, 3, defaultSpeed));

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        Speed speed = loadedResult.get();
        assertThat(speed.getRawSpeedMetersPerSecond().getValue()).isEqualTo(defaultRawSpeed);
        assertThat(speed.getDisplaySpeedMetersPerSecond().getValue()).isEqualTo(defaultSpeed);
        assertThat(speed.getSpeedDisplayUnit().getValue()).isEqualTo(CarUnit.METERS_PER_SEC);
    }

    @Test
    public void getSpeed_readExceptionAfterUnregisteringListener() throws InterruptedException {
        // Create "PERF_VEHICLE_SPEED", "PERF_VEHICLE_SPEED_DISPLAY" and "SPEED_DISPLAY_UNIT_ID"
        // property IDs list.
        mPropertyIds.add(PERF_VEHICLE_SPEED);
        mPropertyIds.add(PERF_VEHICLE_SPEED_DISPLAY);
        mPropertyIds.add(SPEED_DISPLAY_UNIT_ID);

        float defaultSpeed = 20f;
        float defaultRawSpeed = 20.5f;
        int metersPerSec = 0x01;

        AtomicReference<Speed> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Speed> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addSpeedListener(mExecutor, listener);
        mAutomotiveCarInfo.removeSpeedListener(listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(mPropertyIds),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(SPEED_DISPLAY_UNIT_ID, STATUS_SUCCESS, 1,
                metersPerSec));
        mResponse.add(CarPropertyResponse.create(PERF_VEHICLE_SPEED,
                STATUS_SUCCESS, 2, defaultRawSpeed));
        mResponse.add(CarPropertyResponse.create(PERF_VEHICLE_SPEED_DISPLAY,
                STATUS_SUCCESS, 3, defaultSpeed));

        captor.getValue().onCarPropertyResponses(mResponse);
        assertThat(mCountDownLatch.getCount() != 0);

        Speed speed = loadedResult.get();
        assertThrows(NullPointerException.class, () ->
                speed.getRawSpeedMetersPerSecond().getValue());
        assertThrows(NullPointerException.class, () ->
                speed.getDisplaySpeedMetersPerSecond().getValue());
        assertThrows(NullPointerException.class, () ->
                speed.getSpeedDisplayUnit().getValue());
    }

    @Test
    public void getEnergyLevel_verifyResponse() throws InterruptedException {
        // Add "INFO_EV_BATTERY_CAPACITY" and "INFO_FUEL_CAPACITY" to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        int meterDistanceUnit = 0x21;
        int meterVolumeUnit = 0x40;
        float evBatteryCapacity = 100f;
        float evBatteryLevelValue = 50f;
        float fuelCapacity = 120f;
        float fuelLevelValue = 50f;
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.create(INFO_EV_BATTERY_CAPACITY,
                STATUS_SUCCESS, 1, evBatteryCapacity));
        capacities.add(CarPropertyResponse.create(INFO_FUEL_CAPACITY,
                STATUS_SUCCESS, 1, fuelCapacity));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests), any()))
                .thenReturn(future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(EV_BATTERY_LEVEL);
        mPropertyIds.add(FUEL_LEVEL);
        mPropertyIds.add(FUEL_LEVEL_LOW);
        mPropertyIds.add(RANGE_REMAINING);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);
        mPropertyIds.add(FUEL_VOLUME_DISPLAY_UNITS);

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(mPropertyIds), eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(EV_BATTERY_LEVEL,
                STATUS_SUCCESS, 1, evBatteryLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL,
                STATUS_SUCCESS, 1, fuelLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL_LOW,
                STATUS_SUCCESS, 1, true));
        mResponse.add(CarPropertyResponse.create(RANGE_REMAINING,
                STATUS_SUCCESS, 1, 5f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS,
                STATUS_SUCCESS, 1, meterDistanceUnit));
        mResponse.add(CarPropertyResponse.create(FUEL_VOLUME_DISPLAY_UNITS,
                STATUS_SUCCESS, 2, meterVolumeUnit));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EnergyLevel energyLevel = loadedResult.get();
        assertThat(energyLevel.getBatteryPercent().getValue()).isEqualTo(
                evBatteryLevelValue / evBatteryCapacity * 100);
        assertThat(energyLevel.getFuelPercent().getValue()).isEqualTo(
                fuelLevelValue / fuelCapacity * 100);
        assertThat(energyLevel.getEnergyIsLow().getValue()).isEqualTo(
                true);
        assertThat(energyLevel.getRangeRemainingMeters().getValue()).isEqualTo(
                5f);
        assertThat(energyLevel.getDistanceDisplayUnit().getValue()).isEqualTo(2);
        assertThat(energyLevel.getFuelVolumeDisplayUnit().getValue()).isEqualTo(201);
    }

    @Test
    public void getEnergyLevel_readExceptionAfterUnregisteringListener() throws
            InterruptedException {
        // Add "INFO_EV_BATTERY_CAPACITY" and "INFO_FUEL_CAPACITY" to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        int meterDistanceUnit = 0x21;
        int meterVolumeUnit = 0x40;
        float evBatteryCapacity = 100f;
        float evBatteryLevelValue = 50f;
        float fuelCapacity = 120f;
        float fuelLevelValue = 50f;
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.create(INFO_EV_BATTERY_CAPACITY,
                STATUS_SUCCESS, 1, evBatteryCapacity));
        capacities.add(CarPropertyResponse.create(INFO_FUEL_CAPACITY,
                STATUS_SUCCESS, 1, fuelCapacity));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), any())).thenReturn(future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);
        mAutomotiveCarInfo.removeEnergyLevelListener(listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(EV_BATTERY_LEVEL);
        mPropertyIds.add(FUEL_LEVEL);
        mPropertyIds.add(FUEL_LEVEL_LOW);
        mPropertyIds.add(RANGE_REMAINING);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);
        mPropertyIds.add(FUEL_VOLUME_DISPLAY_UNITS);

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(mPropertyIds), eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));
        verify(mPropertyManager, times(1)).submitUnregisterListenerRequest(
                captor.capture());

        mResponse.add(CarPropertyResponse.create(EV_BATTERY_LEVEL,
                STATUS_SUCCESS, 1, evBatteryLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL,
                STATUS_SUCCESS, 1, fuelLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL_LOW,
                STATUS_SUCCESS, 1, true));
        mResponse.add(CarPropertyResponse.create(RANGE_REMAINING,
                STATUS_SUCCESS, 1, 5f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS,
                STATUS_SUCCESS, 1, meterDistanceUnit));
        mResponse.add(CarPropertyResponse.create(FUEL_VOLUME_DISPLAY_UNITS,
                STATUS_SUCCESS, 2, meterVolumeUnit));
        captor.getValue().onCarPropertyResponses(mResponse);
        assertThat(mCountDownLatch.getCount() != 0);

        EnergyLevel energyLevel = loadedResult.get();
        assertThrows(NullPointerException.class, () ->
                energyLevel.getBatteryPercent().getValue());
        assertThrows(NullPointerException.class, () ->
                energyLevel.getFuelPercent().getValue());
        assertThrows(NullPointerException.class, () ->
                energyLevel.getEnergyIsLow().getValue());
        assertThrows(NullPointerException.class, () ->
                energyLevel.getRangeRemainingMeters().getValue());
        assertThrows(NullPointerException.class, () ->
                energyLevel.getDistanceDisplayUnit().getValue());
        assertThrows(NullPointerException.class, () ->
                energyLevel.getFuelVolumeDisplayUnit().getValue());
    }

    @Test
    public void getEnergyLevel_withUnavailableCapacityValues() throws InterruptedException {
        // Add "INFO_EV_BATTERY_CAPACITY" and "INFO_FUEL_CAPACITY" to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        int meterDistanceUnit = 0x21;
        int meterVolumeUnit = 0x40;
        float evBatteryCapacity = 100f;
        float evBatteryLevelValue = 50f;
        float fuelCapacity = 120f;
        float fuelLevelValue = 50f;
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.create(INFO_EV_BATTERY_CAPACITY,
                STATUS_UNAVAILABLE, 1, evBatteryCapacity));
        capacities.add(CarPropertyResponse.create(INFO_FUEL_CAPACITY,
                STATUS_UNAVAILABLE, 1, fuelCapacity));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), any())).thenReturn(future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(EV_BATTERY_LEVEL);
        mPropertyIds.add(FUEL_LEVEL);
        mPropertyIds.add(FUEL_LEVEL_LOW);
        mPropertyIds.add(RANGE_REMAINING);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);
        mPropertyIds.add(FUEL_VOLUME_DISPLAY_UNITS);

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(mPropertyIds), eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(EV_BATTERY_LEVEL,
                STATUS_SUCCESS, 1, evBatteryLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL,
                STATUS_SUCCESS, 1, fuelLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL_LOW,
                STATUS_SUCCESS, 1, true));
        mResponse.add(CarPropertyResponse.create(RANGE_REMAINING,
                STATUS_SUCCESS, 1, 5f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS,
                STATUS_SUCCESS, 1, meterDistanceUnit));
        mResponse.add(CarPropertyResponse.create(FUEL_VOLUME_DISPLAY_UNITS,
                STATUS_SUCCESS, 2, meterVolumeUnit));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EnergyLevel energyLevel = loadedResult.get();

        // Battery percent and fuel percent should be UNIMPLEMENTED_FLOAT since we can not get
        // the capacity of battery and fuel property.
        assertThat(energyLevel.getBatteryPercent().getValue()).isEqualTo(
                CarValue.UNIMPLEMENTED_FLOAT.getValue());
        assertThat(energyLevel.getFuelPercent().getValue()).isEqualTo(
                CarValue.UNIMPLEMENTED_FLOAT.getValue());

        // The other properties should still work without capacity values
        assertThat(energyLevel.getEnergyIsLow().getValue()).isEqualTo(
                true);
        assertThat(energyLevel.getRangeRemainingMeters().getValue()).isEqualTo(
                5f);
        assertThat(energyLevel.getDistanceDisplayUnit().getValue()).isEqualTo(2);
        assertThat(energyLevel.getFuelVolumeDisplayUnit().getValue()).isEqualTo(201);
    }

    @Test
    public void getEnergyLevel_FewerResponsesThanRequests() throws InterruptedException {
        // Add "INFO_EV_BATTERY_CAPACITY" and "INFO_FUEL_CAPACITY" to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        int meterDistanceUnit = 0x21;
        float evBatteryCapacity = 100f;
        float evBatteryLevelValue = 50f;
        float fuelCapacity = 120f;
        float fuelLevelValue = 50f;
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.create(INFO_EV_BATTERY_CAPACITY,
                STATUS_SUCCESS, 1, evBatteryCapacity));
        capacities.add(CarPropertyResponse.create(INFO_FUEL_CAPACITY,
                STATUS_SUCCESS, 1, fuelCapacity));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), any())).thenReturn(future);

        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {};

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list.
        mPropertyIds.add(EV_BATTERY_LEVEL);
        mPropertyIds.add(FUEL_LEVEL);
        mPropertyIds.add(FUEL_LEVEL_LOW);
        mPropertyIds.add(RANGE_REMAINING);
        mPropertyIds.add(DISTANCE_DISPLAY_UNITS);
        mPropertyIds.add(FUEL_VOLUME_DISPLAY_UNITS);

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(mPropertyIds), eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.create(EV_BATTERY_LEVEL,
                STATUS_SUCCESS, 1, evBatteryLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL,
                STATUS_SUCCESS, 1, fuelLevelValue));
        mResponse.add(CarPropertyResponse.create(FUEL_LEVEL_LOW,
                STATUS_SUCCESS, 1, true));
        mResponse.add(CarPropertyResponse.create(RANGE_REMAINING,
                STATUS_SUCCESS, 1, 5f));
        mResponse.add(CarPropertyResponse.create(DISTANCE_DISPLAY_UNITS,
                STATUS_SUCCESS, 1, meterDistanceUnit));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                captor.getValue().onCarPropertyResponses(mResponse));
        assertThat(e).hasMessageThat().contains("Response size is not the same as requested.");
    }
}
