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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.os.SystemClock;

import androidx.car.app.shadows.car.ShadowCar;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

/**
 * Base class for testing with mocked car.
 */
@Config(
        manifest = Config.NONE,
        shadows = {ShadowCar.class}
)
@DoNotInstrument
public class MockedCarTestBase {
    public static final int MODEL_YEAR = 2021;
    public static final String MODEL_NAME = "car_name";
    public static final String MODEL_MAKER = "android";
    public static final boolean FUEL_DOOR_DEFAULT = true;

    @Mock
    private CarPropertyValue<Integer> mModelYearValueMock;
    @Mock
    private CarPropertyValue<String> mModelNameValueMock;
    @Mock
    private CarPropertyValue<String> mManufacturerValueMock;
    @Mock
    private CarPropertyConfig<Integer> mModelYearConfigMock;
    @Mock
    private CarPropertyConfig<String> mModelNameConfigMock;
    @Mock
    private CarPropertyConfig<String> mManufacturerConfigMock;
    @Mock
    private CarPropertyConfig<Boolean> mFuelDoorConfigMock;
    @Mock
    private CarPropertyValue<Boolean> mFuelDoorValueMock;
    @Mock
    private Car mCarMock;
    @Mock
    private CarPropertyManager mCarPropertyManagerMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Mock car
        ShadowCar.setCar(mCarMock);
        when(mCarMock.getCarManager(anyString())).thenReturn(mCarPropertyManagerMock);

        // Mocks car property manager
        doReturn(Collections.singletonList(mModelYearConfigMock)).when(mCarPropertyManagerMock)
                .getPropertyList(
                        argThat((set) -> set.contains(VehiclePropertyIds.INFO_MODEL_YEAR)));
        doReturn(mModelYearValueMock).when(mCarPropertyManagerMock).getProperty(
                any(), eq(VehiclePropertyIds.INFO_MODEL_YEAR), anyInt());
        doReturn(Collections.singletonList(mManufacturerConfigMock)).when(mCarPropertyManagerMock)
                .getPropertyList(argThat((set) -> set.contains(VehiclePropertyIds.INFO_MAKE)));
        doReturn(mManufacturerValueMock).when(mCarPropertyManagerMock).getProperty(
                any(), eq(VehiclePropertyIds.INFO_MAKE), anyInt());
        doReturn(Collections.singletonList(mModelNameConfigMock)).when(mCarPropertyManagerMock)
                .getPropertyList(argThat((set) -> set.contains(VehiclePropertyIds.INFO_MODEL)));
        doReturn(mModelNameValueMock).when(mCarPropertyManagerMock).getProperty(
                any(), eq(VehiclePropertyIds.INFO_MODEL), anyInt());

        // Sets up property configs
        when(mModelYearConfigMock.getPropertyType()).thenReturn(Integer.class);
        when(mModelNameConfigMock.getPropertyType()).thenReturn(String.class);
        when(mManufacturerConfigMock.getPropertyType()).thenReturn(String.class);

        // Sets up property values
        when(mModelYearValueMock.getPropertyId()).thenReturn(VehiclePropertyIds.INFO_MODEL_YEAR);
        when(mModelYearValueMock.getValue()).thenReturn(MODEL_YEAR);
        when(mModelYearValueMock.getStatus()).thenReturn(CarPropertyValue.STATUS_AVAILABLE);
        when(mModelYearValueMock.getTimestamp()).thenReturn(SystemClock.elapsedRealtimeNanos());
        when(mModelNameValueMock.getPropertyId()).thenReturn(VehiclePropertyIds.INFO_MODEL);
        when(mModelNameValueMock.getValue()).thenReturn(MODEL_NAME);
        when(mModelNameValueMock.getStatus()).thenReturn(CarPropertyValue.STATUS_ERROR);
        when(mModelNameValueMock.getTimestamp()).thenReturn(SystemClock.elapsedRealtimeNanos());
        when(mManufacturerValueMock.getPropertyId()).thenReturn(VehiclePropertyIds.INFO_MAKE);
        when(mManufacturerValueMock.getValue()).thenReturn(MODEL_MAKER);
        when(mManufacturerValueMock.getStatus()).thenReturn(CarPropertyValue.STATUS_UNAVAILABLE);
        when(mManufacturerValueMock.getTimestamp()).thenReturn(SystemClock.elapsedRealtimeNanos());

        // Adds fuel_door config and value for testing permission
        doReturn(mFuelDoorConfigMock).when(mCarPropertyManagerMock)
                .getCarPropertyConfig(eq(VehiclePropertyIds.FUEL_DOOR_OPEN));
        doReturn(mFuelDoorValueMock).when(mCarPropertyManagerMock).getProperty(
                any(), eq(VehiclePropertyIds.FUEL_DOOR_OPEN), anyInt());
        when(mFuelDoorConfigMock.getPropertyType()).thenReturn(Boolean.class);
        when(mFuelDoorValueMock.getPropertyId()).thenReturn(VehiclePropertyIds.FUEL_DOOR_OPEN);
        when(mFuelDoorValueMock.getValue()).thenReturn(FUEL_DOOR_DEFAULT);
        when(mFuelDoorValueMock.getStatus()).thenReturn(CarPropertyValue.STATUS_AVAILABLE);
        when(mFuelDoorValueMock.getTimestamp()).thenReturn(SystemClock.elapsedRealtimeNanos());
    }
}
