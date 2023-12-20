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

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.view.Window
import androidx.privacysandbox.sdkruntime.client.EmptyActivity
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import org.junit.Test

class LocalSdkActivityStarterTest {

    @Test
    fun sdkActivity_doesntRegisterAsLauncherActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val launcherActivities = packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.name }
        assertThat(launcherActivities)
            .doesNotContain(SdkActivity::class.qualifiedName)
    }

    @Test
    fun tryStart_whenHandlerRegistered_startSdkActivityAndReturnTrue() {
        val handler = TestHandler()
        val registeredToken = LocalSdkActivityHandlerRegistry.register(
            "LocalSdkActivityStarterTest.sdk",
            handler
        )

        val startResult = with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                LocalSdkActivityStarter.tryStart(this, registeredToken)
            }
        }

        assertThat(startResult).isTrue()

        val activityHolder = handler.waitForActivity()
        assertThat(activityHolder.getActivity()).isInstanceOf(SdkActivity::class.java)

        val sdkActivity = activityHolder.getActivity() as SdkActivity
        assertThat(sdkActivity.window.hasFeature(Window.FEATURE_NO_TITLE)).isTrue()

        assertThat(activityHolder.lifecycle).isSameInstanceAs(sdkActivity.lifecycle)
        assertThat(activityHolder.getOnBackPressedDispatcher())
            .isSameInstanceAs(sdkActivity.onBackPressedDispatcher)
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
