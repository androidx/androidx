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

package androidx.car.app.hardware;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.car.Car;
import android.car.hardware.property.CarPropertyManager;

import androidx.car.app.HandshakeInfo;
import androidx.car.app.HostDispatcher;
import androidx.car.app.HostException;
import androidx.car.app.shadows.car.ShadowCar;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;

@Config(
        manifest = Config.NONE,
        shadows = {ShadowCar.class}
)
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AutomotiveCarHardwareManagerTest {
    @Mock
    private Car mCarMock;
    @Mock
    private CarPropertyManager mCarPropertyManagerMock;

    private Application mContext;
    private ShadowApplication mShadowApplication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCar.setCar(mCarMock);
        when(mCarMock.getCarManager(anyString())).thenReturn(mCarPropertyManagerMock);

        mContext = ApplicationProvider.getApplicationContext();
        mShadowApplication = Shadows.shadowOf(mContext);
        mShadowApplication.grantPermissions(Car.PERMISSION_CAR_INFO);
    }

    @Test
    public void carHardwareManager_lessThanApi3_throws() {
        HostDispatcher dispatcher = new HostDispatcher();
        TestCarContext carContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        carContext.updateHandshakeInfo(new HandshakeInfo("foo", CarAppApiLevels.LEVEL_2));
        assertThrows(HostException.class,
                () -> CarHardwareManager.create(carContext, dispatcher));
    }

    @Test
    public void carHardwareManager_returnsAutomotiveInstance() {
        HostDispatcher dispatcher = new HostDispatcher();
        TestCarContext carContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        // We need API level 3 to access the CarHardwareManager
        carContext.updateHandshakeInfo(new HandshakeInfo("foo", CarAppApiLevels.LEVEL_3));
        CarHardwareManager manager = CarHardwareManager.create(carContext, dispatcher);
        assertThat(manager).isInstanceOf(AutomotiveCarHardwareManager.class);
    }
}
