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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.constraints.ConstraintListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StorageNotLowTrackerTest {

    private StorageNotLowTracker mTracker;
    private ConstraintListener<Boolean> mListener;
    private Context mMockContext;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);

        mTracker = new StorageNotLowTracker(mMockContext);
        mListener = mock(ConstraintListener.class);
    }

    private void mockContextReturns(Intent expectedIntent) {
        when(mMockContext.registerReceiver((BroadcastReceiver) isNull(),
                any(IntentFilter.class))).thenReturn(expectedIntent);
    }

    @Test
    @SmallTest
    public void testGetInitialState_nullIntent() {
        mockContextReturns(null);
        assertThat(mTracker.getInitialState(), is(true));
    }

    @Test
    @SmallTest
    public void testGetInitialState_storageOkIntent() {
        mockContextReturns(new Intent(Intent.ACTION_DEVICE_STORAGE_OK));
        assertThat(mTracker.getInitialState(), is(true));
    }

    @Test
    @SmallTest
    public void testGetInitialState_storageLowIntent() {
        mockContextReturns(new Intent(Intent.ACTION_DEVICE_STORAGE_LOW));
        assertThat(mTracker.getInitialState(), is(false));
    }

    @Test
    @SmallTest
    public void testGetIntentFilter() {
        IntentFilter intentFilter = mTracker.getIntentFilter();
        assertThat(intentFilter.hasAction(Intent.ACTION_DEVICE_STORAGE_OK), is(true));
        assertThat(intentFilter.hasAction(Intent.ACTION_DEVICE_STORAGE_LOW), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_invalidIntentAction_doesNotNotifyListeners() {
        mockContextReturns(null);
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(true);

        mTracker.onBroadcastReceive(mMockContext, new Intent("INVALID"));
        verifyNoMoreInteractions(mListener);
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_notifiesListeners() {
        mockContextReturns(new Intent("INVALID"));
        mTracker.addListener(mListener);
        verify(mListener, never()).onConstraintChanged(anyBoolean());

        mTracker.onBroadcastReceive(mMockContext, new Intent(Intent.ACTION_DEVICE_STORAGE_OK));
        verify(mListener).onConstraintChanged(true);
        mTracker.onBroadcastReceive(mMockContext, new Intent(Intent.ACTION_DEVICE_STORAGE_LOW));
        verify(mListener).onConstraintChanged(false);
    }
}
