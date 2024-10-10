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

import static android.car.VehiclePropertyIds.HVAC_AC_ON;
import static android.car.VehiclePropertyIds.HVAC_AUTO_ON;
import static android.car.VehiclePropertyIds.HVAC_AUTO_RECIRC_ON;
import static android.car.VehiclePropertyIds.HVAC_DEFROSTER;
import static android.car.VehiclePropertyIds.HVAC_DUAL_ON;
import static android.car.VehiclePropertyIds.HVAC_FAN_DIRECTION;
import static android.car.VehiclePropertyIds.HVAC_FAN_SPEED;
import static android.car.VehiclePropertyIds.HVAC_MAX_AC_ON;
import static android.car.VehiclePropertyIds.HVAC_MAX_DEFROST_ON;
import static android.car.VehiclePropertyIds.HVAC_POWER_ON;
import static android.car.VehiclePropertyIds.HVAC_RECIRC_ON;
import static android.car.VehiclePropertyIds.HVAC_SEAT_TEMPERATURE;
import static android.car.VehiclePropertyIds.HVAC_SEAT_VENTILATION;
import static android.car.VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT;
import static android.car.VehiclePropertyIds.HVAC_TEMPERATURE_SET;

import static androidx.car.app.hardware.climate.AutomotiveCarClimate.DEFAULT_SAMPLE_RATE_HZ;
import static androidx.car.app.hardware.climate.AutomotiveCarClimate.HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_CABIN_TEMPERATURE;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_FAN_DIRECTION;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_FAN_SPEED;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AC;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AUTO_MODE;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AUTO_RECIRCULATION;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_DEFROSTER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_DUAL_MODE;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_ELECTRIC_DEFROSTER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_MAX_AC;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_MAX_DEFROSTER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_POWER;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_RECIRCULATION;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_SEAT_TEMPERATURE_LEVEL;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_SEAT_VENTILATION_LEVEL;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_STEERING_WHEEL_HEAT;
import static androidx.car.app.hardware.common.CarValue.STATUS_SUCCESS;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.hardware.property.CarPropertyManager;
import android.util.Pair;

import androidx.car.app.hardware.common.CarPropertyProfile;
import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.CarZone;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.shadows.car.ShadowCar;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowCar.class})
@DoNotInstrument
public class AutomotiveCarClimateTest {
    private static final CarZone FRONT_LEFT_ZONE = new CarZone.Builder()
            .setRow(CarZone.CAR_ZONE_ROW_FIRST)
            .setColumn(CarZone.CAR_ZONE_COLUMN_LEFT).build();
    private static final CarZone FRONT_RIGHT_ZONE = new CarZone.Builder()
            .setRow(CarZone.CAR_ZONE_ROW_FIRST)
            .setColumn(CarZone.CAR_ZONE_COLUMN_RIGHT).build();

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public GrantPermissionRule mPermissionsRule = GrantPermissionRule.grant(
            "android.car.permission.CONTROL_CAR_CLIMATE");
    private List<CarPropertyResponse<?>> mResponse;
    private List<CarPropertyProfile<?>> mCarPropertyProfiles;
    private CountDownLatch mCountDownLatch;
    private final Executor mExecutor = directExecutor();
    private AutomotiveCarClimate mAutomotiveCarClimate;
    private CarZone mCarZone;
    @Mock
    private Car mCarMock;
    @Mock
    private CarPropertyManager mCarPropertyManagerMock;
    @Mock
    private PropertyManager mPropertyManager;

    @Before
    public void setUp() {
        ShadowCar.setCar(mCarMock);
        when(mCarMock.getCarManager(anyString())).thenReturn(mCarPropertyManagerMock);
        mAutomotiveCarClimate = new AutomotiveCarClimate(mPropertyManager);
        mCountDownLatch = new CountDownLatch(1);
        mResponse = new ArrayList<>();
        mCarPropertyProfiles = new ArrayList<>();
        mCarZone = new CarZone.Builder().build();
    }

    @Test
    public void registerHvacPower_verifyResponse() throws InterruptedException {
        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_POWER);
        carClimateBuilder.addCarZones(mCarZone);
        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        RegisterClimateStateRequest.Builder builder =
                new RegisterClimateStateRequest.Builder(false);
        builder.addClimateRegisterFeatures(mCarClimateFeature);

