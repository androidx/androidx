/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import androidx.ink.strokes.InProgressStroke
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InProgressStrokePoolTest {

    @Test
    fun obtain_whenCalledTwice_returnsDifferentInstances() {
        val pool = InProgressStrokePool.create()

        val first = pool.obtain()
        val second = pool.obtain()

        assertThat(second).isNotSameInstanceAs(first)
    }

    @Test
    fun obtain_whenCalledAfterRecycle_returnsSameInstance() {
        val pool = InProgressStrokePool.create()

        val first = pool.obtain()
        pool.recycle(first)
        val second = pool.obtain()

        assertThat(second).isSameInstanceAs(first)
    }

    @Test
    fun trimToSize_whenNegative_throws() {
        assertFailsWith<IllegalArgumentException> { InProgressStrokePool.create().trimToSize(-1) }
    }

    @Test
    fun trimToSize_whenZero_obtainReturnsNewInstance() {
        val pool = InProgressStrokePool.create()
        val obtainedBeforeTrim = mutableSetOf<InProgressStroke>()
        repeat(10) {
            val instance = pool.obtain()
            obtainedBeforeTrim.add(instance)
            pool.recycle(instance)
        }

        pool.trimToSize(0)

        assertThat(pool.obtain()).isNotIn(obtainedBeforeTrim)
    }

    @Test
    fun trimToSize_whenLessThanCurrentPoolSize_obtainReturnsSameInstancesThenNewInstances() {
        val pool = InProgressStrokePool.create()
        val obtainedBeforeTrim = mutableSetOf<InProgressStroke>()
        repeat(10) {
            val instance = pool.obtain()
            obtainedBeforeTrim.add(instance)
        }
        for (instance in obtainedBeforeTrim) {
            pool.recycle(instance)
        }

        pool.trimToSize(3)

        repeat(3) {
            val shouldBeOld = pool.obtain()
            assertThat(shouldBeOld).isIn(obtainedBeforeTrim)
        }
        val shouldBeNew = pool.obtain()
        assertThat(shouldBeNew).isNotIn(obtainedBeforeTrim)
    }
}
