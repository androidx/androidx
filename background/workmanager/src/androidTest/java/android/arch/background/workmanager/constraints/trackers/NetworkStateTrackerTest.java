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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.arch.background.workmanager.constraints.NetworkState;
import android.arch.background.workmanager.constraints.listeners.NetworkStateListener;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NetworkStateTrackerTest {

    private NetworkStateTracker mTracker;
    private NetworkStateListener mMockListener;

    @Before
    public void setUp() {
        mTracker = new TestNetworkStateTracker(InstrumentationRegistry.getTargetContext());
        mMockListener = mock(NetworkStateListener.class);
        mTracker.addListener(mMockListener);
    }

    @Test
    public void testSetNetworkStateAndNotify_firstCall_notifiesListeners() {
        final NetworkState networkState = new NetworkState(false, false, false, false);
        verify(mMockListener, never()).setNetworkState(any(NetworkState.class));
        mTracker.setNetworkStateAndNotify(networkState);
        verify(mMockListener).setNetworkState(networkState);
    }

    @Test
    public void testSetNetworkStateAndNotify_stateChange_notifiesListeners() {
        final InOrder inOrder = inOrder(mMockListener);
        final NetworkState networkState1 = new NetworkState(false, false, false, false);
        final NetworkState networkState2 = new NetworkState(true, false, false, false);
        mTracker.setNetworkStateAndNotify(networkState1);
        mTracker.setNetworkStateAndNotify(networkState2);
        inOrder.verify(mMockListener).setNetworkState(networkState1);
        inOrder.verify(mMockListener).setNetworkState(networkState2);
    }

    @Test
    public void testSetNetworkStateAndNotify_noStateChange_doesNotNotifyListenersTwice() {
        final NetworkState networkState = new NetworkState(false, false, false, false);
        mTracker.setNetworkStateAndNotify(networkState);
        verify(mMockListener).setNetworkState(networkState);
        mTracker.setNetworkStateAndNotify(networkState);
        verifyNoMoreInteractions(mMockListener);
    }

    private static class TestNetworkStateTracker extends NetworkStateTracker {

        TestNetworkStateTracker(Context context) {
            super(context);
        }

        @Override
        public void setUpInitialState(NetworkStateListener listener) {
        }

        @Override
        public void startTracking() {
        }

        @Override
        public void stopTracking() {
        }
    }
}
