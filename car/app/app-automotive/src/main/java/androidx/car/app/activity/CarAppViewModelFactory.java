/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.Application;
import android.content.ComponentName;

import androidx.annotation.RestrictTo;
import androidx.car.app.SessionInfo;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory to provide a unique {@link CarAppViewModel} for each pair of {@link ComponentName} and
 * {@link SessionInfo}.
 *
 */
@RestrictTo(LIBRARY)
class CarAppViewModelFactory implements ViewModelProvider.Factory {
    private static final Map<Pair<ComponentName, SessionInfo>, CarAppViewModelFactory> sInstances =
            new HashMap<>();

    Application mApplication;
    ComponentName mComponentName;
    SessionInfo mSessionInfo;

    private CarAppViewModelFactory(@NonNull ComponentName componentName,
            @NonNull Application application, @NonNull SessionInfo sessionInfo) {
        mComponentName = componentName;
        mApplication = application;
        mSessionInfo = sessionInfo;
    }

    /**
     * Retrieve a singleton instance of CarAppViewModelFactory for the given
     * {@link ComponentName} and {@link SessionInfo}.
     *
     * @return A valid {@link CarAppViewModelFactory}
     */
    static @NonNull CarAppViewModelFactory getInstance(Application application,
            ComponentName componentName, SessionInfo sessionInfo) {
        Pair<ComponentName, SessionInfo> instanceCacheKey = new Pair<>(componentName, sessionInfo);
        CarAppViewModelFactory instance = sInstances.get(instanceCacheKey);
        if (instance == null) {
            instance = new CarAppViewModelFactory(componentName, application, sessionInfo);
            sInstances.put(instanceCacheKey, instance);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ViewModel> @NonNull T create(@NonNull Class<T> modelClass) {
        return (T) new CarAppViewModel(mApplication, mComponentName, mSessionInfo);
    }
}
