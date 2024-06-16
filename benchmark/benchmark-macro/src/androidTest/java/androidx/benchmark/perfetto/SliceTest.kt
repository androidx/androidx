/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.benchmark.perfetto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SliceTest {
    @Test
    fun frameId() {
        assertEquals(1234, Slice("Choreographer#doFrame 1234", 1, 2).frameId)
    }

    @Test
    fun frameId_extended() {
        // some OEMs have added additional metadata to standard tracepoints
        // we'll fix these best effort as they are reported
        assertEquals(123, Slice("Choreographer#doFrame 123 234 extra=91929", 1, 2).frameId)
    }
}
