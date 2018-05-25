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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.models.ResReference
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WritableValueTest {

    @Test
    fun testIntWrite() {
        assertThat(IntValue("-10").write().toString(), `is`("-10"))
        assertThat(IntValue("11").write().toString(), `is`("11"))
    }

    @Test
    fun testStringWrite() {
        assertThat(StringValue("foo").write().toString(), `is`("\"foo\""))
    }

    @Test
    fun testReferenceWrite() {
        assertThat(ReferenceValue(ResReference("foo", "id", "bla")).write().toString(),
                `is`("foo.R.id.bla"))
    }
}