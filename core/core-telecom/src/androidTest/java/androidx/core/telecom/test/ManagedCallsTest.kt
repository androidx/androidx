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

package androidx.core.telecom.test

import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.ManagedConnection
import androidx.core.telecom.test.utils.ManagedConnectionService
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NOTE: This class requires a minSdkVersion = Build.VERSION_CODES.Q
 *
 * [ManagedCallsTest] should be used to test core-telecom with traditional sim calling.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
@RequiresApi(Build.VERSION_CODES.Q)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ManagedCallsTest : BaseTelecomTest() {
    private val address = Uri.parse("tel:555-TEST")
    private val mManagedConnectionService = ManagedConnectionService()
    private val mPhoneAccountHandle =
        PhoneAccountHandle(
            ComponentName(
                "androidx.core.telecom.test",
                "androidx.core.telecom.test.utils.ManagedConnectionService"
            ),
            "2"
        )
    private val mPhoneAccount =
        PhoneAccount.builder(mPhoneAccountHandle, "ManagedJetpackAcct")
            .setAddress(address)
            .setSubscriptionAddress(address)
            .setCapabilities(
                PhoneAccount.CAPABILITY_CALL_PROVIDER or
                    PhoneAccount.CAPABILITY_VIDEO_CALLING or
                    PhoneAccount.CAPABILITY_RTT or
                    PhoneAccount.CAPABILITY_CONNECTION_MANAGER or
                    PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS or
                    PhoneAccount.CAPABILITY_ADHOC_CONFERENCE_CALLING
            )
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .build()

    val OUTGOING_MANAGED_CALL_ATTRIBUTES =
        CallAttributesCompat(
            OUTGOING_NAME,
            Uri.parse("tel:" + TestUtils.TEST_PHONE_NUMBER),
            CallAttributesCompat.DIRECTION_OUTGOING,
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            ALL_CALL_CAPABILITIES
        )

    @Before
    fun setUp() {
        Utils.resetUtils()
        mTelecomManager.registerPhoneAccount(mPhoneAccount)
        TestUtils.enablePhoneAccountHandle(mContext, mPhoneAccountHandle)
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
        mTelecomManager.unregisterPhoneAccount(mPhoneAccountHandle)
    }

    /** verify simulated managed calling is working in the jetpack layer. */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @LargeTest
    @Test
    fun testAddManagedCall() {
        val deferredConnection = CompletableDeferred<ManagedConnection>()
        runBlocking {
            val connection = addManagedCall(OUTGOING_MANAGED_CALL_ATTRIBUTES, deferredConnection)
            disconnectAndDestroyConnection(connection)
        }
    }

    /**
     * ********************************************************************************************
     * Helpers
     * *******************************************************************************************
     */
    private suspend fun addManagedCall(
        callAttributes: CallAttributesCompat,
        deferredConnection: CompletableDeferred<ManagedConnection>
    ): ManagedConnection {
        val request =
            ManagedConnectionService.PendingConnectionRequest(callAttributes, deferredConnection)
        mManagedConnectionService.createConnectionRequest(
            mTelecomManager,
            mPhoneAccountHandle,
            request
        )
        deferredConnection.await()
        val connection = deferredConnection.getCompleted()
        delay(10)
        connection.setActive()
        delay(10)
        return connection
    }

    private fun disconnectAndDestroyConnection(connection: ManagedConnection) {
        connection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        connection.destroy()
    }
}
