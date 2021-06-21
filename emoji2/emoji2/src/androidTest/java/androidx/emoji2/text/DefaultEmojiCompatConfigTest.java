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
import androidx.annotation.Nullable;
import androidx.core.provider.FontRequest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DefaultEmojiCompatConfigTest {

    @Test
    public void onAllApis_callingDoesntCrash_withRealContext() {
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(null);
        EmojiCompat.Config result = factory.create(ApplicationProvider.getApplicationContext());
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
    public void whenNoLookup_returnsNull() throws PackageManager.NameNotFoundException {
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(
                        makeMockHelper(null, generateSignatures(2)));
        EmojiCompat.Config actual = factory.create(mockContext);
        assertNull(actual);
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
                info, signatures);

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(helper);
        EmojiCompat.Config actual = factory.create(mockContext);

        assertNotNull("Expected actual emoji compat config from valid lookup", actual);
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

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(
                        makeMockHelper(info, signatures));
        EmojiCompat.Config actual = factory.create(mockContext);

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
                info, generateSignatures(3));

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(helper);
        factory.create(mockContext);

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

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(
                        makeMockHelper(info, signatures));

        FontRequest request = factory.queryForDefaultFontRequest(mockContext);
        assert request != null;
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

        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(
                        makeMockHelper(info, signatures));
        EmojiCompat.Config actual = factory.create(mockContext);
        EmojiCompat.Config actual2 = factory.create(mockContext);

        assertNotSame(actual, actual2);
    }

    @NonNull
    private DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper makeMockHelper(
            @Nullable ResolveInfo info, @NonNull Signature[] signatures)
            throws PackageManager.NameNotFoundException {
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper helper = mock(
                DefaultEmojiCompatConfig.DefaultEmojiCompatConfigHelper.class);
        when(helper.getSigningSignatures(any(), any())).thenReturn(signatures);
        if (info != null) {
            when(helper.queryIntentContentProviders(any(), any(), anyInt())).thenReturn(
                    Collections.singletonList(info));
            when(helper.getProviderInfo(eq(info))).thenReturn(info.providerInfo);
        } else {
            when(helper.queryIntentContentProviders(any(), any(), anyInt())).thenReturn(
                    Collections.emptyList());
        }
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
