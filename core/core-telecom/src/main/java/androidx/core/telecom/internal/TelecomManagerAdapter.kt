/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.internal

import android.net.Uri
import android.os.Bundle
import android.os.OutcomeReceiver
import android.telecom.CallAttributes
import android.telecom.CallControl
import android.telecom.CallControlCallback
import android.telecom.CallEventCallback
import android.telecom.CallException
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/**
 * [TelecomManagerAdapter] is intended for the [androidx.core.telecom.CallsManager] class. This
 * interface helps utilize the Dependency Injection pattern so that
 * [androidx.core.telecom.CallsManager] can use the platform TelecomManager in production but in
 * testing, different objects can be injected to mock platform behavior.
 */
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal interface TelecomManagerAdapter {

    fun addCall(
        callAttributes: CallAttributes,
        executor: Executor,
        pendingControl: OutcomeReceiver<CallControl, CallException>,
        handshakes: CallControlCallback,
        events: CallEventCallback
    )

    fun registerPhoneAccount(phoneAccount: PhoneAccount?)

    fun placeCall(address: Uri, extras: Bundle)

    fun addNewIncomingCall(handle: PhoneAccountHandle?, extras: Bundle)
}
