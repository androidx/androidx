/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.controller.impl

import androidx.privacysandbox.sdkruntime.client.EmptyActivity
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalClientImportanceListenerRegistryTest {

    @Test
    fun onForegroundImportanceChangedTest() {
        val events = mutableListOf<Boolean>()
        val async = CountDownLatch(1)

        val listener =
            object : SdkSandboxClientImportanceListenerCompat {
                override fun onForegroundImportanceChanged(isForeground: Boolean) {
                    events.add(isForeground)
                    async.countDown()
                }
            }

        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                LocalClientImportanceListenerRegistry.register(
                    sdkPackageName = "LocalClientImportanceListenerRegistryTest.sdk",
                    executor = Runnable::run,
                    listener = listener,
                )
                assertThat(events).isEmpty()

                // Move to background
                assertThat(moveTaskToBack(true)).isTrue()
            }
        }

        async.await()
        assertThat(events).containsExactly(false)

        // Move to foreground
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                assertThat(events).containsExactly(false, true).inOrder()
                LocalClientImportanceListenerRegistry.unregister(listener)
            }
        }
    }

    @Test
    fun unregisterAllListenersForSdk_unregisterOnlyRequestedSdkListeners() {
        val sdkPackage = "LocalClientImportanceListenerRegistryTest.sdkForUnload1"
        val anotherSdkPackage = "LocalClientImportanceListenerRegistryTest.sdkForUnload2"

        val listener =
            object : SdkSandboxClientImportanceListenerCompat {
                override fun onForegroundImportanceChanged(isForeground: Boolean) {}
            }

        LocalClientImportanceListenerRegistry.register(
            sdkPackageName = sdkPackage,
            executor = Runnable::run,
            listener = listener,
        )
        LocalClientImportanceListenerRegistry.register(
            sdkPackageName = anotherSdkPackage,
            executor = Runnable::run,
            listener = listener,
        )
        assertThat(LocalClientImportanceListenerRegistry.hasListenersForSdk(sdkPackage)).isTrue()
        assertThat(LocalClientImportanceListenerRegistry.hasListenersForSdk(anotherSdkPackage))
            .isTrue()

        LocalClientImportanceListenerRegistry.unregisterAllListenersForSdk(sdkPackage)
        assertThat(LocalClientImportanceListenerRegistry.hasListenersForSdk(sdkPackage)).isFalse()
        assertThat(LocalClientImportanceListenerRegistry.hasListenersForSdk(anotherSdkPackage))
            .isTrue()

        LocalClientImportanceListenerRegistry.unregisterAllListenersForSdk(anotherSdkPackage)
        assertThat(LocalClientImportanceListenerRegistry.hasListenersForSdk(sdkPackage)).isFalse()
        assertThat(LocalClientImportanceListenerRegistry.hasListenersForSdk(anotherSdkPackage))
            .isFalse()
    }
}
