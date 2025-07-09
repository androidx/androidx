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

import androidx.privacysandbox.databridge.core.aidl.IKeyUpdateInternalCallback
import androidx.privacysandbox.databridge.core.aidl.ValueInternal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class KeyUpdateInternalCallbackStub : IKeyUpdateInternalCallback.Stub() {
    private var latch = CountDownLatch(1)
    private var keyValueMap: MutableMap<String, Any?> = mutableMapOf()

    override fun onKeyUpdated(keyName: String, data: ValueInternal) {
        keyValueMap[keyName] = data.value
        latch.countDown()
    }

    fun getValue(keyName: String): Any? {
        val res = latch.await(5, TimeUnit.SECONDS)
        if (!res) {
            throw TimeoutException()
        }
        return keyValueMap[keyName]
    }

    fun initializeLatch() {
        latch = CountDownLatch(1)
    }
}
