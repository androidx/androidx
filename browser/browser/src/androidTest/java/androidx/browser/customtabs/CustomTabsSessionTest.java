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

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;

/**
 * Tests for {@link CustomTabsSession}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
@SuppressWarnings("deprecation")
public class CustomTabsSessionTest {
    private TestCustomTabsService mService;
    private TestCustomTabsCallback mCallback;
    private PendingIntent mId;
    private CustomTabsSession mSession;

    @Before
    public void setup() {
        mService = new TestCustomTabsService();
        mCallback = new TestCustomTabsCallback();
        mId = PendingIntent.getBroadcast(InstrumentationRegistry.getInstrumentation().getContext(),
                0, new Intent(), FLAG_IMMUTABLE);
        mSession = new CustomTabsSession(
                mService.getStub(), mCallback.getStub(), new ComponentName("", ""), mId);
    }

    @Test
    public void testIsEngagementSignalsApiAvailable() throws RemoteException {
        when(mService.getMock().isEngagementSignalsApiAvailable(any(ICustomTabsCallback.class),
                any(Bundle.class))).thenReturn(true);
        assertTrue(mSession.isEngagementSignalsApiAvailable(Bundle.EMPTY));
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mService.getMock()).isEngagementSignalsApiAvailable(any(ICustomTabsCallback.class),
                captor.capture());
        assertEquals(mId, captor.getValue().getParcelable(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Test
    public void testSetEngagementSignalsCallback() throws RemoteException {
        when(mService.getMock().setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), any(Bundle.class))).thenReturn(true);
        EngagementSignalsCallback callback = new TestEngagementSignalsCallback();
        assertTrue(mSession.setEngagementSignalsCallback(callback, Bundle.EMPTY));
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mService.getMock()).setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), captor.capture());
        assertEquals(mId, captor.getValue().getParcelable(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Test
    public void testSetEngagementSignalsCallbackWithExecutor() throws RemoteException {
        when(mService.getMock().setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), any(Bundle.class))).thenReturn(true);
        EngagementSignalsCallback callback = new TestEngagementSignalsCallback();
        assertTrue(mSession.setEngagementSignalsCallback((r) -> {
        }, callback, Bundle.EMPTY));
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mService.getMock()).setEngagementSignalsCallback(any(ICustomTabsCallback.class),
                any(IBinder.class), captor.capture());
        assertEquals(mId, captor.getValue().getParcelable(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Test
    public void testPrefetch() throws RemoteException {
        mSession.prefetch(Uri.parse(""), new PrefetchOptions.Builder().build());
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mService.getMock())
                .prefetch(any(ICustomTabsCallback.class), any(Uri.class), captor.capture());
        assertEquals(mId, captor.getValue().getParcelable(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Test
    public void testPrefetchWithMultipleUrls() throws RemoteException {
        mSession.prefetch(List.of(Uri.parse("")), new PrefetchOptions.Builder().build());
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mService.getMock())
                .prefetchWithMultipleUrls(
                        any(ICustomTabsCallback.class),
                        ArgumentMatchers.<List<Uri>>any(),
                        captor.capture());
        assertEquals(mId, captor.getValue().getParcelable(CustomTabsIntent.EXTRA_SESSION_ID));
    }
}
