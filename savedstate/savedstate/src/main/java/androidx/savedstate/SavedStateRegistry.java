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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.Iterator;
import java.util.Map;

/**
 * An interface for plugging components that consumes and contributes to the saved state.
 *
 * <p>This objects lifetime is bound to the lifecycle of owning component: when activity or
 * fragment is recreated, new instance of the object is created as well.
 */
@SuppressLint("RestrictedApi")
public final class SavedStateRegistry {
    private static final String SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.BundlableSavedStateRegistry.key";

    private SafeIterableMap<String, SavedStateProvider> mComponents =
            new SafeIterableMap<>();
    @Nullable
    private Bundle mRestoredState;
    private boolean mRestored;
    private Recreator.SavedStateProvider mRecreatorProvider;
    boolean mAllowingSavingState = true;

    SavedStateRegistry() {
    }

    /**
     * Consumes saved state previously supplied by {@link SavedStateProvider} registered
     * via {@link #registerSavedStateProvider(String, SavedStateProvider)}
     * with the given {@code key}.
     * <p>
     * This call clears an internal reference to returned saved state, so if you call it second time
     * in the row it will return {@code null}.
     * <p>
     * All unconsumed values will be saved during {@code onSaveInstanceState(Bundle savedState)}
     * <p>
     * This method can be called after {@code super.onCreate(savedStateBundle)} of the corresponding
     * component. Calling it before that will result in {@code IllegalArgumentException}.
     * {@link Lifecycle.Event#ON_CREATE} can be used as a signal
     * that a saved state can be safely consumed.
     *
     * @param key a key with which {@link SavedStateProvider} was previously registered.
     * @return {@code S} with the previously saved state or {@code null}
     */
    @MainThread
    @Nullable
    public Bundle consumeRestoredStateForKey(@NonNull String key) {
        if (!mRestored) {
            throw new IllegalStateException("You can consumeRestoredStateForKey "
                    + "only after super.onCreate of corresponding component");
        }
        if (mRestoredState != null) {
            Bundle result = mRestoredState.getBundle(key);
            mRestoredState.remove(key);
            if (mRestoredState.isEmpty()) {
                mRestoredState = null;
            }
            return result;
        }
        return null;
    }

    /**
     * Registers a {@link SavedStateProvider} by the given {@code key}. This
     * {@code savedStateProvider} will be called
     * during state saving phase, returned object will be associated with the given {@code key}
     * and can be used after the restoration via {@link #consumeRestoredStateForKey(String)}.
     * <p>
     * If there is unconsumed value with the same {@code key},
     * the value supplied by {@code savedStateProvider} will be override and
     * will be written to resulting saved state.
     * <p> if a provider was already registered with the given {@code key}, an implementation should
     * throw an {@link IllegalArgumentException}
     *
     * @param key      a key with which returned saved state will be associated
     * @param provider savedStateProvider to get saved state.
     */
    @MainThread
    public void registerSavedStateProvider(@NonNull String key,
            @NonNull SavedStateProvider provider) {
        SavedStateProvider previous = mComponents.putIfAbsent(key, provider);
        if (previous != null) {
            throw new IllegalArgumentException("SavedStateProvider with the given key is"
                    + " already registered");
        }
    }

    /**
     * Unregisters a component previously registered by the given {@code key}
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    public void unregisterSavedStateProvider(@NonNull String key) {
        mComponents.remove(key);
    }

    /**
     * Returns if state was restored after creation and can be safely consumed
     * with {@link #consumeRestoredStateForKey(String)}
     *
     * @return true if state was restored.
     */
    @MainThread
    public boolean isRestored() {
        return mRestored;
    }

    /**
     * Subclasses of this interface will be automatically recreated if they were previously
     * registered via {@link #runOnNextRecreation(Class)}.
     * <p>
     * Subclasses must have a default constructor
     */
    public interface AutoRecreated {
        /**
         * This method will be called during
         * dispatching of {@link androidx.lifecycle.Lifecycle.Event#ON_CREATE} of owning component.
         *
         * @param owner a component that was restarted
         */
        void onRecreated(@NonNull SavedStateRegistryOwner owner);
    }

    /**
     * Executes the given class when the owning component restarted.
     * <p>
     * The given class will be automatically instantiated via default constructor and method
     * {@link AutoRecreated#onRecreated(SavedStateRegistryOwner)} will be called.
     * It is called as part of dispatching of {@link androidx.lifecycle.Lifecycle.Event#ON_CREATE}
     * event.
     *
     * @param clazz that will need to be instantiated on the next component recreation
     * @throws IllegalStateException if you try to call if after {@link Lifecycle.Event#ON_STOP}
     *                               was dispatched
     */
    @MainThread
    public void runOnNextRecreation(@NonNull Class<? extends AutoRecreated> clazz) {
        if (!mAllowingSavingState) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
        if (mRecreatorProvider == null) {
            mRecreatorProvider = new Recreator.SavedStateProvider(this);
        }
        try {
            clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class" + clazz.getSimpleName() + " must have "
                    + "default constructor in order to be automatically recreated", e);
        }
        mRecreatorProvider.add(clazz.getName());
    }

    /**
     * An interface for an owner of this @{code {@link SavedStateRegistry} to restore saved state.
     *
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    void performRestore(@NonNull Lifecycle lifecycle, @Nullable Bundle savedState) {
        if (mRestored) {
            throw new IllegalStateException("SavedStateRegistry was already restored.");
        }
        if (savedState != null) {
            mRestoredState = savedState.getBundle(SAVED_COMPONENTS_KEY);
        }

        lifecycle.addObserver(new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    mAllowingSavingState = true;
                } else if (event == Lifecycle.Event.ON_STOP) {
                    mAllowingSavingState = false;
                }
            }
        });

        mRestored = true;
    }

    /**
     * An interface for an owner of this @{code {@link SavedStateRegistry}
     * to perform state saving, it will call all registered providers and
     * merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    @MainThread
    void performSave(@NonNull Bundle outBundle) {
        Bundle components = new Bundle();
        if (mRestoredState != null) {
            components.putAll(mRestoredState);
        }
        for (Iterator<Map.Entry<String, SavedStateProvider>> it =
                mComponents.iteratorWithAdditions(); it.hasNext(); ) {
            Map.Entry<String, SavedStateProvider> entry1 = it.next();
            components.putBundle(entry1.getKey(), entry1.getValue().saveState());
        }
        outBundle.putBundle(SAVED_COMPONENTS_KEY, components);
    }

    /**
     * This interface marks a component that contributes to saved state.
     */
    public interface SavedStateProvider {
        /**
         * Called to retrieve a state from a component before being killed
         * so later the state can be received from {@link #consumeRestoredStateForKey(String)}
         *
         * @return S with your saved state.
         */
        @NonNull
        Bundle saveState();
    }
}
