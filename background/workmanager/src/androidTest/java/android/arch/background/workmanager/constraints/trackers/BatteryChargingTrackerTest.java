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
package android.arch.background.workmanager.constraints.trackers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.annotation.TargetApi;
import android.arch.background.workmanager.constraints.listeners.BatteryChargingListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BatteryChargingTrackerTest {

    private BatteryChargingTracker mTracker;
    private BatteryChargingListener mListener;

    @Before
    public void setUp() {
        mTracker = new BatteryChargingTracker(InstrumentationRegistry.getTargetContext());
        mListener = mock(BatteryChargingListener.class);
    }

    @Test
    @SmallTest
    public void testInitState() {
        assertThat(mTracker.mIsCharging, is(nullValue()));
        mTracker.initState();
        assertThat(mTracker.mIsCharging, is(notNullValue()));
    }

    @Test
    @SmallTest
    public void testNotifyListener_stateNotInitialized() {
        mTracker.notifyListener(mListener);
        verify(mListener, never()).setBatteryCharging(anyBoolean());
    }

    @Test
    @SmallTest
    public void testNotifyListener_stateInitialized() {
        mTracker.mIsCharging = true;
        mTracker.notifyListener(mListener);
        verify(mListener).setBatteryCharging(mTracker.mIsCharging);
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
        mTracker.mListeners.add(mListener);
        mTracker.onBroadcastReceive(
                InstrumentationRegistry.getTargetContext(),
                new Intent("INVALID"));
        verify(mListener, never()).setBatteryCharging(anyBoolean());
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testOnBroadcastReceive_notifiesListeners_beforeApi23() {
        mTracker.mListeners.add(mListener);
        mTracker.onBroadcastReceive(
                InstrumentationRegistry.getTargetContext(),
                createBatteryChangedIntent(true));
        verify(mListener).setBatteryCharging(true);
        mTracker.onBroadcastReceive(
                InstrumentationRegistry.getTargetContext(),
                createBatteryChangedIntent(false));
        verify(mListener).setBatteryCharging(false);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testOnBroadcastReceive_notifiesListeners_afterApi23() {
        mTracker.mListeners.add(mListener);
        mTracker.onBroadcastReceive(
                InstrumentationRegistry.getTargetContext(),
                createBatteryChangedIntent_afterApi23(true));
        verify(mListener).setBatteryCharging(true);
        mTracker.onBroadcastReceive(
                InstrumentationRegistry.getTargetContext(),
                createBatteryChangedIntent_afterApi23(false));
        verify(mListener).setBatteryCharging(false);
    }

    private Intent createBatteryChangedIntent(boolean plugged) {
        return new Intent(
                plugged ? Intent.ACTION_POWER_CONNECTED : Intent.ACTION_POWER_DISCONNECTED);
    }

    @TargetApi(23)
    private Intent createBatteryChangedIntent_afterApi23(boolean charging) {
        return new Intent(
                charging ? BatteryManager.ACTION_CHARGING : BatteryManager.ACTION_DISCHARGING);
    }
}
