/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DataSourceTest {

    @Test
    fun addInvalidatedCallback_triggersImmediatelyIfAlreadyInvalid() {
        val pagingSource = TestPositionalDataSource(listOf())
        var invalidateCalls = 0

        pagingSource.invalidate()
        pagingSource.addInvalidatedCallback { invalidateCalls++ }
        assertThat(invalidateCalls).isEqualTo(1)
    }

    @Test
    fun addInvalidatedCallback_avoidsRetriggeringWhenCalledRecursively() {
        val pagingSource = TestPositionalDataSource(listOf())
        var invalidateCalls = 0

        pagingSource.addInvalidatedCallback {
            pagingSource.addInvalidatedCallback { invalidateCalls++ }
            pagingSource.invalidate()
            pagingSource.addInvalidatedCallback { invalidateCalls++ }
            invalidateCalls++
        }
        pagingSource.invalidate()
        assertThat(invalidateCalls).isEqualTo(3)
    }
}
