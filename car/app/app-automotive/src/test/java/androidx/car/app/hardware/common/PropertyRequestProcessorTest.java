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

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.car.Car;
import android.car.VehicleAreaType;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;
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
        List<Pair<Integer, Integer>> requests = new ArrayList<>();

        //INFO_MODEL is implemented, ENGINE_OIL_TEMP is not implemented.
        requests.add(new Pair<>(VehiclePropertyIds.INFO_MODEL,
                VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL));
        requests.add(new Pair<>(VehiclePropertyIds.ENGINE_OIL_TEMP,
                VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL));
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

    private static class DefaultCallback extends PropertyRequestProcessor.PropertyEventCallback {

        @Override
        public void onChangeEvent(CarPropertyValue carPropertyValue) {
        }

        @Override
        public void onErrorEvent(CarInternalError carInternalError) {
        }
    }
}
