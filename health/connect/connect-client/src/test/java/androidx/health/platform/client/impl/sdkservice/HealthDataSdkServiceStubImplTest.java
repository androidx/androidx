/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl.sdkservice;

import static androidx.health.platform.client.impl.sdkservice.HealthDataSdkServiceStubImpl.ALLOWED_PACKAGE_NAME;
import static androidx.health.platform.client.service.HealthDataServiceConstants.DEFAULT_PROVIDER_RELEASE_CERT_SHA256;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBinder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class HealthDataSdkServiceStubImplTest {
    private HealthDataSdkServiceStubImpl mService;
    private PackageManager mMockPackageManager;

    @Rule public MockitoRule mRule = MockitoJUnit.rule();
    @Mock private ISetPermissionTokenCallback mSetPermissionTokenCallback;
    @Mock private IGetPermissionTokenCallback mGetPermissionTokenCallback;
    @Captor ArgumentCaptor<String> mStringCaptor;

    @Before
    public void setup() {
        mMockPackageManager = mock(PackageManager.class);
        Context context =
                new ContextWrapper(ApplicationProvider.getApplicationContext()) {
                    @Override
                    public PackageManager getPackageManager() {
                        return mMockPackageManager;
                    }
                };

        mService = new HealthDataSdkServiceStubImpl(context, directExecutor());
    }

    @Test
    public void setPermissionToken_notAllowedPackage_expectError() {
        String packageName = ALLOWED_PACKAGE_NAME + "not_allowed";
        installValidPackageInfo(packageName);
        assertThrows(
                SecurityException.class,
                () -> mService.setPermissionToken(packageName, "token", null));
    }

    @Test
    public void setPermissionToken_healthDataPackageNameAndUidMismatch_expectException() {
        installInvalidPackageInfo(ALLOWED_PACKAGE_NAME);
        assertThrows(
                SecurityException.class,
                () -> mService.setPermissionToken(ALLOWED_PACKAGE_NAME, "token", null));
    }

    @Test
    public void getPermissionToken_notAllowedPackage_expectError() {
        String packageName = ALLOWED_PACKAGE_NAME + "not_allowed";
        installValidPackageInfo(packageName);
        assertThrows(SecurityException.class, () -> mService.getPermissionToken(packageName, null));
    }

    @Test
    public void getPermissionToken_healthDataPackageNameAndUidMismatch_expectException() {
        installInvalidPackageInfo(ALLOWED_PACKAGE_NAME);
        assertThrows(
                SecurityException.class,
                () -> mService.getPermissionToken(ALLOWED_PACKAGE_NAME, null));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void setPermissionToken_getPermissionToken_expectSameToken() throws RemoteException {
        installValidPackageInfo(ALLOWED_PACKAGE_NAME);
        when(mMockPackageManager.hasSigningCertificate(
                        ALLOWED_PACKAGE_NAME,
                        DEFAULT_PROVIDER_RELEASE_CERT_SHA256,
                        PackageManager.CERT_INPUT_SHA256))
                .thenReturn(true);
        mService.setPermissionToken(ALLOWED_PACKAGE_NAME, "token", mSetPermissionTokenCallback);

        verify(mSetPermissionTokenCallback, times(1)).onSuccess();

        mService.getPermissionToken(ALLOWED_PACKAGE_NAME, mGetPermissionTokenCallback);

        verify(mGetPermissionTokenCallback, times(1)).onSuccess(mStringCaptor.capture());
        assertThat(mStringCaptor.getValue()).isEqualTo("token");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void setPermissionToken_signatureMismatch_expectException() {
        installValidPackageInfo(ALLOWED_PACKAGE_NAME);
        when(mMockPackageManager.hasSigningCertificate(
                        ALLOWED_PACKAGE_NAME,
                        DEFAULT_PROVIDER_RELEASE_CERT_SHA256,
                        PackageManager.CERT_INPUT_SHA256))
                .thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> mService.setPermissionToken(ALLOWED_PACKAGE_NAME, "token", null));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void setPermissionToken_sdkTooLow_expectException() {
        installValidPackageInfo(ALLOWED_PACKAGE_NAME);

        assertThrows(
                SecurityException.class,
                () -> mService.setPermissionToken(ALLOWED_PACKAGE_NAME, "token", null));
    }

    private void installValidPackageInfo(String packageName) {
        int uid = 123;
        ShadowBinder.setCallingUid(uid);
        when(mMockPackageManager.getPackagesForUid(uid)).thenReturn(new String[] {packageName});
    }

    private void installInvalidPackageInfo(String packageName) {
        ShadowBinder.setCallingUid(456);
        when(mMockPackageManager.getPackagesForUid(123)).thenReturn(new String[] {packageName});
        when(mMockPackageManager.getPackagesForUid(456)).thenReturn(new String[] {});
    }
}
