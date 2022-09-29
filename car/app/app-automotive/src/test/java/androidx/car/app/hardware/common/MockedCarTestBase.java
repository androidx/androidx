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

import static android.car.VehicleAreaType.VEHICLE_AREA_TYPE_SEAT;

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
import android.util.Pair;

import androidx.car.app.shadows.car.ShadowCar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

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
    public static final int[] AREA_IDS = {1};
    public static final int MIN_PROPERTY_VALUE = 1;
    public static final int MAX_PROPERTY_VALUE = 7;
    public static final ImmutableList<Set<CarZone>> CAR_ZONES =
            ImmutableList.<Set<CarZone>>builder().add(Collections.singleton(
                    new CarZone.Builder().setRow(CarZone.CAR_ZONE_ROW_FIRST)
                    .setColumn(CarZone.CAR_ZONE_COLUMN_LEFT).build())).build();
    public static final ImmutableMap<Set<CarZone>, Pair<Integer, Integer>>
            CAR_ZONE_SET_TO_MIN_MAX_RANGE = ImmutableMap.<Set<CarZone>,
                    Pair<Integer, Integer>>builder()
            .put(CAR_ZONES.get(0), new Pair<>(MIN_PROPERTY_VALUE,
                    MAX_PROPERTY_VALUE)).buildKeepingLast();
    public static final ImmutableMap<Set<CarZone>, Set<Integer>>
            CAR_ZONE_SET_TO_FAN_DIRECTION_VALUES = ImmutableMap.<Set<CarZone>,
                    Set<Integer>>builder()
            .put(CAR_ZONES.get(0), Set.of(1, 2)).buildKeepingLast();
    public static final Integer[] FAN_DIRECTION_VALUE = {1, 2};

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
    private CarPropertyConfig<Integer> mHvacPowerOnMinMaxConfigMock;
    @Mock
    private CarPropertyProfile<Integer> mHvacPowerOnMinMaxMock;
    @Mock
    private CarPropertyConfig<Integer> mCabinTempConfigMock;
    @Mock
    private CarPropertyConfig<Integer> mHvacFanDirectionConfigMock;
    @Mock
    private CarPropertyProfile<Integer> mHvacFanDirectionProfileMock;
    @Mock
    private CarPropertyConfig<Integer[]> mHvacFanDirectionAvailableConfigMock;
    @Mock
    private CarPropertyValue<Integer[]> mHvacFanDirectionAvailableValueMock;
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
        doReturn(Collections.singletonList(mHvacPowerOnMinMaxConfigMock))
                .when(mCarPropertyManagerMock).getPropertyList(
                        argThat((set) -> set.contains(VehiclePropertyIds.HVAC_POWER_ON)));
        doReturn(Collections.singletonList(mCabinTempConfigMock))
                .when(mCarPropertyManagerMock).getPropertyList(
                        argThat((set) -> set.contains(VehiclePropertyIds.HVAC_TEMPERATURE_SET)));
        doReturn(Collections.singletonList(mHvacFanDirectionConfigMock))
                .when(mCarPropertyManagerMock).getPropertyList(
                        argThat((set) -> set.contains(VehiclePropertyIds.HVAC_FAN_DIRECTION)));
        doReturn(Collections.singletonList(mHvacFanDirectionAvailableConfigMock))
                .when(mCarPropertyManagerMock).getPropertyList(
                        argThat((set) -> set.contains(
                                VehiclePropertyIds.HVAC_FAN_DIRECTION_AVAILABLE)));
        doReturn(mHvacFanDirectionAvailableValueMock).when(mCarPropertyManagerMock).getProperty(
                eq(VehiclePropertyIds.HVAC_FAN_DIRECTION_AVAILABLE), anyInt());

        // Sets up property configs
        when(mModelYearConfigMock.getPropertyType()).thenReturn(Integer.class);
        when(mModelNameConfigMock.getPropertyType()).thenReturn(String.class);
        when(mManufacturerConfigMock.getPropertyType()).thenReturn(String.class);
        when(mHvacPowerOnMinMaxConfigMock.getAreaType()).thenReturn(VEHICLE_AREA_TYPE_SEAT);
        when(mHvacPowerOnMinMaxConfigMock.getAreaIds()).thenReturn(AREA_IDS);
        when(mCabinTempConfigMock.getAreaType()).thenReturn(VEHICLE_AREA_TYPE_SEAT);
        when(mCabinTempConfigMock.getAreaIds()).thenReturn(AREA_IDS);
        when(mCabinTempConfigMock.getMinValue(AREA_IDS[0])).thenReturn(MIN_PROPERTY_VALUE);
        when(mCabinTempConfigMock.getMaxValue(AREA_IDS[0])).thenReturn(MAX_PROPERTY_VALUE);
        when(mCabinTempConfigMock.getConfigArray()).thenReturn(
                Arrays.asList(160, 280, 5, 605, 855, 10));
        when(mHvacPowerOnMinMaxConfigMock.getMinValue(AREA_IDS[0])).thenReturn(MIN_PROPERTY_VALUE);
        when(mHvacPowerOnMinMaxConfigMock.getMaxValue(AREA_IDS[0])).thenReturn(MAX_PROPERTY_VALUE);
        when(mHvacFanDirectionAvailableConfigMock.getPropertyType()).thenReturn(Integer[].class);
        when(mHvacFanDirectionAvailableConfigMock.getAreaType()).thenReturn(VEHICLE_AREA_TYPE_SEAT);
        when(mHvacFanDirectionAvailableConfigMock.getAreaIds()).thenReturn(AREA_IDS);

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
        when(mHvacPowerOnMinMaxMock.getPropertyId()).thenReturn(VehiclePropertyIds.HVAC_POWER_ON);
        when(mHvacPowerOnMinMaxMock.getCarZones()).thenReturn(CAR_ZONES);
        when(mHvacPowerOnMinMaxMock.getCarZoneSetsToMinMaxRange())
                .thenReturn(CAR_ZONE_SET_TO_MIN_MAX_RANGE);
        when(mCabinTempConfigMock.getPropertyId())
                .thenReturn(VehiclePropertyIds.HVAC_TEMPERATURE_SET);
        when(mHvacFanDirectionProfileMock.getPropertyId()).thenReturn(
                VehiclePropertyIds.HVAC_FAN_DIRECTION);
        when(mHvacFanDirectionProfileMock.getHvacFanDirection())
                .thenReturn(CAR_ZONE_SET_TO_FAN_DIRECTION_VALUES);
        when(mHvacFanDirectionAvailableValueMock.getValue()).thenReturn(FAN_DIRECTION_VALUE);

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
