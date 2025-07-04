/*
 * Copyright 2019 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.customtabs.IAuthTabCallback;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;

import androidx.browser.auth.AuthTabCallback;
import androidx.browser.auth.AuthTabSession;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Utilities for unit testing Custom Tabs.
 */
// minSdk For Bundle#getBinder
public class TestUtil {

    public static @NonNull CustomTabsSession makeMockSession() {
        return new CustomTabsSession(mock(ICustomTabsService.class),
                mock(ICustomTabsCallback.class), new ComponentName("", ""),
                makeMockPendingIntent());
    }

    public static CustomTabsSession.@NonNull PendingSession makeMockPendingSession() {
        return new CustomTabsSession.PendingSession(
                mock(CustomTabsCallback.class), makeMockPendingIntent());
    }

    public static @NonNull PendingIntent makeMockPendingIntent() {
        return PendingIntent.getBroadcast(mock(Context.class), 0, new Intent(), 0);
    }

    @SuppressWarnings("deprecation")
    public static void assertIntentHasSession(@NonNull Intent intent,
            @NonNull CustomTabsSession session) {
        assertEquals(session.getBinder(), intent.getExtras().getBinder(
                CustomTabsIntent.EXTRA_SESSION));
        assertEquals(session.getId(), intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    /** Create s a mock {@link AuthTabSession} for testing. */
    @NonNull
    public static AuthTabSession makeMockAuthTabSession() {
        IAuthTabCallback callback = mock(IAuthTabCallback.class);
        when(callback.asBinder()).thenReturn(mock(IAuthTabCallback.Stub.class));
        return new AuthTabSession(callback, new ComponentName("", ""), makeMockPendingIntent());
    }

    /** Creates a mock {@link AuthTabSession.PendingSession} for testing. */
    public static AuthTabSession.@NonNull PendingSession makeMockPendingAuthTabSession() {
        return new AuthTabSession.PendingSession(makeMockPendingIntent(), mock(Executor.class),
                mock(AuthTabCallback.class));
    }
}
