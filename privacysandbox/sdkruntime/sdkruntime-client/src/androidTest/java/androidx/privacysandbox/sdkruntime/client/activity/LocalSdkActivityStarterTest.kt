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

package androidx.privacysandbox.sdkruntime.client.activity

import android.os.Binder
import androidx.privacysandbox.sdkruntime.client.EmptyActivity
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.test.core.app.ActivityScenario
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import org.junit.Test

class LocalSdkActivityStarterTest {

    @Test
    fun tryStart_whenHandlerRegistered_startSdkActivityAndReturnTrue() {
        val handler = TestHandler()
        val registeredToken = LocalSdkActivityHandlerRegistry.register(handler)

        val startResult = with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                LocalSdkActivityStarter.tryStart(this, registeredToken)
            }
        }

        assertThat(startResult).isTrue()

        val activityHolder = handler.waitForActivity()
        assertThat(activityHolder.getActivity()).isInstanceOf(SdkActivity::class.java)
    }

    @Test
    fun tryStart_whenHandlerNotRegistered_ReturnFalse() {
        val unregisteredToken = Binder()

        val startResult = with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                LocalSdkActivityStarter.tryStart(this, unregisteredToken)
            }
        }

        assertThat(startResult).isFalse()
    }

    private class TestHandler : SdkSandboxActivityHandlerCompat {
        var result: ActivityHolder? = null
        var async = CountDownLatch(1)

        override fun onActivityCreated(activityHolder: ActivityHolder) {
            result = activityHolder
            async.countDown()
        }

        fun waitForActivity(): ActivityHolder {
            async.await()
            return result!!
        }
    }
}
