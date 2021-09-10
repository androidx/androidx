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

package androidx.paging

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RecordingCallbackTest {
    @Test
    fun recordReplay() {
        val recordingCallback = RecordingCallback()

        @Suppress("DEPRECATION")
        val failCallback = object : PagedList.Callback() {
            override fun onChanged(position: Int, count: Int) = fail("not expected")
            override fun onInserted(position: Int, count: Int) = fail("not expected")
            override fun onRemoved(position: Int, count: Int) = fail("not expected")
        }

        // nothing recorded, verify nothing dispatched
        recordingCallback.dispatchRecordingTo(failCallback)

        recordingCallback.onChanged(1, 2)
        recordingCallback.onInserted(3, 4)
        recordingCallback.onRemoved(5, 6)

        var inc = 0
        @Suppress("DEPRECATION")
        val verifyCallback = object : PagedList.Callback() {
            override fun onChanged(position: Int, count: Int) {
                assertEquals(inc, 0)
                assertEquals(position, 1)
                assertEquals(count, 2)
                inc += 1
            }

            override fun onInserted(position: Int, count: Int) {
                assertEquals(inc, 1)
                assertEquals(position, 3)
                assertEquals(count, 4)
                inc += 1
            }

            override fun onRemoved(position: Int, count: Int) {
                assertEquals(inc, 2)
                assertEquals(position, 5)
                assertEquals(count, 6)
                inc += 1
            }
        }
        recordingCallback.dispatchRecordingTo(verifyCallback)
        assertEquals(3, inc)

        // verify recording cleaned up
        recordingCallback.dispatchRecordingTo(failCallback)
    }
}