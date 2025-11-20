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
import android.telecom.CallException
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccount.CAPABILITY_SELF_MANAGED
import android.telecom.PhoneAccount.CAPABILITY_SUPPORTS_CALL_STREAMING
import android.telecom.PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS
import android.telecom.PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING
import android.telecom.PhoneAccount.CAPABILITY_VIDEO_CALLING
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.AddCallResult
import androidx.core.telecom.internal.utils.BuildVersionAdapter
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestPermissionUtils.createBluetoothPermissionRule
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.GrantPermissionRule
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CallsManagerTest : BaseTelecomTest() {
    private val mTestClassName = "androidx.core.telecom.test"
    @get:Rule val bluetoothPermissionRule: GrantPermissionRule = createBluetoothPermissionRule()

    companion object {
        val TAG = CallsManagerTest::class.java.simpleName
    }

    @After
    fun tearDown() {
        Utils.resetUtils()
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithUpsideDownCakePlusBuild() {
        try {
            setTestBuildVersion(VERSION_CODES.UPSIDE_DOWN_CAKE)
            val account = mCallsManager.getPhoneAccountHandleForPackage()
            assertEquals(mTestClassName, account.componentName.className)
        } finally {
            Utils.resetUtils()
        }
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithUBuildWithTiramisuMinusBuild() {
        try {
            setTestBuildVersion(VERSION_CODES.TIRAMISU)
            val account = mCallsManager.getPhoneAccountHandleForPackage()
            assertEquals(CallsManager.CONNECTION_SERVICE_CLASS, account.componentName.className)
        } finally {
            Utils.resetUtils()
        }
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithInvalidBuild() {
        try {
            setTestBuildVersion(0)
            assertThrows(UnsupportedOperationException::class.java) {
                mCallsManager.getPhoneAccountHandleForPackage()
            }
        } finally {
            Utils.resetUtils()
        }
    }

    @SmallTest
    @Test
    fun testRegisterPhoneAccount() {
        Utils.resetUtils()

        if (Utils.hasInvalidBuildVersion()) {
            assertThrows(UnsupportedOperationException::class.java) {
                mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
            }
        } else {

            mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
            val account = mCallsManager.getBuiltPhoneAccount()!!
            assertNotNull(account.extras)
            assertTrue(account.extras.getBoolean(CallsManager.PLACEHOLDER_VALUE_ACCOUNT_BUNDLE))
            if (!Utils.shouldUseBackwardsCompatImplementation()) {
                assertTrue(
                    Utils.hasCapability(
                        CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS,
                        account.capabilities,
                    )
                )
            } else {
                assertTrue(
                    account.capabilities and CAPABILITY_SELF_MANAGED == CAPABILITY_SELF_MANAGED
                )
            }
        }
    }

    /**
     * Register all the capabilities currently exposed by the CallsManager class and verify they are
     * re-mapped to the correct platform capabilities.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegisterAllCapabilities() {
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING
        )

        val phoneAccount = mCallsManager.getBuiltPhoneAccount()!!
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_SELF_MANAGED))
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_SUPPORTS_VIDEO_CALLING))
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_VIDEO_CALLING))
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_SUPPORTS_CALL_STREAMING))
    }

    /**
     * Ensure all backwards compat builds can register video capabilities and the values are
     * re-mapped to the correct platform capabilities.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @SmallTest
    @Test
    fun testRegisterVideoCapabilitiesOnly() {
        mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING)

        val phoneAccount = mCallsManager.getBuiltPhoneAccount()!!
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_SELF_MANAGED))
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_SUPPORTS_VIDEO_CALLING))
        assertTrue(phoneAccount.hasCapabilities(CAPABILITY_VIDEO_CALLING))
    }

    /**
     * Verify that calls starting in the video state that are originally initialized with the
     * earpiece route are switched to the speaker phone audio route. This test creates VoIP calls
     * using the APIs introduced in Android U.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testAddOutgoingVideoCall_CallEndpointShouldBeSpeaker_Transactional() {
        runBlocking { assertVideoCallStartsWithSpeakerEndpoint() }
    }

    /**
     * Verify that calls starting in the video state that are originally initialized with the
     * earpiece route are switched to the speaker phone audio route. This test creates VoIP calls
     * using the legacy ConnectionService method.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @SmallTest
    @Test
    fun testAddOutgoingVideoCall_CallEndpointShouldBeSpeaker_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking { assertVideoCallStartsWithSpeakerEndpoint() }
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalAppActions::class)
    @SmallTest
    @Test
    fun testPauseExecutionOrThrow_Transactional() {
        runBlocking {
            val cd = CompletableDeferred<AddCallResult>()
            cd.complete(AddCallResult.SuccessCallSession())
            mCallsManager.pauseExecutionUntilCallIsReadyOrTimeout(cd)
        }
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @OptIn(ExperimentalAppActions::class)
    @SmallTest
    @Test
    fun testPauseExecutionOrThrow_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking {
            val cd = CompletableDeferred<AddCallResult>()
            cd.complete(AddCallResult.Error(CallException.CODE_ERROR_UNKNOWN))
            try {
                mCallsManager.pauseExecutionUntilCallIsReadyOrTimeout(cd)
                fail(
                    "failed to throw a CallException out to the client when the platform signaled" +
                        " it failed to add the call"
                )
            } catch (e: androidx.core.telecom.CallException) {
                Log.i(TAG, "callException=[$e] was thrown as expected")
            }
        }
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testEndToEndSelectingAStartingEndpointTransactional() {
        runBlocking { assertStartingCallEndpoint(coroutineContext) }
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @SmallTest
    @Test
    fun testEndToEndSelectingAStartingEndpointBackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking { assertStartingCallEndpoint(coroutineContext) }
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegister_default_onModernSdk_usesModernPath() {
        setTestBuildVersion(VERSION_CODES.UPSIDE_DOWN_CAKE) // SDK 34

        // Use default parameter for backwardsCompatSdkLevel (33)
        mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)

        val account = mCallsManager.getBuiltPhoneAccount()!!
        val handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Should use modern path
        assertEquals(VERSION_CODES.TIRAMISU, CallsManager.mBackwardsCompatUpperBound)
        assertTrue(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(mContext.packageName, handle.componentName.className)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.TIRAMISU)
    @SmallTest
    @Test
    fun testRegister_default_onLegacySdk_usesLegacyPath() {
        setTestBuildVersion(VERSION_CODES.TIRAMISU) // SDK 33

        mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)

        val account = mCallsManager.getBuiltPhoneAccount()!!
        val handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Should use legacy path
        assertEquals(VERSION_CODES.TIRAMISU, CallsManager.mBackwardsCompatUpperBound)
        assertFalse(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(CallsManager.CONNECTION_SERVICE_CLASS, handle.componentName.className)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegister_override_forceLegacyPathOnModernSdk() {
        setTestBuildVersion(35) // VanillaIceCream

        // Force legacy path by setting the upper bound to the current SDK
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_BASELINE,
            backwardsCompatSdkLevel = 35,
        )

        val account = mCallsManager.getBuiltPhoneAccount()!!
        val handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Should be forced to use legacy path
        assertEquals(35, CallsManager.mBackwardsCompatUpperBound)
        assertFalse(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(CallsManager.CONNECTION_SERVICE_CLASS, handle.componentName.className)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegister_override_doesNotApplyOnNewerSdk() {
        setTestBuildVersion(36) // Baklava

        // Set the upper bound below the current SDK
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_BASELINE,
            backwardsCompatSdkLevel = 35,
        )

        val account = mCallsManager.getBuiltPhoneAccount()!!
        val handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Should revert to modern path because current SDK (36) is > bound (35)
        assertEquals(35, CallsManager.mBackwardsCompatUpperBound)
        assertTrue(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(mContext.packageName, handle.componentName.className)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegister_override_clampsValueTooHigh() {
        setTestBuildVersion(35) // VanillaIceCream

        // Pass an override value (40) that is higher than the current SDK (35)
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_BASELINE,
            backwardsCompatSdkLevel = 40,
        )

        val account = mCallsManager.getBuiltPhoneAccount()!!
        val handle = mCallsManager.getPhoneAccountHandleForPackage()

        // The value should be clamped to 35, forcing the legacy path
        assertEquals(35, CallsManager.mBackwardsCompatUpperBound)
        assertFalse(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(CallsManager.CONNECTION_SERVICE_CLASS, handle.componentName.className)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegister_override_clampsValueTooLow() {
        setTestBuildVersion(VERSION_CODES.UPSIDE_DOWN_CAKE) // SDK 34

        // Pass an override value (32) lower than the minimum (33)
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_BASELINE,
            backwardsCompatSdkLevel = 32,
        )

        val account = mCallsManager.getBuiltPhoneAccount()!!
        val handle = mCallsManager.getPhoneAccountHandleForPackage()

        // The value is clamped to 33. Since current SDK (34) > 33, it should use the modern path.
        assertEquals(VERSION_CODES.TIRAMISU, CallsManager.mBackwardsCompatUpperBound)
        assertTrue(
            account.hasCapabilities(PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS)
        )
        assertEquals(mContext.packageName, handle.componentName.className)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRegister_reRegistration_updatesImplementationPath() {
        setTestBuildVersion(35) // SDK 35

        // Step 1: Register with default (modern path)
        mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)

        var account = mCallsManager.getBuiltPhoneAccount()!!
        var handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Verify modern path
        assertEquals(33, CallsManager.mBackwardsCompatUpperBound)
        assertTrue(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(mTestClassName, handle.componentName.className)

        // Step 2: Re-register, forcing legacy path
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_BASELINE,
            backwardsCompatSdkLevel = 35,
        )

        account = mCallsManager.getBuiltPhoneAccount()!!
        handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Verify legacy path
        assertEquals(35, CallsManager.mBackwardsCompatUpperBound)
        assertFalse(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(CallsManager.CONNECTION_SERVICE_CLASS, handle.componentName.className)

        // Step 3: Re-register, reverting to modern path
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_BASELINE,
            backwardsCompatSdkLevel = 33, // Default
        )

        account = mCallsManager.getBuiltPhoneAccount()!!
        handle = mCallsManager.getPhoneAccountHandleForPackage()

        // Verify modern path again
        assertEquals(33, CallsManager.mBackwardsCompatUpperBound)
        assertTrue(account.hasCapabilities(CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS))
        assertEquals(mTestClassName, handle.componentName.className)
    }

    private suspend fun assertStartingCallEndpoint(coroutineContext: CoroutineContext) {
        mCallsManager.registerAppWithTelecom(
            CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING
        )
        var preCallEndpointsScope: CoroutineScope? = null
        try {
            val endpointsFlow = mCallsManager.getAvailableStartingCallEndpoints()

            val initialEndpointsJob = CompletableDeferred<List<CallEndpointCompat>>()
            CoroutineScope(coroutineContext).launch {
                preCallEndpointsScope = this
                Log.i(TAG, "launched initialEndpointsJob")
                endpointsFlow.collect {
                    it.forEach { endpoint ->
                        Log.i(TAG, "endpointsFlow: collecting endpoint=[$endpoint]")
                    }
                    initialEndpointsJob.complete(it)
                }
            }
            Log.i(TAG, "initialEndpointsJob STARTED")
            initialEndpointsJob.await()
            Log.i(TAG, "initialEndpointsJob COMPLETED")
            val initialEndpoints = initialEndpointsJob.getCompleted()
            val earpieceEndpoint =
                initialEndpoints.find { it.type == CallEndpointCompat.TYPE_EARPIECE }
            if (initialEndpoints.size > 1 && earpieceEndpoint != null) {
                Log.i(TAG, "found 2 endpoints, including TYPE_EARPIECE")
                mCallsManager.addCall(
                    CallAttributesCompat(
                        OUTGOING_NAME,
                        TestUtils.TEST_ADDRESS,
                        CallAttributesCompat.DIRECTION_OUTGOING,
                        CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                        ALL_CALL_CAPABILITIES,
                        earpieceEndpoint,
                    ),
                    TestUtils.mOnAnswerLambda,
                    TestUtils.mOnDisconnectLambda,
                    TestUtils.mOnSetActiveLambda,
                    TestUtils.mOnSetInActiveLambda,
                ) {
                    Log.i(TAG, "addCallWithStartingCallEndpoint: running CallControlScope")
                    launch {
                        val waitUntilEarpieceEndpointJob = CompletableDeferred<CallEndpointCompat>()

                        val flowsJob = launch {
                            val earpieceFlow =
                                currentCallEndpoint.filter {
                                    Log.i(TAG, "currentCallEndpoint: e=[$it]")
                                    it.type == CallEndpointCompat.TYPE_EARPIECE
                                }

                            earpieceFlow.collect {
                                Log.i(TAG, "earpieceFlow.collect=[$it]")
                                waitUntilEarpieceEndpointJob.complete(it)
                            }
                        }

                        Log.i(TAG, "addCallWithStartingCallEndpoint: before await")
                        waitUntilEarpieceEndpointJob.await()
                        Log.i(TAG, "addCallWithStartingCallEndpoint: after await")

                        // at this point, the CallEndpoint has been found
                        val endpoint = waitUntilEarpieceEndpointJob.getCompleted()
                        assertNotNull(endpoint)
                        assertEquals(CallEndpointCompat.TYPE_EARPIECE, endpoint.type)

                        // finally, terminate the call
                        disconnect(DisconnectCause(DisconnectCause.LOCAL))
                        // stop collecting flows so the test can end
                        flowsJob.cancel()
                        Log.i(TAG, " flowsJob.cancel()")
                    }
                }
            } else {
                Log.i(
                    TAG,
                    "assertStartingCallEndpoint: " +
                        "endpoints.size=[${initialEndpoints.size}], earpiece=[$earpieceEndpoint]",
                )
                preCallEndpointsScope?.cancel()
            }
        } finally {
            preCallEndpointsScope?.cancel()
        }
    }

    suspend fun assertVideoCallStartsWithSpeakerEndpoint() {
        assertWithinTimeout_addCall(
            CallAttributesCompat(
                TestUtils.OUTGOING_NAME,
                TestUtils.TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
            )
        ) {
            launch {
                val waitUntilSpeakerEndpointJob = CompletableDeferred<CallEndpointCompat>()

                val flowsJob = launch {
                    val speakerFlow =
                        currentCallEndpoint.filter { it.type == CallEndpointCompat.TYPE_SPEAKER }

                    speakerFlow.collect {
                        Log.i(TAG, "speakerFlow.collect=[$it]")
                        waitUntilSpeakerEndpointJob.complete(it)
                    }
                }

                Log.i(TAG, "before await")
                waitUntilSpeakerEndpointJob.await()
                Log.i(TAG, "after await")

                // at this point, the CallEndpoint has been found
                val speakerEndpoint = waitUntilSpeakerEndpointJob.getCompleted()
                assertNotNull(speakerEndpoint)
                assertEquals(CallEndpointCompat.TYPE_SPEAKER, speakerEndpoint.type)

                // finally, terminate the call
                disconnect(DisconnectCause(DisconnectCause.LOCAL))
                // stop collecting flows so the test can end
                flowsJob.cancel()
                Log.i(TAG, " flowsJob.cancel()")
            }
        }
    }

    private fun setTestBuildVersion(sdk: Int) {
        val testAdapter =
            object : BuildVersionAdapter {
                override fun hasInvalidBuildVersion(): Boolean = sdk < VERSION_CODES.O

                override fun getCurrentSdk(): Int = sdk
            }
        Utils.setUtils(testAdapter)
    }
}
