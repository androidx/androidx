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
import static android.car.VehiclePropertyIds.HVAC_POWER_ON;

import static androidx.car.app.hardware.climate.AutomotiveCarClimate.DEFAULT_SAMPLE_RATE_HZ;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_AC;
import static androidx.car.app.hardware.climate.ClimateProfileRequest.FEATURE_HVAC_POWER;
import static androidx.car.app.hardware.common.CarValue.STATUS_SUCCESS;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.hardware.property.CarPropertyManager;

import androidx.annotation.NonNull;
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, shadows = {ShadowCar.class})
@DoNotInstrument
public class AutomotiveCarClimateTest {
    @Rule
    public GrantPermissionRule mPermissionsRule = GrantPermissionRule.grant(
            "android.car.permission.CONTROL_CAR_CLIMATE");
    private List<CarPropertyResponse<?>> mResponse;
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
        MockitoAnnotations.initMocks(this);
        ShadowCar.setCar(mCarMock);
        when(mCarMock.getCarManager(anyString())).thenReturn(mCarPropertyManagerMock);
        mAutomotiveCarClimate = new AutomotiveCarClimate(mPropertyManager);
        mCountDownLatch = new CountDownLatch(1);
        mResponse = new ArrayList<>();
        mCarZone = new CarZone.Builder().build();
    }

    @Test
    public void registerHvacPower_verifyResponse() throws InterruptedException {
        CarClimateFeature.Builder mCarClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_POWER);
        mCarClimateBuilder.addCarZones(mCarZone);
        CarClimateFeature mCarClimateFeature = new CarClimateFeature(mCarClimateBuilder);
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
    public void fetchHvacPower_verifyResponse() throws InterruptedException {
        CarZone frontLeft = new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_LEFT).build();
        CarZone frontRight = new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_RIGHT).build();
        List<CarZone> carZones = new ArrayList<>();
        carZones.add(frontLeft);
        carZones.add(frontRight);
        List<Integer> propertyIds = Collections.singletonList(HVAC_POWER_ON);
        mResponse.add(CarPropertyResponse.builder().setPropertyId(HVAC_POWER_ON).setCarZones(
                carZones).setValue(true).setStatus(
                STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                        listenableCarPropertyResponse);

        AtomicReference<List<CarZone>> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacPowerProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
                loadedResult.set(supportedCarZones);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder mCarClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_POWER);
        mCarClimateBuilder.addCarZones(frontLeft);
        mCarClimateBuilder.addCarZones(frontRight);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(mCarClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(carZones);
    }

    @Test
    public void fetchHvacAC_verifyResponse() throws InterruptedException {
        CarZone frontLeft = new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_LEFT).build();
        CarZone frontRight = new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_RIGHT).build();
        List<CarZone> carZones = new ArrayList<>();
        carZones.add(frontLeft);
        carZones.add(frontRight);
        List<Integer> propertyIds = Collections.singletonList(HVAC_AC_ON);
        mResponse.add(CarPropertyResponse.builder().setPropertyId(HVAC_AC_ON).setCarZones(
                carZones).setValue(true).setStatus(
                STATUS_SUCCESS).build());
        ListenableFuture<List<CarPropertyResponse<?>>> listenableCarPropertyResponse =
                Futures.immediateFuture(mResponse);
        when(mPropertyManager.fetchSupportedZonesResponse(
                eq(propertyIds), eq(mExecutor))).thenReturn(
                listenableCarPropertyResponse);

        AtomicReference<List<CarZone>> loadedResult = new AtomicReference<>();
        CarClimateProfileCallback listener = new CarClimateProfileCallback() {
            @Override
            public void onHvacAcProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
                loadedResult.set(supportedCarZones);
                mCountDownLatch.countDown();
            }
        };

        CarClimateFeature.Builder mCarClimateBuilder = new CarClimateFeature.Builder(
                FEATURE_HVAC_AC);
        mCarClimateBuilder.addCarZones(frontLeft);
        mCarClimateBuilder.addCarZones(frontRight);

        CarClimateFeature mCarClimateFeature = new CarClimateFeature(mCarClimateBuilder);
        ClimateProfileRequest.Builder builder =
                new ClimateProfileRequest.Builder();
        builder.addClimateProfileFeatures(mCarClimateFeature);
        mAutomotiveCarClimate.fetchClimateProfile(mExecutor, builder.build(), listener);
        verify(mPropertyManager, times(1)).fetchSupportedZonesResponse(
                eq(propertyIds),
                eq(mExecutor));
        mCountDownLatch.await();

        assertThat(loadedResult.get()).isEqualTo(carZones);
    }
}
