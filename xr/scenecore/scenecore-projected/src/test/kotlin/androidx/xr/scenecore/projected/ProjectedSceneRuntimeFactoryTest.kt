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

package androidx.xr.scenecore.projected

import android.app.Activity
import androidx.xr.runtime.interfaces.Feature
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedSceneRuntimeFactoryTest {
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).get()
    }

    @Test
    fun getRequirements_returnsProjectedFeature() {
        val factory = ProjectedSceneRuntimeFactory()

        val requirements = factory.requirements

        assertThat(requirements).containsExactly(Feature.PROJECTED, Feature.FULLSTACK)
    }

    @Test
    fun createRuntime_returnsProjectedRuntimeAndBindsConnection() = runTest {
        val mockClient = mock<ProjectedSceneCoreServiceClient>()
        whenever(mockClient.bindService(any())).thenReturn(mock())
        val factory = ProjectedSceneRuntimeFactory(serviceClientProvider = { mockClient })

        val runtime = factory.create(activity)

        assertThat(runtime).isInstanceOf(ProjectedSceneRuntime::class.java)
        verify(mockClient).bindService(activity)
    }

    @Test
    fun create_propagatesException_whenBindServiceFails() = runTest {
        val mockClient = mock<ProjectedSceneCoreServiceClient>()

        // Set up the mock to throw an exception when bindService is called
        whenever(mockClient.bindService(any())).thenThrow(IllegalStateException())

        val factory = ProjectedSceneRuntimeFactory(serviceClientProvider = { mockClient })

        // Verify that the exception thrown by the client bubbles up out of the factory
        assertThrows(IllegalStateException::class.java) { factory.create(activity) }
    }

    @Test
    fun create_throwsException_whenCalledFromMainThread() {
        val factory = ProjectedSceneRuntimeFactory()
        val exception = assertThrows(IllegalStateException::class.java) { factory.create(activity) }
        assertThat(exception).hasMessageThat().contains("cannot be bound from the main thread")
    }
}
