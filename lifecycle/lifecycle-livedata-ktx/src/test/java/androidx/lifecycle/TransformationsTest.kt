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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class TransformationsTest {

    @get:Rule
    val mInstantTaskExecutorRule = InstantTaskExecutorRule()

    private val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.STARTED
        }
        override fun getLifecycle() = registry
    }

    @Test fun map() {
        val source = MutableLiveData<String>()
        val mapped = source.map { input -> input.length }
        var receivedValue = -1
        mapped.observe(lifecycleOwner) { receivedValue = it }
        source.value = "four"
        assertThat(receivedValue).isEqualTo(4)
    }

    @Test fun switchMap() {
        val trigger = MutableLiveData<Int>()
        val first = MutableLiveData<String>()
        val second = MutableLiveData<String>()
        val result = trigger.switchMap { input -> if (input == 1) first else second }

        var receivedValue = ""
        result.observe(lifecycleOwner) { receivedValue = it }
        first.value = "first"
        trigger.value = 1
        second.value = "second"
        assertThat(receivedValue).isEqualTo("first")
        trigger.value = 2
        assertThat(receivedValue).isEqualTo("second")
        first.value = "failure"
        assertThat(receivedValue).isEqualTo("second")
    }

    @Test fun distinctUntilChanged() {
        val originalLiveData = MutableLiveData<String>()
        val dedupedLiveData = originalLiveData.distinctUntilChanged()

        var counter = 0
        dedupedLiveData.observe(lifecycleOwner) { counter++ }
        assertThat(counter).isEqualTo(0)

        originalLiveData.value = "new value"
        assertThat(dedupedLiveData.value).isEqualTo("new value")
        assertThat(counter).isEqualTo(1)

        originalLiveData.value = "new value"
        assertThat(counter).isEqualTo(1)

        originalLiveData.value = "newer value"
        assertThat(dedupedLiveData.value).isEqualTo("newer value")
        assertThat(counter).isEqualTo(2)
    }
}
