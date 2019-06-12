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

package androidx.lifecycle;

/**
 * Marks a class as a LifecycleObserver. It does not have any methods, instead, use either:
 * <ul>
 * <li>{@link DefaultLifecycleObserver} if you are interested only in certain events,
 * for example only in {@code ON_CREATE} and {@code ON_DESTROY};
 * <li>{@link LifecycleEventObserver} if you need to process all events in a stream-like manner.
 * </ul>
 * <p>
 *
 * @see Lifecycle Lifecycle - for samples and usage patterns.
 */
@SuppressWarnings("WeakerAccess")
public interface LifecycleObserver {

}
