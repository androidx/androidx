/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.RetainObserver
import androidx.compose.runtime.Stable
import kotlin.test.assertEquals
import kotlin.test.fail

@Stable
class CountingRetainObject : RetainObserver {
    var retained = 0
        private set

    var entered = 0
        private set

    var exited = 0
        private set

    var retired = 0
        private set

    override fun onRetained() {
        retained++
    }

    override fun onEnteredComposition() {
        entered++
        assertValidCounts()
    }

    override fun onExitedComposition() {
        exited++
        assertValidCounts()
    }

    override fun onRetired() {
        retired++
        assertValidCounts()
    }

    fun assertCounts(
        retained: Int = this.retained,
        entered: Int = this.entered,
        exited: Int = this.exited,
        retired: Int = this.retired,
    ) {
        assertEquals(
            "[Retained: $retained, Entered: $entered, Exited: $exited, Retired: $retired]",
            "[Retained: ${this.retained}, Entered: ${this.entered}, Exited: ${this.exited}, " +
                "Retired: ${this.retired}]",
            "Received an unexpected number of callback invocations",
        )
    }

    private fun assertValidCounts() {
        if (retained == 0 && entered + exited + retired > 0) {
            fail("RetainObject received events without being retained")
        }

        if (retained < retired) {
            fail("RetainObject was retired more times than it was retained")
        }

        if (exited > entered) {
            fail("RetainObject exited the composition more times than it entered")
        }

        if (entered > retained + exited) {
            fail("RetainObject re-entered the composition without first exiting")
        }
    }
}
