/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.provider.FontRequest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DefaultEmojiCompatConfigTest {

    @Before
    public void before() {
        DefaultEmojiCompatConfig.resetInstance();
    }

    @AfterClass
    public static void after() {
        DefaultEmojiCompatConfig.resetInstance();
    }

    @Test
    public void onAllApis_callingDoesntCrash_withRealContext() {
        EmojiCompat.Config result =
                DefaultEmojiCompatConfig.create(ApplicationProvider.getApplicationContext());
        if (providerOnSystem()) {
            assertNotNull(result);
        } else {
            assertNull(result);
        }
    }

    private boolean providerOnSystem() {
        if (Build.VERSION.SDK_INT < 19) {
            return false;
        }
        List<ResolveInfo> result = ApplicationProvider.getApplicationContext()
                .getPackageManager().queryIntentContentProviders(generateIntent(), 0);
        return result.stream().anyMatch((item) ->
                (item.providerInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        == ApplicationInfo.FLAG_SYSTEM);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenNoLookup_returnsNull() {
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));
        EmojiCompat.Config actual = DefaultEmojiCompatConfig.create(mockContext);
        assertNull(actual);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenNoLookup_onlyQueriesOnce_onRepeatedQueries() {
        Context mockContext = mock(Context.class);
        PackageManager mockPackageManager = mock(PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

        DefaultEmojiCompatConfig.create(mockContext);
        verify(mockPackageManager).queryIntentContentProviders(any(), anyInt());

        DefaultEmojiCompatConfig.create(mockContext);
        verifyNoMoreInteractions(mockPackageManager);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenProviderFound_returnsConfig() throws PackageManager.NameNotFoundException {
        ResolveInfo info = generateResolveInfo(
                "some package", "some authority", ApplicationInfo.FLAG_SYSTEM
        );
        Signature[] signatures = generateSignatures(3);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = makeMockHelper(
                info, signatures, true);

        EmojiCompat.Config actual = DefaultEmojiCompatConfig.create(mockContext, helper);

        assertNotNull("Expected actual emoji compat config from valid lookup", actual);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenProviderFound_onlyQueriesOnce() throws PackageManager.NameNotFoundException {
        ResolveInfo info = generateResolveInfo(
                "some package", "some authority", ApplicationInfo.FLAG_SYSTEM
        );
        Signature[] signatures = generateSignatures(3);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = makeMockHelper(
                info, signatures, true);

        EmojiCompat.Config actual = DefaultEmojiCompatConfig.create(mockContext, helper);

        assertNotNull(actual); // just ensuring test isn't false-positive
        verify(helper).queryIntentContentProviders(any(), any(), anyInt());
        verify(helper).getSigningSignatures(any(), any());
        verify(helper).getProviderInfo(eq(info));

        DefaultEmojiCompatConfig.create(mockContext);
        verifyNoMoreInteractions(helper);

    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenProviderFound_butNotSystemInstalled_returnsNull()
            throws PackageManager.NameNotFoundException {
        ResolveInfo info = generateResolveInfo(
                "some package", "some authority", 0
        );
        Signature[] signatures = generateSignatures(3);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = makeMockHelper(
                info, signatures, false);

        EmojiCompat.Config actual = DefaultEmojiCompatConfig.create(mockContext, helper);

        assertNull("Expected no emoji compat config when no system package", actual);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenQueryingIntent_usesRightIntent() throws PackageManager.NameNotFoundException {
        ResolveInfo info = generateResolveInfo(
                "some package", "some authority", 0
        );

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = makeMockHelper(
                info, generateSignatures(3), false);

        DefaultEmojiCompatConfig.create(mockContext, helper);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(helper).queryIntentContentProviders(any(), intentArgumentCaptor.capture(),
                anyInt());

        assertEquals(generateIntent().getAction(), intentArgumentCaptor.getValue().getAction());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenProviderFound_configMapsCorrectly()
            throws PackageManager.NameNotFoundException {
        String packageName = "queried package name";
        String authority = "queried package authority";
        ResolveInfo info = generateResolveInfo(packageName, authority, ApplicationInfo.FLAG_SYSTEM);
        Signature[] signatures = generateSignatures(7);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = makeMockHelper(
                info, signatures, true);

        DefaultEmojiCompatConfig.create(mockContext, helper);

        FontRequest request = DefaultEmojiCompatConfig.getFontRequest();
        assertEquals(packageName, request.getProviderPackage());
        assertEquals(authority, request.getProviderAuthority());
        assertEquals("emojicompat-emoji-font", request.getQuery());
        assertEquals(0, request.getCertificatesArrayResId());
        assert request.getCertificates() != null;
        assertCertificatesEqual(signatures, request.getCertificates().get(0));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenProviderFound_returnsDifferentConfig_everyCallToGet()
            throws PackageManager.NameNotFoundException {
        ResolveInfo info = generateResolveInfo(
                "some package", "some authority", ApplicationInfo.FLAG_SYSTEM
        );
        Signature[] signatures = generateSignatures(3);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = makeMockHelper(
                info, signatures, true);

        EmojiCompat.Config actual = DefaultEmojiCompatConfig.create(mockContext, helper);
        EmojiCompat.Config actual2 = DefaultEmojiCompatConfig.create(mockContext, helper);

        assertNotSame(actual, actual2);
    }

    @NonNull
    private DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper makeMockHelper(
            @NonNull ResolveInfo info, @NonNull Signature[] signatures, boolean isSystemInstalled)
            throws PackageManager.NameNotFoundException {
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = mock(
                DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper.class);
        when(helper.getSigningSignatures(any(), any())).thenReturn(signatures);
        when(helper.queryIntentContentProviders(any(), any(), anyInt())).thenReturn(
                Collections.singletonList(info));
        when(helper.getProviderInfo(eq(info))).thenReturn(info.providerInfo);
        return helper;
    }

    private void assertCertificatesEqual(Signature[] signatures, List<byte[]> certificates) {
        assertEquals(signatures.length, certificates.size());
        for (int i = 0; i < signatures.length; i++) {
            assertArrayEquals(signatures[i].toByteArray(), certificates.get(i));
        }
    }

    private Intent generateIntent() {
        return new Intent("androidx.content.action.LOAD_EMOJI_FONT");
    }

    private Signature[] generateSignatures(int size) {
        Signature[] signatures = new Signature[size];
        for (int i = 0; i < size; i++) {
            signatures[i] = new Signature(String.format("%024x", 0));
        }
        return signatures;
    }

    private ResolveInfo generateResolveInfo(String packageName, String authority, int flags) {
        ResolveInfo info = new ResolveInfo();
        info.providerInfo = new ProviderInfo();
        info.providerInfo.authority = authority;
        info.providerInfo.packageName = packageName;
        info.providerInfo.name = "name to make toString happy :)";
        info.providerInfo.applicationInfo = new ApplicationInfo();
        info.providerInfo.applicationInfo.flags = flags;
        return info;
    }
}
