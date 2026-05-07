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

package androidx.compose.remote.creation.dsl

import org.junit.Assert.assertEquals
import org.junit.Test

class RcTypesTest {

    @Test
    fun testRcSp() {
        val sp1 = 16.rsp
        val sp2 = 16.5f.rsp
        val sp3 = 18.0.rsp

        assertEquals(16f, sp1.value)
        assertEquals(16.5f, sp2.value)
        assertEquals(18f, sp3.value)
    }

    @Test
    fun testRcDp() {
        val dp = 10.rdp
        assertEquals(10f, dp.value)
    }

    @Test
    fun testRcPx() {
        val px = 20.rpx
        assertEquals(20f, px.value)
    }
}
