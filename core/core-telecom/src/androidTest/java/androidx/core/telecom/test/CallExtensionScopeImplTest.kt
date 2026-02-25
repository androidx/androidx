/*
 * Copyright 2025 The Android Open Source Project
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
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telecom.Call
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import androidx.core.telecom.CallsManager.Companion.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED
import androidx.core.telecom.extensions.CallExtensionScopeImpl
import androidx.core.telecom.extensions.CallExtensionScopeImpl.Companion.CAPABILITY_EXCHANGE
import androidx.core.telecom.extensions.CallExtensionScopeImpl.Companion.EXTRAS
import androidx.core.telecom.extensions.CallExtensionScopeImpl.Companion.NONE
import androidx.core.telecom.extensions.CallProxy
import androidx.core.telecom.extensions.ExtensionCallDetails
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_VOIP_API_VERSION
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalCoroutinesApi::class)
class CallExtensionScopeImplTest {

    private lateinit var mContext: Context
    private lateinit var mScope: CoroutineScope
    private lateinit var mCallExtensionScope: CallExtensionScopeImpl
    private lateinit var mFakeCallProxy: FakeCallProxy

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mScope = CoroutineScope(Dispatchers.Unconfined)
        mFakeCallProxy = FakeCallProxy()
        mCallExtensionScope = CallExtensionScopeImpl(mContext, mScope, mFakeCallProxy)
    }

    @After
    fun tearDown() {
        mScope.cancel()
    }

    @Test
    fun testResolveCallExtensionsType_DetailsNull_ReturnsNone() = runBlocking {
        // Set details to null in the proxy
        mFakeCallProxy.detailsToReturn = null

        // The method should wait for details (with timeout) and safely return NONE if still null.
        val type = mCallExtensionScope.resolveCallExtensionsType()
        assertEquals(NONE, type)
    }

    @Test
    fun testResolveCallExtensionsType_Managed_ReturnsNone() = runBlocking {
        mFakeCallProxy.detailsToReturn =
            ExtensionCallDetails(
                hasTransactionalProperty = false,
                isSelfManagedProperty = false,
                accountHandle = null,
                extras = Bundle(),
            )
        assertNotNull(mFakeCallProxy.getExtensionDetails())
        assertFalse(mFakeCallProxy.getExtensionDetails()!!.isSelfManagedProperty)
        val type = mCallExtensionScope.resolveCallExtensionsType()
        assertEquals(NONE, type)
    }

    @Test
    fun testResolveCallExtensionsType_waitsForDetails_EXTRAS() = runTest {
        runResolveCallExtensionTest(extraKey = EXTRA_VOIP_API_VERSION, expectedResult = EXTRAS)
    }

    @Test
    fun testResolveCallExtensionsType_waitsForDetails_BACKWARDS_COMPAT() = runTest {
        runResolveCallExtensionTest(
            extraKey = EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED,
            expectedResult = CAPABILITY_EXCHANGE,
        )
    }

    @Test
    fun testGetPhoneAccountIfAllowed_NullHandle_ReturnsNull() = runBlocking {
        val result = mCallExtensionScope.getPhoneAccountIfAllowed(null)
        assertNull(result)
    }

    @Test
    fun testGetPhoneAccountIfAllowed_SecurityException_ReturnsNull() = runBlocking {
        val handle = PhoneAccountHandle(ComponentName(mContext, "Test"), "id")
        mFakeCallProxy.throwSecurityException = true

        val result = mCallExtensionScope.getPhoneAccountIfAllowed(handle)
        assertNull(result)
    }

    @Test
    fun testGetPhoneAccountIfAllowed_Success_ReturnsAccount() = runBlocking {
        val handle = PhoneAccountHandle(ComponentName(mContext, "Test"), "id")
        val expectedAccount = PhoneAccount.builder(handle, "Test").build()
        mFakeCallProxy.phoneAccountToReturn = expectedAccount

        val result = mCallExtensionScope.getPhoneAccountIfAllowed(handle)
        assertEquals(expectedAccount, result)
    }

    /**
     * Shared logic for testing different extension types. Replaces manual delays with structured
     * coroutine control.
     */
    private suspend fun TestScope.runResolveCallExtensionTest(
        extraKey: String,
        expectedResult: Int,
    ) {
        // 1. Setup
        val fakeCallProxy = FakeCallProxy()
        val scope = CallExtensionScopeImpl(mContext, this, fakeCallProxy)

        // 2. Start resolution (it will suspend immediately)
        val deferredType = async { scope.resolveCallExtensionsType() }
        yield() // Let the coroutine start

        assertFalse("Should be suspended waiting for details", deferredType.isCompleted)

        // 3. Emit intermediate (invalid) details
        fakeCallProxy.detailsFlow.emit(createDetails(Bundle()))
        yield()
        assertFalse("Should still be suspended; extras are empty", deferredType.isCompleted)

        // 4. Emit valid details
        fakeCallProxy.stateToReturn = Call.STATE_ACTIVE
        val validExtras = Bundle().apply { putInt(extraKey, 1) }
        fakeCallProxy.detailsFlow.emit(createDetails(validExtras))

        // 5. Assert result
        assertEquals(expectedResult, deferredType.await())
    }

    // Helper to keep the test body clean
    private fun createDetails(extras: Bundle) = ExtensionCallDetails(false, true, null, extras)

    internal class FakeCallProxy : CallProxy {
        var detailsToReturn: ExtensionCallDetails? = null
        val detailsFlow = MutableSharedFlow<ExtensionCallDetails>(replay = 1)
        var stateToReturn: Int = Call.STATE_NEW
        var phoneAccountToReturn: PhoneAccount? = null
        var throwSecurityException = false
        var callback: Call.Callback? = null

        override var call: Call? = null

        override fun getExtensionDetails(): ExtensionCallDetails? {
            return detailsToReturn
        }

        override fun getExtensionDetailsFlow(): Flow<ExtensionCallDetails> {
            return detailsFlow
        }

        override fun getState(): Int {
            return stateToReturn
        }

        override fun registerCallback(callback: Call.Callback, handler: Handler) {
            this.callback = callback
        }

        override fun unregisterCallback(callback: Call.Callback) {
            if (this.callback == callback) {
                this.callback = null
            }
        }

        override fun sendCallEvent(event: String, extras: Bundle) {
            // No-op
        }

        override fun getPhoneAccount(accountHandle: PhoneAccountHandle): PhoneAccount? {
            if (throwSecurityException) {
                throw SecurityException("Fake security exception")
            }
            return phoneAccountToReturn
        }
    }
}
