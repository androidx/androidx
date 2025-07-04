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

package androidx.privacysandbox.databridge.integration.testutils

import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.KeyUpdateCallback
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.collections.forEach

class KeyUpdateCallbackImpl : KeyUpdateCallback {
    private var keyUpdatedCounterMap = mutableMapOf<Key, Int>()
    private var keyToValueMap = mutableMapOf<Key, Any?>()
    // The latch will be used to ensure that the counter value and the value has been updated.
    // Wait for the latch in [getCounterForKey] or [getValueForKey] to ensure that the
    // [onKeyUpdated] function has been called
    private val latchMap = mutableMapOf<Key, CountDownLatch>()

    override fun onKeyUpdated(key: Key, value: Any?) {
        val counter = keyUpdatedCounterMap[key]
        keyUpdatedCounterMap[key] = if (counter == null) 1 else counter + 1

        keyToValueMap[key] = value
        latchMap[key]?.countDown()
    }

    fun initializeLatch(keys: List<Key>) {
        keys.forEach { key -> latchMap[key] = CountDownLatch(1) }
    }

    fun getCounterForKey(key: Key): Int {
        val res = latchMap[key]?.await(5, TimeUnit.SECONDS)
        res?.let {
            if (!it) {
                throw TimeoutException()
            }
        }
        return keyUpdatedCounterMap[key] ?: 0
    }

    fun getValueForKey(key: Key): Any? {
        val res = latchMap[key]?.await(5, TimeUnit.SECONDS)
        res?.let {
            if (!it) {
                throw TimeoutException()
            }
        }
        return keyToValueMap[key]
    }
}
