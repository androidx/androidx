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

package android.arch.background.workmanager.constraints.trackers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.constraints.NetworkState;
import android.arch.background.workmanager.constraints.listeners.NetworkStateListener;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NetworkStateContainerTest {
    private static final NetworkState INITIAL_STATE = new NetworkState(false, false, false, false);
    private static final NetworkState MOCK_NEW_STATE = new NetworkState(true, false, true, true);

    private NetworkStateContainer mContainer;
    private NetworkStateListener mMockListener;
    private List<NetworkStateListener> mMockListeners;

    @Before
    public void setUp() {
        mContainer = new NetworkStateContainer(INITIAL_STATE);
        mMockListener = mock(NetworkStateListener.class);
        mMockListeners = new ArrayList<>();
        mMockListeners.add(mMockListener);
    }

    @Test
    public void testGetState_initialState() {
        assertThat(mContainer.getState(), is(INITIAL_STATE));
    }

    @Test
    public void testSetStateAndNotify_stateChange() {
        verify(mMockListener, never()).setNetworkState(any(NetworkState.class));
        mContainer.setStateAndNotify(MOCK_NEW_STATE, mMockListeners);
        verify(mMockListener).setNetworkState(MOCK_NEW_STATE);
    }

    @Test
    public void testSetStateAndNotify_noStateChange() {
        mContainer.setStateAndNotify(INITIAL_STATE, mMockListeners);
        verify(mMockListener, never()).setNetworkState(any(NetworkState.class));
    }
}
