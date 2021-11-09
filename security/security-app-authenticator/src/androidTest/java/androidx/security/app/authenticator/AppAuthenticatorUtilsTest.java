/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.MessageDigest;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppAuthenticatorUtilsTest {
    private static final byte[] TEST_DATA = new byte[]{0x01, 0x23, 0x7f, (byte) 0xab};

    private Context mContext;
    private AppAuthenticatorUtils mAppAuthenticatorUtils;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mAppAuthenticatorUtils = new AppAuthenticatorUtils(mContext);
    }

    @Test
    public void getApiLevel_returnsPlatformLevel() throws Exception {
        // The default behavior of getApiLevel should return the value of Build.VERSION.SDK_INT on
        // the device.
        assertEquals(Build.VERSION.SDK_INT, AppAuthenticatorUtils.getApiLevel());
    }

    @Test
    public void toHexString_returnsExpectedString() throws Exception {
        // toHexString accepts a byte array and should return a String hex representation of the
        // array.
        assertEquals("01237fab", AppAuthenticatorUtils.toHexString(TEST_DATA));
    }

    @Test
    public void getCallingUid_returnsExpectedUid() throws Exception {
        // The AppAuthenticatorUtils provides an instance method to obtain the UID of the calling
        // process to facilitate tests; this test verifies a base AppAuthenticatorUtils instance
        // returns the expected UID.
        assertEquals(Binder.getCallingUid(), mAppAuthenticatorUtils.getCallingUid());
    }

    @Test
    public void getCallingPid_returnsExpectedPid() throws Exception {
        // The AppAuthenticatorUtils provides an instance method to obtain the PID of the calling
        // process to facilitate tests; this test verifies a base AppAuthenticatorUtils instance
        // returns the expected PID.
        assertEquals(Binder.getCallingPid(), mAppAuthenticatorUtils.getCallingPid());
    }

    @Test
    public void getUidForPackage_returnsExpectedUid() throws Exception {
        // The AppAuthenticatorUtils provides an instance method to obtain the UID of the
        // specified package to facilitate tests; this test verifies a base AppAuthenticatorUtils
        // instance returns the expected UID for the package.
        PackageInfo packageInfo =
                mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);

        assertEquals(packageInfo.applicationInfo.uid,
                mAppAuthenticatorUtils.getUidForPackage(mContext.getPackageName()));
    }

    @RunWith(Parameterized.class)
    public static class AppAuthenticatorUtilsParameterizedTest {
        private String mDigestAlgorithm;

        // Verify the returned digest for each of the supported digest algorithms.
        @Parameterized.Parameters
        public static Object[] getDigestAlgorithms() {
            return new Object[]{
                    "SHA-256",
                    "SHA-384",
                    "SHA-512",
            };
        }

        public AppAuthenticatorUtilsParameterizedTest(final String digestAlgorithm) {
            mDigestAlgorithm = digestAlgorithm;
        }

        @Test
        public void computeDigest_returnsExpectedDigest() throws Exception {
            assertEquals(getExpectedDigest(mDigestAlgorithm, TEST_DATA),
                    AppAuthenticatorUtils.computeDigest(mDigestAlgorithm, TEST_DATA));
        }

        private String getExpectedDigest(String digestAlgorithm, byte[] data) throws Exception {
            MessageDigest messageDigest = MessageDigest.getInstance(digestAlgorithm);
            return AppAuthenticatorUtils.toHexString(messageDigest.digest(data));
        }
    }
}
