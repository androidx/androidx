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

package androidx.slice.compat;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CompatPermissionManagerTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @FlakyTest
    @Test
    public void testAutoGrant() {
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("my.authority")
                .path("my_path")
                .build();
        final String testPermission = "android.permission.SOME_PERMISSION";

        final int grantedPid = Process.myPid();
        final int grantedUid = Process.myUid();

        final int nonGrantedPid = grantedPid + 1;
        final int nonGrantedUid = grantedUid + 1;

        Context permContext = new ContextWrapper(mContext) {
            @Override
            public int checkPermission(String permission, int pid, int uid) {
                if (testPermission.equals(permission)) {
                    if (grantedUid == uid) {
                        return PackageManager.PERMISSION_GRANTED;
                    } else if (nonGrantedUid == uid) {
                        return PackageManager.PERMISSION_DENIED;
                    }
                }
                return super.checkPermission(permission, pid, uid);
            }

            @Override
            public PackageManager getPackageManager() {
                final PackageManager realPm = super.getPackageManager();
                PackageManager pm = mock(PackageManager.class, withSettings()
                        .defaultAnswer(new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                return invocation.getMethod().invoke(realPm,
                                        invocation.getArguments());
                            }
                        }));
                when(pm.getPackagesForUid(grantedUid)).thenReturn(new String[] { "grant_pkg"});
                when(pm.getPackagesForUid(nonGrantedUid)).thenReturn(new String[] { "other_pkg"});
                return pm;
            }
        };
        CompatPermissionManager manager = spy(new CompatPermissionManager(permContext, "nothing", 0,
                new String[] {testPermission}));

        assertEquals(PERMISSION_DENIED, manager.checkSlicePermission(uri,
                nonGrantedPid, nonGrantedUid));

        assertEquals(PERMISSION_GRANTED, manager.checkSlicePermission(uri, grantedPid, grantedUid));
        verify(manager).grantSlicePermission(eq(uri), eq("grant_pkg"));

    }

}
