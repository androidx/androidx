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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProfileRuleParserTests {
    @Test
    fun parseInvalidRule() {
        val rule = "androidx/Foo"
        val profileRule = ProfileRule.parse(rule)
        assertNull(profileRule)
    }

    @Test
    fun parseClassRule() {
        val rule = "Landroidx/Foo/Bar;"
        val profileRule = ProfileRule.parse(rule)
        assertNotNull(profileRule)
        assertEquals(profileRule.flags.isEmpty(), true)
        assertEquals(profileRule.classDescriptor, "androidx/Foo/Bar")
        assertEquals(profileRule.methodDescriptor?.isEmpty(), true)
    }

    @Test
    fun parseProfileRuleTestWithMethodDescriptor() {
        // https://youtrack.jetbrains.com/issue/KT-2425
        val dollar = "$"
        val rule =
            "HPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;-><init>(Ljava/util/Map;)V"
        val profileRule = ProfileRule.parse(rule)
        assertNotNull(profileRule)
        assertEquals(profileRule.flags, "HP")
        assertEquals(
            profileRule.classDescriptor,
            "androidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo"
        )
        assertEquals(profileRule.methodDescriptor, "<init>(Ljava/util/Map;)V")
    }
}
