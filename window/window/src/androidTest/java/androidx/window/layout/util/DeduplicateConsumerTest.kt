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

package androidx.window.layout.util

import androidx.core.util.Consumer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeduplicateConsumerTest {

    @Test
    fun deduplicateConsumer_accept_onlyNotifiesIfChanged() {
        val callback = mock<Consumer<String>>()
        val consumer = DeduplicateConsumer(callback)

        consumer.accept("test")
        consumer.accept("test")
        consumer.accept("other")
        consumer.accept("other")
        consumer.accept("test")

        verify(callback, times(3)).accept(any())
        verify(callback, times(2)).accept("test")
        verify(callback, times(1)).accept("other")
    }
}
