/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

internal enum class LifecycleSource {
    ACTIVITY,
    ACTIVITY_CALLBACK
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityLifecycleTest {

    @Test
    @Throws(Throwable::class)
    fun testLifecycleObserver() {
        lateinit var events: List<Pair<LifecycleSource, Lifecycle.Event>>
        ActivityScenario.launch(LifecycleComponentActivity::class.java).use { scenario ->
            events = scenario.withActivity { this.events }
        }

        // The Activity's lifecycle callbacks should fire first,
        // followed by the activity's lifecycle observers
        assertThat(events)
            .containsExactly(
                LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_CREATE,
                LifecycleSource.ACTIVITY to Lifecycle.Event.ON_CREATE,
                LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_START,
                LifecycleSource.ACTIVITY to Lifecycle.Event.ON_START,
                LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_RESUME,
                LifecycleSource.ACTIVITY to Lifecycle.Event.ON_RESUME,
                // Now the order reverses as things unwind
                LifecycleSource.ACTIVITY to Lifecycle.Event.ON_PAUSE,
                LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_PAUSE,
                LifecycleSource.ACTIVITY to Lifecycle.Event.ON_STOP,
                LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_STOP,
                LifecycleSource.ACTIVITY to Lifecycle.Event.ON_DESTROY,
                LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_DESTROY
            ).inOrder()
    }
}

class LifecycleComponentActivity : ComponentActivity() {
    internal val events = mutableListOf<Pair<LifecycleSource, Lifecycle.Event>>()

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            events.add(LifecycleSource.ACTIVITY to event)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        events.add(LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_CREATE)
    }

    override fun onStart() {
        super.onStart()
        events.add(LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        events.add(LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        events.add(LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        events.add(LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        events.add(LifecycleSource.ACTIVITY_CALLBACK to Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
