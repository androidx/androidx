/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import com.google.common.truth.Truth
import java.util.concurrent.atomic.AtomicInteger

/**
 * Partial copy from
 * https://github.com/Kotlin/kotlinx.coroutines/blob/master/core/kotlinx-coroutines-core/test/TestBase.kt
 * to track execution order.
 */
class Expectations {
    private var counter = AtomicInteger(0)

    fun expect(expected: Int) {
        val order = counter.incrementAndGet()
        Truth.assertThat(order).isEqualTo(expected)
    }

    fun expectUnreached() {
        throw AssertionError("should've not reached here")
    }

    fun expectTotal(total: Int) {
        Truth.assertThat(counter.get()).isEqualTo(total)
    }
}