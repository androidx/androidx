/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider;

import android.content.pm.SigningInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 28)
public class CallingAppInfoJavaTest {

    @Test
    public void constructor_success() {
        new CallingAppInfo("name", new SigningInfo());
    }

    @Test
    public void constructor_success_withOrigin() {
        new CallingAppInfo("name", new SigningInfo(), "origin");
    }

    @Test
    public void constructor_fail_emptyPackageName() {
        Assert.assertThrows(
                "Expected exception from no package name",
                IllegalArgumentException.class,
                () -> new CallingAppInfo("", new SigningInfo(), "origin"));
    }

    @Test
    public void constructor_fail_nullPackageName() {
        Assert.assertThrows(
                "Expected exception from null package name",
                NullPointerException.class,
                () -> new CallingAppInfo(null, new SigningInfo(), "origin"));
    }

    @Test
    public void constructor_fail_nullSigningInfo() {
        Assert.assertThrows(
                "Expected exception from null signing info",
                NullPointerException.class,
                () -> new CallingAppInfo("package", null, "origin"));
    }
}
