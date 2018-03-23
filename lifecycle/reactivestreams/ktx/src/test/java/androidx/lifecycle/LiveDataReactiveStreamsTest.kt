/*
 * Copyright (C) 2018 The Android Open Source Project
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
import io.reactivex.processors.PublishProcessor
import io.reactivex.processors.ReplayProcessor
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LiveDataReactiveStreamsTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var lifecycleOwner: LifecycleOwner

    @Before fun init() {
        lifecycleOwner = object : LifecycleOwner {
            internal var registry = LifecycleRegistry(this)

            init {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            override fun getLifecycle(): Lifecycle {
                return registry
            }
        }
    }

    @Test fun convertsFromPublisher() {
        val processor = PublishProcessor.create<String>()
        val liveData = processor.toLiveData()

        val output = mutableListOf<String?>()
        liveData.observe(lifecycleOwner, Observer { output.add(it) })

        processor.onNext("foo")
        processor.onNext("bar")
        processor.onNext("baz")

        assertThat(output).containsExactly("foo", "bar", "baz")
    }

    @Test fun convertsToPublisherWithSyncData() {
        val liveData = MutableLiveData<String>()
        liveData.value = "foo"

        val outputProcessor = ReplayProcessor.create<String>()
        liveData.toPublisher(lifecycleOwner).subscribe(outputProcessor)

        liveData.value = "bar"
        liveData.value = "baz"

        assertThat(outputProcessor.values).asList().containsExactly("foo", "bar", "baz")
    }
}
