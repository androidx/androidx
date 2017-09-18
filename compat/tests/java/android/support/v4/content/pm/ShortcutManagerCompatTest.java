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

package android.support.v4.content.pm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.app.TestSupportActivity;
import android.support.v4.graphics.drawable.IconCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ShortcutManagerCompatTest extends BaseInstrumentationTestCase<TestSupportActivity> {

    Context mContext;
    ShortcutInfoCompat mInfoCompat;

    public ShortcutManagerCompatTest() {
        super(TestSupportActivity.class);
    }

    @Before
    public void setup() {
        mContext = spy(mActivityTestRule.getActivity());
        mInfoCompat = new ShortcutInfoCompat.Builder(mContext, "test-id")
                .setIcon(IconCompat.createWithBitmap(Bitmap.createBitmap(
                        10, 10, Bitmap.Config.ARGB_8888)))
                .setShortLabel("Test shortcut")
                .setIntent(new Intent("Dummy"))
                .build();
    }

    @Test
    @SmallTest
    @TargetApi(26)
    public void testIsRequestPinShortcutSupported_v26() throws Throwable {
        if (!(Build.VERSION.SDK_INT >= 26)) {
            return;
        }

        ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        when(mockShortcutManager.isRequestPinShortcutSupported()).thenReturn(true, false, true);

        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
        assertFalse(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
        verify(mockShortcutManager, times(3)).isRequestPinShortcutSupported();
    }

    @Test
    @SmallTest
    @TargetApi(26)
    public void testRequestPinShortcut_v26()  throws Throwable {
        if (!(Build.VERSION.SDK_INT >= 26)) {
            return;
        }

        ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        when(mockShortcutManager.requestPinShortcut(
                any(ShortcutInfo.class), any(IntentSender.class))).thenReturn(true);

        assertTrue(ShortcutManagerCompat.requestPinShortcut(mContext, mInfoCompat, null));
        ArgumentCaptor<ShortcutInfo> captor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mockShortcutManager, times(1)).requestPinShortcut(captor.capture(),
                (IntentSender) isNull());
        assertEquals("test-id", captor.getValue().getId());
    }

    @Test
    @SmallTest
    @TargetApi(26)
    public void testCreateShortcutResultIntent_v26()  throws Throwable {
        if (!(Build.VERSION.SDK_INT >= 26)) {
            return;
        }

        ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));

        when(mockShortcutManager.createShortcutResultIntent(any(ShortcutInfo.class)))
                .thenReturn(new Intent("some-dummy-action"));

        Intent result = ShortcutManagerCompat.createShortcutResultIntent(mContext, mInfoCompat);
        verifyLegacyIntent(result);
        assertEquals("some-dummy-action", result.getAction());

        ArgumentCaptor<ShortcutInfo> captor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mockShortcutManager, times(1)).createShortcutResultIntent(captor.capture());
        assertEquals("test-id", captor.getValue().getId());
    }

    @SmallTest
    @Test
    public void testIsRequestPinShortcutSupported_v4() throws Throwable {
        if (Build.VERSION.SDK_INT >= 26) {
            return;
        }
        setMockPm(mockResolveInfo(null));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // We do not have the permission
        setMockPm(mockResolveInfo("com.android.permission.something-we-dont-have"));
        assertFalse(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // There are no receivers
        setMockPm();
        assertFalse(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // At least one receiver is supported
        setMockPm(mockResolveInfo("com.android.permission.something-we-dont-have"),
                mockResolveInfo(null));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // We have the permission
        setMockPm(mockResolveInfo(ShortcutManagerCompat.INSTALL_SHORTCUT_PERMISSION));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
    }

    @LargeTest
    @Test
    public void testRequestPinShortcut_v4_noCallback()  throws Throwable {
        if (Build.VERSION.SDK_INT >= 26) {
            return;
        }

        setMockPm(mockResolveInfo(null));

        BlockingBroadcastReceiver receiver =
                new BlockingBroadcastReceiver(ShortcutManagerCompat.ACTION_INSTALL_SHORTCUT);
        assertTrue(ShortcutManagerCompat.requestPinShortcut(mContext, mInfoCompat, null));
        verifyLegacyIntent(receiver.blockingGetIntent());
    }

    @LargeTest
    @Test
    public void testRequestPinShortcut_v4_withCallback()  throws Throwable {
        if (Build.VERSION.SDK_INT >= 26) {
            return;
        }

        setMockPm(mockResolveInfo(null));

        BlockingBroadcastReceiver receiver =
                new BlockingBroadcastReceiver(ShortcutManagerCompat.ACTION_INSTALL_SHORTCUT);
        BlockingBroadcastReceiver callback =
                new BlockingBroadcastReceiver("shortcut-callback");

        assertTrue(ShortcutManagerCompat.requestPinShortcut(mContext, mInfoCompat,
                PendingIntent.getBroadcast(mContext, 0, new Intent("shortcut-callback"),
                        PendingIntent.FLAG_ONE_SHOT).getIntentSender()));
        verifyLegacyIntent(receiver.blockingGetIntent());
        assertNotNull(callback.blockingGetIntent());
    }

    @SmallTest
    @Test
    public void testCreateShortcutResultIntent_v4() throws Throwable {
        if (Build.VERSION.SDK_INT >= 26) {
            return;
        }

        verifyLegacyIntent(ShortcutManagerCompat.createShortcutResultIntent(mContext, mInfoCompat));
    }

    private void verifyLegacyIntent(Intent intent) {
        assertNotNull(intent);
        assertEquals("Test shortcut", intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
        assertEquals("Dummy", ((Intent) intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT))
                .getAction());
    }

    private void setMockPm(ResolveInfo... infos) {
        PackageManager pm = mock(PackageManager.class);
        when(pm.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(Arrays.asList(infos));
        reset(mContext);
        doReturn(pm).when(mContext).getPackageManager();
    }

    private ResolveInfo mockResolveInfo(String permission) {
        ActivityInfo aInfo = new ActivityInfo();
        aInfo.packageName = mContext.getPackageName();
        aInfo.permission = permission;
        ResolveInfo rInfo = new ResolveInfo();
        rInfo.activityInfo = aInfo;
        return rInfo;
    }

    private class BlockingBroadcastReceiver extends BroadcastReceiver {

        private final CountDownLatch mLatch = new CountDownLatch(1);
        private Intent mIntent;

        BlockingBroadcastReceiver(String action) {
            mContext.registerReceiver(this, new IntentFilter(action));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mIntent = intent;
            mLatch.countDown();
        }

        public Intent blockingGetIntent() throws InterruptedException {
            mLatch.await(5, TimeUnit.SECONDS);
            mContext.unregisterReceiver(this);
            return mIntent;
        }
    }
}
