/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.savedstate;


import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.savedstate.SavedStateRegistry.AutoRecreated;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressLint("RestrictedApi")
final class Recreator implements GenericLifecycleObserver {

    static final String CLASSES_KEY = "classes_to_restore";
    static final String COMPONENT_KEY = "androidx.savedstate.Restarter";

    private final SavedStateRegistryOwner mOwner;

    Recreator(SavedStateRegistryOwner owner) {
        mOwner = owner;
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (event != Lifecycle.Event.ON_CREATE) {
            throw new AssertionError("Next event must be ON_CREATE");
        }
        source.getLifecycle().removeObserver(this);
        Bundle bundle = mOwner.getSavedStateRegistry()
                .consumeRestoredStateForKey(COMPONENT_KEY);
        if (bundle == null) {
            return;
        }
        ArrayList<String> classes = bundle.getStringArrayList(CLASSES_KEY);
        if (classes == null) {
            throw new IllegalStateException("Bundle with restored state for the component \""
                    + COMPONENT_KEY + "\" must contain list of strings by the key \""
                    + CLASSES_KEY + "\"");
        }
        for (String className : classes) {
            reflectiveNew(className);
        }
    }

    private void reflectiveNew(String className) {
        Class<? extends AutoRecreated> clazz;
        try {
            clazz = Class.forName(className, false,
                    Recreator.class.getClassLoader()).asSubclass(AutoRecreated.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + className + " wasn't found", e);
        }

        Constructor<? extends AutoRecreated> constructor;
        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Class" + clazz.getSimpleName() + " must have "
                    + "default constructor in order to be automatically recreated", e);
        }
        constructor.setAccessible(true);

        AutoRecreated newInstance;
        try {
            newInstance = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + className, e);
        }
        newInstance.onRecreated(mOwner);
    }

    static final class SavedStateProvider implements SavedStateRegistry.SavedStateProvider {
        @SuppressWarnings("WeakerAccess") // synthetic access
        final Set<String> mClasses = new HashSet<>();

        SavedStateProvider(final SavedStateRegistry registry) {
            registry.registerSavedStateProvider(COMPONENT_KEY, this);
        }

        @NonNull
        @Override
        public Bundle saveState() {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(CLASSES_KEY, new ArrayList<>(mClasses));
            return bundle;
        }

        void add(String className) {
            mClasses.add(className);
        }
    }
}
