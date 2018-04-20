/*
 * Copyright (C) 2017 The Android Open Source Project
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
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.constraints.ConstraintListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BatteryChargingTrackerTest {

    private BatteryChargingTracker mTracker;
    private ConstraintListener<Boolean> mListener;
    private Context mMockContext;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);

        mTracker = new BatteryChargingTracker(mMockContext);
        mListener = mock(ConstraintListener.class);
    }

    private void mockContextReturns(Intent expectedIntent) {
        when(mMockContext.registerReceiver((BroadcastReceiver) isNull(),
                any(IntentFilter.class))).thenReturn(expectedIntent);
    }

    private Intent createBatteryChangedIntent(boolean charging) {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 23) {
            int status = charging ? BatteryManager.BATTERY_STATUS_CHARGING
                    : BatteryManager.BATTERY_STATUS_DISCHARGING;
            intent.putExtra(BatteryManager.EXTRA_STATUS, status);
        } else {
            int plugged = charging ? 1 : 0;
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        }
        return intent;
    }

    private Intent createChargingIntent(boolean charging) {
        return new Intent(
                charging ? Intent.ACTION_POWER_CONNECTED : Intent.ACTION_POWER_DISCONNECTED);
    }

    @TargetApi(23)
    private Intent createChargingIntent_afterApi23(boolean charging) {
        return new Intent(
                charging ? BatteryManager.ACTION_CHARGING : BatteryManager.ACTION_DISCHARGING);
    }

    @Test
    @SmallTest
    public void testGetInitialState_nullIntent() {
        mockContextReturns(null);
        assertThat(mTracker.getInitialState(), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testGetInitialState_chargingIntent() {
        mockContextReturns(createBatteryChangedIntent(true));
        assertThat(mTracker.getInitialState(), is(true));
    }

    @Test
    @SmallTest
    public void testGetInitialState_dischargingIntent() {
        mockContextReturns(createBatteryChangedIntent(false));
        assertThat(mTracker.getInitialState(), is(false));
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testGetIntentFilter_beforeApi23() {
        IntentFilter intentFilter = mTracker.getIntentFilter();
        assertThat(intentFilter.hasAction(Intent.ACTION_POWER_CONNECTED), is(true));
        assertThat(intentFilter.hasAction(Intent.ACTION_POWER_DISCONNECTED), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testGetIntentFilter_afterApi23() {
        IntentFilter intentFilter = mTracker.getIntentFilter();
        assertThat(intentFilter.hasAction(BatteryManager.ACTION_CHARGING), is(true));
        assertThat(intentFilter.hasAction(BatteryManager.ACTION_DISCHARGING), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_invalidIntentAction_doesNotNotifyListeners() {
        mockContextReturns(createBatteryChangedIntent(true));
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(true);

        mTracker.onBroadcastReceive(
                InstrumentationRegistry.getTargetContext(),
                new Intent("INVALID"));
        verifyNoMoreInteractions(mListener);
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testOnBroadcastReceive_notifiesListeners_beforeApi23() {
        mockContextReturns(createBatteryChangedIntent(false));
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(false);

        mTracker.onBroadcastReceive(mMockContext, createChargingIntent(true));
        verify(mListener).onConstraintChanged(true);
        mTracker.onBroadcastReceive(mMockContext, createChargingIntent(false));
        verify(mListener, times(2)).onConstraintChanged(false);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testOnBroadcastReceive_notifiesListeners_afterApi23() {
        mockContextReturns(null);
        mTracker.addListener(mListener);
        verify(mListener, never()).onConstraintChanged(anyBoolean());

        mTracker.onBroadcastReceive(mMockContext, createChargingIntent_afterApi23(true));
        verify(mListener).onConstraintChanged(true);
        mTracker.onBroadcastReceive(mMockContext, createChargingIntent_afterApi23(false));
        verify(mListener).onConstraintChanged(false);
    }
}
