/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.slice.compat.SliceProviderCompat.PERMS_PREFIX;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.slice.compat.CompatPermissionManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SdkSuppress(minSdkVersion = 24)
public class SlicePermissionTest {

    private static final Uri BASE_URI = Uri.parse("content://androidx.slice.core.permission/");
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private String mTestPkg;
    private int mTestUid;
    private int mTestPid;
    private SliceManager mSliceManager;

    @Before
    public void setup() throws NameNotFoundException {
        mSliceManager = SliceManager.getInstance(mContext);
        mTestPkg = mContext.getPackageName();
        mTestUid = mContext.getPackageManager().getPackageUid(mTestPkg, 0);
        mTestPid = Process.myPid();
    }

    @After
    public void tearDown() {
        mSliceManager.revokeSlicePermission(mTestPkg, BASE_URI);
    }

    @Test
    public void testGrant() {
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, BASE_URI);

        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));
    }

    @Test
    public void testGrantParent() {
        Uri uri = BASE_URI.buildUpon()
                .appendPath("something")
                .build();

        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, BASE_URI);

        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));
    }

    @Test
    public void testGrantParentExpands() {
        Uri uri = BASE_URI.buildUpon()
                .appendPath("something")
                .build();

        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, uri);

        // Only sub-path granted.
        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, BASE_URI);

        // Now all granted.
        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));
        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));
    }

    @Test
    public void testGrantChild() {
        Uri uri = BASE_URI.buildUpon()
                .appendPath("something")
                .build();

        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, uri);

        // Still no permission because only a child was granted
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));
    }

    @Test
    public void testRevoke() {
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, BASE_URI);

        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.revokeSlicePermission(mTestPkg, BASE_URI);

        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));
    }

    @Test
    public void testRevokeParent() {
        Uri uri = BASE_URI.buildUpon()
                .appendPath("something")
                .build();
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, uri);

        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));

        mSliceManager.revokeSlicePermission(mTestPkg, BASE_URI);

        // Revoked because parent was revoked
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(uri, mTestPid, mTestUid));
    }

    @Test
    public void testRevokeChild() {
        Uri uri = BASE_URI.buildUpon()
                .appendPath("something")
                .build();
        assertEquals(PERMISSION_DENIED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.grantSlicePermission(mTestPkg, BASE_URI);

        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));

        mSliceManager.revokeSlicePermission(mTestPkg, uri);

        // Not revoked because child was revoked.
        assertEquals(PERMISSION_GRANTED,
                mSliceManager.checkSlicePermission(BASE_URI, mTestPid, mTestUid));
    }

    public static class PermissionProvider extends SliceProvider {
        @Override
        public boolean onCreateSliceProvider() {
            return true;
        }

        @NonNull
        protected CompatPermissionManager onCreatePermissionManager(
                @NonNull String[] autoGrantPermissions) {
            return new CompatPermissionManager(getContext(), PERMS_PREFIX + getClass().getName(),
                    -1 /* Different uid to run permissions */, autoGrantPermissions);
        }

        @Override
        public Slice onBindSlice(@NonNull Uri sliceUri) {
            return null;
        }
    }
}
