/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.testing;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.jspecify.annotations.NonNull;

/**
 * A {@link LifecycleOwner} that is used to represent the lifecycle of the car app for testing.
 *
 */
@RestrictTo(Scope.LIBRARY)
public class TestLifecycleOwner implements LifecycleOwner {
    public final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    @Override
    public @NonNull Lifecycle getLifecycle() {
        return mRegistry;
    }

    /**
     * Provides the {@link LifecycleRegistry} to allow for setting specific lifecycle state for the
     * {@link androidx.car.app.CarAppService} for testing.
     *
     * <p>This is useful if you want to test {@link androidx.car.app.Screen}
     * lifecycle callbacks due to being pushed or popped from the screen stack via {@link
     * androidx.car.app.ScreenManager} APIs, but don't want to setup the {@link
     * androidx.car.app.CarAppService} for testing.
     */
    public @NonNull LifecycleRegistry getRegistry() {
        return mRegistry;
    }
}
