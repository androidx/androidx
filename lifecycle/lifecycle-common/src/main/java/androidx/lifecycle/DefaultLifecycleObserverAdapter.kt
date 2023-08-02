/*
 * Copyright (C) 2017 The Android Open Source Project
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

internal class DefaultLifecycleObserverAdapter(
    private val defaultLifecycleObserver: DefaultLifecycleObserver,
    private val lifecycleEventObserver: LifecycleEventObserver?
) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> defaultLifecycleObserver.onCreate(source)
            Lifecycle.Event.ON_START -> defaultLifecycleObserver.onStart(source)
            Lifecycle.Event.ON_RESUME -> defaultLifecycleObserver.onResume(source)
            Lifecycle.Event.ON_PAUSE -> defaultLifecycleObserver.onPause(source)
            Lifecycle.Event.ON_STOP -> defaultLifecycleObserver.onStop(source)
            Lifecycle.Event.ON_DESTROY -> defaultLifecycleObserver.onDestroy(source)
            Lifecycle.Event.ON_ANY ->
                throw IllegalArgumentException("ON_ANY must not been send by anybody")
        }
        lifecycleEventObserver?.onStateChanged(source, event)
    }
}
