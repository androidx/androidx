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

import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.JetpackConnectionService
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.TEST_CALL_ATTRIB_NAME
import androidx.core.telecom.test.utils.TestUtils.TEST_PHONE_NUMBER_9001
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@RequiresApi(VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class JetpackConnectionServiceTest : BaseTelecomTest() {
    private val callChannels = CallChannels()

    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        callChannels.closeAllChannels()
    }

    /**
     * Ensure an outgoing Connection object has its properties set before sending it off to the
     * platform. The properties should reflect everything that is set in CallAttributes.
     */
    @SmallTest
    @Test
    fun testConnectionServicePropertiesAreSet_outgoingCall() {
        // create the CallAttributes
        val attributes =
            TestUtils.createCallAttributes(
                CallAttributesCompat.DIRECTION_OUTGOING,
                mPackagePhoneAccountHandle
            )
        // simulate the connection being created
        val connection =
            mConnectionService.createSelfManagedConnection(
                createConnectionRequest(attributes),
                CallAttributesCompat.DIRECTION_OUTGOING
            )
        // verify / assert connection properties
        verifyConnectionPropertiesBasics(connection)
        assertEquals(Connection.STATE_DIALING, connection!!.state)
    }

    /**
     * Ensure an incoming Connection object has its properties set before sending it off to the
     * platform. The properties should reflect everything that is set in CallAttributes.
     */
    @SmallTest
    @Test
    fun testConnectionServicePropertiesAreSet_incomingCall() {
        // create the CallAttributes
        val attributes =
            TestUtils.createCallAttributes(
                CallAttributesCompat.DIRECTION_INCOMING,
                mPackagePhoneAccountHandle
            )
        // simulate the connection being created
        val connection =
            mConnectionService.createSelfManagedConnection(
                createConnectionRequest(attributes),
                CallAttributesCompat.DIRECTION_INCOMING
            )
        // verify / assert connection properties
        verifyConnectionPropertiesBasics(connection)
        assertEquals(Connection.STATE_RINGING, connection!!.state)
    }

    /**
     * Ensure an incoming Connection object has its extras set before sending it off to the
     * platform.
     */
    @SmallTest
    @Test
    fun testConnectionServiceExtrasAreSet_incomingCall() {
        // create the CallAttributes
        val attributes =
            TestUtils.createCallAttributes(
                CallAttributesCompat.DIRECTION_INCOMING,
                mPackagePhoneAccountHandle
            )
        // simulate the connection being created
        val connection =
            mConnectionService.createSelfManagedConnection(
                createConnectionRequest(attributes),
                CallAttributesCompat.DIRECTION_INCOMING
            )
        // verify / assert connection extras
        val unwrappedConnection = connection!!
        assertTrue(
            unwrappedConnection.extras.getBoolean(
                CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED
            )
        )
    }

    /**
     * ensure JetpackConnectionService#onCreateOutgoingConnection does not throw an exception if any
     * of the arguments are null.
     */
    @SmallTest
    @Test
    fun testOnCreateOutgoingConnectionWithNullArgs() {
        mConnectionService.onCreateOutgoingConnection(
            null /* connectionManagerPhoneAccount */,
            null /* request */
        )
    }

    /**
     * ensure JetpackConnectionService#onCreateOutgoingConnectionFailed does not throw an exception
     * if any of the arguments are null.
     */
    @SmallTest
    @Test
    fun testOnCreateOutgoingConnectionFailedWithNullArgs() {
        mConnectionService.onCreateOutgoingConnectionFailed(
            null /* connectionManagerPhoneAccount */,
            null /* request */
        )
    }

    /**
     * ensure JetpackConnectionService#onCreateIncomingConnection does not throw an exception if any
     * of the arguments are null.
     */
    @SmallTest
    @Test
    fun testOnCreateIncomingConnectionWithNullArgs() {
        mConnectionService.onCreateIncomingConnection(
            null /* connectionManagerPhoneAccount */,
            null /* request */
        )
    }

    /**
     * ensure JetpackConnectionService#onCreateIncomingConnectionFailed does not throw an exception
     * if any of the arguments are null.
     */
    @SmallTest
    @Test
    fun testOnCreateIncomingConnectionFailedWithNullArgs() {
        mConnectionService.onCreateIncomingConnectionFailed(
            null /* connectionManagerPhoneAccount */,
            null /* request */
        )
    }

    /**
     * Ensure an outgoing Connection object has its extras set before sending it off to the
     * platform.
     */
    @SmallTest
    @Test
    fun testConnectionServiceExtrasAreSet_outgoingCall() {
        // create the CallAttributes
        val attributes =
            TestUtils.createCallAttributes(
                CallAttributesCompat.DIRECTION_OUTGOING,
                mPackagePhoneAccountHandle
            )
        // simulate the connection being created
        val connection =
            mConnectionService.createSelfManagedConnection(
                createConnectionRequest(attributes),
                CallAttributesCompat.DIRECTION_OUTGOING
            )
        // verify / assert connection extras
        val unwrappedConnection = connection!!
        assertTrue(
            unwrappedConnection.extras.getBoolean(
                CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED
            )
        )
    }

    private fun verifyConnectionPropertiesBasics(connection: Connection?) {
        // assert it's not null
        assertNotNull(connection)
        // unwrap for testing
        val unwrappedConnection = connection!!
        // assert all the properties are the same
        assertEquals(TEST_CALL_ATTRIB_NAME, unwrappedConnection.callerDisplayName)
        assertEquals(TEST_PHONE_NUMBER_9001, unwrappedConnection.address)
        assertEquals(
            Connection.CAPABILITY_HOLD,
            unwrappedConnection.connectionCapabilities and Connection.CAPABILITY_HOLD
        )
        assertEquals(
            Connection.CAPABILITY_SUPPORT_HOLD,
            unwrappedConnection.connectionCapabilities and Connection.CAPABILITY_SUPPORT_HOLD
        )
        assertEquals(0, JetpackConnectionService.mPendingConnectionRequests.size)
    }

    private fun createConnectionRequest(
        callAttributesCompat: CallAttributesCompat
    ): ConnectionRequest {
        // wrap in PendingRequest
        val pendingRequestId = "123"
        val pendingRequestIdBundle = Bundle()
        pendingRequestIdBundle.putString(
            JetpackConnectionService.REQUEST_ID_MATCHER_KEY,
            pendingRequestId
        )
        val pr =
            JetpackConnectionService.PendingConnectionRequest(
                pendingRequestId,
                callAttributesCompat,
                callChannels,
                mWorkerContext,
                null,
                TestUtils.mOnAnswerLambda,
                TestUtils.mOnDisconnectLambda,
                TestUtils.mOnSetActiveLambda,
                TestUtils.mOnSetInActiveLambda,
                TestUtils.mOnEventLambda,
                MutableSharedFlow(),
                null,
                CompletableDeferred()
            )

        // add to the list of pendingRequests
        JetpackConnectionService.mPendingConnectionRequests.add(pr)
        // create a ConnectionRequest
        return ConnectionRequest(
            mPackagePhoneAccountHandle,
            TEST_PHONE_NUMBER_9001,
            pendingRequestIdBundle
        )
    }
}
