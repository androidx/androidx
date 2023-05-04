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

import static com.google.common.truth.Truth.assertThat;

import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.RangingResult.RangingResultPosition;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.mock.TestUwbManager;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class UwbClientSessionScopeRxTest {
    private static final UwbDevice UWB_DEVICE = UwbDevice.createForAddress(new byte[0]);

    private final UwbManager mUwbManager = new TestUwbManager();
    private final RangingParameters rangingParameters = new RangingParameters(
            RangingParameters.CONFIG_UNICAST_DS_TWR,
            0,
            0,
            /*sessionKeyInfo=*/ null,
            /*subSessionKeyInfo=*/ null,
            /*complexChannel=*/ null,
            ImmutableList.of(UWB_DEVICE),
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
    );

    @Test
    public void testRangingResultObservable_returnsRangingResultObservable() {
        Single<UwbControleeSessionScope> controleeSessionScopeSingle =
                UwbManagerRx.controleeSessionScopeSingle(mUwbManager);
        UwbControleeSessionScope controleeSessionScope = controleeSessionScopeSingle.blockingGet();

        Observable<RangingResult> rangingResultObservable =
                UwbClientSessionScopeRx.rangingResultsObservable(controleeSessionScope,
                        rangingParameters);
        RangingResult rangingResult = rangingResultObservable.blockingFirst();

        assertThat(rangingResult instanceof RangingResult.RangingResultPosition).isTrue();
        assertThat(
                ((RangingResultPosition) rangingResult).getPosition().getDistance().getValue())
                .isEqualTo(1.0f);
    }

    @Test
    public void testRangingResultFlowable_returnsRangingResultFlowable() {
        Single<UwbControleeSessionScope> controleeSessionScopeSingle =
                UwbManagerRx.controleeSessionScopeSingle(mUwbManager);
        UwbControleeSessionScope controleeSessionScope = controleeSessionScopeSingle.blockingGet();

        Flowable<RangingResult> rangingResultFlowable =
                UwbClientSessionScopeRx.rangingResultsFlowable(controleeSessionScope,
                        rangingParameters);
        RangingResult rangingResult = rangingResultFlowable.blockingFirst();

        assertThat(rangingResult instanceof RangingResult.RangingResultPosition).isTrue();
        assertThat(
                ((RangingResultPosition) rangingResult).getPosition().getDistance().getValue())
                .isEqualTo(1.0f);
    }
}

