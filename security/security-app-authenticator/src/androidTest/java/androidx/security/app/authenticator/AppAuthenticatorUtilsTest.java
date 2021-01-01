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

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.MessageDigest;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppAuthenticatorUtilsTest {
    private static final byte[] TEST_DATA = new byte[]{0x01, 0x23, 0x7f, (byte) 0xab};

    @Test
    public void getApiLevel_returnsPlatformLevel() throws Exception {
        // The default behavior of getApiLevel should return the value of Build.VERSION.SDK_INT on
        // the device.
        assertEquals(AppAuthenticatorUtils.getApiLevel(), Build.VERSION.SDK_INT);
    }

    @Test
    public void toHexString_returnsExpectedString() throws Exception {
        // toHexString accepts a byte array and should return a String hex representation of the
        // array.
        assertEquals(AppAuthenticatorUtils.toHexString(TEST_DATA), "01237fab");
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
            assertEquals(AppAuthenticatorUtils.computeDigest(mDigestAlgorithm, TEST_DATA),
                    getExpectedDigest(mDigestAlgorithm, TEST_DATA));
        }

        private String getExpectedDigest(String digestAlgorithm, byte[] data) throws Exception {
            MessageDigest messageDigest = MessageDigest.getInstance(digestAlgorithm);
            return AppAuthenticatorUtils.toHexString(messageDigest.digest(data));
        }
    }
}
