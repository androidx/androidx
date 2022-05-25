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

package androidx.lifecycle;

import static androidx.lifecycle.Lifecycle.State.INITIALIZED;
import static androidx.lifecycle.Lifecycle.State.STARTED;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

class LegacySavedStateHandleController {

    private LegacySavedStateHandleController() {}

    static final String TAG_SAVED_STATE_HANDLE_CONTROLLER = "androidx.lifecycle.savedstate.vm.tag";

    static SavedStateHandleController create(SavedStateRegistry registry, Lifecycle lifecycle,
            String key, Bundle defaultArgs) {
        Bundle restoredState = registry.consumeRestoredStateForKey(key);
        SavedStateHandle handle = SavedStateHandle.createHandle(restoredState, defaultArgs);
        SavedStateHandleController controller = new SavedStateHandleController(key, handle);
        controller.attachToLifecycle(registry, lifecycle);
        tryToAddRecreator(registry, lifecycle);
        return controller;
    }

    static final class OnRecreation implements SavedStateRegistry.AutoRecreated {

        @Override
        public void onRecreated(@NonNull SavedStateRegistryOwner owner) {
            if (!(owner instanceof ViewModelStoreOwner)) {
                throw new IllegalStateException(
                        "Internal error: OnRecreation should be registered only on components"
                                + " that implement ViewModelStoreOwner");
            }
            ViewModelStore viewModelStore = ((ViewModelStoreOwner) owner).getViewModelStore();
            SavedStateRegistry savedStateRegistry = owner.getSavedStateRegistry();
            for (String key : viewModelStore.keys()) {
                ViewModel viewModel = viewModelStore.get(key);
                attachHandleIfNeeded(viewModel, savedStateRegistry, owner.getLifecycle());
            }
            if (!viewModelStore.keys().isEmpty()) {
                savedStateRegistry.runOnNextRecreation(OnRecreation.class);
            }
        }
    }

    static void attachHandleIfNeeded(ViewModel viewModel, SavedStateRegistry registry,
            Lifecycle lifecycle) {
        SavedStateHandleController controller = viewModel.getTag(
                TAG_SAVED_STATE_HANDLE_CONTROLLER);
        if (controller != null && !controller.isAttached()) {
            controller.attachToLifecycle(registry, lifecycle);
            tryToAddRecreator(registry, lifecycle);
        }
    }

    private static void tryToAddRecreator(SavedStateRegistry registry, Lifecycle lifecycle) {
        Lifecycle.State currentState = lifecycle.getCurrentState();
        if (currentState == INITIALIZED || currentState.isAtLeast(STARTED)) {
            registry.runOnNextRecreation(OnRecreation.class);
        } else {
            lifecycle.addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source,
                        @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_START) {
                        lifecycle.removeObserver(this);
                        registry.runOnNextRecreation(OnRecreation.class);
                    }
                }
            });
        }
    }
}
