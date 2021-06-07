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

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
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

import java.util.Arrays;

@SmallTest
@RunWith(AndroidJUnit4.class)
/** Tests for {@link IntentCompat}. */
public class IntentCompatTest {

    private Context mContext;
    private PackageManager mPackageManager = mock(PackageManager.class);
    private static final String SYSTEM_APP_PACKAGE_NAME = "system.app";

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
    public void makeIntentToAllowlistUnusedAppRestrictions_api31Plus() {
        String packageName = "package.name";
        Intent activityIntent = IntentCompat.makeIntentToAllowlistUnusedAppRestrictions(
                mContext, packageName);

        assertThat(activityIntent.getAction()).isEqualTo(ACTION_APPLICATION_DETAILS_SETTINGS);
        assertThat(activityIntent.getData()).isEqualTo(Uri.parse(packageName));
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void makeIntentToAllowlistUnusedAppRestrictions_api30() {
        String packageName = "package.name";
        Intent activityIntent = IntentCompat.makeIntentToAllowlistUnusedAppRestrictions(
                mContext, packageName);

        assertThat(activityIntent.getAction())
                .isEqualTo("android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getData()).isEqualTo(Uri.parse(packageName));
    }

    @Test
    @SdkSuppress(maxSdkVersion = Q)
    public void makeIntentToAllowlistUnusedAppRestrictions_preApi30_revocationSystemAppNotExists() {
        // Don't install a system app that can resolve the permission auto-revocation intent

        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.makeIntentToAllowlistUnusedAppRestrictions(
                mContext, "package.name"));
    }

    @Test
    @SdkSuppress(maxSdkVersion = Q)
    public void makeIntentToAllowlistUnusedAppRestrictions_preApi30_revocationSystemAppExists() {
        setupPermissionRevocationSystemApp(SYSTEM_APP_PACKAGE_NAME);
        Intent activityIntent = IntentCompat.makeIntentToAllowlistUnusedAppRestrictions(
                mContext, "package.name");

        assertThat(activityIntent.getAction()).isEqualTo(
                "android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getPackage()).isEqualTo(SYSTEM_APP_PACKAGE_NAME);
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void areUnusedAppRestrictionsAvailable_api30Plus_returnsTrue() {
        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isTrue();
    }

    @Test
    @SdkSuppress(maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAvailable_preApi30_noRevocationSystemApps_returnsFalse() {
        // Don't install a system app that can resolve the permission auto-revocation intent

        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isFalse();
    }

    @Test
    @SdkSuppress(maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAvailable_preApi30_revocationSystemApp_returnsTrue() {
        setupPermissionRevocationSystemApp(SYSTEM_APP_PACKAGE_NAME);

        assertThat(IntentCompat.areUnusedAppRestrictionsAvailable(mContext)).isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void areUnusedAppRestrictionsAllowlisted_api30Plus_returnsPackageManagerAllowlisted() {
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        assertThat(IntentCompat.areUnusedAppRestrictionsAllowlisted(mContext)).isTrue();
    }

    @Test
    @SdkSuppress(maxSdkVersion = Q)
    public void areUnusedAppRestrictionsAllowlisted_preApi30_returnsFalse() {
        assertThat(IntentCompat.areUnusedAppRestrictionsAllowlisted(mContext)).isFalse();
    }

    /**
     * Setup an application that can handle unused app restriction features. In this case, this is
     * a permission revocation system app.
     */
    private void setupPermissionRevocationSystemApp(String packageName) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.uid = 12345;
        appInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        appInfo.packageName = packageName;

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.name = "Name needed to keep toString() happy :)";
        activityInfo.applicationInfo = appInfo;

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        resolveInfo.providerInfo = new ProviderInfo();
        resolveInfo.providerInfo.name = "Name needed to keep toString() happy :)";

        // Mark the application as being able to resolve the intent
        when(mPackageManager.queryIntentActivities(
                nullable(Intent.class), eq(PackageManager.MATCH_SYSTEM_ONLY)))
                .thenReturn(Arrays.asList(resolveInfo));
    }
}
