/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import androidx.camera.camera2.pipe.core.TokenLockImpl
import androidx.camera.camera2.pipe.core.acquire
import androidx.camera.camera2.pipe.core.acquireOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class TokenLockInstrumentationTest {

    @Test
    fun tokenLockStressTest() = runBlocking {
        val tokenLock = TokenLockImpl(10)

        // This will cause each "launch" to run on a different thread.
        withContext(Dispatchers.Default) {
            val n = 100 // number of coroutines to launch
            val k = 1000 // times tokens are acquired by the coroutine
            repeat(n) {
                launch {
                    repeat(k) {
                        (1..5).forEach {
                            tokenLock.acquire(it.toLong()).close()
                            tokenLock.acquireOrNull(6 - it.toLong())?.close()
                        }
                    }
                }
            }
        }

        assertThat(tokenLock.available).isEqualTo(10)
    }
}