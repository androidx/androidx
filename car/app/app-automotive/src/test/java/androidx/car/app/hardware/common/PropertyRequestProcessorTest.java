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

package androidx.car.app.hardware.common;

import static android.car.VehiclePropertyIds.HVAC_POWER_ON;
import static android.car.VehiclePropertyIds.HVAC_TEMPERATURE_SET;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.car.Car;
import android.car.VehicleAreaType;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PropertyRequestProcessorTest extends MockedCarTestBase {
    private static final int WAIT_CALLBACK_MS = 50;
    private PropertyRequestProcessor mRequestProcessor;
    @Before
    public void setUp() {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        // Sets application's permission
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = Shadows.shadowOf(application);
        shadowApplication.grantPermissions(Car.PERMISSION_CAR_INFO);
        mRequestProcessor = new PropertyRequestProcessor(application, new DefaultCallback());
    }

    /**
     * Tests for
     * {@link PropertyRequestProcessor#fetchCarPropertyValues(
     * List, PropertyRequestProcessor.OnGetPropertiesListener)}.
     */
    @Test
    public void fetchCarPropertyValuesTest() throws Exception {
        TestListener listener = new TestListener();
        List<PropertyIdAreaId> requests = new ArrayList<>();

        //INFO_MODEL is implemented, ENGINE_OIL_TEMP is not implemented.
        requests.add(PropertyIdAreaId.builder()
                .setAreaId(VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
                .setPropertyId(VehiclePropertyIds.INFO_MODEL)
                .build());
        requests.add(PropertyIdAreaId.builder()
                .setAreaId(VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
                .setPropertyId(VehiclePropertyIds.ENGINE_OIL_TEMP)
                .build());
        mRequestProcessor.fetchCarPropertyValues(requests, listener);
        listener.assertOnGetPropertiesCalled();
        List<CarInternalError> internalErrors = listener.getCarInternalErrors();
        List<CarPropertyValue<?>> propertyValues = listener.getCarPropertyValues();

        assertThat(propertyValues.size()).isEqualTo(1);
        assertThat(propertyValues.get(0).getPropertyId()).isEqualTo(VehiclePropertyIds.INFO_MODEL);
        assertThat(internalErrors.size()).isEqualTo(1);
        assertThat(internalErrors.get(0).getPropertyId()).isEqualTo(
                VehiclePropertyIds.ENGINE_OIL_TEMP);
    }

    /**
     * Tests for
     * {@link PropertyRequestProcessor#fetchCarPropertyProfiles(
     * List, OnGetCarPropertyProfilesListener)} for HVAC_POWER_ON property.
     */
    @Test
    public void fetchCarPropertyProfilesTest() throws Exception {
        TestPropertyProfileListener listener = new TestPropertyProfileListener();
        List<Integer> propertyIds = new ArrayList<>();
        ImmutableList<Set<CarZone>> carZones =
                ImmutableList.<Set<CarZone>>builder().add(Collections.singleton(
                        new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                                .setColumn(CarZone.CAR_ZONE_COLUMN_LEFT).build())).build();

        propertyIds.add(HVAC_POWER_ON);
        mRequestProcessor.fetchCarPropertyProfiles(propertyIds, listener);
        listener.assertOnGetCarPropertyProfilesCalled();
        List<CarPropertyProfile<?>> carPropertyProfiles = listener.getCarPropertyProfiles();

        assertThat(carPropertyProfiles.size()).isEqualTo(1);
        assertThat(carPropertyProfiles.get(0).getPropertyId()).isEqualTo(HVAC_POWER_ON);
        assertThat(carPropertyProfiles.get(0).getCarZones()).isEqualTo(carZones);
        assertThat(carPropertyProfiles.get(0).getCarZoneSetsToMinMaxRange())
                .isEqualTo(CAR_ZONE_SET_TO_MIN_MAX_RANGE);
    }

    /**
     * Tests for
     * {@link PropertyRequestProcessor#fetchCarPropertyProfiles(
     * List, OnGetCarPropertyProfilesListener)} for HVAC_TEMPERATURE_SET property.
     */
    @Test
    public void fetchCarPropertyCabinTemperatureProfilesTest() throws Exception {
        TestPropertyProfileListener listener = new TestPropertyProfileListener();
        List<Integer> propertyIds = new ArrayList<>();
        ImmutableList<Set<CarZone>> carZones =
                ImmutableList.<Set<CarZone>>builder().add(Collections.singleton(
                        new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                                .setColumn(CarZone.CAR_ZONE_COLUMN_LEFT).build())).build();

        propertyIds.add(HVAC_TEMPERATURE_SET);
        mRequestProcessor.fetchCarPropertyProfiles(propertyIds, listener);
        listener.assertOnGetCarPropertyProfilesCalled();
        List<CarPropertyProfile<?>> carPropertyProfiles = listener.getCarPropertyProfiles();

        assertThat(carPropertyProfiles.size()).isEqualTo(1);
        assertThat(carPropertyProfiles.get(0).getPropertyId()).isEqualTo(HVAC_TEMPERATURE_SET);
        assertThat(carPropertyProfiles.get(0).getCarZoneSetsToMinMaxRange())
                .isEqualTo(null);
        assertThat(carPropertyProfiles.get(0).getCelsiusRange())
                .isEqualTo(new Pair<>(16.0f, 28.0f));
        assertThat(carPropertyProfiles.get(0).getFahrenheitRange())
                .isEqualTo(new Pair<>(60.5f, 85.5f));
        assertThat(carPropertyProfiles.get(0).getCelsiusIncrement())
                .isEqualTo(0.5f);
        assertThat(carPropertyProfiles.get(0).getFahrenheitIncrement())
                .isEqualTo(1f);
    }

    private static class TestListener implements PropertyRequestProcessor.OnGetPropertiesListener {
        private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        private List<CarPropertyValue<?>> mCarPropertyValues = new ArrayList<>();
        private List<CarInternalError> mCarInternalErrors = new ArrayList<>();
        @Override
        public void onGetProperties(List<CarPropertyValue<?>> propertyValues,
                List<CarInternalError> errors) {
            mCarInternalErrors = errors;
            mCarPropertyValues = propertyValues;
            mCountDownLatch.countDown();
        }

        public void assertOnGetPropertiesCalled() throws InterruptedException {
            if (!mCountDownLatch.await(WAIT_CALLBACK_MS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Callback is not called in ms: "
                        + WAIT_CALLBACK_MS);
            }
        }

        public List<CarPropertyValue<?>> getCarPropertyValues() {
            return mCarPropertyValues;
        }

        public List<CarInternalError> getCarInternalErrors() {
            return mCarInternalErrors;
        }
    }

    private static class TestPropertyProfileListener implements
            PropertyRequestProcessor.OnGetCarPropertyProfilesListener {
        private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        private List<CarPropertyProfile<?>> mCarPropertyProfiles = new ArrayList<>();
        @Override
        public void onGetCarPropertyProfiles(List<CarPropertyProfile<?>> propertyProfiles) {
            mCarPropertyProfiles = propertyProfiles;
            mCountDownLatch.countDown();
        }

        public void assertOnGetCarPropertyProfilesCalled() throws InterruptedException {
            if (!mCountDownLatch.await(WAIT_CALLBACK_MS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Callback is not called in ms: "
                        + WAIT_CALLBACK_MS);
            }
        }

        public List<CarPropertyProfile<?>> getCarPropertyProfiles() {
            return mCarPropertyProfiles;
        }
    }

    private static class DefaultCallback extends PropertyRequestProcessor.PropertyEventCallback {

        @Override
        public void onChangeEvent(CarPropertyValue carPropertyValue) {
        }

        @Override
        public void onErrorEvent(CarInternalError carInternalError) {
        }
    }
}
