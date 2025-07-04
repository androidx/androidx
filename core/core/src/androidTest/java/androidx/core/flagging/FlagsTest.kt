/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.flagging

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FlagsTest {

    @SdkSuppress(minSdkVersion = 36)
    @Test
    fun testGetBooleanFlagValue_withRealPackageAndMissingFlag_returnsDefault() {
        assertEquals(false, Flags.getBooleanFlagValue("android.os", "_missing_flag", false))
        assertEquals(true, Flags.getBooleanFlagValue("android.os", "_missing_flag", true))
    }

    @Test
    fun testGetBooleanFlagValue_withMissingPackage_returnsDefault() {
        assertEquals(false, Flags.getBooleanFlagValue("_missing_package", "_missing_flag", false))
        assertEquals(true, Flags.getBooleanFlagValue("_missing_package", "_missing_flag", true))
    }
}
