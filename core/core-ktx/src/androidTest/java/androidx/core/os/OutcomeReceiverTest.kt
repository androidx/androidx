/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.os

import android.os.OutcomeReceiver
import androidx.core.util.TestContinuation
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

@SdkSuppress(minSdkVersion = 31)
@SmallTest
class OutcomeReceiverTest {
    @Test
    fun outcomeReceiverResumed() {
        val continuation = TestContinuation<Boolean>()
        val outcomeReceiver: OutcomeReceiver<Boolean, Exception> = continuation.asOutcomeReceiver()
        outcomeReceiver.onResult(true)

        // This should not resume the continuation again
        outcomeReceiver.onResult(false)
        assertEquals(true, continuation.result?.getOrThrow())
    }

    // No @Test! This just checks that type inference causes the following code to compile
    // and does not require specifying generics of result type + exception type explicitly.
    @Suppress("unused")
    private fun typeInferenceCompiles() {
        sampleOutcomeReceiverMethod(TestContinuation<String>().asOutcomeReceiver())
    }

    /**
     * A sample method that accepts an [OutcomeReceiver] to test that the type inference
     * of [asOutcomeReceiver] behaves as expected
     */
    @Suppress("UNUSED_PARAMETER")
    private fun sampleOutcomeReceiverMethod(receiver: OutcomeReceiver<String, IOException>) {
        // This space for rent
    }
}
