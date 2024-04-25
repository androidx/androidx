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

package androidx.core.telecom.test.utils

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.telecom.CallAttributes
import android.telecom.CallControl
import android.telecom.CallControlCallback
import android.telecom.CallEventCallback
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallException
import androidx.core.telecom.internal.TelecomManagerAdapter
import java.util.concurrent.Executor

class TelecomManagerFailPlatformSide : TelecomManagerAdapter {

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    var mCurrentCallException: Int = CallException.ERROR_UNKNOWN

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun setCallException(errorCode: Int) {
        mCurrentCallException = errorCode
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun addCall(
        callAttributes: CallAttributes,
        executor: Executor,
        pendingControl: OutcomeReceiver<CallControl, android.telecom.CallException>,
        handshakes: CallControlCallback,
        events: CallEventCallback
    ) {
        pendingControl.onError(
            android.telecom.CallException("", mCurrentCallException))
    }

    override fun registerPhoneAccount(phoneAccount: PhoneAccount?) {
        // pass through. Essentially a mock.
    }

    override fun placeCall(address: Uri, extras: Bundle) {
        // pass through. Essentially a mock.
    }

    override fun addNewIncomingCall(handle: PhoneAccountHandle?, extras: Bundle) {
        // pass through. Essentially a mock.
    }
}
