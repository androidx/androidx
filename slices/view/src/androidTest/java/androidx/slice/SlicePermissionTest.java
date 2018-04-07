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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Process;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SlicePermissionTest {

    private static final Uri BASE_URI = Uri.parse("content://androidx.slice.view.test/");
    private final Context mContext = InstrumentationRegistry.getContext();
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

}
