/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

/**
 * Specialization of {@link LifecycleProvider} that explicitly returns {@link LifecycleRegistry}
 * This method may be used if an object which updates state of {@link Lifecycle} doesn't own it.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface LifecycleRegistryProvider extends LifecycleProvider {
    @Override
    LifecycleRegistry getLifecycle();
}
