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

import static org.junit.Assert.assertEquals;

import android.content.pm.PackageInfo;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public final class PackageInfoCompatTest {
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
}
