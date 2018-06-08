/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.constraints.NetworkState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public class NetworkStateTrackerTest {

    private NetworkStateTracker mTracker;

    private Context mMockContext;
    private ConnectivityManager mMockConnectivityManager;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        mMockConnectivityManager = mock(ConnectivityManager.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);
        when(mMockContext.getSystemService(eq(Context.CONNECTIVITY_SERVICE)))
                .thenReturn(mMockConnectivityManager);

        mTracker = new NetworkStateTracker(mMockContext);
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 15)
    public void testGetInitialState_nullNetworkInfoSdk15() {
        // API < 16 conservatively treats null networkInfo as metered.
        NetworkState expectedState = new NetworkState(false, false, true, false);
        assertThat(mTracker.getInitialState(), is(expectedState));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 16)
    public void testGetInitialState_nullNetworkInfo() {
        NetworkState expectedState = new NetworkState(false, false, false, false);
        assertThat(mTracker.getInitialState(), is(expectedState));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testStartTracking_afterApi24() {
        mTracker.startTracking();
        verify(mMockConnectivityManager)
                .registerDefaultNetworkCallback(any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 23)
    public void testStartTracking_beforeApi24() {
        mTracker.startTracking();
        ArgumentCaptor<IntentFilter> argCaptor = ArgumentCaptor.forClass(IntentFilter.class);
        verify(mMockContext).registerReceiver(any(BroadcastReceiver.class), argCaptor.capture());

        IntentFilter intentFilter = argCaptor.getValue();
        assertThat(intentFilter.hasAction(ConnectivityManager.CONNECTIVITY_ACTION), is(true));
        assertThat(intentFilter.countActions(), is(1));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testStopTracking_afterApi24() {
        mTracker.stopTracking();
        verify(mMockConnectivityManager)
                .unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 23)
    public void testStopTracking_beforeApi24() {
        mTracker.stopTracking();
        verify(mMockContext).unregisterReceiver(any(BroadcastReceiver.class));
    }
}
