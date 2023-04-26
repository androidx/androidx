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

package androidx.core.telecom

import android.content.Context
import android.os.Build.VERSION_CODES
import android.telecom.PhoneAccount.CAPABILITY_SELF_MANAGED
import android.telecom.PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.internal.utils.BuildVersionAdapter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
class CallsManagerTest {
    private val mTestClassName = "androidx.core.telecom.test"
    private val mContext: Context = ApplicationProvider.getApplicationContext()
    private val mCallsManager = CallsManager(mContext)

    private val mV2Build = object : BuildVersionAdapter {
        override fun hasPlatformV2Apis(): Boolean {
            return true
        }

        override fun hasInvalidBuildVersion(): Boolean {
            return false
        }
    }

    private val mBackwardsCompatBuild = object : BuildVersionAdapter {
        override fun hasPlatformV2Apis(): Boolean {
            return false
        }

        override fun hasInvalidBuildVersion(): Boolean {
            return false
        }
    }

    private val mInvalidBuild = object : BuildVersionAdapter {
        override fun hasPlatformV2Apis(): Boolean {
            return false
        }

        override fun hasInvalidBuildVersion(): Boolean {
            return true
        }
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithUBuild() {
        Utils.setUtils(mV2Build)
        val account = mCallsManager.getPhoneAccountHandleForPackage()
        assertEquals(mTestClassName, account.componentName.className)
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithUBuildWithTminusBuild() {
        Utils.setUtils(mBackwardsCompatBuild)
        val account = mCallsManager.getPhoneAccountHandleForPackage()
        assertEquals(CallsManager.CONNECTION_SERVICE_CLASS, account.componentName.className)
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithInvalidBuild() {
        Utils.setUtils(mInvalidBuild)
        assertThrows(UnsupportedOperationException::class.java) {
            mCallsManager.getPhoneAccountHandleForPackage()
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

            if (Utils.hasPlatformV2Apis()) {
                assertTrue(
                    Utils.hasCapability(
                        CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS,
                        account.capabilities
                    )
                )
            } else {
                assertTrue(
                    account.capabilities and CAPABILITY_SELF_MANAGED ==
                        CAPABILITY_SELF_MANAGED
                )
            }
        }
    }
}