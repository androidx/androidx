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
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;

import static androidx.core.content.PackageManagerCompat.APP_HIBERNATION_DISABLED;
import static androidx.core.content.PackageManagerCompat.APP_HIBERNATION_ENABLED;
import static androidx.core.content.PackageManagerCompat.PERMISSION_REVOCATION_DISABLED;
import static androidx.core.content.PackageManagerCompat.PERMISSION_REVOCATION_ENABLED;
import static androidx.core.content.PackageManagerCompat.UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE;
import static androidx.core.content.UnusedAppRestrictionsBackportService.ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION;

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
import android.os.Build;

import androidx.annotation.RequiresApi;
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
    private PackageManager mPackageManager = mock(PackageManager.class);
    private static final String VERIFIER_PACKAGE_NAME = "verifier.package.name";
    private static final String VERIFIER_PACKAGE_NAME2 = "verifier.package.name.2";
    private static final String NON_VERIFIER_PACKAGE_NAME = "non.verifier.package.name";
    private ArgumentCaptor<Intent> mIntentCaptor = ArgumentCaptor.forClass(Intent.class);

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_disabled_returnsAppHibernationDisabled()
            throws Exception {
        // Mark the application as exempt from app hibernation, so the feature is disabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(APP_HIBERNATION_DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_enabled_returnsAppHibernationEnabled()
            throws Exception {
        // Mark the application as _not_ exempt from app hibernation, so the feature is enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(APP_HIBERNATION_ENABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30_disabled_returnsPermRevocationDisabled()
            throws Exception {
        // Mark the application as exempt from permission revocation, so the feature is disabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(PERMISSION_REVOCATION_DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30Plus_enabled_returnsPermRevocationEnabled()
            throws Exception {
        // Mark the application as _not_ exempt from permission revocation, so the feature is
        // enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(PERMISSION_REVOCATION_ENABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_noRevocationApp_returnsNotAvailable()
            throws Exception {
        // Don't install an app that can resolve the permission auto-revocation intent

        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE);
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

        assertThat(resultFuture.get()).isEqualTo(UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_verifierRevocationApp_bindsService() {
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

    @Test
    @SdkSuppress(maxSdkVersion = LOLLIPOP)
    public void getUnusedAppRestrictionsStatus_preApi23_returnsFeatureNotAvailable()
            throws Exception {
        ListenableFuture<Integer> resultFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(mContext);

        assertThat(resultFuture.get()).isEqualTo(UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE);
    }

    /**
     * Setup applications with the verifier role can handle unused app restriction features. In
     * this case, they are permission revocation apps.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
