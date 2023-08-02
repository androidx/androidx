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
package androidx.health.platform.client.impl.sdkservice;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;

@RunWith(RobolectricTestRunner.class)
public class HealthDataSdkServiceTest {

    private ServiceController<HealthDataSdkService> mController;

    @Before
    public void setup() {
        mController = Robolectric.buildService(HealthDataSdkService.class).create();
    }

    @Test
    public void validBindAction_expectNotNullStub() {
        Intent intent = new Intent(HealthDataSdkService.BIND_ACTION);
        intent.setPackage(getApplicationContext().getPackageName());
        HealthDataSdkServiceStubImpl stub =
                (HealthDataSdkServiceStubImpl) mController.get().onBind(intent);

        assertThat(stub).isNotNull();
    }

    @Test
    public void invalidBindAction_expectNullStub() {
        Intent intent = new Intent(HealthDataSdkService.BIND_ACTION + "invalid");
        intent.setPackage(getApplicationContext().getPackageName());
        HealthDataSdkServiceStubImpl stub =
                (HealthDataSdkServiceStubImpl) mController.get().onBind(intent);

        assertThat(stub).isNull();
    }
}
