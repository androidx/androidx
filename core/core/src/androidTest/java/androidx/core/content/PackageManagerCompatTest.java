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

package androidx.core.content;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;

import static androidx.core.content.UnusedAppRestrictionsBackportService.ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_30;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_31;
import static androidx.core.content.UnusedAppRestrictionsConstants.DISABLED;
import static androidx.core.content.UnusedAppRestrictionsConstants.ERROR;
import static androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
/** Tests for {@link PackageManagerCompat}. */
public class PackageManagerCompatTest {

    private Context mContext;
    private final PackageManager mPackageManager = mock(PackageManager.class);
    private static final String VERIFIER_PACKAGE_NAME = "verifier.package.name";
    private static final String VERIFIER_PACKAGE_NAME2 = "verifier.package.name.2";
    private static final String NON_VERIFIER_PACKAGE_NAME = "non.verifier.package.name";
    private final ArgumentCaptor<Intent> mIntentCaptor = ArgumentCaptor.forClass(Intent.class);

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    // UserManagerCompat#isUserUnlocked always returns true if on a pre-N OS version
    @SdkSuppress(minSdkVersion = N)
    public void getUnusedAppRestrictionsStatus_whenLockedDirectBootMode_returnsErrorStatus()
            throws Exception {
        UserManager userManager = mock(UserManager.class);
        when(mContext.getSystemService(UserManager.class)).thenReturn(userManager);
        when(userManager.isUserUnlocked()).thenReturn(false);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(ERROR);
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30Plus_preApi30App_returnsErrorStatus()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting pre API 30
        appInfo.targetSdkVersion = Q;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(ERROR);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_preApi30App_returnsErrorStatus()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting pre API 30
        appInfo.targetSdkVersion = Q;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(ERROR);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_api31App_disabled_returnsDisabledStatus()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting API 31
        appInfo.targetSdkVersion = 31;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        // Mark the application as exempt from app hibernation, so the feature is disabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_api31App_enabled_returnsApi31Status()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting API 31
        appInfo.targetSdkVersion = 31;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        // Mark the application as _not_ exempt from app hibernation, so the feature is enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(API_31);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_api30App_enabled_returnsApi30Status()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting below API 31
        appInfo.targetSdkVersion = R;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        // Mark the application as _not_ exempt from app hibernation, so the feature is enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(API_30);
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30_disabled_returnsDisabledStatus()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting API 30+
        appInfo.targetSdkVersion = R;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        // Mark the application as exempt from permission revocation, so the feature is disabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30Plus_enabled_returnsApi30Status()
            throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting API 30+
        appInfo.targetSdkVersion = R;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        // Mark the application as _not_ exempt from permission revocation, so the feature is
        // enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(API_30);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_noRevocationApp_returnsFeatureNotAvailable()
            throws Exception {
        // Don't install an app that can resolve the permission auto-revocation intent

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(FEATURE_NOT_AVAILABLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_noVerifierRevokeApp_returnsNotAvailable()
            throws Exception {
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(NON_VERIFIER_PACKAGE_NAME));
        // Do not set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                NON_VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_DENIED);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(FEATURE_NOT_AVAILABLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_verifierRevocationApp_bindsService() {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting API 30+
        appInfo.targetSdkVersion = R;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);

        PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        verify(mContext).bindService(
                mIntentCaptor.capture(),
                any(UnusedAppRestrictionsBackportServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE));
        Intent actualIntent = mIntentCaptor.getValue();
        assertThat(actualIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
        assertThat(actualIntent.getAction())
                .isEqualTo(ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION);
        // We do not check the future value as this would require constructing a fake service to
        // connect to.
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_manyVerifierRevocationApps_doesNotThrow() {
        ApplicationInfo appInfo = new ApplicationInfo();
        // Mark the application as targeting API 30+
        appInfo.targetSdkVersion = R;
        when(mContext.getApplicationInfo()).thenReturn(appInfo);
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME,
                VERIFIER_PACKAGE_NAME2));
        // Set both apps as the Verifier on the device, but we should have a graceful failure.
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME2)).thenReturn(PERMISSION_GRANTED);

        PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        verify(mContext).bindService(
                mIntentCaptor.capture(),
                any(UnusedAppRestrictionsBackportServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE));
        Intent actualIntent = mIntentCaptor.getValue();
        assertThat(actualIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
        assertThat(actualIntent.getAction())
                .isEqualTo(ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION);
        // We do not check the future value as this would require constructing a fake service to
        // connect to.
    }

    /**
     * Setup applications with the verifier role can handle unused app restriction features. In
     * this case, they are permission revocation apps.
     */
    @SuppressWarnings("deprecation")
    static void setupPermissionRevocationApps(
            PackageManager packageManager, List<String> packageNames) {
        List<ResolveInfo> resolveInfos = new ArrayList<>();

        for (String packageName : packageNames) {
            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.uid = 12345;
            appInfo.packageName = packageName;

            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = packageName;
            activityInfo.name = "Name needed to keep toString() happy :)";
            activityInfo.applicationInfo = appInfo;

            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            resolveInfo.providerInfo = new ProviderInfo();
            resolveInfo.providerInfo.name = "Name needed to keep toString() happy :)";

            resolveInfos.add(resolveInfo);
        }

        // Mark the applications as being able to resolve the AUTO_REVOKE_PERMISSIONS intent
        when(packageManager.queryIntentActivities(
                nullable(Intent.class), eq(0))).thenReturn(resolveInfos);
    }
}
