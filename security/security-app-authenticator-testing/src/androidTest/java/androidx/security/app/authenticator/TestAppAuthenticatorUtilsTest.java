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

package androidx.security.app.authenticator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class TestAppAuthenticatorUtilsTest {
    private static final String TEST_PACKAGE = "com.android.app1";

    private TestAppAuthenticatorUtils.Builder mBuilder;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mBuilder = new TestAppAuthenticatorUtils.Builder(context);
    }

    @Test
    public void getUidForPackage_defaultConfig_returnsBinderCallingUid() throws Exception {
        // By default the TestAppAuthenticatorUtils should return Binder#getCallingUid as the UID
        // of any specified package.
        TestAppAuthenticatorUtils utils = mBuilder.build();

        assertEquals(Binder.getCallingUid(), utils.getUidForPackage(TEST_PACKAGE));
    }

    @Test
    public void getUidForPackage_setUidForPackage_returnsSetUid() throws Exception {
        // The TestAppAuthentictorUtils allows the UID of a package to be explicit set to verify
        // cases where the UID of the specified package does not match the UID of the calling
        // package.
        TestAppAuthenticatorUtils utils = mBuilder.setUidForPackage(TEST_PACKAGE, 1234).build();

        assertEquals(1234, utils.getUidForPackage(TEST_PACKAGE));

    }

    @Test
    public void getUidForPackage_packageNotInstalled_throwsException() throws Exception {
        // The TestAppAuthenticatorUtils can be configured to treat a package as not installed;
        // this will result in a PackageManager.NameNotFoundException being thrown, similar to
        // what is thrown by the platform when invoking PackageManager#getPackageInfo with a
        // package that is not installed on the device.
        TestAppAuthenticatorUtils utils = mBuilder.setPackageNotInstalled(TEST_PACKAGE).build();

        assertThrows(PackageManager.NameNotFoundException.class,
                () -> utils.getUidForPackage(TEST_PACKAGE));
    }
}
