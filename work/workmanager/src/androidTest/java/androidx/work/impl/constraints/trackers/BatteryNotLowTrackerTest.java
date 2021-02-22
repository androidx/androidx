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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.impl.constraints.ConstraintListener;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BatteryNotLowTrackerTest {

    private static final int STATUS_CHARGING = BatteryManager.BATTERY_STATUS_CHARGING;
    private static final int UNKNOWN_STATUS = BatteryManager.BATTERY_STATUS_UNKNOWN;
    private static final float BELOW_THRESHOLD = BatteryNotLowTracker.BATTERY_LOW_THRESHOLD;
    private static final float ABOVE_THRESHOLD = BELOW_THRESHOLD + 0.01f;

    private Context mMockContext;
    private BatteryNotLowTracker mTracker;
    private ConstraintListener<Boolean> mListener;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);

        mTracker = new BatteryNotLowTracker(mMockContext, new InstantWorkTaskExecutor());
        mListener = mock(ConstraintListener.class);
    }

    private void mockContextReturns(Intent expectedIntent) {
        when(mMockContext.registerReceiver((BroadcastReceiver) isNull(),
                any(IntentFilter.class))).thenReturn(expectedIntent);
    }

    private Intent createBatteryChangedIntent(int status, float percent) {
        int scale = 100;
        int level = (int) (scale * percent);

        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, scale);
        return intent;
    }

    private void initialStateHelper(int status, float percentage, boolean expectedBatteryNotLow) {
        mockContextReturns(createBatteryChangedIntent(status, percentage));
        assertThat(mTracker.getInitialState(), is(expectedBatteryNotLow));
    }

    @Test
    @SmallTest
    public void testGetInitialState_nullIntent() {
        mockContextReturns(null);
        assertThat(mTracker.getInitialState(), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testKnownStatus_belowThreshold() {
        initialStateHelper(STATUS_CHARGING, BELOW_THRESHOLD, false);
    }

    @Test
    @SmallTest
    public void testKnownStatus_aboveThreshold() {
        initialStateHelper(STATUS_CHARGING, ABOVE_THRESHOLD, true);
    }

    @Test
    @SmallTest
    public void testUnknownStatus_belowThreshold() {
        initialStateHelper(UNKNOWN_STATUS, BELOW_THRESHOLD, true);
    }

    @Test
    @SmallTest
    public void testUnknownStatus_aboveThreshold() {
        initialStateHelper(UNKNOWN_STATUS, ABOVE_THRESHOLD, true);
    }

    @Test
    @SmallTest
    public void testGetIntentFilter() {
        IntentFilter intentFilter = mTracker.getIntentFilter();
        assertThat(intentFilter.hasAction(Intent.ACTION_BATTERY_OKAY), is(true));
        assertThat(intentFilter.hasAction(Intent.ACTION_BATTERY_LOW), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_invalidIntentAction_doesNotNotifyListeners() {
        mockContextReturns(
                createBatteryChangedIntent(STATUS_CHARGING, ABOVE_THRESHOLD));
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(true);

        mTracker.onBroadcastReceive(mMockContext, new Intent("INVALID"));
        verifyNoMoreInteractions(mListener);
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_notifiesListeners() {
        mockContextReturns(
                createBatteryChangedIntent(STATUS_CHARGING, BELOW_THRESHOLD));
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(false);

        mTracker.onBroadcastReceive(mMockContext, new Intent(Intent.ACTION_BATTERY_OKAY));
        verify(mListener).onConstraintChanged(true);
        mTracker.onBroadcastReceive(mMockContext, new Intent(Intent.ACTION_BATTERY_LOW));
        verify(mListener, times(2)).onConstraintChanged(false);
    }
}
