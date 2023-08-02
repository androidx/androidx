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

package androidx.car.app.validation;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.content.res.Resources;

import androidx.car.app.HostInfo;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;

/** Tests for {@link HostValidator} with mocked API 28 PackageManager */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = 28)
public class HostValidatorTestApi28 {
    private static final String VALID_PACKAGE_NAME = "com.foo";
    private static final Signature[] VALID_SIGNATURES = new Signature[]{
            new Signature("0123"),
            new Signature("4567"),
    };
    private static final String[] VALID_DIGESTS = new String[]{
            "b71de80778f2783383f5d5a3028af84eab2f18a4eb38968172ca41724dd4b3f4",
            "0aca264d8aa9b222fa45034f123b1a068550527774ea23ca741281f80dd6029d",
    };
    private static final int NON_SYSTEM_UID = 123;

    private HostValidator.Builder mHostValidatorBuilder;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ShadowLog.stream = System.out;
        Context context = spy(ApplicationProvider.getApplicationContext());
        when(context.getPackageManager()).thenReturn(mPackageManager);
        mHostValidatorBuilder = new HostValidator.Builder(context);
        when(context.getResources()).thenReturn(mResources);
    }

    @Test
    public void isValidHost_withRotatedSignatures_originalSignature_accepted() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURES);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        mHostValidatorBuilder.addAllowedHost(VALID_PACKAGE_NAME, VALID_DIGESTS[0]);
        HostValidator hostValidator = mHostValidatorBuilder.build();

        assertThat(hostValidator.isValidHost(hostInfo)).isTrue();
    }

    @Test
    public void isValidHost_withRotatedSignatures_newSignature_accepted() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURES);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        mHostValidatorBuilder.addAllowedHost(VALID_PACKAGE_NAME, VALID_DIGESTS[1]);
        HostValidator hostValidator = mHostValidatorBuilder.build();

        assertThat(hostValidator.isValidHost(hostInfo)).isTrue();
    }

    @SuppressWarnings("deprecation")
    private void installPackage(String packageName, Signature[] signatures) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.uid = NON_SYSTEM_UID;
        packageInfo.packageName = packageName;
        packageInfo.signingInfo = mock(SigningInfo.class);
        when(packageInfo.signingInfo.getSigningCertificateHistory()).thenReturn(signatures);
        try {
            when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
        } catch (PackageManager.NameNotFoundException ex) {
            throw new IllegalStateException("Error mocking package manager", ex);
        }
    }
}
