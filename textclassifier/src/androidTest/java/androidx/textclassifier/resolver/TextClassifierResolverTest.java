/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier.resolver;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@SmallTest
public class TextClassifierResolverTest {
    private static final byte[] VALID_CERTIFICATE = new byte[]{0x1, 0x2, 0x3};
    private static final byte[] INVALID_CERTIFICATE = new byte[]{0x4, 0x5, 0x6};

    private static final PackageInfo VALID_PACKAGE = new PackageInfo();
    static {
        VALID_PACKAGE.packageName = "pkg.a";
        VALID_PACKAGE.signatures = new Signature[]{new Signature(VALID_CERTIFICATE)};
    }

    // Package doesn't have the TCS implemented / exported.
    private static final PackageInfo NO_SERVICE_PACKAGE = new PackageInfo();
    static {
        NO_SERVICE_PACKAGE.packageName = "pkg.d";
        NO_SERVICE_PACKAGE.signatures = new Signature[]{new Signature(VALID_CERTIFICATE)};
    }

    private static final PackageInfo[] INSTALLED_PACKAGES = new PackageInfo[]{
            VALID_PACKAGE, NO_SERVICE_PACKAGE
    };

    private static final TextClassifierEntry VALID_ENTRY =
            createTextClassifierEntryFromPackage(VALID_PACKAGE);

    // An entry with a package that is not installed.
    private static final TextClassifierEntry NOT_INSTALLED_ENTRY =
            TextClassifierEntry.createPackageEntry("not.exist", "not.exist");

    // An entry with the certificate that does not match with the actual one.
    private static final TextClassifierEntry INVALID_CERT_ENTRY =
            TextClassifierEntry.createPackageEntry(VALID_ENTRY.packageName,
                    Base64.encodeToString(INVALID_CERTIFICATE, Base64.NO_WRAP));

    private static final TextClassifierEntry NO_SERVICE_ENTRY =
            createTextClassifierEntryFromPackage(NO_SERVICE_PACKAGE);

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private TextClassifierResolver mTextClassifierResolver;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        when(mPackageManager.getPackageInfo(anyString(), anyInt())).then(new Answer<PackageInfo>() {
                @Override
                public PackageInfo answer(InvocationOnMock invocation) throws Throwable {
                    String packageName = invocation.getArgument(0);
                    for (PackageInfo packageInfo : INSTALLED_PACKAGES) {
                        if (packageInfo.packageName.equals(packageName)) {
                            return packageInfo;
                        }
                    }
                    throw new PackageManager.NameNotFoundException();
                }});

        when(mPackageManager.resolveService(
                any(Intent.class), anyInt())).then(new Answer<ResolveInfo>() {
                    @Override
                    public ResolveInfo answer(InvocationOnMock invocation) throws Throwable {
                        Intent intent = invocation.getArgument(0);
                        if (NO_SERVICE_PACKAGE.packageName.equals(intent.getPackage())) {
                            return null;
                        }
                        return new ResolveInfo();
                    }});

        if (BuildCompat.isAtLeastP()) {
            when(mPackageManager.hasSigningCertificate(anyString(), any(byte[].class), anyInt()))
                    .then(new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                                String packageName = invocation.getArgument(0);
                                byte[] certificate = invocation.getArgument(1);
                                return VALID_PACKAGE.packageName.equals(packageName)
                                        && Arrays.equals(
                                        certificate, computeSha256DigestBytes(VALID_CERTIFICATE));
                            }});
        }
        mTextClassifierResolver = new TextClassifierResolver(mContext);
    }

    @Test
    public void testFindBestMatch_oneValidEntry() {
        assertThat(mTextClassifierResolver.findBestMatch(Collections.singletonList(VALID_ENTRY)))
                .isEqualTo(VALID_ENTRY);
    }

    @Test
    public void testFindBestMatch_invalidEntriesComeFirst() {
        assertThat(mTextClassifierResolver.findBestMatch(
                Arrays.asList(
                        NOT_INSTALLED_ENTRY,
                        INVALID_CERT_ENTRY,
                        NO_SERVICE_ENTRY,
                        VALID_ENTRY)))
                .isEqualTo(VALID_ENTRY);
    }

    @Test
    public void testFindBestMatch_noValidEntries() {
        assertThat(mTextClassifierResolver.findBestMatch(
                Arrays.asList(
                        NOT_INSTALLED_ENTRY,
                        INVALID_CERT_ENTRY,
                        NO_SERVICE_ENTRY)))
                .isNull();
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testFindBestMatch_system_P() {
        when(mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE)).thenReturn(
                InstrumentationRegistry.getTargetContext().getSystemService(
                        Context.TEXT_CLASSIFICATION_SERVICE));

        TextClassifierEntry bestMatch = mTextClassifierResolver.findBestMatch(
                Arrays.asList(
                        TextClassifierEntry.createOemEntry(),
                        TextClassifierEntry.createAospEntry()));
        assertThat(bestMatch.isOem() || bestMatch.isAosp()).isTrue();
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    public void testFindBestMatch_system_beforeP() {
        when(mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE)).thenReturn(
                InstrumentationRegistry.getTargetContext().getSystemService(
                        Context.TEXT_CLASSIFICATION_SERVICE));

        TextClassifierEntry bestMatch = mTextClassifierResolver.findBestMatch(
                Arrays.asList(
                        TextClassifierEntry.createOemEntry(),
                        TextClassifierEntry.createAospEntry()));
        assertThat(bestMatch.isAosp()).isTrue();
    }

    private static TextClassifierEntry createTextClassifierEntryFromPackage(
            PackageInfo packageInfo) {
        return TextClassifierEntry.createPackageEntry(
                packageInfo.packageName,
                Base64.encodeToString(
                        computeSha256DigestBytes(packageInfo.signatures[0].toByteArray()),
                        Base64.NO_WRAP));
    }

    private static byte[] computeSha256DigestBytes(@NonNull byte[] data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            /* can't happen */
            throw new RuntimeException("Not support SHA256!");
        }
        messageDigest.update(data);
        return messageDigest.digest();
    }
}
