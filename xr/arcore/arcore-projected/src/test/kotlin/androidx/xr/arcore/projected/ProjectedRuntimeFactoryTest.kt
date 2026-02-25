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

package androidx.xr.arcore.projected

import android.app.Activity
import androidx.xr.runtime.interfaces.Feature
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedRuntimeFactoryTest {
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).get()
    }

    @Test
    fun getRequirements_returnsProjectedFeature() {
        val factory = ProjectedRuntimeFactory()

        val requirements = factory.requirements

        assertThat(requirements).containsExactly(Feature.PROJECTED, Feature.FULLSTACK)
    }

    @Test
    fun createRuntime_returnsProjectedRuntime() = runTest {
        val factory = ProjectedRuntimeFactory()

        val runtime = factory.createRuntime(activity, EmptyCoroutineContext)

        assertThat(runtime).isInstanceOf(ProjectedRuntime::class.java)
    }
}