        AtomicReference<CarValue<Boolean>> loadedResult = new AtomicReference<>();
        CarClimateStateCallback listener = new CarClimateStateCallback() {
            @Override
            public void onHvacPowerStateAvailable(@NonNull CarValue<Boolean> hvacPowerState) {
                loadedResult.set(hvacPowerState);
                mCountDownLatch.countDown();
            }
        };

        mAutomotiveCarClimate.registerClimateStateCallback(mExecutor, builder.build(), listener);

        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder().put(HVAC_POWER_ON,
                        Collections.singletonList(mCarZone)).buildKeepingLast();

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE_HZ), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(HVAC_POWER_ON).setCarZones(
                Collections.singletonList(mCarZone)).setValue(true).setStatus(
                STATUS_SUCCESS).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        CarValue<Boolean> carValue = loadedResult.get();
        assertThat(carValue.getValue()).isEqualTo(true);
        assertThat(carValue.getCarZones()).isEqualTo(Collections.singletonList(mCarZone));
        assertThat(carValue.getStatus()).isEqualTo(STATUS_SUCCESS);
    }

    @Test
    public void registerElectricDefroster_verifyResponse() throws InterruptedException {
        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_ELECTRIC_DEFROSTER);
        carClimateBuilder.addCarZones(mCarZone);
        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        RegisterClimateStateRequest.Builder builder =
                new RegisterClimateStateRequest.Builder(false);
        builder.addClimateRegisterFeatures(mCarClimateFeature);

        AtomicReference<CarValue<Boolean>> loadedResult = new AtomicReference<>();
        CarClimateStateCallback listener = new CarClimateStateCallback() {
            @Override
            public void onElectricDefrosterStateAvailable(
                    @NonNull CarValue<Boolean> electricDefrosterState) {
                loadedResult.set(electricDefrosterState);
                mCountDownLatch.countDown();
            }
        };

        mAutomotiveCarClimate.registerClimateStateCallback(mExecutor, builder.build(), listener);

        Map<Integer, List<CarZone>> propertyIdsWithCarZones =
                ImmutableMap.<Integer, List<CarZone>>builder()
                        .put(HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID,
                                Collections.singletonList(mCarZone))
                        .buildKeepingLast();

        ArgumentCaptor<OnCarPropertyResponseListener> captor = ArgumentCaptor.forClass(
                OnCarPropertyResponseListener.class);
        verify(mPropertyManager).submitRegisterListenerRequest(eq(propertyIdsWithCarZones),
                eq(DEFAULT_SAMPLE_RATE_HZ), captor.capture(), eq(mExecutor));

        mResponse.add(CarPropertyResponse.builder().setPropertyId(
                        HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID)
                .setCarZones(Collections.singletonList(mCarZone)).setValue(true).setStatus(
                        STATUS_SUCCESS).build());

        captor.getValue().onCarPropertyResponses(mResponse);
        mCountDownLatch.await();

        CarValue<Boolean> carValue = loadedResult.get();
        assertThat(carValue.getValue()).isEqualTo(true);
        assertThat(carValue.getCarZones()).isEqualTo(Collections.singletonList(mCarZone));
        assertThat(carValue.getStatus()).isEqualTo(STATUS_SUCCESS);
    }

    @Test
    public void fetchHvacPower_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_POWER_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_POWER_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacPowerProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacPowerProfileAvailable(@NonNull HvacPowerProfile hvacPowerProfile) {
                loadedResult.set(hvacPowerProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_POWER);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchCabinConfigArrayTemperature_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_TEMPERATURE_SET);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_TEMPERATURE_SET)
                .setCelsiusIncrement(1f)
                .setCelsiusRange(new Pair<>(16.0f, 18.0f))
                .setFahrenheitRange(new Pair<>(60.5f, 64.5f))
                .setFahrenheitIncrement(0.5f)
                .setCarZoneSetsToMinMaxRange(null)
                .setCarZones(carZones)
                .setStatus(STATUS_SUCCESS)
                .build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfiles =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfiles);

        AtomicReference<CabinTemperatureProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onCabinTemperatureProfileAvailable(@NonNull CabinTemperatureProfile
                    cabinTemperatureProfile) {
                loadedResult.set(cabinTemperatureProfile);
                CabinTemperatureProfile response = loadedResult.get();
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_CABIN_TEMPERATURE);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        CabinTemperatureProfile cabinTemperatureProfile = loadedResult.get();
        assertThat(cabinTemperatureProfile.getSupportedMinMaxCelsiusRange())
                .isEqualTo(new Pair<>(16.0f, 18.0f));
        assertThat(cabinTemperatureProfile.getSupportedMinMaxFahrenheitRange())
                .isEqualTo(new Pair<>(60.5f, 64.5f));
        assertThat(cabinTemperatureProfile.getCelsiusSupportedIncrement())
                .isEqualTo(1f);
        assertThat(cabinTemperatureProfile.getFahrenheitSupportedIncrement())
                .isEqualTo(0.5f);
        assertThrows(IllegalStateException.class,
                cabinTemperatureProfile::getCarZoneSetsToCabinCelsiusTemperatureRanges);
    }

    @Test
    public void fetchMinMaxCabinTemperature_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_TEMPERATURE_SET);
        Map<Set<CarZone>, Pair<Object, Object>> requestMinMaxValueMap = new HashMap<>();
        requestMinMaxValueMap.put(Collections.singleton(FRONT_LEFT_ZONE), new Pair<>(16f, 32f));
        requestMinMaxValueMap.put(Collections.singleton(FRONT_RIGHT_ZONE), new Pair<>(16f, 32f));
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_TEMPERATURE_SET)
                .setCelsiusRange(null)
                .setFahrenheitRange(null)
                .setCelsiusIncrement(-1f)
                .setFahrenheitIncrement(-1f)
                .setCarZoneSetsToMinMaxRange(requestMinMaxValueMap)
                .setCarZones(carZones)
                .setStatus(STATUS_SUCCESS)
                .build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfiles =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfiles);

        AtomicReference<CabinTemperatureProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onCabinTemperatureProfileAvailable(@NonNull CabinTemperatureProfile
                    cabinTemperatureProfile) {
                loadedResult.set(cabinTemperatureProfile);
                CabinTemperatureProfile response = loadedResult.get();
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder mCarClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_CABIN_TEMPERATURE);
        mCarClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        mCarClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(mCarClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        CabinTemperatureProfile cabinTemperatureProfile = loadedResult.get();
        Map<Set<CarZone>, Pair<Object, Object>> responseMinMaxValueMap = new HashMap<>();
        responseMinMaxValueMap.put(Collections.singleton(FRONT_LEFT_ZONE),
                new Pair<>(16.0f, 32.0f));
        responseMinMaxValueMap.put(Collections.singleton(FRONT_RIGHT_ZONE),
                new Pair<>(16.0f, 32.0f));
        assertThat(cabinTemperatureProfile.getCarZoneSetsToCabinCelsiusTemperatureRanges())
                .isEqualTo(responseMinMaxValueMap);
        assertThrows(IllegalStateException.class,
                cabinTemperatureProfile::getSupportedMinMaxCelsiusRange);
        assertThrows(IllegalStateException.class,
                cabinTemperatureProfile::getSupportedMinMaxFahrenheitRange);
        assertThrows(IllegalStateException.class,
                cabinTemperatureProfile::getCelsiusSupportedIncrement);
        assertThrows(IllegalStateException.class,
                cabinTemperatureProfile::getFahrenheitSupportedIncrement);
    }

    @Test
    public void fetchFanSpeed_verifyResponse() throws InterruptedException {
        Map<Set<CarZone>, Pair<Object, Object>> minMaxValueMap = new HashMap<>();
        minMaxValueMap.put(Collections.singleton(FRONT_LEFT_ZONE), new Pair<>(1, 7));
        minMaxValueMap.put(Collections.singleton(FRONT_RIGHT_ZONE), new Pair<>(2, 6));
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_FAN_SPEED);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_FAN_SPEED)
                .setCarZones(carZones).setCarZoneSetsToMinMaxRange(minMaxValueMap)
                .setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(listenableCarPropertyProfile);

        AtomicReference<FanSpeedLevelProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onFanSpeedLevelProfileAvailable(@NonNull FanSpeedLevelProfile
                    fanSpeedLevelProfile) {
                loadedResult.set(fanSpeedLevelProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_FAN_SPEED);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getCarZoneSetsToFanSpeedLevelRanges()).isEqualTo(
                minMaxValueMap);
    }

    @Test
    public void fetchFanDirection_verifyResponse() throws InterruptedException {
        Map<Set<CarZone>, Set<Integer>> fanDirectionValues = new HashMap<>();
        fanDirectionValues.put(Collections.singleton(FRONT_LEFT_ZONE), Collections.singleton(1));
        fanDirectionValues.put(Collections.singleton(FRONT_RIGHT_ZONE), Collections.singleton(6));
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_FAN_DIRECTION)
                .setCarZones(carZones).setHvacFanDirection(fanDirectionValues)
                .setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        List<Integer> propertyIds = Collections.singletonList(HVAC_FAN_DIRECTION);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<FanDirectionProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onFanDirectionProfileAvailable(@NonNull FanDirectionProfile
                    fanDirectionProfile) {
                loadedResult.set(fanDirectionProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_FAN_DIRECTION);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getCarZoneSetsToFanDirectionValues()).isEqualTo(
                fanDirectionValues);
    }

    @Test
    public void fetchSeatTemperature_verifyResponse() throws InterruptedException {
        Map<Set<CarZone>, Pair<Object, Object>> minMaxValueMap = new HashMap<>();
        minMaxValueMap.put(Collections.singleton(FRONT_LEFT_ZONE), new Pair<>(1, 7));
        minMaxValueMap.put(Collections.singleton(FRONT_RIGHT_ZONE), new Pair<>(2, 6));
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_SEAT_TEMPERATURE)
                .setCarZones(carZones).setCarZoneSetsToMinMaxRange(minMaxValueMap)
                .setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        List<Integer> propertyIds = Collections.singletonList(HVAC_SEAT_TEMPERATURE);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<SeatTemperatureProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onSeatTemperatureLevelProfileAvailable(@NonNull SeatTemperatureProfile
                    supportedCarZonesAndFanDirectionLevels) {
                loadedResult.set(supportedCarZonesAndFanDirectionLevels);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_SEAT_TEMPERATURE_LEVEL);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getCarZoneSetsToSeatTemperatureValues()).isEqualTo(
                minMaxValueMap);
    }

    @Test
    public void fetchSeatVentilation_verifyResponse() throws InterruptedException {
        Map<Set<CarZone>, Pair<Object, Object>> minMaxValueMap = new HashMap<>();
        minMaxValueMap.put(Collections.singleton(FRONT_LEFT_ZONE), new Pair<>(1, 7));
        minMaxValueMap.put(Collections.singleton(FRONT_RIGHT_ZONE), new Pair<>(2, 6));
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_SEAT_VENTILATION)
                .setCarZones(carZones).setCarZoneSetsToMinMaxRange(minMaxValueMap)
                .setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        List<Integer> propertyIds = Collections.singletonList(HVAC_SEAT_VENTILATION);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<SeatVentilationProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onSeatVentilationLevelProfileAvailable(@NonNull SeatVentilationProfile
                    supportedCarZonesAndSeatVentilationLevels) {
                loadedResult.set(supportedCarZonesAndSeatVentilationLevels);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_SEAT_VENTILATION_LEVEL);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getCarZoneSetsToSeatVentilationValues()).isEqualTo(
                minMaxValueMap);
    }

    @Test
    public void fetchSteeringWheelHeat_verifyResponse() throws InterruptedException {
        Map<Set<CarZone>, Pair<Object, Object>> minMaxValueMap = new HashMap<>();
        minMaxValueMap.put(Collections.singleton(FRONT_LEFT_ZONE), new Pair<>(1, 7));
        minMaxValueMap.put(Collections.singleton(FRONT_RIGHT_ZONE), new Pair<>(2, 6));
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(
                        HVAC_STEERING_WHEEL_HEAT)
                .setCarZones(carZones).setCarZoneSetsToMinMaxRange(minMaxValueMap)
                .setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        List<Integer> propertyIds = Collections.singletonList(HVAC_STEERING_WHEEL_HEAT);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<SteeringWheelHeatProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onSteeringWheelHeatProfileAvailable(@NonNull SteeringWheelHeatProfile
                    steeringWheelHeatProfile) {
                loadedResult.set(steeringWheelHeatProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_STEERING_WHEEL_HEAT);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getCarZoneSetsToSteeringWheelHeatValues()).isEqualTo(
                minMaxValueMap);
    }

    @Test
    public void fetchHvacRecirculation_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_RECIRC_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        List<Integer> propertyIds = Collections.singletonList(HVAC_RECIRC_ON);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacRecirculationProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacRecirculationProfileAvailable(@NonNull HvacRecirculationProfile
                    hvacRecirculationProfile) {
                loadedResult.set(hvacRecirculationProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_RECIRCULATION);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZones()).isEqualTo(carZones);
    }

    @Test
    public void fetchHvacAutoRecirculation_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_AUTO_RECIRC_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_AUTO_RECIRC_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacAutoRecirculationProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacAutoRecirculationProfileAvailable(
                    @NonNull HvacAutoRecirculationProfile hvacAutoRecirculationProfile) {
                loadedResult.set(hvacAutoRecirculationProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_AUTO_RECIRCULATION);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchHvacAutoMode_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_AUTO_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_AUTO_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacAutoModeProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacAutoModeProfileAvailable(@NonNull HvacAutoModeProfile
                    hvacAutoModeProfile) {
                loadedResult.set(hvacAutoModeProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_AUTO_MODE);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchHvacDualMode_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_DUAL_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_DUAL_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacDualModeProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacDualModeProfileAvailable(@NonNull HvacDualModeProfile
                    hvacDualModeProfile) {
                loadedResult.set(hvacDualModeProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_DUAL_MODE);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchDefroster_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_DEFROSTER);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_DEFROSTER)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<DefrosterProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onDefrosterProfileAvailable(@NonNull DefrosterProfile defrosterProfile) {
                loadedResult.set(defrosterProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_DEFROSTER);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchMaxDefroster_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_MAX_DEFROST_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_MAX_DEFROST_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<MaxDefrosterProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onMaxDefrosterProfileAvailable(@NonNull MaxDefrosterProfile
                    maxDefrosterProfile) {
                loadedResult.set(maxDefrosterProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_MAX_DEFROSTER);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchElectricDefroster_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(
                HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(
                        HVAC_ELECTRIC_DEFROSTER_ON_PROPERTY_ID)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<ElectricDefrosterProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onElectricDefrosterProfileAvailable(@NonNull ElectricDefrosterProfile
                    electricDefrosterProfile) {
                loadedResult.set(electricDefrosterProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_ELECTRIC_DEFROSTER);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchHvacAc_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_AC_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_AC_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacAcProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacAcProfileAvailable(@NonNull HvacAcProfile
                    hvacAcProfile) {
                loadedResult.set(hvacAcProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_AC);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }

    @Test
    public void fetchHvacMaxAc_verifyResponse() throws InterruptedException {
        List<Set<CarZone>> carZones = new ArrayList<>();
        carZones.add(Collections.singleton(FRONT_LEFT_ZONE));
        carZones.add(Collections.singleton(FRONT_RIGHT_ZONE));
        List<Integer> propertyIds = Collections.singletonList(HVAC_MAX_AC_ON);
        mCarPropertyProfiles.add(CarPropertyProfile.builder().setPropertyId(HVAC_MAX_AC_ON)
                .setCarZones(carZones).setStatus(STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyProfile<?>>> listenableCarPropertyProfile =
                Futures.immediateFuture(mCarPropertyProfiles);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyProfile);

        AtomicReference<HvacMaxAcModeProfile> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacMaxAcModeProfileAvailable(@NonNull HvacMaxAcModeProfile
                    hvacMaxAcModeProfile) {
                loadedResult.set(hvacMaxAcModeProfile);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder carClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_MAX_AC);
        carClimateBuilder.addCarZones(FRONT_LEFT_ZONE);
        carClimateBuilder.addCarZones(FRONT_RIGHT_ZONE);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(carClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get().getSupportedCarZoneSets()).isEqualTo(carZones);
    }
}
