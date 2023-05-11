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
import android.os.Build
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi
import androidx.core.telecom.internal.JetpackConnectionService
import androidx.core.telecom.internal.utils.Utils
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.testutils.TestExecutor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Before

@RequiresApi(Build.VERSION_CODES.O)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O /* api=26 */)
abstract class BaseTelecomTest {
    val mContext: Context = ApplicationProvider.getApplicationContext()
    val mWorkerExecutor = TestExecutor()
    val mWorkerContext: CoroutineContext = mWorkerExecutor.asCoroutineDispatcher()

    lateinit var mCallsManager: CallsManager
    lateinit var mPackagePhoneAccountHandle: PhoneAccountHandle
    internal lateinit var mConnectionService: JetpackConnectionService

    @Before
    fun setUpBase() {
        Utils.resetUtils()
        mCallsManager = CallsManager(mContext)
        mConnectionService = mCallsManager.mConnectionService
        mCallsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
        mPackagePhoneAccountHandle = mCallsManager.getPhoneAccountHandleForPackage()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @After
    fun onDestroyBase() {
        Utils.resetUtils()
        JetpackConnectionService.mPendingConnectionRequests.clear()
    }
}