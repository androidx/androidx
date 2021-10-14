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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utilities for unit testing Custom Tabs.
 */
// minSdk For Bundle#getBinder
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TestUtil {

    @NonNull
    public static CustomTabsSession makeMockSession() {
        return new CustomTabsSession(mock(ICustomTabsService.class),
                mock(ICustomTabsCallback.class), new ComponentName("", ""),
                makeMockPendingIntent());
    }

    @NonNull
    public static CustomTabsSession.PendingSession makeMockPendingSession() {
        return new CustomTabsSession.PendingSession(
                mock(CustomTabsCallback.class), makeMockPendingIntent());
    }

    @NonNull
    private static PendingIntent makeMockPendingIntent() {
        return PendingIntent.getBroadcast(mock(Context.class), 0, new Intent(), 0);
    }

    public static void assertIntentHasSession(@NonNull Intent intent,
            @NonNull CustomTabsSession session) {
        assertEquals(session.getBinder(), intent.getExtras().getBinder(
                CustomTabsIntent.EXTRA_SESSION));
        assertEquals(session.getId(), intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }
}
