/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest.permission;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.test.core.content.pm.ApplicationInfoBuilder;
import androidx.test.core.content.pm.PackageInfoBuilder;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(AndroidJUnit4.class)
public class ContentUriValidatorTest {
    private static final String TEST_PACKAGE_NAME = "com.example.mypackage";
    private static final String TEST_PROVIDER_AUTHORITY = "example_authority";

    private ContentUriValidator mValidatorUnderTest;
    private final ContentUriValidator.UriPermissionValidator mMockPermissionValidator =
            mock(ContentUriValidator.UriPermissionValidator.class);

    @Before
    public void setUp() {
        mValidatorUnderTest =
                new ContentUriValidator(
                        getApplicationContext(), TEST_PACKAGE_NAME, mMockPermissionValidator);
    }

    @Test
    public void validateUri_okForExportedSamePackageProviderWithoutPermission() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("content://" + TEST_PROVIDER_AUTHORITY + "/myimage");

        assertThat(mValidatorUnderTest.validateUri(uri)).isTrue();
    }

    @Test
    public void validateUri_userIdIsStripped() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("content://myuser@" + TEST_PROVIDER_AUTHORITY + "/myimage");

        assertThat(mValidatorUnderTest.validateUri(uri)).isTrue();
    }

    @Test
    public void validateUri_notOkForNonExistentAuthority() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("content://thisauthoritydoesnotexist/myimage");

        assertThat(mValidatorUnderTest.validateUri(uri)).isFalse();
    }

    @Test
    public void validateUri_notOkIfPackageNameDoesNotMatch() {
        setupFakeApp(
                "com.a.totally.different.app",
                TEST_PROVIDER_AUTHORITY,
                /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("content://thisauthoritydoesnotexist/myimage");

        assertThat(mValidatorUnderTest.validateUri(uri)).isFalse();
    }

    @Test
    public void validateUri_notOkIfProviderNotExported() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ false);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("content://" + TEST_PROVIDER_AUTHORITY + "/myimage");

        assertThat(mValidatorUnderTest.validateUri(uri)).isFalse();
    }

    @Test
    public void validateUri_notOkIfProviderRequiresPermission() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(false);

        Uri uri = Uri.parse("content://" + TEST_PROVIDER_AUTHORITY + "/myimage");

        assertThat(mValidatorUnderTest.validateUri(uri)).isFalse();
    }

    @Test
    public void validateUri_rejectsRelativeUri() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("foo/bar/baz");

        assertThat(mValidatorUnderTest.validateUri(uri)).isFalse();
    }

    @Test
    public void validateUri_rejectsNoAuthority() {
        setupFakeApp(
                TEST_PACKAGE_NAME, TEST_PROVIDER_AUTHORITY, /* contentProviderExported= */ true);
        when(mMockPermissionValidator.canAccessUri(any())).thenReturn(true);

        Uri uri = Uri.parse("content:foo/bar/baz");

        assertThat(mValidatorUnderTest.validateUri(uri)).isFalse();
    }

    private void setupFakeApp(
            String packageName, String contentProviderAuthority, boolean contentProviderExported) {
        ProviderInfo fakeProviderInfo = new ProviderInfo();
        fakeProviderInfo.authority = contentProviderAuthority;
        fakeProviderInfo.exported = contentProviderExported;

        // Just pick a random permission here...
        fakeProviderInfo.readPermission = permission.ACCEPT_HANDOVER;
        fakeProviderInfo.grantUriPermissions = true;

        PackageInfo pi =
                PackageInfoBuilder.newBuilder()
                        .setPackageName(packageName)
                        .setApplicationInfo(
                                ApplicationInfoBuilder.newBuilder()
                                        .setName("TestPackage")
                                        .setPackageName(packageName)
                                        .build())
                        .build();
        pi.providers = new ProviderInfo[] {fakeProviderInfo};
        pi.applicationInfo.uid = 1001;

        ShadowPackageManager spm = shadowOf(getApplicationContext().getPackageManager());
        spm.installPackage(pi);
    }
}
