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
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ScheduledExecutorService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedSceneRuntimeTest {

    private lateinit var activity: Activity
    private lateinit var serviceClient: ProjectedSceneCoreServiceClient
    private lateinit var executor: ScheduledExecutorService
    private lateinit var runtime: ProjectedSceneRuntime

    @Before
    fun setUp() {
        activity = mock()
        serviceClient = mock()
        executor = mock()
        // We can call the internal constructor directly since we are in the same package
        runtime = ProjectedSceneRuntime(activity, serviceClient, executor)
    }

    @Test
    fun create_returnsProjectedSceneRuntime() {
        val createdRuntime = ProjectedSceneRuntime.create(activity, serviceClient, executor)

        assertThat(createdRuntime).isInstanceOf(ProjectedSceneRuntime::class.java)
    }

    @Test
    fun destroy_unbindsService() {
        runtime.destroy()

        verify(serviceClient).unbindService()
    }
}
