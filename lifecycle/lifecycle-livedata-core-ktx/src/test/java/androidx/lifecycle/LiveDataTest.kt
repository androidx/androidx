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
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
class LiveDataTest {

    @get:Rule
    val mInstantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun observe() {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = UnconfinedTestDispatcher())

        val liveData = MutableLiveData<String>()
        var value = ""
        liveData.observe<String>(lifecycleOwner) { newValue ->
            value = newValue
        }

        liveData.value = "261"
        assertThat(value).isEqualTo("261")
    }
}
