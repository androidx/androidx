/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.browser.customtabs;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link CustomTabsSession}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CustomTabsSessionTest {
    private TestCustomTabsService mService;
    private TestCustomTabsCallback mCallback;
    private CustomTabsSession mSession;

    @Before
    public void setup() {
        mService = new TestCustomTabsService();
        mCallback = new TestCustomTabsCallback();
        mSession = new CustomTabsSession(
                mService.getStub(), mCallback.getStub(), new ComponentName("", ""), null);
    }

    @Test
    public void testIsEngagementSignalsApiAvailable() throws RemoteException {
        when(mService.getMock().isEngagementSignalsApiAvailable(any(ICustomTabsCallback.class),
                any(Bundle.class))).thenReturn(true);
        assertTrue(mSession.isEngagementSignalsApiAvailable(Bundle.EMPTY));
    }

    @Test
    public void testSetEngagementSignalsCallback() throws RemoteException {
        when(mService.getMock().setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), any(Bundle.class))).thenReturn(true);
        EngagementSignalsCallback callback = new TestEngagementSignalsCallback();
        assertTrue(mSession.setEngagementSignalsCallback(callback, Bundle.EMPTY));
        verify(mService.getMock()).setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), any(Bundle.class));
    }

    @Test
    public void testSetEngagementSignalsCallbackWithExecutor() throws RemoteException {
        when(mService.getMock().setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), any(Bundle.class))).thenReturn(true);
        EngagementSignalsCallback callback = new TestEngagementSignalsCallback();
        assertTrue(mSession.setEngagementSignalsCallback((r) -> {
        }, callback, Bundle.EMPTY));
        verify(mService.getMock()).setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), any(Bundle.class));
    }
}
