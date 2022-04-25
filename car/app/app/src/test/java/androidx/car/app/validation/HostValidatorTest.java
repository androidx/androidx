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

import static android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;

import androidx.annotation.IdRes;
import androidx.car.app.HostInfo;
import androidx.car.app.R;
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

import java.util.List;

/** Tests for {@link HostValidator}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = 23)
public class HostValidatorTest {
    private static final String VALID_PACKAGE_NAME = "com.foo";
    private static final String ALTERNATIVE_VALID_PACKAGE_NAME = "com.bar";
    private static final Signature VALID_SIGNATURE = new Signature("0123");
    private static final String VALID_DIGEST = "b71de80778f2783383f5d5a3028af84eab2f18a"
            + "4eb38968172ca41724dd4b3f4";
    private static final int NON_SYSTEM_UID = 123;
    private static final int INVALID_UID = 234;
    @IdRes
    private static final int MOCK_ALLOW_LIST_HOSTS_RES_ID = 234;
    private static final String TEMPLATE_RENDERER_PERMISSION = "android.car.permission"
            + ".TEMPLATE_RENDERER";

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
    public void isValidHost_allowedHost_accepted() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        mHostValidatorBuilder.addAllowedHost(VALID_PACKAGE_NAME, VALID_DIGEST);
        HostValidator hostValidator = mHostValidatorBuilder.build();

        assertThat(hostValidator.isValidHost(hostInfo)).isTrue();
    }

    @Test
    public void isValidHost_unknownHost_rejected() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE);

        HostValidator hostValidator = mHostValidatorBuilder.build();
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        assertThat(hostValidator.isValidHost(hostInfo)).isFalse();
    }

    @Test
    public void isValidHost_allowListedHosts_accepted() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE);
        when(mResources.getStringArray(eq(MOCK_ALLOW_LIST_HOSTS_RES_ID)))
                .thenReturn(new String[] {
                        VALID_DIGEST + "," + VALID_PACKAGE_NAME
                });

        mHostValidatorBuilder.addAllowedHosts(MOCK_ALLOW_LIST_HOSTS_RES_ID);
        HostValidator hostValidator = mHostValidatorBuilder.build();
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        assertThat(hostValidator.isValidHost(hostInfo)).isTrue();
    }

    @Test
    public void allowListedHosts_sample_isReadProperly() {
        HostValidator.Builder builder =
                new HostValidator.Builder(ApplicationProvider.getApplicationContext());
        builder.addAllowedHosts(R.array.hosts_allowlist_sample);
        HostValidator hostValidator = builder.build();

        assertThat(hostValidator.getAllowedHosts().size()).isEqualTo(2);
        assertThat(hostValidator.getAllowedHosts().values()
                .stream()
                .mapToLong(List::size)
                .sum()).isEqualTo(6);
    }

    @Test
    public void isValidHost_mismatchingPackageName_rejected() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        mHostValidatorBuilder.addAllowedHost(ALTERNATIVE_VALID_PACKAGE_NAME,
                VALID_DIGEST);
        HostValidator hostValidator = mHostValidatorBuilder.build();

        assertThat(hostValidator.isValidHost(hostInfo)).isFalse();
    }

    @Test
    public void isValidHost_allowedUnknownHosts_unknownHostAccepted() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        HostValidator hostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;

        assertThat(hostValidator.isValidHost(hostInfo)).isTrue();
    }

    @Test
    public void isValidHost_hostHoldingPermission_accepted() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE,
                TEMPLATE_RENDERER_PERMISSION);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, NON_SYSTEM_UID);

        HostValidator hostValidator = mHostValidatorBuilder.build();

        assertThat(hostValidator.isValidHost(hostInfo)).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void isValidHost_mismatchingUid_throws() {
        installPackage(VALID_PACKAGE_NAME, VALID_SIGNATURE, TEMPLATE_RENDERER_PERMISSION);
        HostInfo hostInfo = new HostInfo(VALID_PACKAGE_NAME, INVALID_UID);

        HostValidator hostValidator = mHostValidatorBuilder.build();
        hostValidator.isValidHost(hostInfo);
    }

    @Test
    public void allowHosts_malformedEntry_throws() {
        when(mResources.getStringArray(eq(MOCK_ALLOW_LIST_HOSTS_RES_ID)))
                .thenReturn(new String[] {
                        // Note missing comma between certificate and package name
                        VALID_DIGEST + VALID_PACKAGE_NAME
                });
    }

    @SuppressWarnings("deprecation")
    private void installPackage(String packageName, Signature signature,
            String permission) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.uid = NON_SYSTEM_UID;
        packageInfo.packageName = packageName;
        packageInfo.signatures = new Signature[] { signature };
        if (permission != null) {
            packageInfo.requestedPermissions = new String[] { permission };

            // Per PackageParser#generatePackageInfo, a requestedPermissionsFlag for a permission
            // is (REQUESTED_PERMISSION_REQUIRED | REQUESTED_PERMISSION_GRANTED). Since
            // REQUESTED_PERMISSION_REQUIRED is deprecated but still used in PackageParser, hard
            // code the granted flag here.
            int requestedPermissionGranted = REQUESTED_PERMISSION_GRANTED | 1;
            packageInfo.requestedPermissionsFlags = new int[] { requestedPermissionGranted };
        }
        try {
            when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
        } catch (PackageManager.NameNotFoundException ex) {
            throw new IllegalStateException("Error mocking package manager", ex);
        }
    }

    private void installPackage(String packageName, Signature signature) {
        installPackage(packageName, signature, null);
    }
}
