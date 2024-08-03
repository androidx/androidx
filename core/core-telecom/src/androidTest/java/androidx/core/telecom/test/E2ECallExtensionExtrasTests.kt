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

import android.Manifest
import android.os.Build
import android.telecom.Call
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.CallExtensionScopeImpl
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class helps verify the E2E behavior for calls added via Jetpack to ensure that the call
 * details contain the appropriate extension extras that define the support for capability exchange
 * between the VOIP app and ICS.
 *
 * Note: Currently, this test only verifies the presence of [CallsManager.PROPERTY_IS_TRANSACTIONAL]
 * (only in V) in the call properties, if the phone account supports transactional ops (U+ devices),
 * or if the [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] key is present in the call
 * extras (pre-U devices). In the future, this will be expanded to be provide more robust testing to
 * verify binder functionality as well as supporting the case for auto
 * ([CallExtensionScopeImpl.EXTRA_VOIP_API_VERSION]).
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class E2ECallExtensionExtrasTests : BaseTelecomTest() {
    companion object {
        /** Logging for within the test class. */
        internal val TAG = E2ECallExtensionExtrasTests::class.simpleName
    }

    /**
     * Grant READ_PHONE_NUMBERS permission as part of testing
     * [InCallServiceCompat#resolveCallExtensionsType].
     */
    @get:Rule
    val readPhoneNumbersRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_PHONE_NUMBERS)!!

    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    /**
     * ********************************************************************************************
     * V2 APIs (Android U and above) tests
     * *******************************************************************************************
     */

    /**
     * For U+ devices using the v2 APIs, assert that the incoming call details either support the
     * [CallsManager.PROPERTY_IS_TRANSACTIONAL] property (V) or the phone account supports
     * transactional operations (U+).
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testCapabilityExchangeIncoming_V2() {
        setUpV2Test()
        addAndVerifyCallExtensionTypeE2E(TestUtils.INCOMING_CALL_ATTRIBUTES)
    }

    /**
     * For U+ devices using the v2 APIs, assert that the outgoing call details either support the
     * [CallsManager.PROPERTY_IS_TRANSACTIONAL] property (V) or the phone account supports
     * transactional operations (U+).
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testCapabilityExchangeOutgoing_V2() {
        setUpV2Test()
        addAndVerifyCallExtensionTypeE2E(TestUtils.OUTGOING_CALL_ATTRIBUTES)
    }

    /**
     * ********************************************************************************************
     * Backwards Compatibility Layer tests
     * *******************************************************************************************
     */

    /**
     * For pre-U devices using the backwards compatibility library, assert that the incoming call
     * details contain the [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] key
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCapabilityExchangeIncoming_BackwardsCompat() {
        setUpBackwardsCompatTest()
        addAndVerifyCallExtensionTypeE2E(TestUtils.INCOMING_CALL_ATTRIBUTES)
    }

    /**
     * For pre-U devices using the backwards compatibility library, assert that the outgoing call
     * details contain the [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] key
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCapabilityExchangeOutgoing_BackwardsCompat() {
        setUpBackwardsCompatTest()
        addAndVerifyCallExtensionTypeE2E(TestUtils.OUTGOING_CALL_ATTRIBUTES)
    }

    /**
     * ********************************************************************************************
     * Helpers
     * *******************************************************************************************
     */

    /**
     * Helper to add a call via CallsManager#addCall and block (if needed) until the connection
     * extras are propagated into the call details.
     *
     * @param callAttributesCompat for the call.
     */
    private fun addAndVerifyCallExtensionTypeE2E(callAttributesCompat: CallAttributesCompat) {
        runBlocking {
            usingIcs { ics ->
                assertWithinTimeout_addCall(callAttributesCompat) {
                    launch {
                        try {
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            Assert.assertNotNull("The returned Call object is <NULL>", call!!)
                            val extensions = CallExtensionScopeImpl(mContext, this, call)
                            // Assert the call extra or call property from the details
                            assertCallExtraOrProperty(extensions, call)
                        } finally {
                            // Always send disconnect signal if possible.
                            assertEquals(
                                CallControlResult.Success(),
                                disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            )
                        }
                    }
                }
            }
        }
    }

    /** Helper to assert the call extra or property set on the call coming from Telecom. */
    private suspend fun assertCallExtraOrProperty(extensions: CallExtensionScopeImpl, call: Call) {
        val type = extensions.resolveCallExtensionsType()
        assertEquals(CallExtensionScopeImpl.CAPABILITY_EXCHANGE, type)
        // Assert the specifics of the extensions are correct. Note, resolveCallExtensionsType also
        // internally assures the details are set properly
        val callDetails = call.details!!
        if (Utils.hasPlatformV2Apis()) {
            if (TestUtils.buildIsAtLeastV()) {
                assertTrue(callDetails.hasProperty(CallsManager.PROPERTY_IS_TRANSACTIONAL))
            }
        } else {
            val containsBackwardsCompatKey =
                callDetails.extras != null &&
                    callDetails.extras.containsKey(
                        CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED
                    )
            assertTrue(containsBackwardsCompatKey)
        }
    }
}
