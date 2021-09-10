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

package androidx.core.content.pm;

import static android.os.Build.VERSION_CODES.P;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import androidx.collection.ArrayMap;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.List;
import java.util.Map;

@SmallTest
public final class PackageInfoCompatTest {

    private static final String NON_EXISTENT_PACKAGE = "com.example.app.non_existent_package_name";

    private final Context mContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private PackageManager mPackageManager = mContext.getPackageManager();

    @Test
    public void getLongVersionCodeLowerBitsOnly() {
        PackageInfo info = new PackageInfo();
        info.versionCode = 12345;

        assertEquals(12345L, PackageInfoCompat.getLongVersionCode(info));
    }

    @SdkSuppress(minSdkVersion = P)
    @Test
    public void getLongVersionCodeLowerAndUpperBits() {
        PackageInfo info = new PackageInfo();
        info.setLongVersionCode(Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, PackageInfoCompat.getLongVersionCode(info));
    }

    /**
     * Only verifies non-null return, to avoid hard coding certs. Actual equality and proper
     * return value is verified as part of {@link PackageInfoCompatHasSignaturesTest}.
     */
    @Test
    public void getSignaturesNonNull() throws PackageManager.NameNotFoundException {
        List<Signature> signatures = PackageInfoCompat.getSignatures(mPackageManager,
                mContext.getPackageName());

        assertThat(signatures).isNotEmpty();
    }

    @Test(expected = PackageManager.NameNotFoundException.class)
    public void getSignaturesThrowOnNotFound() throws PackageManager.NameNotFoundException {
        PackageInfoCompat.getSignatures(mPackageManager, NON_EXISTENT_PACKAGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasSignaturesThrowOnInvalidType() throws PackageManager.NameNotFoundException {
        Map<byte[], Integer> map = new ArrayMap<>(1);
        map.put(new byte[100], PackageManager.CERT_INPUT_SHA256 + 1);
        PackageInfoCompat.hasSignatures(mPackageManager, mContext.getPackageName(), map, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasSignaturesThrowOnNullBytes() throws PackageManager.NameNotFoundException {
        Map<byte[], Integer> map = new ArrayMap<>(1);
        map.put(null, PackageManager.CERT_INPUT_SHA256);
        PackageInfoCompat.hasSignatures(mPackageManager, mContext.getPackageName(), map, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasSignaturesThrowOnNullType() throws PackageManager.NameNotFoundException {
        Map<byte[], Integer> map = new ArrayMap<>(1);
        map.put(new byte[100], null);
        PackageInfoCompat.hasSignatures(mPackageManager, mContext.getPackageName(), map, false);
    }

    @Test(expected = PackageManager.NameNotFoundException.class)
    public void hasSignaturesThrowOnNotFound() throws PackageManager.NameNotFoundException {
        Map<byte[], Integer> map = new ArrayMap<>(1);
        map.put(new byte[100], PackageManager.CERT_INPUT_SHA256);
        PackageInfoCompat.hasSignatures(mPackageManager, NON_EXISTENT_PACKAGE, map, false);
    }
}
