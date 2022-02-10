/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BaselineProfilesTest {
    @Test
    fun filterBaselineProfileRules() {
        val profile = """
            Landroidx/Foo/Bar;
            [Landroidx/Foo/Bar;
            HSPLjava/io/DataOutputStream;->writeByte(I)V+]Ljava/io/OutputStream;missing_types
        """.trimIndent()

        val filtered = filterProfileRulesToTargetP(profile)
        assertEquals(filtered.lines().size, 1)
        assertEquals("Landroidx/Foo/Bar;", filtered)
    }
}
