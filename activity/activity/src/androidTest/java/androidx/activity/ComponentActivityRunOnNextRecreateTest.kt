/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.activity

import android.os.Bundle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityRunOnNextRecreateTest {

    private class Restarted2 : SavedStateRegistry.AutoRecreated {
        override fun onRecreated(owner: SavedStateRegistryOwner) {
            (owner as? AutoRestarterActivity)?.restartedValue = "restarted"
        }
    }

    @Test
    fun test() {
        with(ActivityScenario.launch(AutoRestarterActivity::class.java)) {
            withActivity {
                savedStateRegistry.runOnNextRecreation(Restarted2::class.java)
            }
            recreate()
            assertThat(withActivity { observerExecuted }).isTrue()
        }
    }
}

class AutoRestarterActivity : ComponentActivity() {
    var restartedValue: String? = null
    var observerExecuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            lifecycle.addObserver(LifecycleEventObserver { _, _ ->
                assertThat(restartedValue).isEqualTo("restarted")
                observerExecuted = true
            })
        }
    }
}