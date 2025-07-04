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
import androidx.xr.runtime.internal.RuntimeFactory
import com.google.common.truth.Truth.assertThat
import java.util.ServiceLoader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArCoreRuntimeFactoryTest {

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    @Test
    fun class_isDiscoverableViaServiceLoader() {
        // FakeRuntimeFactory is also included, so look for the correct factory.
        var pass = false
        for (runtime in ServiceLoader.load(RuntimeFactory::class.java)) {
            if (ArCoreRuntimeFactory::class.java.isInstance(runtime)) {
                pass = true
            }
        }
        assertThat(pass).isEqualTo(true)
    }

    @Test
    fun createRuntime_createsArCoreRuntime() {
        val factory = ArCoreRuntimeFactory()

        activityRule.scenario.onActivity {
            val runtime = factory.createRuntime(it)

            assertThat(runtime).isInstanceOf(ArCoreRuntime::class.java)
        }
    }
}
