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

package androidx.core.uwb.rxjava3;

import static androidx.core.uwb.rxjava3.mock.TestUwbManager.DEVICE_ADDRESS;

import static com.google.common.truth.Truth.assertThat;

import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.mock.TestUwbManager;

import org.junit.Test;

import io.reactivex.rxjava3.core.Single;

public class UwbManagerRxTest {
    private final UwbManager mUwbManager = new TestUwbManager();

    @Test
    public void testControleeSessionScopeSingle_returnsControleeSessionScope() {
        Single<UwbControleeSessionScope> controleeSessionScopeSingle =
                UwbManagerRx.controleeSessionScopeSingle(mUwbManager);

        UwbControleeSessionScope controleeSessionScope = controleeSessionScopeSingle.blockingGet();

        assertThat(controleeSessionScope).isNotNull();
        assertThat(controleeSessionScope.getLocalAddress().getAddress()).isEqualTo(DEVICE_ADDRESS);
    }

    @Test
    public void testControllerSessionScopeSingle_returnsControllerSessionScope() {
        Single<UwbControllerSessionScope> controllerSessionScopeSingle =
                UwbManagerRx.controllerSessionScopeSingle(mUwbManager);

        UwbControllerSessionScope controllerSessionScope =
                controllerSessionScopeSingle.blockingGet();

        assertThat(controllerSessionScope).isNotNull();
        assertThat(controllerSessionScope.getLocalAddress().getAddress()).isEqualTo(DEVICE_ADDRESS);
    }
}