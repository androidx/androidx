/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
@OptIn(ExperimentalTestApi::class)
class RobolectricIdlingResourceTest {

    @Test
    fun testIdlingResourcesAreQueried() = runComposeUiTest {
        val idlingResource =
            object : IdlingResource {
                var readCount = MutableStateFlow(0)

                override var isIdleNow: Boolean = false
                    get() {
                        readCount.update { it + 1 }
                        return field
                    }

                suspend fun waitForTenReads() {
                    val start = readCount.value
                    readCount.collect {
                        if (it >= start + 10) {
                            isIdleNow = true
                        }
                    }
                }
            }

        registerIdlingResource(idlingResource)
        val executor = Executors.newSingleThreadExecutor()
        executor.execute { runBlocking { idlingResource.waitForTenReads() } }

        val startReadCount = idlingResource.readCount.value
        assertThat(idlingResource.isIdleNow).isFalse()

        waitForIdle()
        val endReadCount = idlingResource.readCount.value

        assertThat(idlingResource.isIdleNow).isTrue()
        assertThat(startReadCount).isEqualTo(0)
        assertThat(endReadCount - startReadCount).isAtLeast(10)

        unregisterIdlingResource(idlingResource)
        executor.shutdownNow()
    }
}
