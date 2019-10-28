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
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityLifecycleTest {

    @Test
    @Throws(Throwable::class)
    fun testLifecycleObserver() {
        lateinit var activity: LifecycleComponentActivity
        lateinit var activityCallbackLifecycleOwner: LifecycleOwner
        lateinit var lifecycleObserver: LifecycleEventObserver
        ActivityScenario.launch(LifecycleComponentActivity::class.java).use { scenario ->
            activity = scenario.withActivity { this }
            activityCallbackLifecycleOwner = activity.activityCallbackLifecycleOwner
            lifecycleObserver = activity.lifecycleObserver
        }

        // The Activity's lifecycle callbacks should fire first,
        // followed by the activity's lifecycle observers
        verify(lifecycleObserver)
                .onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_CREATE)
        verify(lifecycleObserver)
                .onStateChanged(activity, Lifecycle.Event.ON_CREATE)
        verify(lifecycleObserver)
                .onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_START)
        verify(lifecycleObserver)
                .onStateChanged(activity, Lifecycle.Event.ON_START)
        verify(lifecycleObserver)
                .onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_RESUME)
        verify(lifecycleObserver)
                .onStateChanged(activity, Lifecycle.Event.ON_RESUME)
        // Now the order reverses as things unwind
        verify(lifecycleObserver)
                .onStateChanged(activity, Lifecycle.Event.ON_PAUSE)
        verify(lifecycleObserver)
                .onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        verify(lifecycleObserver)
                .onStateChanged(activity, Lifecycle.Event.ON_STOP)
        verify(lifecycleObserver)
                .onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_STOP)
        verify(lifecycleObserver)
                .onStateChanged(activity, Lifecycle.Event.ON_DESTROY)
        verify(lifecycleObserver)
                .onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        verifyNoMoreInteractions(lifecycleObserver)
    }
}

class LifecycleComponentActivity : ComponentActivity() {
    val activityCallbackLifecycleOwner: LifecycleOwner = mock(LifecycleOwner::class.java)
    val lifecycleObserver: LifecycleEventObserver = mock(LifecycleEventObserver::class.java)

    init {
        lifecycle.addObserver(lifecycleObserver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_CREATE)
    }

    override fun onStart() {
        super.onStart()
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleObserver.onStateChanged(activityCallbackLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
