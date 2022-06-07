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
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;

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
import org.robolectric.shadows.ShadowBinder;

@RunWith(RobolectricTestRunner.class)
public class HealthDataSdkServiceStubImplTest {
    private HealthDataSdkServiceStubImpl mService;

    @Rule public MockitoRule mRule = MockitoJUnit.rule();
    @Mock private ISetPermissionTokenCallback mSetPermissionTokenCallback;
    @Mock private IGetPermissionTokenCallback mGetPermissionTokenCallback;
    @Captor ArgumentCaptor<String> mStringCaptor;

    @Before
    public void setup() {
        mService =
                new HealthDataSdkServiceStubImpl(
                        ApplicationProvider.getApplicationContext(), directExecutor());
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
    public void setPermissionToken_getPermissionToken_expectSameToken() throws RemoteException {
        installValidPackageInfo(ALLOWED_PACKAGE_NAME);
        mService.setPermissionToken(ALLOWED_PACKAGE_NAME, "token", mSetPermissionTokenCallback);

        verify(mSetPermissionTokenCallback, times(1)).onSuccess();

        mService.getPermissionToken(ALLOWED_PACKAGE_NAME, mGetPermissionTokenCallback);

        verify(mGetPermissionTokenCallback, times(1)).onSuccess(mStringCaptor.capture());
        assertThat(mStringCaptor.getValue()).isEqualTo("token");
    }

    private static void installValidPackageInfo(String packageName) {
        installPackageInfo(packageName, 123);
        ShadowBinder.setCallingUid(123);
    }

    private static void installInvalidPackageInfo(String packageName) {
        installPackageInfo(packageName, 123);
        ShadowBinder.setCallingUid(456);
    }

    private static void installPackageInfo(String packageName, int uid) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.uid = uid;
        shadowOf(getApplicationContext().getPackageManager()).installPackage(packageInfo);
    }
}
