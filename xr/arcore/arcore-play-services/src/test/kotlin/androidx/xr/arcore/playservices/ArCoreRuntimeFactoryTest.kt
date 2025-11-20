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

package androidx.xr.arcore.playservices

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import com.google.common.truth.Truth.assertThat
import java.util.ServiceLoader
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArCoreRuntimeFactoryTest {

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    @Test
    @Ignore("TODO(b/436933956) - temporary disabled until dependencies are fixed")
    fun class_isDiscoverableViaServiceLoader() {
        // FakeRuntimeFactory is also included, so look for the correct factory.
        var pass = false
        for (runtimeFactory in ServiceLoader.load(PerceptionRuntimeFactory::class.java)) {
            if (ArCoreRuntimeFactory::class.java.isInstance(runtimeFactory)) {
                pass = true
            }
        }
        assertThat(pass).isEqualTo(true)
    }

    @Test
    fun createRuntime_createsArCoreRuntime() {
        val factory = ArCoreRuntimeFactory()

        activityRule.scenario.onActivity {
            val runtime = factory.createRuntime(it, EmptyCoroutineContext)

            assertThat(runtime).isInstanceOf(ArCoreRuntime::class.java)
        }
    }
}
