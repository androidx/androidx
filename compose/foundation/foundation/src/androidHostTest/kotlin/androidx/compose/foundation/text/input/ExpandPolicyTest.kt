/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpandPolicyTest {

    @Test
    fun insideOnly() {
        assertThat(ExpandPolicy.InsideOnly.startExpands).isFalse()
        assertThat(ExpandPolicy.InsideOnly.endExpands).isFalse()
    }

    @Test
    fun atStart() {
        assertThat(ExpandPolicy.AtStart.startExpands).isTrue()
        assertThat(ExpandPolicy.AtStart.endExpands).isFalse()
    }

    @Test
    fun atEnd() {
        assertThat(ExpandPolicy.AtEnd.startExpands).isFalse()
        assertThat(ExpandPolicy.AtEnd.endExpands).isTrue()
    }

    @Test
    fun atBoth() {
        assertThat(ExpandPolicy.AtBoth.startExpands).isTrue()
        assertThat(ExpandPolicy.AtBoth.endExpands).isTrue()
    }

    @Test
    fun constructor() {
        val policy1 = ExpandPolicy(startExpands = true, endExpands = false)
        assertThat(policy1).isEqualTo(ExpandPolicy.AtStart)

        val policy2 = ExpandPolicy(startExpands = false, endExpands = true)
        assertThat(policy2).isEqualTo(ExpandPolicy.AtEnd)

        val policy3 = ExpandPolicy(startExpands = true, endExpands = true)
        assertThat(policy3).isEqualTo(ExpandPolicy.AtBoth)

        val policy4 = ExpandPolicy(startExpands = false, endExpands = false)
        assertThat(policy4).isEqualTo(ExpandPolicy.InsideOnly)
    }
}
