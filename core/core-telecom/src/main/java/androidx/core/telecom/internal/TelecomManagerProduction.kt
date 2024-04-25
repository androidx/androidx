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

import android.content.Context
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.OutcomeReceiver
import android.telecom.CallAttributes
import android.telecom.CallControl
import android.telecom.CallControlCallback
import android.telecom.CallEventCallback
import android.telecom.CallException
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/**
 * This implementation of [TelecomManagerAdapter] should be used in production.  In production
 * runtime, the client should be calling into the platform TelecomManager.
 */
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(VERSION_CODES.O)
internal class TelecomManagerProduction(context: Context) : TelecomManagerAdapter {
    private val mTelecomManager: TelecomManager =
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Suppress("ClassVerificationFailure")
    override fun addCall(
        callAttributes: CallAttributes,
        executor: Executor,
        pendingControl: OutcomeReceiver<CallControl, CallException>,
        handshakes: CallControlCallback,
        events: CallEventCallback
    ) {
        mTelecomManager.addCall(callAttributes, executor, pendingControl, handshakes, events)
    }

    @RequiresApi(VERSION_CODES.O)
    override fun registerPhoneAccount(phoneAccount: PhoneAccount?) {
        mTelecomManager.registerPhoneAccount(phoneAccount)
    }

    @RequiresApi(VERSION_CODES.O)
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    override fun placeCall(address: Uri, extras: Bundle) {
        mTelecomManager.placeCall(address, extras)
    }

    @RequiresApi(VERSION_CODES.O)
    override fun addNewIncomingCall(handle: PhoneAccountHandle?, extras: Bundle) {
        mTelecomManager.addNewIncomingCall(handle, extras)
    }
}
