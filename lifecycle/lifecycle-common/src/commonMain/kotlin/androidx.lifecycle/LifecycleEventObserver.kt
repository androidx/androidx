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
package androidx.lifecycle

/**
 * Class that can receive any lifecycle change and dispatch it to the receiver.
 *
 * If a class implements both this interface and
 * [androidx.lifecycle.DefaultLifecycleObserver], then
 * methods of `DefaultLifecycleObserver` will be called first, and then followed by the call
 * of [LifecycleEventObserver.onStateChanged]
 *
 * If a class implements this interface and in the same time uses [OnLifecycleEvent], then
 * annotations will be ignored.
 */
public fun interface LifecycleEventObserver : LifecycleObserver {
    /**
     * Called when a state transition event happens.
     *
     * @param source The source of the event
     * @param event The event
     */
    public fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event)
}
