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

package androidx.core.content.pm;

import static android.graphics.drawable.Icon.TYPE_ADAPTIVE_BITMAP;
import static android.graphics.drawable.Icon.TYPE_BITMAP;

import static androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_DYNAMIC;
import static androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_MANIFEST;
import static androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.core.app.TestActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
@RunWith(AndroidJUnit4.class)
public class ShortcutManagerCompatTest extends BaseInstrumentationTestCase<TestActivity> {

    private static final String SHORTCUT_ICON_PATH = "shortcut_icons";
    private static final String TEST_AUTHORITY = "moocow";

    Context mContext;
    ShortcutInfoCompat mInfoCompat;
    ShortcutInfoCompatSaver<Void> mShortcutInfoCompatSaver;
    ShortcutInfoChangeListener mShortcutInfoChangeListener;

    public ShortcutManagerCompatTest() {
        super(TestActivity.class);
    }

    @Before
    public void setup() {
        mContext = spy(mActivityTestRule.getActivity());
        mInfoCompat = new ShortcutInfoCompat.Builder(mContext, "test-id")
                .setIcon(IconCompat.createWithBitmap(Bitmap.createBitmap(
                        10, 10, Bitmap.Config.ARGB_8888)))
                .setShortLabel("Test shortcut")
                .setIntent(new Intent("No-op"))
                .build();
        mShortcutInfoCompatSaver = mock(ShortcutInfoCompatSaver.class);
        ShortcutManagerCompat.setShortcutInfoCompatSaver(mShortcutInfoCompatSaver);

        mShortcutInfoChangeListener = mock(ShortcutInfoChangeListener.class);
        ShortcutManagerCompat.setShortcutInfoChangeListeners(
                Collections.singletonList(mShortcutInfoChangeListener));
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 26)
    public void testIsRequestPinShortcutSupported_v26() throws Throwable {
        ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        when(mockShortcutManager.isRequestPinShortcutSupported()).thenReturn(true, false, true);

        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
        assertFalse(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
        verify(mockShortcutManager, times(3)).isRequestPinShortcutSupported();
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 26)
    public void testRequestPinShortcut_v26()  throws Throwable {
        ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        when(mockShortcutManager.requestPinShortcut(
                any(ShortcutInfo.class), nullable(IntentSender.class))).thenReturn(true);

        assertTrue(ShortcutManagerCompat.requestPinShortcut(mContext, mInfoCompat, null));
        ArgumentCaptor<ShortcutInfo> captor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mockShortcutManager, times(1)).requestPinShortcut(captor.capture(),
                (IntentSender) isNull());
        assertEquals("test-id", captor.getValue().getId());
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 26)
    public void testCreateShortcutResultIntent_v26()  throws Throwable {
        ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));

        when(mockShortcutManager.createShortcutResultIntent(any(ShortcutInfo.class)))
                .thenReturn(new Intent("some-no-op-action"));

        Intent result = ShortcutManagerCompat.createShortcutResultIntent(mContext, mInfoCompat);
        verifyLegacyIntent(result);
        assertEquals("some-no-op-action", result.getAction());

        ArgumentCaptor<ShortcutInfo> captor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mockShortcutManager, times(1)).createShortcutResultIntent(captor.capture());
        assertEquals("test-id", captor.getValue().getId());
    }

    @MediumTest
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    public void testIsRequestPinShortcutSupported_v4() throws Throwable {
        setMocks(mockResolveInfo(null));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // We do not have the permission
        setMocks(mockResolveInfo("com.android.permission.something-we-dont-have"));
        assertFalse(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // There are no receivers
        setMocks();
        assertFalse(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // At least one receiver is supported
        setMocks(mockResolveInfo("com.android.permission.something-we-dont-have"),
                mockResolveInfo(null));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));

        // We have the permission
        setMocks(mockResolveInfo(ShortcutManagerCompat.INSTALL_SHORTCUT_PERMISSION));
        assertTrue(ShortcutManagerCompat.isRequestPinShortcutSupported(mContext));
    }

    @LargeTest
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    public void testRequestPinShortcut_v4_noCallback() {
        setMocks(mockResolveInfo(null));
        assertTrue(ShortcutManagerCompat.requestPinShortcut(mContext, mInfoCompat, null));
        final ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).sendBroadcast(argument.capture());
        verifyLegacyIntent(argument.getValue());
    }

    @LargeTest
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    public void testRequestPinShortcut_v4_withCallback() {
        setMocks(mockResolveInfo(null));
        assertTrue(ShortcutManagerCompat.requestPinShortcut(mContext, mInfoCompat,
                PendingIntent.getBroadcast(mContext, 0, new Intent("shortcut-callback"),
                        PendingIntent.FLAG_ONE_SHOT).getIntentSender()));
        final ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).sendOrderedBroadcast(argument.capture(), nullable(String.class),
                any(BroadcastReceiver.class), nullable(Handler.class), anyInt(),
                nullable(String.class), nullable(Bundle.class));
        verifyLegacyIntent(argument.getValue());
    }

    @MediumTest
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    public void testCreateShortcutResultIntent_v4() throws Throwable {
        verifyLegacyIntent(ShortcutManagerCompat.createShortcutResultIntent(mContext, mInfoCompat));
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testShortcut() {
        final List<String> shortcutIds = Lists.newArrayList("test-id");
        final String disableMessage = "disabled";
        final List<ShortcutInfoCompat> shortcuts = Lists.newArrayList(mInfoCompat);
        final ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));

        reset(mockShortcutManager);
        reset(mShortcutInfoCompatSaver);
        reset(mShortcutInfoChangeListener);
        ShortcutManagerCompat.enableShortcuts(mContext, shortcuts);
        if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager).enableShortcuts(shortcutIds);
        }
        verify(mShortcutInfoCompatSaver).addShortcuts(shortcuts);
        verify(mShortcutInfoChangeListener).onShortcutAdded(shortcuts);

        reset(mockShortcutManager);
        reset(mShortcutInfoCompatSaver);
        reset(mShortcutInfoChangeListener);
        ShortcutManagerCompat.removeLongLivedShortcuts(mContext, shortcutIds);
        if (Build.VERSION.SDK_INT >= 30) {
            verify(mockShortcutManager).removeLongLivedShortcuts(shortcutIds);
        } else if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager).removeDynamicShortcuts(shortcutIds);
        }
        verify(mShortcutInfoCompatSaver).removeShortcuts(shortcutIds);
        verify(mShortcutInfoChangeListener).onShortcutRemoved(shortcutIds);

        reset(mockShortcutManager);
        reset(mShortcutInfoCompatSaver);
        reset(mShortcutInfoChangeListener);
        ShortcutManagerCompat.disableShortcuts(mContext, shortcutIds, disableMessage);
        if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager).disableShortcuts(shortcutIds, disableMessage);
        }
        verify(mShortcutInfoCompatSaver).removeShortcuts(shortcutIds);
        verify(mShortcutInfoChangeListener).onShortcutRemoved(shortcutIds);

        reset(mockShortcutManager);
        reset(mShortcutInfoCompatSaver);
        reset(mShortcutInfoChangeListener);
        when(mockShortcutManager.setDynamicShortcuts(ArgumentMatchers.<ShortcutInfo>anyList()))
                .thenReturn(true);
        ShortcutManagerCompat.setDynamicShortcuts(mContext, shortcuts);
        if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager)
                    .setDynamicShortcuts(ArgumentMatchers.<ShortcutInfo>anyList());
        }
        verify(mShortcutInfoCompatSaver).removeAllShortcuts();
        verify(mShortcutInfoChangeListener).onAllShortcutsRemoved();
        verify(mShortcutInfoCompatSaver).addShortcuts(shortcuts);
        verify(mShortcutInfoChangeListener).onShortcutAdded(shortcuts);
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testExcludedShortcuts() throws Throwable {
        final ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        when(mockShortcutManager.getMaxShortcutCountPerActivity()).thenReturn(5);

        final ShortcutInfoCompat excludedShortcut1 = new ShortcutInfoCompat.Builder(
                mContext, "my-shortcut")
                .setShortLabel("bitmap")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setExcludedFromSurfaces(ShortcutInfoCompat.SURFACE_LAUNCHER)
                .build();
        final ShortcutInfoCompat excludedShortcut2 = new ShortcutInfoCompat.Builder(
                mContext, "my-shortcut-2")
                .setShortLabel("bitmap")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setExcludedFromSurfaces(ShortcutInfoCompat.SURFACE_LAUNCHER)
                .build();
        final ShortcutInfoCompat excludedShortcut3 = new ShortcutInfoCompat.Builder(
                mContext, "my-shortcut-3")
                .setShortLabel("bitmap")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setExcludedFromSurfaces(ShortcutInfoCompat.SURFACE_LAUNCHER)
                .build();

        final List<ShortcutInfoCompat> excludedShortcuts = new ArrayList<>();
        excludedShortcuts.add(excludedShortcut1);
        excludedShortcuts.add(excludedShortcut2);
        excludedShortcuts.add(excludedShortcut3);

        ShortcutManagerCompat.addDynamicShortcuts(mContext, excludedShortcuts);
        if (Build.VERSION.SDK_INT > 31) {
            verify(mockShortcutManager).addDynamicShortcuts(
                    ArgumentMatchers.<ShortcutInfo>anyList());
        } else {
            verify(mockShortcutManager).addDynamicShortcuts(Collections.EMPTY_LIST);
        }

        reset(mockShortcutManager);
        when(mockShortcutManager.setDynamicShortcuts(ArgumentMatchers.<ShortcutInfo>anyList()))
                .thenReturn(true);
        when(mockShortcutManager.getMaxShortcutCountPerActivity()).thenReturn(5);

        ShortcutManagerCompat.setDynamicShortcuts(mContext, excludedShortcuts);
        if (Build.VERSION.SDK_INT > 31) {
            verify(mockShortcutManager).setDynamicShortcuts(
                    ArgumentMatchers.<ShortcutInfo>anyList());
        } else {
            if (Build.VERSION.SDK_INT >= 25) {
                verify(mockShortcutManager).setDynamicShortcuts(Collections.EMPTY_LIST);
            }
            verify(mShortcutInfoCompatSaver).removeAllShortcuts();
            verify(mShortcutInfoCompatSaver).addShortcuts(Collections.EMPTY_LIST);
        }
        verify(mShortcutInfoChangeListener).onAllShortcutsRemoved();
        verify(mShortcutInfoChangeListener).onShortcutAdded(excludedShortcuts);

        reset(mockShortcutManager);
        when(mockShortcutManager.getMaxShortcutCountPerActivity()).thenReturn(5);
        ShortcutManagerCompat.pushDynamicShortcut(mContext, excludedShortcut1);
        if (Build.VERSION.SDK_INT > 31) {
            verify(mockShortcutManager).pushDynamicShortcut(any(ShortcutInfo.class));
        } else if (Build.VERSION.SDK_INT >= 30) {
            verify(mockShortcutManager, never())
                    .pushDynamicShortcut(any(ShortcutInfo.class));
        } else if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager, never()).addDynamicShortcuts(anyList());
        }

        reset(mockShortcutManager);
        when(mockShortcutManager.getMaxShortcutCountPerActivity()).thenReturn(5);
        ShortcutManagerCompat.updateShortcuts(mContext, excludedShortcuts);
        if (Build.VERSION.SDK_INT > 31) {
            verify(mockShortcutManager).updateShortcuts(anyList());
        } else if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager).updateShortcuts(Collections.EMPTY_LIST);
        }

        reset(mockShortcutManager);
        when(mockShortcutManager.getMaxShortcutCountPerActivity()).thenReturn(5);
        ShortcutManagerCompat.enableShortcuts(mContext, excludedShortcuts);
        if (Build.VERSION.SDK_INT > 31) {
            verify(mockShortcutManager).enableShortcuts(anyList());
        } else if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager).enableShortcuts(Collections.EMPTY_LIST);
        }
    }

    @MediumTest
    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testGetShortcut() throws Throwable {
        final int flag = FLAG_MATCH_MANIFEST | FLAG_MATCH_DYNAMIC | FLAG_MATCH_PINNED;
        final ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        ShortcutManagerCompat.getShortcuts(mContext, flag);

        if (Build.VERSION.SDK_INT >= 30) {
            verify(mockShortcutManager).getShortcuts(flag);
        } else if (Build.VERSION.SDK_INT >= 25) {
            verify(mockShortcutManager).getManifestShortcuts();
            verify(mockShortcutManager).getDynamicShortcuts();
            verify(mockShortcutManager).getPinnedShortcuts();
        } else {
            verify(mShortcutInfoCompatSaver).getShortcuts();
        }
    }

    @MediumTest
    @Test
    public void testDynamicShortcuts() {
        if (Build.VERSION.SDK_INT >= 25) {
            ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
            doReturn(mockShortcutManager).when(mContext).getSystemService(
                    eq(Context.SHORTCUT_SERVICE));
            when(mockShortcutManager.addDynamicShortcuts(ArgumentMatchers.<ShortcutInfo>anyList()))
                    .thenReturn(true);
        }
        assertTrue(ShortcutManagerCompat.addDynamicShortcuts(mContext, getShortcutInfoCompats()));
        removeShortcuts();
    }

    @MediumTest
    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testPushDynamicShortcuts() throws Throwable {
        // setup mock objects
        final ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(
                eq(Context.SHORTCUT_SERVICE));
        when(mockShortcutManager.addDynamicShortcuts(ArgumentMatchers.<ShortcutInfo>anyList()))
                .thenReturn(true);
        when(mockShortcutManager.getMaxShortcutCountPerActivity()).thenReturn(4);
        doReturn(getShortcutInfos()).when(mockShortcutManager).getDynamicShortcuts();
        doReturn(getShortcutInfoCompats()).when(mShortcutInfoCompatSaver).getShortcuts();
        // push a new shortcut
        final ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(
                mContext, "my-shortcut")
                .setShortLabel("bitmap")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(0)
                .build();
        ShortcutManagerCompat.pushDynamicShortcut(mContext, shortcutInfo);
        if (Build.VERSION.SDK_INT >= 30) {
            verify(mockShortcutManager).pushDynamicShortcut(any(ShortcutInfo.class));
        } else if (Build.VERSION.SDK_INT >= 25) {
            // verify the shortcut with lowest rank has been removed
            final ArgumentCaptor<List<String>> stringCaptor =
                    ArgumentCaptor.forClass(ArrayList.class);
            verify(mockShortcutManager).removeDynamicShortcuts(stringCaptor.capture());
            verifyShortcutRemoved("shortcut-3", stringCaptor);
            // verify the new shortcut has been added
            final ArgumentCaptor<List<ShortcutInfo>> shortcutInfoCaptor =
                    ArgumentCaptor.forClass(ArrayList.class);
            verify(mockShortcutManager).addDynamicShortcuts(shortcutInfoCaptor.capture());
            final List<ShortcutInfo> actualShortcutInfos = shortcutInfoCaptor.getValue();
            assertEquals(1, actualShortcutInfos.size());
            assertEquals(shortcutInfo.getId(), actualShortcutInfos.get(0).getId());
        }
        // verify the shortcut with lowest rank has been removed
        final ArgumentCaptor<List<String>> stringCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(mShortcutInfoCompatSaver).removeShortcuts(stringCaptor.capture());
        verifyShortcutRemoved("uri-bitmap-shortcut", stringCaptor);
        // verify the new shortcut has been added
        final ArgumentCaptor<List<ShortcutInfoCompat>> shortcutInfoCaptor =
                ArgumentCaptor.forClass(ArrayList.class);
        verify(mShortcutInfoCompatSaver).addShortcuts(shortcutInfoCaptor.capture());
        verify(mShortcutInfoChangeListener).onShortcutAdded(shortcutInfoCaptor.capture());
        verify(mShortcutInfoChangeListener, times(1))
                .onShortcutUsageReported(Collections.singletonList(shortcutInfo.getId()));
        verify(mockShortcutManager, times(1)).reportShortcutUsed(shortcutInfo.getId());
        final List<ShortcutInfoCompat> actualShortcutInfos = shortcutInfoCaptor.getValue();
        assertEquals(1, actualShortcutInfos.size());
        assertEquals(shortcutInfo, actualShortcutInfos.get(0));
    }

    private void verifyShortcutRemoved(final String expected,
            final ArgumentCaptor<List<String>> stringCaptor) {
        final List<String> actualStrings = stringCaptor.getValue();
        assertEquals(1, actualStrings.size());
        assertEquals(expected, actualStrings.get(0));
    }

    @MediumTest
    @Test
    public void testConvertUriIconsToBitmapIcons() {
        ArrayList<ShortcutInfoCompat> shortcuts = getShortcutInfoCompats();
        assertEquals(6, shortcuts.size());
        ShortcutManagerCompat.convertUriIconsToBitmapIcons(mContext, shortcuts);
        assertEquals(4, shortcuts.size());  // shortcut with invalid icon uri was removed
        for (ShortcutInfoCompat info : shortcuts) {
            assertTrue(info.mIcon.mType == TYPE_BITMAP
                    || info.mIcon.mType == TYPE_ADAPTIVE_BITMAP);
            assertNotNull(info.mIcon.getBitmap());
        }
    }

    @LargeTest
    @Test
    public void testGetIconDimension() {
        assertTrue(ShortcutManagerCompat.getIconMaxWidth(mContext) >= 0);
        assertTrue(ShortcutManagerCompat.getIconMaxHeight(mContext) >= 0);
    }

    @SmallTest
    @Test
    public void testShortcutInfoListenerServiceDiscovery() {
        ShortcutManagerCompat.setShortcutInfoChangeListeners(null);
        // Initialize the listener.
        ShortcutManagerCompat.removeAllDynamicShortcuts(mContext);

        List<ShortcutInfoChangeListener> listeners =
                ShortcutManagerCompat.getShortcutInfoChangeListeners();
        assertEquals(1, listeners.size());
        assertTrue(listeners.get(0) instanceof TestShortcutInfoChangeListener);
    }

    @SmallTest
    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testReportShortcutUsed() {
        final ShortcutManager mockShortcutManager = mock(ShortcutManager.class);
        doReturn(mockShortcutManager).when(mContext).getSystemService(
                eq(Context.SHORTCUT_SERVICE));

        ShortcutManagerCompat.reportShortcutUsed(mContext, "id");

        verify(mockShortcutManager, times(1)).reportShortcutUsed("id");
        verify(mShortcutInfoChangeListener, times(1))
                .onShortcutUsageReported(Collections.singletonList("id"));
    }

    private void verifyLegacyIntent(Intent intent) {
        assertNotNull(intent);
        assertEquals("Test shortcut", intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
        assertEquals("No-op", ((Intent) intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT))
                .getAction());
    }

    private void setMocks(ResolveInfo... infos) {
        PackageManager pm = mock(PackageManager.class);
        when(pm.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(Arrays.asList(infos));
        reset(mContext);
        doReturn(pm).when(mContext).getPackageManager();
        doNothing().when(mContext).sendBroadcast(any(Intent.class));
        doNothing().when(mContext).sendOrderedBroadcast(any(Intent.class), nullable(String.class),
                any(BroadcastReceiver.class), nullable(Handler.class), anyInt(),
                nullable(String.class), nullable(Bundle.class));
    }

    private ResolveInfo mockResolveInfo(String permission) {
        ActivityInfo aInfo = new ActivityInfo();
        aInfo.packageName = mContext.getPackageName();
        aInfo.permission = permission;
        ResolveInfo rInfo = new ResolveInfo();
        rInfo.activityInfo = aInfo;
        return rInfo;
    }

    private ArrayList<ShortcutInfo> getShortcutInfos() {
        ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "shortcut-1")
                .setShortLabel("first")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(0)
                .build()
                .toShortcutInfo());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "shortcut-2")
                .setShortLabel("second")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(2)
                .build()
                .toShortcutInfo());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "shortcut-3")
                .setShortLabel("third")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build()
                .toShortcutInfo());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "shortcut-4")
                .setShortLabel("fourth")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build()
                .toShortcutInfo());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "shortcut-5")
                .setShortLabel("fifth")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build()
                .toShortcutInfo());

        return shortcuts;
    }

    private ArrayList<ShortcutInfoCompat> getShortcutInfoCompats() {
        ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "bitmap-shortcut")
                .setShortLabel("bitmap")
                .setIcon(createBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(0)
                .build());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "adaptive-bitmap-shortcut")
                .setShortLabel("adaptive bitmap")
                .setIcon(createAdaptiveBitmapIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(2)
                .build());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "uri-bitmap-shortcut")
                .setShortLabel("uri bitmap")
                .setIcon(createUriIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext, "uri-adaptive-bitmap-shortcut")
                .setShortLabel("uri adaptive bitmap")
                .setIcon(createUriAdaptiveIcon())
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext,
                "uri-adaptive-bitmap-shortcut-with-invalid-uri")
                .setShortLabel("uri adaptive bitmap with invalid uri")
                .setIcon(IconCompat.createWithAdaptiveBitmapContentUri(
                        "http://non-existing-uri"))
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build());

        shortcuts.add(new ShortcutInfoCompat.Builder(mContext,
                "shortcut-without-icon")
                .setShortLabel("shortcut without icon")
                .setIntent(new Intent().setAction(Intent.ACTION_DEFAULT))
                .setRank(4)
                .build());

        return shortcuts;
    }

    private void removeShortcuts() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(mContext);
    }

    private Bitmap createRawBitmap(String text) {
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        float density = mContext.getResources().getDisplayMetrics().density;
        int x = (int) (72 * density);
        int y = (int) (72 * density);
        Bitmap bmp = Bitmap.createBitmap(x, y, conf);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.BLUE);

        Paint mTextPaint = new Paint();
        mTextPaint.setColor(Color.RED);
        mTextPaint.setTextSize(mContext.getResources().getDimension(R.dimen.text_medium_size));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2)
                - ((mTextPaint.descent() + mTextPaint.ascent()) / 2));
        canvas.drawText(text, xPos, yPos, mTextPaint);
        return bmp;
    }

    private Uri bitmapToUri(Bitmap bmp, String name) {
        File root = new File(mContext.getFilesDir(), SHORTCUT_ICON_PATH);
        if (!root.exists()) {
            root.mkdir();
        }
        File file = new File(root, name);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return FileProvider.getUriForFile(mContext, TEST_AUTHORITY, file);
    }

    private IconCompat createBitmapIcon() {
        return IconCompat.createWithBitmap(createRawBitmap("B"));
    }

    private IconCompat createAdaptiveBitmapIcon() {
        return IconCompat.createWithAdaptiveBitmap(createRawBitmap("AB"));
    }

    private IconCompat createUriIcon() {
        Uri uri = bitmapToUri(createRawBitmap("U"), "uri-icon");
        return IconCompat.createWithContentUri(uri);
    }

    private IconCompat createUriAdaptiveIcon() {
        Uri uri = bitmapToUri(createRawBitmap("AU"), "uri-adaptive-icon");
        return IconCompat.createWithAdaptiveBitmapContentUri(uri);
    }
}
