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
package androidx.fragment.app

import android.os.SystemClock
import org.mockito.exceptions.base.MockitoAssertionError
import org.mockito.internal.verification.VerificationModeFactory
import org.mockito.internal.verification.api.VerificationData
import org.mockito.verification.VerificationMode

private const val TIME_SLICE: Long = 50

fun within(timeout: Long): VerificationMode {
    return object : VerificationMode {

        override fun verify(data: VerificationData) {
            var remainingTime = timeout
            var errorToRethrow: MockitoAssertionError? = null
            // Loop in the same way we do in PollingCheck, sleeping and then testing for the target
            // invocation
            while (remainingTime > 0) {
                SystemClock.sleep(TIME_SLICE)

                try {
                    val actualInvocations = data.allInvocations
                    // Iterate over all invocations so far to see if we have a match
                    for (invocation in actualInvocations) {
                        if (data.target.matches(invocation)) {
                            // Found our match within our timeout. Mark all invocations as verified
                            markAllInvocationsAsVerified(data)
                            // and return
                            return
                        }
                    }
                } catch (assertionError: MockitoAssertionError) {
                    errorToRethrow = assertionError
                }

                remainingTime -= TIME_SLICE
            }

            if (errorToRethrow != null) {
                throw errorToRethrow
            }

            throw MockitoAssertionError(
                "Timed out while waiting ${remainingTime}ms for ${data.target}"
            )
        }

        override fun description(description: String): VerificationMode {
            return VerificationModeFactory.description(this, description)
        }

        private fun markAllInvocationsAsVerified(data: VerificationData) {
            for (invocation in data.allInvocations) {
                invocation.markVerified()
                data.target.captureArgumentsFrom(invocation)
            }
        }
    }
}
