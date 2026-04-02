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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/** Test of RcFloat class verifying internal consistency and expression generation. */
class RcFloatTest {

    private val testProfile =
        Profile(CoreDocument.DOCUMENT_API_LEVEL, 0, RcPlatformServices.None) { _, profile, _ ->
            RemoteComposeWriter(profile)
        }

    @Test
    fun plus_withRcFloat() {
        val a = RcFloat(1f)
        val b = RcFloat(2f)
        val res = a + b
        assertArrayEquals(floatArrayOf(1.0f, 2.0f, Rc.FloatExpression.ADD), res.toArray(), 0.0001f)
    }

    @Test
    fun minus_withFloat() {
        val a = RcFloat(10f)
        val res = a - 3f
        assertArrayEquals(floatArrayOf(10.0f, 3.0f, Rc.FloatExpression.SUB), res.toArray(), 0.0001f)
    }

    @Test
    fun complex_expression() {
        val w = RcFloat(100f)
        val h = RcFloat(200f)
        // (w / 2) + (h * 0.1)
        val res = (w / 2f) + (h * 0.1f)
        assertArrayEquals(
            floatArrayOf(
                100.0f,
                2.0f,
                Rc.FloatExpression.DIV,
                200.0f,
                0.1f,
                Rc.FloatExpression.MUL,
                Rc.FloatExpression.ADD,
            ),
            res.toArray(),
            0.0001f,
        )
    }

    @Test
    fun unaryMinus() {
        val a = RcFloat(5f)
        val res = -a
        assertArrayEquals(floatArrayOf(5.0f, -1.0f, Rc.FloatExpression.MUL), res.toArray(), 0.0001f)
    }

    @Test
    fun rf_extensions() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)
        scope.apply {
            val a = 5.rf
            val b = 10.5f.rf
            assertArrayEquals(floatArrayOf(5.0f), a.toArray(), 0.0001f)
            assertArrayEquals(floatArrayOf(10.5f), b.toArray(), 0.0001f)
        }
    }

    @Test
    fun mixed_literals() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)
        scope.apply {
            val a = 10.rf
            val res = 5f + a * 2f
            // RPN: 5.0, 10.0, 2.0, MUL, ADD
            assertArrayEquals(
                floatArrayOf(5.0f, 10.0f, 2.0f, Rc.FloatExpression.MUL, Rc.FloatExpression.ADD),
                res.toArray(),
                0.0001f,
            )
        }
    }
}
