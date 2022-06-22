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

import android.content.Context;

import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.RangingResult.RangingResultPosition;
import androidx.core.uwb.UwbClientSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.mock.TestUwbManager;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class UwbClientSessionScopeRxTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final UwbManager uwbManager = new TestUwbManager(context);
    private final UwbDevice uwbDevice = UwbDevice.createForAddress(new byte[0]);
    private final RangingParameters rangingParameters = new RangingParameters(
            RangingParameters.UWB_CONFIG_ID_1,
            0,
            /*sessionKeyInfo=*/ null,
            /*complexChannel=*/ null,
            ImmutableList.of(uwbDevice),
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
    );

    @Test
    public void testRangingResultObservable_returnsRangingResultObservable() {
        Single<UwbClientSessionScope> clientSessionScopeSingle =
                UwbManagerRx.clientSessionScopeSingle(uwbManager);
        UwbClientSessionScope clientSessionScope = clientSessionScopeSingle.blockingGet();

        Observable<RangingResult> rangingResultObservable =
                UwbClientSessionScopeRx.rangingResultsObservable(clientSessionScope,
                        rangingParameters);
        RangingResult rangingResult = rangingResultObservable.blockingFirst();

        assertThat(rangingResult instanceof RangingResult.RangingResultPosition).isTrue();
        assertThat(
                ((RangingResultPosition) rangingResult).getPosition().getDistance().getValue())
                .isEqualTo(1.0f);
    }

    @Test
    public void testRangingResultFlowable_returnsRangingResultFlowable() {
        Single<UwbClientSessionScope> clientSessionScopeSingle =
                UwbManagerRx.clientSessionScopeSingle(uwbManager);
        UwbClientSessionScope clientSessionScope = clientSessionScopeSingle.blockingGet();

        Flowable<RangingResult> rangingResultFlowable =
                UwbClientSessionScopeRx.rangingResultsFlowable(clientSessionScope,
                        rangingParameters);
        RangingResult rangingResult = rangingResultFlowable.blockingFirst();

        assertThat(rangingResult instanceof RangingResult.RangingResultPosition).isTrue();
        assertThat(
                ((RangingResultPosition) rangingResult).getPosition().getDistance().getValue())
                .isEqualTo(1.0f);
    }
}

