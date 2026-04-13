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

package androidx.xr.scenecore.projected.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class FakeProjectedSceneCoreServiceClientTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val client = FakeProjectedSceneCoreServiceClient()

    @Test
    fun bindService_setsIsBoundAndReturnsFakeService() = runBlocking {
        val service = client.bindService(context)
        assertThat(client.isBound).isTrue()
        assertThat(service).isEqualTo(client.fakeService)
        assertThat(client.service).isEqualTo(client.fakeService)
    }

    @Test
    fun unbindService_setsIsBoundFalseAndClearsService() = runBlocking {
        client.bindService(context)
        client.unbindService()
        assertThat(client.isBound).isFalse()
        assertThat(client.service).isNull()
    }

    @Test
    fun bindService_createsNewServiceIfNull() = runBlocking {
        client.bindService(context)
        client.unbindService() // service becomes null
        val service = client.bindService(context) // service is created again
        assertThat(client.isBound).isTrue()
        assertThat(service).isNotNull()
        assertThat(client.service).isEqualTo(service)
    }
}
