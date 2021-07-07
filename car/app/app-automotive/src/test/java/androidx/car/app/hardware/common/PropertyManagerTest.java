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

import static org.junit.Assert.assertThrows;

import android.app.Application;
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.util.SparseArray;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PropertyManagerTest extends MockedCarTestBase {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private PropertyManager mPropertyManager;

    @Before
    public void setUp() {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        // Sets application's permission
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = Shadows.shadowOf(application);
        shadowApplication.grantPermissions(Car.PERMISSION_CAR_INFO);
        mPropertyManager = new PropertyManager(ApplicationProvider.getApplicationContext());
    }

    /**
     * Tests {@link PropertyManager#submitGetPropertyRequest(List, Executor)} with single request.
     */
    @Test
    public void submitGetPropertyRequestTest_singleProperty() throws Exception {
        List<GetPropertyRequest> requests = new ArrayList<>();
        requests.add(GetPropertyRequest.create(VehiclePropertyIds.INFO_MODEL_YEAR));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(requests, mExecutor);

        List<CarPropertyResponse<?>> responses = future.get();
        CarPropertyResponse<?> infoYearResponse = responses.get(0);

        assertThat(responses.size()).isEqualTo(requests.size());
        assertThat(infoYearResponse.getPropertyId()).isEqualTo(VehiclePropertyIds.INFO_MODEL_YEAR);
        assertThat(infoYearResponse.getValue()).isEqualTo(MODEL_YEAR);
        assertThat(infoYearResponse.getStatus()).isEqualTo(CarValue.STATUS_SUCCESS);
    }

    /**
     * Tests {@link PropertyManager#submitGetPropertyRequest(List, Executor)} multi requests.
     */
    @Test
    public void submitGetPropertyRequestTest_multipleProperties() throws Exception {
        List<GetPropertyRequest> requests = new ArrayList<>();
        requests.add(GetPropertyRequest.create(VehiclePropertyIds.INFO_MODEL_YEAR));
        requests.add(GetPropertyRequest.create(VehiclePropertyIds.INFO_MODEL));
        requests.add(GetPropertyRequest.create(VehiclePropertyIds.INFO_MAKE));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(requests, mExecutor);

        List<CarPropertyResponse<?>> responses = future.get();
        SparseArray<CarPropertyResponse<?>> responsesMap = getCarPropertyResponseMap(responses);
        CarPropertyResponse<?> infoMakerResponse = responsesMap.get(VehiclePropertyIds.INFO_MAKE);
        CarPropertyResponse<?> infoModelResponse = responsesMap.get(VehiclePropertyIds.INFO_MODEL);
        CarPropertyResponse<?> infoYearResponse =
                responsesMap.get(VehiclePropertyIds.INFO_MODEL_YEAR);

        assertThat(responses.size()).isEqualTo(requests.size());
        assertThat(infoMakerResponse.getValue()).isEqualTo(MODEL_MAKER);
        assertThat(infoMakerResponse.getStatus()).isEqualTo(CarValue.STATUS_UNAVAILABLE);
        assertThat(infoModelResponse.getValue()).isEqualTo(MODEL_NAME);
        assertThat(infoModelResponse.getStatus()).isEqualTo(CarValue.STATUS_UNKNOWN);
        assertThat(infoYearResponse.getValue()).isEqualTo(MODEL_YEAR);
        assertThat(infoYearResponse.getStatus()).isEqualTo(CarValue.STATUS_SUCCESS);
    }

    /**
     * Tests get property without permissions should throw exception
     */
    @Test
    public void submitGetPropertyRequestTest_withoutPermission() {
        List<GetPropertyRequest> requests = new ArrayList<>();
        requests.add(GetPropertyRequest.create(VehiclePropertyIds.FUEL_DOOR_OPEN));

        assertThrows(SecurityException.class, () ->
                mPropertyManager.submitGetPropertyRequest(requests, mExecutor));
    }

    /**
     * Gets an unimplemented property
     */
    @Test
    public void submitGetPropertyRequestTest_unimplementedProperty() throws Exception {
        List<GetPropertyRequest> requests = new ArrayList<>();
        requests.add(GetPropertyRequest.create(VehiclePropertyIds.INFO_FUEL_TYPE));

        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(requests, mExecutor);
        List<CarPropertyResponse<?>> responses = future.get();
        CarPropertyResponse<?> response = responses.get(0);

        assertThat(response.getPropertyId()).isEqualTo(VehiclePropertyIds.INFO_FUEL_TYPE);
        assertThat(responses.size()).isEqualTo(requests.size());
        assertThat(response.getValue()).isNull();
        assertThat(response.getStatus()).isEqualTo(CarValue.STATUS_UNIMPLEMENTED);
    }

    private static SparseArray<CarPropertyResponse<?>> getCarPropertyResponseMap(
            List<CarPropertyResponse<?>> responses) {
        SparseArray<CarPropertyResponse<?>> responseSparseArray = new SparseArray<>();
        for (CarPropertyResponse<?> response : responses) {
            responseSparseArray.put(response.getPropertyId(), response);
        }
        return responseSparseArray;
    }
}
