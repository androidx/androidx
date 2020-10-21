/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.core.appdigest;

import static androidx.core.appdigest.Checksum.TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256;
import static androidx.core.appdigest.Checksum.TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_MD5;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_MERKLE_ROOT_4K_SHA256;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_SHA1;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_SHA256;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_SHA512;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.PendingIntent;
import android.app.UiAutomation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.os.BuildCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

@SuppressWarnings("deprecation") /* WHOLE_MD5, WHOLE_SHA1 */
@RunWith(AndroidJUnit4.class)
public class ChecksumsTest {
    private static final String V2V3_PACKAGE_NAME = "androidx.core.appdigest.test";
    private static final String V4_PACKAGE_NAME = "com.example.helloworld";
    private static final String FIXED_PACKAGE_NAME = "android.appsecurity.cts.tinyapp";

    private static final String TEST_V4_APK = "HelloWorld5.apk";
    private static final String TEST_V4_SPLIT0 = "HelloWorld5_hdpi-v4.apk";
    private static final String TEST_V4_SPLIT1 = "HelloWorld5_mdpi-v4.apk";
    private static final String TEST_V4_SPLIT2 = "HelloWorld5_xhdpi-v4.apk";
    private static final String TEST_V4_SPLIT3 = "HelloWorld5_xxhdpi-v4.apk";
    private static final String TEST_V4_SPLIT4 = "HelloWorld5_xxxhdpi-v4.apk";

    private static final String TEST_FIXED_APK = "CtsPkgInstallTinyAppV2V3V4.apk";
    private static final String TEST_FIXED_APK_V1 = "CtsPkgInstallTinyAppV1.apk";
    private static final String TEST_FIXED_APK_V2_SHA512 =
            "CtsPkgInstallTinyAppV2V3V4-Sha512withEC.apk";
    private static final String TEST_FIXED_APK_VERITY = "CtsPkgInstallTinyAppV2V3V4-Verity.apk";

    private static final String TEST_FIXED_APK_MD5 = "c19868da017dc01467169f8ea7c5bc57";
    private static final String TEST_FIXED_APK_V2_SHA256 =
            "1eec9e86e322b8d7e48e255fc3f2df2dbc91036e63982ff9850597c6a37bbeb3";
    private static final String TEST_FIXED_APK_SHA256 =
            "91aa30c1ce8d0474052f71cb8210691d41f534989c5521e27e794ec4f754c5ef";
    private static final String TEST_FIXED_APK_SHA512 =
            "b59467fe578ebc81974ab3aaa1e0d2a76fef3e4ea7212a6f2885cec1af525357"
                    + "11e2e94496224cae3eba8dc992144ade321540ebd458ec5b9e6a4cc51170e018";

    private static final int ALL_CHECKSUMS =
            TYPE_WHOLE_MERKLE_ROOT_4K_SHA256 | TYPE_WHOLE_MD5 | TYPE_WHOLE_SHA1 | TYPE_WHOLE_SHA256
                    | TYPE_WHOLE_SHA512
                    | TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256 | TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512;
    private static final char[] HEX_LOWER_CASE_DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private Context mContext;
    private Executor mExecutor;

    @BeforeClass
    public static void onBeforeClass() throws Exception {
        uninstallPackageSilently(FIXED_PACKAGE_NAME);
    }

    private static Checksum[] getChecksums(@NonNull Context context, @NonNull Executor executor,
            @NonNull String packageName,
            boolean includeSplits,
            @Checksum.Type int required, @NonNull List<Certificate> trustedInstallers)
            throws Exception {
        Checksum[] checksums = Checksums.getChecksums(context, packageName, includeSplits,
                required, trustedInstallers, executor).get();

        Arrays.sort(checksums, new Comparator<Checksum>() {
            @Override
            public int compare(Checksum lhs, Checksum rhs) {
                final String lhsSplit = lhs.getSplitName();
                final String rhsSplit = rhs.getSplitName();
                if ((lhsSplit == rhsSplit) || (lhsSplit != null && lhsSplit.equals(rhsSplit))) {
                    return Integer.signum(lhs.getType() - rhs.getType());
                }
                if (lhsSplit == null) {
                    return -1;
                }
                if (rhsSplit == null) {
                    return +1;
                }
                return lhsSplit.compareTo(rhsSplit);
            }
        });

        return checksums;
    }

