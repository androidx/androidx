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
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
/** Tests for {@link IntentCompat}. */
public class IntentCompatTest {

    private Context mContext;
    private PackageManager mPackageManager = mock(PackageManager.class);
    private static final String PACKAGE_NAME = "package.name";
    private static final String VERIFIER_PACKAGE_NAME = "verifier.package.name";
    private static final String VERIFIER_PACKAGE_NAME2 = "verifier.package.name.2";
    private static final String NON_VERIFIER_PACKAGE_NAME = "non.verifier.package.name";

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    @SdkSuppress(maxSdkVersion = ICE_CREAM_SANDWICH)
    public void makeMainSelectorActivity_preApi14() {
        String selectorAction = Intent.ACTION_MAIN;
        String selectorCategory = Intent.CATEGORY_APP_BROWSER;

        Intent activityIntent = IntentCompat.makeMainSelectorActivity(selectorAction,
                selectorCategory);

        assertThat(activityIntent.getAction()).isEqualTo(selectorAction);
        assertThat(activityIntent.getCategories()).containsExactly(selectorCategory);
    }

    @Test
    @SdkSuppress(minSdkVersion = ICE_CREAM_SANDWICH_MR1)
    public void makeMainSelectorActivity() {
        String selectorAction = Intent.ACTION_MAIN;
        String selectorCategory = Intent.CATEGORY_APP_BROWSER;

        Intent activityIntent = IntentCompat.makeMainSelectorActivity(selectorAction,
                selectorCategory);

        Intent expectedIntent = Intent.makeMainSelectorActivity(selectorAction,
                selectorCategory);
        assertThat(activityIntent.filterEquals(expectedIntent)).isTrue();
    }

    @Test
    // TODO: replace with VERSION_CODES.S once it's defined
    @SdkSuppress(minSdkVersion = 31)
    public void createManageUnusedAppRestrictionsIntent_api31Plus() {
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(ACTION_APPLICATION_DETAILS_SETTINGS);
        assertThat(activityIntent.getData()).isEqualTo(Uri.parse(PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void createManageUnusedAppRestrictionsIntent_api30() {
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction())
                .isEqualTo("android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getData()).isEqualTo(Uri.parse(PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_noRevocationApp() {
        // Don't install an app that can resolve the permission auto-revocation intent

        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, NON_VERIFIER_PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_noVerifierRevocationApp() {
        setupPermissionRevocationApps(Arrays.asList(NON_VERIFIER_PACKAGE_NAME));
        // Do not set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                NON_VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_DENIED);

        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                        mContext, PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_verifierRevocationApp() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(
                "android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_manyVerifierRevocationApps() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME, VERIFIER_PACKAGE_NAME2));
        // Set both apps as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME2)).thenReturn(PERMISSION_GRANTED);

        assertThrows(RuntimeException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                        mContext, PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(maxSdkVersion = LOLLIPOP)
    public void createManageUnusedAppRestrictionsIntent_preApi23() {
        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                        mContext, PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void areUnusedAppRestrictionsAvailable_api30Plus_returnsTrue() {
        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAvailable_preApi30_noRevocationApp_returnsFalse() {
        // Don't install an app that can resolve the permission auto-revocation intent

        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAvailable_preApi30_noVerifierRevocationApp_returnsFalse() {
        setupPermissionRevocationApps(Arrays.asList(NON_VERIFIER_PACKAGE_NAME));
        // Do not set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                NON_VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_DENIED);

        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAvailable_preApi30_verifierRevocationApp_returnsTrue() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);

        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAvailable_preApi30_manyVerifierRevocationApps_throws() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME, VERIFIER_PACKAGE_NAME2));
        // Set both apps as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME2)).thenReturn(PERMISSION_GRANTED);

        assertThrows(RuntimeException.class,
                () -> IntentCompat.areUnusedAppRestrictionsAvailable(mContext));
    }

    @Test
    @SdkSuppress(maxSdkVersion = LOLLIPOP)
    public void areUnusedAppRestrictionsAvailable_preApi23_returnsFalse() {
        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void areUnusedAppRestrictionsAllowlisted_api30Plus_returnsPackageManagerAllowlisted() {
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        assertThat(IntentCompat.areUnusedAppRestrictionsAllowlisted(mContext)).isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAllowlisted_preApi30_returnsFalse() {
        assertThat(IntentCompat.areUnusedAppRestrictionsAllowlisted(mContext)).isFalse();
    }

    @Test
    @SdkSuppress(maxSdkVersion = LOLLIPOP)
    public void areUnusedAppRestrictionsAllowlisted_preApi23_returnsFalse() {
        assertThat(IntentCompat.areUnusedAppRestrictionsAllowlisted(mContext)).isFalse();
    }

    /**
     * Setup applications that can handle unused app restriction features. In this case,
     * they are permission revocation apps.
     */
    private void setupPermissionRevocationApps(List<String> packageNames) {
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
        when(mPackageManager.queryIntentActivities(
                nullable(Intent.class), eq(0))).thenReturn(resolveInfos);
    }
}
