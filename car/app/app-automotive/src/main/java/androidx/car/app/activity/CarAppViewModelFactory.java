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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory to provide a unique {@link CarAppViewModel} for each given {@link ComponentName}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
class CarAppViewModelFactory implements ViewModelProvider.Factory {
    private static final Map<ComponentName, CarAppViewModelFactory> sInstances = new HashMap<>();

    Application mApplication;
    ComponentName mComponentName;

    private CarAppViewModelFactory(@NonNull ComponentName componentName,
            @NonNull Application application) {
        mComponentName = componentName;
        mApplication = application;
    }

    /**
     * Retrieve a singleton instance of CarAppViewModelFactory for the given key.
     *
     * @return A valid {@link CarAppViewModelFactory}
     */
    @NonNull
    static CarAppViewModelFactory getInstance(Application application,
            ComponentName componentName) {
        CarAppViewModelFactory instance = sInstances.get(componentName);
        if (instance == null) {
            instance = new CarAppViewModelFactory(componentName, application);
            sInstances.put(componentName, instance);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new CarAppViewModel(mApplication, mComponentName);
    }
}