    public static InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    private static String readFullStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        writeFullStream(inputStream, result, -1);
        return result.toString("UTF-8");
    }

    private static void writeFullStream(InputStream inputStream, OutputStream outputStream,
            long expected)
            throws IOException {
        byte[] buffer = new byte[1024];
        long total = 0;
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
            total += length;
        }
        if (expected > 0) {
            Assert.assertEquals(expected, total);
        }
    }

    private static String executeShellCommand(String command) throws IOException {
        if (Build.VERSION.SDK_INT >= 24) {
            return InstallerApi24.executeShellCommand(command);
        }
        return "";
    }

    private static void uninstallPackageSilently(String packageName) throws IOException {
        executeShellCommand("pm uninstall " + packageName);
    }

    @NonNull
    private static String bytesToHexString(byte[] array) {
        int offset = 0;
        int length = array.length;

        char[] digits = HEX_LOWER_CASE_DIGITS;
        char[] buf = new char[length * 2];

        int bufIndex = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = array[i];
            buf[bufIndex++] = digits[(b >>> 4) & 0x0F];
            buf[bufIndex++] = digits[b & 0x0F];
        }

        return new String(buf);
    }

    @NonNull
    private static byte[] hexStringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Before
    public void onBefore() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mExecutor = Executors.newCachedThreadPool();
    }

    @After
    public void onAfter() throws Exception {
        uninstallPackageSilently(V4_PACKAGE_NAME);
        assertFalse(isAppInstalled(V4_PACKAGE_NAME));
        uninstallPackageSilently(FIXED_PACKAGE_NAME);
        assertFalse(isAppInstalled(FIXED_PACKAGE_NAME));
    }

    @SmallTest
    @Test
    public void testDefaultChecksums() throws Exception {
        Checksum[] checksums = getChecksums(V2V3_PACKAGE_NAME, true, 0, Checksums.TRUST_NONE);
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(1, checksums.length);
            assertEquals(checksums[0].getType(),
                    android.content.pm.Checksum.TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256);
        } else {
            assertEquals(0, checksums.length);
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testSplitsDefaultChecksums() throws Exception {
        installSplits(new String[]{TEST_V4_APK, TEST_V4_SPLIT0, TEST_V4_SPLIT1, TEST_V4_SPLIT2,
                TEST_V4_SPLIT3, TEST_V4_SPLIT4});
        assertTrue(isAppInstalled(V4_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(V4_PACKAGE_NAME, true, 0, Checksums.TRUST_NONE);
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(checksums.length, 6);
            // v2/v3 signature use 1M merkle tree.
            assertEquals(null, checksums[0].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[0].getType());
            assertEquals("config.hdpi", checksums[1].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[1].getType());
            assertEquals("config.mdpi", checksums[2].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[2].getType());
            assertEquals("config.xhdpi", checksums[3].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[3].getType());
            assertEquals("config.xxhdpi", checksums[4].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[4].getType());
            assertEquals("config.xxxhdpi", checksums[5].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[5].getType());
        } else {
            assertEquals(0, checksums.length);
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testSplitsSha256() throws Exception {
        installSplits(new String[]{TEST_V4_APK, TEST_V4_SPLIT0, TEST_V4_SPLIT1, TEST_V4_SPLIT2,
                TEST_V4_SPLIT3, TEST_V4_SPLIT4});
        assertTrue(isAppInstalled(V4_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(V4_PACKAGE_NAME, true, TYPE_WHOLE_SHA256,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(checksums.length, 12);
            // v2/v3 signature use 1M merkle tree.
            assertEquals(null, checksums[0].getSplitName());
            assertEquals(TYPE_WHOLE_SHA256, checksums[0].getType());
            assertEquals(bytesToHexString(checksums[0].getValue()),
                    "ce4ad41be1191ab3cdfef09ab6fb3c5d057e15cb3553661b393f770d9149f1cc");
            assertEquals(null, checksums[1].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[1].getType());
            assertEquals(checksums[2].getSplitName(), "config.hdpi");
            assertEquals(checksums[2].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[2].getValue()),
                    "336a47c278f6b6c22abffefa6a62971fd0bd718d6947143e6ed1f6f6126a8196");
            assertEquals("config.hdpi", checksums[3].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[3].getType());
            assertEquals(checksums[4].getSplitName(), "config.mdpi");
            assertEquals(checksums[4].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[4].getValue()),
                    "17fe9f85e6f29a7354932002c8bc4cb829e1f4acf7f30626bd298c810bb13215");
            assertEquals("config.mdpi", checksums[5].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[5].getType());
            assertEquals(checksums[6].getSplitName(), "config.xhdpi");
            assertEquals(checksums[6].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[6].getValue()),
                    "71a0b0ac5970def7ad80071c909be1e446174a9b39ea5cbf3004db05f87bcc4b");
            assertEquals("config.xhdpi", checksums[7].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[7].getType());
            assertEquals(checksums[8].getSplitName(), "config.xxhdpi");
            assertEquals(checksums[8].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[8].getValue()),
                    "cf6eaee309cf906df5519b9a449ab136841cec62857e283fb4fd20dcd2ea14aa");
            assertEquals("config.xxhdpi", checksums[9].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[9].getType());
            assertEquals(checksums[10].getSplitName(), "config.xxxhdpi");
            assertEquals(checksums[10].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[10].getValue()),
                    "e7c51a01794d33e13d005b62e5ae96a39215bc588e0a2ef8f6161e1e360a17cc");
            assertEquals("config.xxxhdpi", checksums[11].getSplitName());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[11].getType());
        } else {
            assertEquals(6, checksums.length);
            assertEquals(checksums[0].getSplitName(), null);
            assertEquals(checksums[0].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[0].getValue()),
                    "ce4ad41be1191ab3cdfef09ab6fb3c5d057e15cb3553661b393f770d9149f1cc");
            assertEquals(checksums[1].getSplitName(), "config.hdpi");
            assertEquals(checksums[1].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[1].getValue()),
                    "336a47c278f6b6c22abffefa6a62971fd0bd718d6947143e6ed1f6f6126a8196");
            assertEquals(checksums[2].getSplitName(), "config.mdpi");
            assertEquals(checksums[2].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[2].getValue()),
                    "17fe9f85e6f29a7354932002c8bc4cb829e1f4acf7f30626bd298c810bb13215");
            assertEquals(checksums[3].getSplitName(), "config.xhdpi");
            assertEquals(checksums[3].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[3].getValue()),
                    "71a0b0ac5970def7ad80071c909be1e446174a9b39ea5cbf3004db05f87bcc4b");
            assertEquals(checksums[4].getSplitName(), "config.xxhdpi");
            assertEquals(checksums[4].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[4].getValue()),
                    "cf6eaee309cf906df5519b9a449ab136841cec62857e283fb4fd20dcd2ea14aa");
            assertEquals(checksums[5].getSplitName(), "config.xxxhdpi");
            assertEquals(checksums[5].getType(), TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[5].getValue()),
                    "e7c51a01794d33e13d005b62e5ae96a39215bc588e0a2ef8f6161e1e360a17cc");
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedDefaultChecksums() throws Exception {
        installPackage(TEST_FIXED_APK);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(FIXED_PACKAGE_NAME, true, 0,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(1, checksums.length);
            // v2/v3 signature use 1M merkle tree.
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[0].getType());
            assertEquals(TEST_FIXED_APK_V2_SHA256, bytesToHexString(checksums[0].getValue()));
            assertNull(checksums[0].getInstallerCertificate());
        } else {
            assertEquals(0, checksums.length);
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedV1DefaultChecksums() throws Exception {
        installPackage(TEST_FIXED_APK_V1);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(FIXED_PACKAGE_NAME, true, 0,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        assertEquals(0, checksums.length);
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedSha512DefaultChecksums() throws Exception {
        installPackage(TEST_FIXED_APK_V2_SHA512);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(FIXED_PACKAGE_NAME, true, 0,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(1, checksums.length);
            // v2/v3 signature use 1M merkle tree.
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512, checksums[0].getType());
            assertEquals(bytesToHexString(checksums[0].getValue()),
                    "6b866e8a54a3e358dfc20007960fb96123845f6c6d6c45f5fddf88150d71677f"
                            + "4c3081a58921c88651f7376118aca312cf764b391cdfb8a18c6710f9f27916a0");
            assertNull(checksums[0].getInstallerCertificate());
        } else {
            assertEquals(0, checksums.length);
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedVerityDefaultChecksums() throws Exception {
        installPackage(TEST_FIXED_APK_VERITY);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(FIXED_PACKAGE_NAME, true, 0,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        // No usable hashes as verity-in-v2-signature does not cover the whole file.
        assertEquals(0, checksums.length);
    }

    @LargeTest
    @Test
    public void testAllChecksums() throws Exception {
        Checksum[] checksums = getChecksums(V2V3_PACKAGE_NAME, true, ALL_CHECKSUMS,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(checksums.length, 7);
            assertEquals(TYPE_WHOLE_MERKLE_ROOT_4K_SHA256, checksums[0].getType());
            assertEquals(TYPE_WHOLE_MD5, checksums[1].getType());
            assertEquals(TYPE_WHOLE_SHA1, checksums[2].getType());
            assertEquals(TYPE_WHOLE_SHA256, checksums[3].getType());
            assertEquals(TYPE_WHOLE_SHA512, checksums[4].getType());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256, checksums[5].getType());
            assertEquals(TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512, checksums[6].getType());
        } else {
            assertEquals(5, checksums.length);
            assertEquals(TYPE_WHOLE_MERKLE_ROOT_4K_SHA256, checksums[0].getType());
            assertEquals(TYPE_WHOLE_MD5, checksums[1].getType());
            assertEquals(TYPE_WHOLE_SHA1, checksums[2].getType());
            assertEquals(TYPE_WHOLE_SHA256, checksums[3].getType());
            assertEquals(TYPE_WHOLE_SHA512, checksums[4].getType());
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedAllChecksums() throws Exception {
        installPackage(TEST_FIXED_APK);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(FIXED_PACKAGE_NAME, true, ALL_CHECKSUMS,
                Checksums.TRUST_NONE);
        validateFixedAllChecksums(checksums);
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedAllChecksumsDirectExecutor() throws Exception {
        installPackage(TEST_FIXED_APK);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(mContext, new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, FIXED_PACKAGE_NAME, true, ALL_CHECKSUMS, Checksums.TRUST_NONE);
        validateFixedAllChecksums(checksums);
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedAllChecksumsSingleThread() throws Exception {
        installPackage(TEST_FIXED_APK);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(mContext, Executors.newSingleThreadExecutor(),
                FIXED_PACKAGE_NAME, true, ALL_CHECKSUMS, Checksums.TRUST_NONE);
        validateFixedAllChecksums(checksums);
    }

    @SdkSuppress(minSdkVersion = 29)
    @LargeTest
    @Test
    public void testFixedV1AllChecksums() throws Exception {
        installPackage(TEST_FIXED_APK_V1);
        assertTrue(isAppInstalled(FIXED_PACKAGE_NAME));

        Checksum[] checksums = getChecksums(FIXED_PACKAGE_NAME, true, ALL_CHECKSUMS,
                Checksums.TRUST_NONE);
        assertNotNull(checksums);
        assertEquals(5, checksums.length);
        assertEquals(TYPE_WHOLE_MERKLE_ROOT_4K_SHA256, checksums[0].getType());
        assertEquals("1e8f831ef35257ca30d11668520aaafc6da243e853531caabc3b7867986f8886",
                bytesToHexString(checksums[0].getValue()));
        assertEquals(TYPE_WHOLE_MD5, checksums[1].getType());
        assertEquals(bytesToHexString(checksums[1].getValue()), "78e51e8c51e4adc6870cd71389e0f3db");
        assertEquals(TYPE_WHOLE_SHA1, checksums[2].getType());
        assertEquals("f6654505f2274fd9bfc098b660cdfdc2e4da6d53",
                bytesToHexString(checksums[2].getValue()));
        assertEquals(TYPE_WHOLE_SHA256, checksums[3].getType());
        assertEquals("43755d36ec944494f6275ee92662aca95079b3aa6639f2d35208c5af15adff78",
                bytesToHexString(checksums[3].getValue()));
        assertEquals(TYPE_WHOLE_SHA512, checksums[4].getType());
        assertEquals("030fc815a4957c163af2bc6f30dd5b48ac09c94c25a824a514609e1476f91421"
                        + "e2c8b6baa16ef54014ad6c5b90c37b26b0f5c8aeb01b63a1db2eca133091c8d1",
                bytesToHexString(checksums[4].getValue()));
    }

    private void validateFixedAllChecksums(Checksum[] checksums) {
        assertNotNull(checksums);
        if (BuildCompat.isAtLeastR()) {
            assertEquals(checksums.length, 7);
            assertEquals(checksums[0].getType(),
                    android.content.pm.Checksum.TYPE_WHOLE_MERKLE_ROOT_4K_SHA256);
            assertEquals(bytesToHexString(checksums[0].getValue()),
                    "90553b8d221ab1b900b242a93e4cc659ace3a2ff1d5c62e502488b385854e66a");
            assertEquals(checksums[1].getType(), android.content.pm.Checksum.TYPE_WHOLE_MD5);
            assertEquals(bytesToHexString(checksums[1].getValue()), TEST_FIXED_APK_MD5);
            assertEquals(checksums[2].getType(), android.content.pm.Checksum.TYPE_WHOLE_SHA1);
            assertEquals(bytesToHexString(checksums[2].getValue()),
                    "331eef6bc57671de28cbd7e32089d047285ade6a");
            assertEquals(checksums[3].getType(), android.content.pm.Checksum.TYPE_WHOLE_SHA256);
            assertEquals(bytesToHexString(checksums[3].getValue()), TEST_FIXED_APK_SHA256);
            assertEquals(checksums[4].getType(), android.content.pm.Checksum.TYPE_WHOLE_SHA512);
            assertEquals(bytesToHexString(checksums[4].getValue()),
                    "b59467fe578ebc81974ab3aaa1e0d2a76fef3e4ea7212a6f2885cec1af5253571"
                            + "1e2e94496224cae3eba8dc992144ade321540ebd458ec5b9e6a4cc51170e018");
            assertEquals(checksums[5].getType(),
                    android.content.pm.Checksum.TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256);
            assertEquals(bytesToHexString(checksums[5].getValue()), TEST_FIXED_APK_V2_SHA256);
            assertEquals(checksums[6].getType(),
                    android.content.pm.Checksum.TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512);
            assertEquals(bytesToHexString(checksums[6].getValue()),
                    "ef80a8630283f60108e8557c924307d0ccdfb6bbbf2c0176bd49af342f43bc84"
                            + "5f2888afcb71524196dda0d6dd16a6a3292bb75b431b8ff74fb60d796e882f80");
        } else {
            assertEquals(5, checksums.length);
            assertEquals(TYPE_WHOLE_MERKLE_ROOT_4K_SHA256, checksums[0].getType());
            assertEquals("90553b8d221ab1b900b242a93e4cc659ace3a2ff1d5c62e502488b385854e66a",
                    bytesToHexString(checksums[0].getValue()));
            assertEquals(TYPE_WHOLE_MD5, checksums[1].getType());
            assertEquals(TEST_FIXED_APK_MD5, bytesToHexString(checksums[1].getValue()));
            assertEquals(TYPE_WHOLE_SHA1, checksums[2].getType());
            assertEquals("331eef6bc57671de28cbd7e32089d047285ade6a",
                    bytesToHexString(checksums[2].getValue()));
            assertEquals(TYPE_WHOLE_SHA256, checksums[3].getType());
            assertEquals(TEST_FIXED_APK_SHA256, bytesToHexString(checksums[3].getValue()));
            assertEquals(TYPE_WHOLE_SHA512, checksums[4].getType());
            assertEquals(TEST_FIXED_APK_SHA512, bytesToHexString(checksums[4].getValue()));
        }
    }

    private Checksum[] getChecksums(@NonNull String packageName, boolean includeSplits,
            @Checksum.Type int required, @NonNull List<Certificate> trustedInstallers)
            throws Exception {
        return getChecksums(mContext, mExecutor, packageName, includeSplits, required,
                trustedInstallers);
    }

    private void installPackage(String baseName) throws Exception {
        installSplits(new String[]{baseName});
    }

    void installSplits(String[] names) throws Exception {
        if (Build.VERSION.SDK_INT >= 24) {
            new InstallerApi24(mContext).installSplits(names);
        }
    }

    private boolean isAppInstalled(String packageName) throws IOException {
        if (Build.VERSION.SDK_INT >= 24) {
            return InstallerApi24.isAppInstalled(packageName);
        }
        return false;
    }

    @RequiresApi(24)
    static class InstallerApi24 {
        private Context mContext;

        InstallerApi24(Context context) {
            mContext = context;
        }

        private static void writeFileToSession(PackageInstaller.Session session, String name,
                String apk) throws IOException {
            try (OutputStream os = session.openWrite(name, 0, -1);
                 InputStream is = getResourceAsStream(apk)) {
                assertNotNull(name, is);
                writeFullStream(is, os, -1);
            }
        }

        static UiAutomation getUiAutomation() {
            return InstrumentationRegistry.getInstrumentation().getUiAutomation();
        }

        static String executeShellCommand(String command) throws IOException {
            final ParcelFileDescriptor stdout = getUiAutomation().executeShellCommand(command);
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(stdout)) {
                return readFullStream(inputStream);
            }
        }

        static boolean isAppInstalled(final String packageName) throws IOException {
            final String commandResult = executeShellCommand("pm list packages");
            final int prefixLength = "package:".length();
            return Arrays.stream(commandResult.split("\\r?\\n"))
                    .anyMatch(new Predicate<String>() {
                        @Override
                        public boolean test(String line) {
                            return line.substring(prefixLength).equals(packageName);
                        }
                    });
        }

        void installSplits(String[] names) throws Exception {
            getUiAutomation().adoptShellPermissionIdentity(Manifest.permission.INSTALL_PACKAGES);
            try {
                final PackageInstaller installer =
                        mContext.getPackageManager().getPackageInstaller();
                final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);

                final int sessionId = installer.createSession(params);
                PackageInstaller.Session session = installer.openSession(sessionId);

                for (String name : names) {
                    writeFileToSession(session, name, name);
                }

                commitSession(session);
            } finally {
                getUiAutomation().dropShellPermissionIdentity();
            }
        }

        private void commitSession(PackageInstaller.Session session) throws Exception {
            final ResolvableFuture<Intent> result = ResolvableFuture.create();

            // Create a single-use broadcast receiver
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    context.unregisterReceiver(this);
                    result.set(intent);
                }
            };

            // Create a matching intent-filter and register the receiver
            final int resultId = result.hashCode();
            final String action = "androidx.core.appdigest.COMMIT_COMPLETE." + resultId;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(action);
            mContext.registerReceiver(broadcastReceiver, intentFilter);

            Intent intent = new Intent(action);
            PendingIntent sender = PendingIntent.getBroadcast(mContext, resultId, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            session.commit(sender.getIntentSender());

            Intent commitResult = result.get();
            final int status = commitResult.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            assertEquals(commitResult.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) + " OR "
                            + commitResult.getExtras().get(Intent.EXTRA_INTENT),
                    PackageInstaller.STATUS_SUCCESS, status);
        }
    }
}
