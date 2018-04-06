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

import android.content.pm.PermissionInfo;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PermissionInfoCompatTest {
    @Test
    public void testGetProtectionAndFlags() {
        PermissionInfo pi = new PermissionInfo();

        pi.protectionLevel = PermissionInfo.PROTECTION_DANGEROUS
                | PermissionInfo.PROTECTION_FLAG_PRIVILEGED;

        Assert.assertEquals(PermissionInfo.PROTECTION_DANGEROUS,
                PermissionInfoCompat.getProtection(pi));

        Assert.assertEquals(PermissionInfo.PROTECTION_FLAG_PRIVILEGED,
                PermissionInfoCompat.getProtectionFlags(pi));

        pi.protectionLevel = 0xf | 0xfff0;
        Assert.assertEquals(0xf, PermissionInfoCompat.getProtection(pi));
        Assert.assertEquals(0xfff0, PermissionInfoCompat.getProtectionFlags(pi));
    }
}
