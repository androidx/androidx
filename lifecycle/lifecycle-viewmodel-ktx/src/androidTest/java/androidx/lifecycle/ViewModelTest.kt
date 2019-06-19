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

package androidx.lifecycle

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class ViewModelTest {

    @Test fun testVmScope() {
        val vm = object : ViewModel() {}
        val job1 = vm.viewModelScope.launch { delay(1000) }
        val job2 = vm.viewModelScope.launch { delay(1000) }
        vm.clear()
        Truth.assertThat(job1.isCancelled).isTrue()
        Truth.assertThat(job2.isCancelled).isTrue()
    }

    @Test fun testStartJobInClearedVM() {
        val vm = object : ViewModel() {}
        vm.clear()
        val job1 = vm.viewModelScope.launch { delay(1000) }
        Truth.assertThat(job1.isCancelled).isTrue()
    }

    @Test fun testSameScope() {
        val vm = object : ViewModel() {}
        val scope1 = vm.viewModelScope
        val scope2 = vm.viewModelScope
        Truth.assertThat(scope1).isSameInstanceAs(scope2)
        vm.clear()
        val scope3 = vm.viewModelScope
        Truth.assertThat(scope3).isSameInstanceAs(scope2)
    }

    @Test fun testJobIsSuperVisor() {
        val vm = object : ViewModel() {}
        val scope = vm.viewModelScope
        val delayingDeferred = scope.async { delay(Long.MAX_VALUE) }
        val failingDeferred = scope.async { throw Error() }

        runBlocking {
            try {
                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                failingDeferred.await()
            } catch (e: Error) {
            }
            Truth.assertThat(delayingDeferred.isActive).isTrue()
            delayingDeferred.cancelAndJoin()
        }
    }
}