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
import static androidx.car.app.hardware.common.CarValue.STATUS_UNKNOWN;
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
import androidx.car.app.hardware.common.CarZone;
import androidx.car.app.hardware.common.GetPropertyRequest;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.shadows.car.ShadowCar;

import com.google.common.collect.ImmutableMap;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private List<CarZone> mCarZones;
    @Mock
    private Car mCarMock;
    @Mock
    private CarPropertyManager mCarPropertyManagerMock;
    @Mock
    private PropertyManager mPropertyManager;
    private static final List<CarZone> GLOBAL_ZONE = Collections.singletonList(
            CarZone.CAR_ZONE_GLOBAL);

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
        mCarZones = Arrays.asList(CarZone.CAR_ZONE_GLOBAL);
    }

    @Test
    public void fetchModel_returnsModelWithUnknownValuesIfNoResponses()
            throws InterruptedException {
        // Add "INFO_MAKE", "INFO_MODEL" and "INFO_MODEL_YEAR" of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MAKE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL_YEAR));
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor))).thenReturn(
                Futures.immediateFuture(mResponse));
        AtomicReference<Model> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Model> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.fetchModel(mExecutor, listener);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new Model.Builder().build());
        verify(mPropertyManager).submitGetPropertyRequest(eq(mGetPropertyRequests), eq(mExecutor));
    }

    @Test
    public void fetchModel_handlesResponsesWithDifferentStatuses() throws InterruptedException {
        // Add "INFO_MAKE", "INFO_MODEL" and "INFO_MODEL_YEAR" of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MAKE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL_YEAR));

        String testMake = "Toy Vehicle";
        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MAKE).setStatus(
                STATUS_SUCCESS).setValue(testMake).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MODEL).setStatus(
                STATUS_UNAVAILABLE).setTimestampMillis(2L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MODEL_YEAR).setStatus(
                CarValue.STATUS_UNKNOWN).setTimestampMillis(3L).build());
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor))).thenReturn(Futures.immediateFuture(mResponse));
        AtomicReference<Model> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Model> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.fetchModel(mExecutor, listener);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new Model.Builder().setManufacturer(
                new CarValue<>(testMake, 1, CarValue.STATUS_SUCCESS)).setName(
                new CarValue<>(null, 2, CarValue.STATUS_UNAVAILABLE)).setYear(
                new CarValue<>(null, 3, CarValue.STATUS_UNKNOWN)).build());
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
    }

    @Test
    public void getModel_SuccessfulResponse() throws InterruptedException {
        // Add "INFO_MAKE", "INFO_MODEL" and "INFO_MODEL_YEAR" of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MAKE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL_YEAR));

        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MAKE).setStatus(
                STATUS_SUCCESS).setValue("Toy Vehicle").setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MODEL).setStatus(
                STATUS_SUCCESS).setValue("Speedy Model").setTimestampMillis(2L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MODEL_YEAR).setStatus(
                STATUS_SUCCESS).setValue(2020).setTimestampMillis(3L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        AtomicReference<Model> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Model> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchModel(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
        mCountDownLatch.await();
        Model mModel = loadedResult.get();
        assertThat(mModel.getName().getValue()).isEqualTo("Speedy Model");
        assertThat(mModel.getManufacturer().getValue()).isEqualTo("Toy Vehicle");
        assertThat(mModel.getYear().getValue()).isEqualTo(2020);
        assertThat(mModel.getManufacturer().getTimestampMillis()).isEqualTo(1);
        assertThat(mModel.getName().getTimestampMillis()).isEqualTo(2);
        // test CarZone
        assertThat(mModel.getName().getCarZones()).isEqualTo(GLOBAL_ZONE);
        assertThat(mModel.getManufacturer().getCarZones()).isEqualTo(GLOBAL_ZONE);
        assertThat(mModel.getYear().getCarZones()).isEqualTo(GLOBAL_ZONE);
        assertThat(mModel.getYear().getTimestampMillis()).isEqualTo(3);
    }

    @Test
    public void getModel_MoreResponsesThanRequestsFailure() throws InterruptedException {
        // Add "INFO_MAKE" and "INFO_MODEL" of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MAKE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_MODEL));

        // Add "make", "model", "year" values to the response.
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MAKE).setStatus(
                STATUS_SUCCESS).setValue("Toy Vehicle").setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MODEL).setStatus(
                STATUS_SUCCESS).setValue("Speedy Model").setTimestampMillis(2L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_MODEL_YEAR).setStatus(
                STATUS_SUCCESS).setValue(2020).setTimestampMillis(3L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        OnCarDataAvailableListener<Model> listener = (data) -> {
        };

        // Given that the number of values in the response is more(3) than what was requested(2),
        // there should be null pointer exception.
        assertThrows(NullPointerException.class,
                () -> mAutomotiveCarInfo.fetchModel(mExecutor, listener));
    }

    @Test
    public void getEnergyProfile_SuccessfulResponse() throws InterruptedException {
        // chademo in car service
        int chademoInVehicle = 4;

        // Add "INFO_EV_CONNECTOR_TYPE" and "INFO_FUEL_TYPE" type of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_TYPE));

        // Add "evConnector" and "fuel" type of the vehicle to the response.
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_EV_CONNECTOR_TYPE).setStatus(
                STATUS_SUCCESS).setValue(new Integer[]{chademoInVehicle}).setTimestampMillis(
                1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_FUEL_TYPE).setStatus(
                STATUS_SUCCESS).setValue(new Integer[]{FUEL_TYPE_UNLEADED}).setTimestampMillis(
                2L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor))).thenReturn(listenableCarPropertyResponse);
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };
        mAutomotiveCarInfo.fetchEnergyProfile(mExecutor, listener);
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
        mCountDownLatch.await();
        EnergyProfile energyProfile = loadedResult.get();
        List<Integer> evConnector = new ArrayList<Integer>();
        evConnector.add(EVCONNECTOR_TYPE_CHADEMO);
        List<Integer> fuel = new ArrayList<Integer>();
        fuel.add(FUEL_TYPE_UNLEADED);
        assertThat(energyProfile.getEvConnectorTypes().getValue()).isEqualTo(evConnector);
        assertThat(energyProfile.getFuelTypes().getValue()).isEqualTo(fuel);
    }

    @Test
    public void fetchEnergyProfile_handlesResponsesWithDifferentStatuses()
            throws InterruptedException {
        // Add "INFO_EV_CONNECTOR_TYPE" and "INFO_FUEL_TYPE" type of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_TYPE));

        // Add "evConnector" and "fuel" type of the vehicle to the response.
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_EV_CONNECTOR_TYPE).setStatus(
                STATUS_UNAVAILABLE).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(INFO_FUEL_TYPE).setStatus(
                CarValue.STATUS_UNKNOWN).setTimestampMillis(2L).build());
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor))).thenReturn(Futures.immediateFuture(mResponse));
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.fetchEnergyProfile(mExecutor, listener);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new EnergyProfile.Builder().setEvConnectorTypes(
                new CarValue<>(null, 1, STATUS_UNAVAILABLE)).setFuelTypes(
                new CarValue<>(null, 2, STATUS_UNKNOWN)).build());
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
    }

    @Test
    public void fetchEnergyProfile_returnsEnergyProfileWithUnknownValuesIfNoResponses()
            throws InterruptedException {
        // Add "INFO_EV_CONNECTOR_TYPE" and "INFO_FUEL_TYPE" type of the vehicle to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_TYPE));
        // Leave the response empty.
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor))).thenReturn(
                Futures.immediateFuture(mResponse));
        AtomicReference<EnergyProfile> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyProfile> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.fetchEnergyProfile(mExecutor, listener);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new EnergyProfile.Builder().build());
        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
    }

    @Test
    public void getMileage_verifyResponse() throws InterruptedException {
        // VehicleUnit.METER in car service
        int meterUnit = 0x21;

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(PERF_ODOMETER, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();

        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(PERF_ODOMETER).setStatus(
                STATUS_SUCCESS).setValue(1f).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterUnit).setTimestampMillis(2L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        Mileage mileage = loadedResult.get();
        assertThat(mileage.getOdometerMeters().getValue()).isEqualTo(1f);
        assertThat(mileage.getDistanceDisplayUnit().getValue()).isEqualTo(2);
    }

    @Test
    public void addMileageListener_handlesResponsesWithDifferentStatuses()
            throws InterruptedException {
        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(PERF_ODOMETER, mCarZones).put(
                        DISTANCE_DISPLAY_UNITS, mCarZones).buildKeepingLast();

        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(PERF_ODOMETER).setStatus(
                STATUS_UNAVAILABLE).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_UNKNOWN).setTimestampMillis(2L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new Mileage.Builder().setOdometerMeters(
                new CarValue<>(null, 1, STATUS_UNAVAILABLE)).setDistanceDisplayUnit(
                new CarValue<>(null, 2, STATUS_UNKNOWN)).build());
    }

    @Test
    public void addMileageListener_returnsMileageWithUnknownValuesIfNoResponses()
            throws InterruptedException {
        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(PERF_ODOMETER, mCarZones).put(
                        DISTANCE_DISPLAY_UNITS, mCarZones).buildKeepingLast();
        AtomicReference<Mileage> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Mileage> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addMileageListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new Mileage.Builder().build());
    }

    @Test
    public void getMileage_multiplRequestsSameListener() throws InterruptedException {
        // VehicleUnit.METER in car service
        int meterUnit = 0x21;

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(PERF_ODOMETER, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();

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
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), captor.capture(),
                eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(PERF_ODOMETER).setStatus(
                STATUS_SUCCESS).setValue(1f).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterUnit).setTimestampMillis(2L).build());

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

        // Create "PERF_ODOMETER" and "DISTANCE_DISPLAY_UNITS" property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(PERF_ODOMETER, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();

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
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), firstCaptor.capture(),
                eq(firstExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(PERF_ODOMETER).setStatus(
                STATUS_SUCCESS).setValue(1f).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterUnit).setTimestampMillis(2L).build());

        firstCaptor.getValue().onCarPropertyResponses(mResponse);

        // Send request for second listener.
        mAutomotiveCarInfo.addMileageListener(secondExecutor, secondListener);

        ArgumentCaptor<OnCarPropertyResponseListener> secondCaptor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);

        // Listener request would be submitted twice by now.
        verify(mPropertyManager, times(2)).submitRegisterListenerRequest(
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), secondCaptor.capture(),
                eq(secondExecutor));
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
    public void getEvStatus_verifyResponse() throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(EV_CHARGE_PORT_OPEN, mCarZones)
                        .put(EV_CHARGE_PORT_CONNECTED, mCarZones)
                        .buildKeepingLast();

        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(EV_CHARGE_PORT_OPEN).setStatus(
                STATUS_SUCCESS).setValue(true).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                EV_CHARGE_PORT_CONNECTED).setStatus(STATUS_SUCCESS).setValue(
                false).setTimestampMillis(2L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EvStatus evStatus = loadedResult.get();
        assertThat(evStatus.getEvChargePortOpen().getValue()).isEqualTo(true);
        assertThat(evStatus.getEvChargePortConnected().getValue()).isEqualTo(false);
    }

    @Test
    public void addEvStatusListener_handlesResponsesWithDifferentStatuses()
            throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(EV_CHARGE_PORT_OPEN,
                        mCarZones).put(EV_CHARGE_PORT_CONNECTED, mCarZones).buildKeepingLast();

        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(EV_CHARGE_PORT_OPEN).setStatus(
                STATUS_UNKNOWN).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                EV_CHARGE_PORT_CONNECTED).setStatus(STATUS_UNAVAILABLE).setTimestampMillis(
                2L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new EvStatus.Builder().setEvChargePortOpen(
                new CarValue<>(null, 1, STATUS_UNKNOWN)).setEvChargePortConnected(
                new CarValue<>(null, 2, STATUS_UNAVAILABLE)).build());
    }

    @Test
    public void addEvStatusListener_returnsEvStatusWithUnknownValuesIfNoResponses()
            throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(EV_CHARGE_PORT_OPEN,
                                mCarZones).put(EV_CHARGE_PORT_CONNECTED, mCarZones)
                        .buildKeepingLast();
        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new EvStatus.Builder().build());
    }

    @Test
    public void getEvStatus_withInvalidResponse_verifyResponse() throws InterruptedException {
        // Create "EV_CHARGE_PORT_OPEN" and "EV_CHARGE_PORT_CONNECTED" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(EV_CHARGE_PORT_OPEN, mCarZones)
                        .put(EV_CHARGE_PORT_CONNECTED, mCarZones)
                        .buildKeepingLast();

        AtomicReference<EvStatus> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EvStatus> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEvStatusListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder()
                .setPropertyId(EV_CHARGE_PORT_OPEN)
                .setStatus(STATUS_SUCCESS)
                .setValue(true)
                .setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                EV_CHARGE_PORT_CONNECTED).setStatus(CarValue.STATUS_UNIMPLEMENTED).setValue(
                null).setTimestampMillis(1L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EvStatus evStatus = loadedResult.get();
        assertThat(evStatus.getEvChargePortOpen().getValue()).isEqualTo(true);
        assertThat(evStatus.getEvChargePortConnected().getStatus()).isEqualTo(
                CarValue.STATUS_UNIMPLEMENTED);
    }

    @Config(minSdk = 31)
    @Test
    public void getTollCard_verifyResponseApi31() throws InterruptedException {
        // Create "TOLL_CARD_STATUS_ID" request property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(TOLL_CARD_STATUS_ID, mCarZones)
                        .buildKeepingLast();

        AtomicReference<TollCard> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<TollCard> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addTollListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(TOLL_CARD_STATUS_ID).setStatus(
                STATUS_SUCCESS).setValue(TollCard.TOLLCARD_STATE_VALID).setTimestampMillis(
                1L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        TollCard tollCard = loadedResult.get();
        assertThat(tollCard.getCardState().getValue()).isEqualTo(TollCard.TOLLCARD_STATE_VALID);
        assertThat(tollCard.getCardState().getCarZones()).isEqualTo(GLOBAL_ZONE);
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
        assertThat(tollCard.getCardState().getCarZones().isEmpty()).isTrue();
    }

    @Test
    public void getSpeed_verifyResponse() throws InterruptedException {
        // Create "PERF_VEHICLE_SPEED", "PERF_VEHICLE_SPEED_DISPLAY" and "SPEED_DISPLAY_UNIT_ID"
        // property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(PERF_VEHICLE_SPEED, mCarZones)
                        .put(PERF_VEHICLE_SPEED_DISPLAY, mCarZones)
                        .put(SPEED_DISPLAY_UNIT_ID, mCarZones)
                        .buildKeepingLast();

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
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(SPEED_DISPLAY_UNIT_ID).setStatus(
                STATUS_SUCCESS).setValue(metersPerSec).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(PERF_VEHICLE_SPEED).setStatus(
                STATUS_SUCCESS).setValue(defaultRawSpeed).setTimestampMillis(2L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                PERF_VEHICLE_SPEED_DISPLAY).setStatus(STATUS_SUCCESS).setValue(
                defaultSpeed).setTimestampMillis(3L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        Speed speed = loadedResult.get();
        assertThat(speed.getRawSpeedMetersPerSecond().getValue()).isEqualTo(defaultRawSpeed);
        assertThat(speed.getDisplaySpeedMetersPerSecond().getValue()).isEqualTo(defaultSpeed);
        assertThat(speed.getSpeedDisplayUnit().getValue()).isEqualTo(CarUnit.METERS_PER_SEC);
    }

    @Test
    public void addSpeedListener_handlesResponsesWithDifferentStatuses()
            throws InterruptedException {
        // Create "PERF_VEHICLE_SPEED", "PERF_VEHICLE_SPEED_DISPLAY" and "SPEED_DISPLAY_UNIT_ID"
        // property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(PERF_VEHICLE_SPEED,
                        mCarZones).put(PERF_VEHICLE_SPEED_DISPLAY, mCarZones).put(
                        SPEED_DISPLAY_UNIT_ID, mCarZones).buildKeepingLast();

        AtomicReference<Speed> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Speed> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addSpeedListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(PERF_VEHICLE_SPEED).setStatus(
                STATUS_UNAVAILABLE).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                PERF_VEHICLE_SPEED_DISPLAY).setStatus(STATUS_UNKNOWN).setTimestampMillis(
                2L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(SPEED_DISPLAY_UNIT_ID).setStatus(
                STATUS_SUCCESS).setTimestampMillis(3L).setValue(/*VehicleUnit.METER_PER_SEC=*/
                0x01).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new Speed.Builder().setRawSpeedMetersPerSecond(
                new CarValue<>(null, 1, STATUS_UNAVAILABLE)).setDisplaySpeedMetersPerSecond(
                new CarValue<>(null, 2, STATUS_UNKNOWN)).setSpeedDisplayUnit(
                new CarValue<>(CarUnit.METERS_PER_SEC, 3, STATUS_SUCCESS)).build());
    }

    @Test
    public void addSpeedListener_returnsSpeedWithUnknownValuesIfNoResponses()
            throws InterruptedException {
        // Create "PERF_VEHICLE_SPEED", "PERF_VEHICLE_SPEED_DISPLAY" and "SPEED_DISPLAY_UNIT_ID"
        // property IDs list with car zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(PERF_VEHICLE_SPEED,
                                mCarZones).put(PERF_VEHICLE_SPEED_DISPLAY, mCarZones)
                        .put(SPEED_DISPLAY_UNIT_ID, mCarZones)
                        .buildKeepingLast();
        AtomicReference<Speed> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<Speed> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addSpeedListener(mExecutor, listener);

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE), captor.capture(), eq(mExecutor));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new Speed.Builder().build());
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
        capacities.add(CarPropertyResponse.builder().setPropertyId(
                INFO_EV_BATTERY_CAPACITY).setStatus(STATUS_SUCCESS).setValue(
                evBatteryCapacity).setTimestampMillis(1L).build());
        capacities.add(CarPropertyResponse.builder().setPropertyId(INFO_FUEL_CAPACITY).setStatus(
                STATUS_SUCCESS).setValue(fuelCapacity).setTimestampMillis(1L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> future = Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests), any())).thenReturn(
                future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(EV_BATTERY_LEVEL, mCarZones)
                        .put(FUEL_LEVEL, mCarZones)
                        .put(FUEL_LEVEL_LOW, mCarZones)
                        .put(RANGE_REMAINING, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .put(FUEL_VOLUME_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), captor.capture(),
                eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(EV_BATTERY_LEVEL).setStatus(
                STATUS_SUCCESS).setValue(evBatteryLevelValue).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL).setStatus(
                STATUS_SUCCESS).setValue(fuelLevelValue).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL_LOW).setStatus(
                STATUS_SUCCESS).setValue(true).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(RANGE_REMAINING).setStatus(
                STATUS_SUCCESS).setValue(5f).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterDistanceUnit).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                FUEL_VOLUME_DISPLAY_UNITS).setStatus(STATUS_SUCCESS).setValue(
                meterVolumeUnit).setTimestampMillis(1L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EnergyLevel energyLevel = loadedResult.get();
        assertThat(energyLevel.getBatteryPercent().getValue()).isEqualTo(
                evBatteryLevelValue / evBatteryCapacity * 100);
        assertThat(energyLevel.getFuelPercent().getValue()).isEqualTo(
                fuelLevelValue / fuelCapacity * 100);
        assertThat(energyLevel.getEnergyIsLow().getValue()).isEqualTo(true);
        assertThat(energyLevel.getRangeRemainingMeters().getValue()).isEqualTo(5f);
        assertThat(energyLevel.getDistanceDisplayUnit().getValue()).isEqualTo(2);
        assertThat(energyLevel.getFuelVolumeDisplayUnit().getValue()).isEqualTo(201);
    }

    @Test
    public void addEnergyLevelListener_handlesReponsesWithDifferentStatuses()
            throws InterruptedException {
        // Add "INFO_EV_BATTERY_CAPACITY" and "INFO_FUEL_CAPACITY" to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        int meterDistanceUnit = 0x21;
        float evBatteryCapacity = 100f;
        float fuelCapacity = 120f;
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.builder().setPropertyId(
                INFO_EV_BATTERY_CAPACITY).setStatus(STATUS_SUCCESS).setValue(
                evBatteryCapacity).setTimestampMillis(1L).build());
        capacities.add(CarPropertyResponse.builder().setPropertyId(INFO_FUEL_CAPACITY).setStatus(
                STATUS_SUCCESS).setValue(fuelCapacity).setTimestampMillis(1L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> future = Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests), any())).thenReturn(
                future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(EV_BATTERY_LEVEL, mCarZones).put(
                        FUEL_LEVEL, mCarZones).put(FUEL_LEVEL_LOW, mCarZones).put(RANGE_REMAINING,
                        mCarZones).put(DISTANCE_DISPLAY_UNITS, mCarZones).put(
                        FUEL_VOLUME_DISPLAY_UNITS, mCarZones).buildKeepingLast();

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), captor.capture(),
                eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(EV_BATTERY_LEVEL).setStatus(
                STATUS_UNKNOWN).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL).setStatus(
                STATUS_UNAVAILABLE).setTimestampMillis(2L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL_LOW).setStatus(
                STATUS_SUCCESS).setValue(true).setTimestampMillis(3L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(RANGE_REMAINING).setStatus(
                STATUS_UNKNOWN).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterDistanceUnit).setTimestampMillis(4L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                FUEL_VOLUME_DISPLAY_UNITS).setStatus(STATUS_UNAVAILABLE).setTimestampMillis(
                6L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new EnergyLevel.Builder().setBatteryPercent(
                new CarValue<>(null, 1, STATUS_UNKNOWN)).setFuelPercent(
                new CarValue<>(null, 2, STATUS_UNAVAILABLE)).setEnergyIsLow(
                new CarValue<>(true, 3, STATUS_SUCCESS)).setRangeRemainingMeters(
                new CarValue<>(null, 1, STATUS_UNKNOWN)).setFuelVolumeDisplayUnit(
                new CarValue<>(null, 6, STATUS_UNAVAILABLE)).setDistanceDisplayUnit(
                new CarValue<>(CarUnit.METER, 4, STATUS_SUCCESS)).build());
    }

    @Test
    public void addEnergyLevelListener_returnsEnergyLevelWithUnknownValuesIfNoResponses()
            throws InterruptedException {
        // Add "INFO_EV_BATTERY_CAPACITY" and "INFO_FUEL_CAPACITY" to the request.
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        mGetPropertyRequests.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests), any())).thenReturn(
                Futures.immediateFuture(capacities));
        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests), eq(mExecutor));
        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(EV_BATTERY_LEVEL, mCarZones)
                        .put(FUEL_LEVEL, mCarZones)
                        .put(FUEL_LEVEL_LOW, mCarZones)
                        .put(RANGE_REMAINING, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .put(FUEL_VOLUME_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();
        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), captor.capture(),
                eq(mExecutor));
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(new EnergyLevel.Builder().build());
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
        float evBatteryLevelValue = 50f;
        float fuelLevelValue = 50f;
        List<CarPropertyResponse<?>> capacities = new ArrayList<>();
        capacities.add(CarPropertyResponse.builder().setPropertyId(
                INFO_EV_BATTERY_CAPACITY).setStatus(STATUS_UNAVAILABLE).setTimestampMillis(
                1L).build());
        capacities.add(CarPropertyResponse.builder().setPropertyId(INFO_FUEL_CAPACITY).setStatus(
                STATUS_UNAVAILABLE).setTimestampMillis(1L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> future = Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests), any())).thenReturn(
                future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(EV_BATTERY_LEVEL, mCarZones)
                        .put(FUEL_LEVEL, mCarZones)
                        .put(FUEL_LEVEL_LOW, mCarZones)
                        .put(RANGE_REMAINING, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .put(FUEL_VOLUME_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(
                eq(mGetPropertyRequests),
                eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), captor.capture(),
                eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(EV_BATTERY_LEVEL).setStatus(
                STATUS_SUCCESS).setValue(evBatteryLevelValue).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL).setStatus(
                STATUS_SUCCESS).setValue(fuelLevelValue).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL_LOW).setStatus(
                STATUS_SUCCESS).setValue(true).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(RANGE_REMAINING).setStatus(
                STATUS_SUCCESS).setValue(5f).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterDistanceUnit).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                FUEL_VOLUME_DISPLAY_UNITS).setStatus(STATUS_SUCCESS).setValue(
                meterVolumeUnit).setTimestampMillis(1L).build());
        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        EnergyLevel energyLevel = loadedResult.get();

        // Battery percent and fuel percent should be UNKNOWN_FLOAT since we can not get
        // the capacity of battery and fuel property.
        assertThat(energyLevel.getBatteryPercent().getValue()).isEqualTo(
                CarValue.UNKNOWN_FLOAT.getValue());
        assertThat(energyLevel.getFuelPercent().getValue()).isEqualTo(
                CarValue.UNKNOWN_FLOAT.getValue());

        // The other properties should still work without capacity values
        assertThat(energyLevel.getEnergyIsLow().getValue()).isEqualTo(true);
        assertThat(energyLevel.getRangeRemainingMeters().getValue()).isEqualTo(5f);
        assertThat(energyLevel.getDistanceDisplayUnit().getValue()).isEqualTo(2);
        assertThat(energyLevel.getFuelVolumeDisplayUnit().getValue()).isEqualTo(201);
    }

    @Test
    public void getEnergyLevel_SuccessfulPartialResponses() throws InterruptedException {
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
        capacities.add(CarPropertyResponse.builder().setPropertyId(
                INFO_EV_BATTERY_CAPACITY).setStatus(STATUS_SUCCESS).setValue(
                evBatteryCapacity).setTimestampMillis(1L).build());
        capacities.add(CarPropertyResponse.builder().setPropertyId(INFO_FUEL_CAPACITY).setStatus(
                STATUS_SUCCESS).setValue(fuelCapacity).setTimestampMillis(1L).build());
        ListenableFuture<List<CarPropertyResponse<?>>> future = Futures.immediateFuture(capacities);
        when(mPropertyManager.submitGetPropertyRequest(eq(mGetPropertyRequests), any())).thenReturn(
                future);

        AtomicReference<EnergyLevel> loadedResult = new AtomicReference<>();
        OnCarDataAvailableListener<EnergyLevel> listener = (data) -> {
            loadedResult.set(data);
            mCountDownLatch.countDown();
        };

        mAutomotiveCarInfo.addEnergyLevelListener(mExecutor, listener);

        // Create "EV_BATTERY_LEVEL", "FUEL_LEVEL", "FUEL_LEVEL_LOW", "RANGE_REMAINING",
        // "DISTANCE_DISPLAY_UNITS" and "FUEL_VOLUME_DISPLAY_UNITS" property IDs list with car
        // zones.
        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(EV_BATTERY_LEVEL, mCarZones)
                        .put(FUEL_LEVEL, mCarZones)
                        .put(FUEL_LEVEL_LOW, mCarZones)
                        .put(RANGE_REMAINING, mCarZones)
                        .put(DISTANCE_DISPLAY_UNITS, mCarZones)
                        .put(FUEL_VOLUME_DISPLAY_UNITS, mCarZones)
                        .buildKeepingLast();

        verify(mPropertyManager, times(1)).submitGetPropertyRequest(eq(mGetPropertyRequests),
                eq(mExecutor));
        verify(mPropertyManager, times(1)).submitRegisterListenerRequest(
                eq(propertyIdsWithCarZones), eq(DEFAULT_SAMPLE_RATE), captor.capture(),
                eq(mExecutor));

        // Missing response for FUEL_VOLUME_DISPLAY_UNITS.
        mResponse.add(CarPropertyResponse.builder().setPropertyId(EV_BATTERY_LEVEL).setStatus(
                STATUS_SUCCESS).setValue(evBatteryLevelValue).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL).setStatus(
                STATUS_SUCCESS).setValue(fuelLevelValue).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(FUEL_LEVEL_LOW).setStatus(
                STATUS_SUCCESS).setValue(true).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(RANGE_REMAINING).setStatus(
                STATUS_SUCCESS).setValue(5f).setTimestampMillis(1L).build());
        mResponse.add(CarPropertyResponse.builder().setPropertyId(DISTANCE_DISPLAY_UNITS).setStatus(
                STATUS_SUCCESS).setValue(meterDistanceUnit).setTimestampMillis(1L).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        // The partial responses will be returned successfully.
        EnergyLevel energyLevel = loadedResult.get();
        assertThat(energyLevel.getBatteryPercent().getValue()).isEqualTo(
                evBatteryLevelValue / evBatteryCapacity * 100);
        assertThat(energyLevel.getFuelPercent().getValue()).isEqualTo(
                fuelLevelValue / fuelCapacity * 100);
        assertThat(energyLevel.getEnergyIsLow().getValue()).isEqualTo(true);
        assertThat(energyLevel.getRangeRemainingMeters().getValue()).isEqualTo(5f);
        assertThat(energyLevel.getDistanceDisplayUnit().getValue()).isEqualTo(2);
    }
}
