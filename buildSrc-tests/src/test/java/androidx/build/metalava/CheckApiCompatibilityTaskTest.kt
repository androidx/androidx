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

package androidx.build.metalava

import androidx.build.Version
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CheckApiCompatibilityTaskTest {
    @Test
    fun `Finalized APIs should not change within a version`() {
        assertFalse(shouldFreezeApis(
            Version("1.0.0"),
            Version("1.1.0-alpha01")
        ))
        assertFalse(shouldFreezeApis(
            Version("1.1.0-alpha01"),
            Version("1.1.0-beta01")
        ))
        assertTrue(shouldFreezeApis(
            Version("1.1.0-beta01"),
            Version("1.1.0-beta01")
        ))
        assertTrue(shouldFreezeApis(
            Version("1.1.0-beta01"),
            Version("1.1.0")
        ))
    }
}
