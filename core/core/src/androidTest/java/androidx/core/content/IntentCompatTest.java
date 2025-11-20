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
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import static androidx.core.content.PackageManagerCompatTest.setupPermissionRevocationApps;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void createManageUnusedAppRestrictionsIntent_api30() {
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction())
                .isEqualTo("android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
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
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(NON_VERIFIER_PACKAGE_NAME));
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
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(
                "android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_manyVerifierRevocationApps() {
        setupPermissionRevocationApps(mPackageManager,
                Arrays.asList(VERIFIER_PACKAGE_NAME, VERIFIER_PACKAGE_NAME2));
        // Set both apps as the Verifier on the device, but we should fail gracefully.
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME2)).thenReturn(PERMISSION_GRANTED);
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(
                "android.intent.action.AUTO_REVOKE_PERMISSIONS");
        // Verify that we use the first Verifier's package name.
        assertThat(activityIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    public void getParcelableExtra() {
        Intent intent = new Intent();
        Signature signature = new Signature("");
        intent.putExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertEquals(Signature.class, Objects.requireNonNull(
                        IntentCompat.getParcelableExtra(intent, "extra", Signature.class))
                .getClass());
    }

    @Test
    public void getParcelableExtra_returnsNullOnClassMismatch() {
        Intent intent = new Intent();
        Signature signature = new Signature("");
        intent.putExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertNull(IntentCompat.getParcelableExtra(intent, "extra", Intent.class));
    }

    @Test
    public void getParcelableArrayExtra_postU() {
        if (Build.VERSION.SDK_INT < 34) return;
        Intent intent = new Intent();
        Signature[] signature = new Signature[] { new Signature("") };
        intent.putExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertEquals(Signature[].class, Objects.requireNonNull(
                IntentCompat.getParcelableArrayExtra(intent, "extra",
                        Signature.class)).getClass());
    }

    @Test
    public void getParcelableArrayExtra_returnsNullOnClassMismatch_postU() {
        if (Build.VERSION.SDK_INT < 34) return;
        Intent intent = new Intent();
        Signature[] signature = new Signature[] { new Signature("") };
        intent.putExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertNull(IntentCompat.getParcelableArrayExtra(intent, "extra", Intent.class));
    }

    @Test
    public void getParcelableArrayExtra_preU() {
        if (Build.VERSION.SDK_INT >= 34) return;
        Intent intent = new Intent();
        Signature[] signature = new Signature[] { new Signature("") };
        intent.putExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertEquals(Parcelable[].class, Objects.requireNonNull(
                IntentCompat.getParcelableArrayExtra(intent, "extra",
                        Signature.class)).getClass());

        assertNotEquals(Signature[].class, Objects.requireNonNull(
                IntentCompat.getParcelableArrayExtra(intent, "extra",
                        Signature.class)).getClass());

        // We do not check clazz Pre-U
        assertEquals(Parcelable[].class, Objects.requireNonNull(
                IntentCompat.getParcelableArrayExtra(intent, "extra",
                        Intent.class)).getClass());
    }

    @Test
    public void getParcelableArrayListExtra() {
        Intent intent = new Intent();
        ArrayList<Signature> signature = Lists.newArrayList(new Signature(""));
        intent.putParcelableArrayListExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertEquals(Signature.class, Objects.requireNonNull(
                IntentCompat.getParcelableArrayListExtra(intent, "extra",
                        Signature.class)).get(0).getClass());
    }

    @Test
    public void getParcelableArrayListExtra_returnsNullOnClassMismatch_postU() {
        if (Build.VERSION.SDK_INT < 34) return;
        Intent intent = new Intent();
        ArrayList<Signature> signature = Lists.newArrayList(new Signature(""));
        intent.putParcelableArrayListExtra("extra", signature);
        parcelAndUnparcel(intent);

        assertNull(IntentCompat.getParcelableArrayListExtra(intent, "extra", Intent.class));
    }

    @Test
    public void getParcelableArrayListExtra_noTypeCheck_preU() {
        if (Build.VERSION.SDK_INT >= 34) return;
        Intent intent = new Intent();
        ArrayList<Signature> signature = Lists.newArrayList(new Signature(""));
        intent.putParcelableArrayListExtra("extra", signature);
        parcelAndUnparcel(intent);

        Object extra = Objects.requireNonNull(
                IntentCompat.getParcelableArrayListExtra(intent, "extra",
                        Intent.class)).get(0);
        assertEquals(Signature.class, extra.getClass());
    }

    private void parcelAndUnparcel(Intent intent) {
        Parcel p = Parcel.obtain();
        intent.writeToParcel(p, 0);
        p.setDataPosition(0);
        intent.readFromParcel(p);
        p.recycle();
    }

    @Test
    public void getSerializableExtra() {
        Intent intent = new Intent();
        String s = "Hello World";
        intent.putExtra("serializable", s);
        parcelAndUnparcel(intent);

        assertEquals(s, Objects.requireNonNull(
                IntentCompat.getSerializableExtra(intent, "serializable", String.class)));
    }
}
