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

import androidx.car.app.HandshakeInfo;
import androidx.car.app.HostDispatcher;
import androidx.car.app.HostException;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ProjectedCarHardwareManagerTest {
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
    public void carHardwareManager_returnsProjectedInstance() {
        HostDispatcher dispatcher = new HostDispatcher();
        TestCarContext carContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        // We need API level 3 to access the CarHardwareManager
        carContext.updateHandshakeInfo(new HandshakeInfo("foo", CarAppApiLevels.LEVEL_3));
        CarHardwareManager manager = CarHardwareManager.create(carContext, dispatcher);
        assertThat(manager).isInstanceOf(ProjectedCarHardwareManager.class);
    }
}
