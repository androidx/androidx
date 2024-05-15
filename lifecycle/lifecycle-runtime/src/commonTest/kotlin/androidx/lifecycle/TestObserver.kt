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

package androidx.lifecycle

internal open class TestObserver(
    private val onEvent: (LifecycleObserver, Lifecycle.Event) -> Unit = { _, _ -> }
) : DefaultLifecycleObserver, LifecycleEventObserver {
    var onCreateCallCount = 0
    override fun onCreate(owner: LifecycleOwner) {
        onCreateCallCount++
        onEvent(this, Lifecycle.Event.ON_CREATE)
    }

    var onStartCallCount = 0
    override fun onStart(owner: LifecycleOwner) {
        onStartCallCount++
        onEvent(this, Lifecycle.Event.ON_START)
    }

    var onResumeCallCount = 0
    override fun onResume(owner: LifecycleOwner) {
        onResumeCallCount++
        onEvent(this, Lifecycle.Event.ON_RESUME)
    }

    var onPauseCallCount = 0
    override fun onPause(owner: LifecycleOwner) {
        onPauseCallCount++
        onEvent(this, Lifecycle.Event.ON_PAUSE)
    }

    var onStopCallCount = 0
    override fun onStop(owner: LifecycleOwner) {
        onStopCallCount++
        onEvent(this, Lifecycle.Event.ON_STOP)
    }

    var onDestroyCallCount = 0
    override fun onDestroy(owner: LifecycleOwner) {
        onDestroyCallCount++
        onEvent(this, Lifecycle.Event.ON_DESTROY)
    }

    val onStateChangedEvents = mutableListOf<Lifecycle.Event>()
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        onStateChangedEvents.add(event)
    }
}
