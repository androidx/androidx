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

/**
 * Marker interface that indicates a class is a [LifecycleObserver].
 *
 * This interface is the common type for all lifecycle observer implementations. It allows you to
 * add any observer type to a [Lifecycle] using [Lifecycle.addObserver].
 *
 * **Deprecation Note**: Do not implement this interface directly. The `lifecycle-compiler`
 * annotation processor and annotating methods with `@OnLifecycleEvent` are deprecated.
 *
 * Implement one of the following interfaces instead:
 * - [DefaultLifecycleObserver] (Recommended): Implement this interface to receive specific
 *   lifecycle callbacks (such as `onCreate`, `onStart`, and others) using overridden methods.
 * - [LifecycleEventObserver]: Implement this interface if you need to handle all lifecycle event
 *   state transitions in a single `onStateChanged` callback.
 *
 * @see Lifecycle for details and usage patterns.
 */
public interface LifecycleObserver
