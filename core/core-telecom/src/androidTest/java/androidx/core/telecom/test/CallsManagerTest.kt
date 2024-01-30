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
import android.telecom.PhoneAccount.CAPABILITY_SELF_MANAGED
import android.telecom.PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@RequiresApi(VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CallsManagerTest : BaseTelecomTest() {
    private val mTestClassName = "androidx.core.telecom.test"

    @SmallTest
    @Test
    fun testGetPhoneAccountWithUBuild() {
        try {
            Utils.setUtils(TestUtils.mV2Build)
            val account = mCallsManager.getPhoneAccountHandleForPackage()
            assertEquals(mTestClassName, account.componentName.className)
        } finally {
            Utils.resetUtils()
        }
    }

    @SmallTest
    @Test
    fun testGetPhoneAccountWithUBuildWithTminusBuild() {
        try {
            Utils.setUtils(TestUtils.mBackwardsCompatBuild)
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
            Utils.setUtils(TestUtils.mInvalidBuild)
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
