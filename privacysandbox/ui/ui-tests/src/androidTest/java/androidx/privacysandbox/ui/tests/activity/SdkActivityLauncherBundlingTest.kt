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

package androidx.privacysandbox.ui.tests.activity

import android.os.Binder
import android.os.IBinder
import androidx.privacysandbox.ui.client.toLauncherInfo
import androidx.privacysandbox.ui.core.SdkActivityLauncher
import androidx.privacysandbox.ui.provider.SdkActivityLauncherFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkActivityLauncherBundlingTest {

    @Test
    fun unbundledSdkActivityLauncher_launchesActivities(): Unit = runBlocking {
        val launcher = TestSdkActivityLauncher()
        val launcherInfo = launcher.toLauncherInfo()

        val unbundledLauncher = SdkActivityLauncherFactory.fromLauncherInfo(launcherInfo)
        val token = Binder()
        val result = unbundledLauncher.launchSdkActivity(token)

        assertThat(result).isTrue()
        assertThat(launcher.tokensReceived).containsExactly(token)
    }

    @Test
    fun unbundledSdkActivityLauncher_rejectsActivityLaunches(): Unit = runBlocking {
        val launcher = TestSdkActivityLauncher()
        launcher.allowActivityLaunches = false
        val launcherInfo = launcher.toLauncherInfo()

        val unbundledLauncher = SdkActivityLauncherFactory.fromLauncherInfo(launcherInfo)
        val token = Binder()
        val result = unbundledLauncher.launchSdkActivity(token)

        assertThat(result).isFalse()
        assertThat(launcher.tokensReceived).containsExactly(token)
    }

    class TestSdkActivityLauncher : SdkActivityLauncher {
        var allowActivityLaunches: Boolean = true

        var tokensReceived = mutableListOf<IBinder>()

        override suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder):
            Boolean {
            tokensReceived.add(sdkActivityHandlerToken)
            return allowActivityLaunches
        }
    }
}