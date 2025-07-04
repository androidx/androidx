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

package androidx.privacysandbox.databridge.client.util

import androidx.privacysandbox.databridge.core.aidl.IRemoveValuesResultCallback
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RemoveValuesResultCallbackStub : IRemoveValuesResultCallback.Stub() {
    private val latch = CountDownLatch(1)
    private var mExceptionName: String? = null
    private var mExceptionMessage: String? = null

    override fun removeValuesResult(exceptionName: String?, exceptionMessage: String?) {
        mExceptionName = exceptionName
        mExceptionMessage = exceptionMessage
        latch.countDown()
    }

    fun getException(): List<String?> {
        latch.await(5, TimeUnit.SECONDS)
        return listOf(mExceptionName, mExceptionMessage)
    }
}
