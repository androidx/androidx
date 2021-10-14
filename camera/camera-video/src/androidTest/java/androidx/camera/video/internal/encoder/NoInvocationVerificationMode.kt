/*
 * Copyright 2020 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.video.internal.encoder

import android.os.SystemClock
import androidx.annotation.RequiresApi
import org.mockito.exceptions.base.MockitoAssertionError
import org.mockito.exceptions.base.MockitoException
import org.mockito.internal.invocation.InvocationMarker
import org.mockito.internal.invocation.InvocationsFinder
import org.mockito.internal.verification.VerificationModeFactory
import org.mockito.internal.verification.api.VerificationData
import org.mockito.verification.Timeout
import org.mockito.verification.VerificationMode

private class NoInvocationVerificationMode : VerificationMode {

    private val noInvokeDuration: Long
    private var invocationCount = -1
    private var checkNoInvocationStartTime = 0L

    constructor(noInvokeDuration: Long) {
        if (noInvokeDuration < 0) {
            throw MockitoException("Negative value is not allowed here")
        }
        this.noInvokeDuration = noInvokeDuration
    }

    override fun verify(data: VerificationData?) {
        val invocations = data!!.allInvocations
        val wanted = data.target
        val actualInvocations = InvocationsFinder.findInvocations(invocations, wanted)
        val actualCount = actualInvocations.size
        val currentTime = SystemClock.uptimeMillis()

        if (invocationCount == -1) {
            invocationCount = actualCount
            checkNoInvocationStartTime = currentTime
            throw MockitoAssertionError("The first check for no invocation condition.")
        } else if (invocationCount == -1 || actualCount > invocationCount) {
            invocationCount = actualCount
            checkNoInvocationStartTime = currentTime
            throw MockitoAssertionError("There is new invocation.")
        } else if (currentTime - checkNoInvocationStartTime > noInvokeDuration) {
            InvocationMarker.markVerified(actualInvocations, wanted)
        } else {
            throw MockitoAssertionError("Keep monitoring invocation")
        }
    }

    override fun description(description: String?): VerificationMode {
        return VerificationModeFactory.description(this, description)
    }
}

fun noInvocation(noInvocationDuration: Long, timeout: Long): Timeout {
    return Timeout(timeout, NoInvocationVerificationMode(noInvocationDuration))
}
