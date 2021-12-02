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

package androidx.core.view;

import android.annotation.SuppressLint;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper class for implementing {@link MenuHost}.
 */
public class MenuHostHelper {

    private final Runnable mOnInvalidateMenuCallback;
    private final CopyOnWriteArrayList<MenuProvider> mMenuProviders = new CopyOnWriteArrayList<>();
    private final Map<MenuProvider, LifecycleContainer> mProviderToLifecycleContainers =
            new HashMap<>();

    /**
     * Construct a new MenuHostHelper.
     *
     * @param onInvalidateMenuCallback callback to invalidate the menu
     *                                 whenever there may be a change to it
     */
    public MenuHostHelper(@NonNull Runnable onInvalidateMenuCallback) {
        mOnInvalidateMenuCallback = onInvalidateMenuCallback;
    }

    /**
     * Inflates the entire {@link Menu}, which will include all
     * {@link MenuItem}s provided by all current {@link MenuProvider}s.
     *
     * @param menu         the menu to inflate all the menu items into
     * @param menuInflater the inflater to be used to inflate the menu
     */
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        for (MenuProvider menuProvider : mMenuProviders) {
            menuProvider.onCreateMenu(menu, menuInflater);
        }
    }

    /**
     * Called whenever one of the menu items from any of the current
     * {@link MenuProvider}s is selected.
     *
     * @param item the menu item that was selected
     * @return {@code true} to indicate the menu processing was consumed
     * by one of the {@link MenuProvider}s, {@code false} otherwise.
     */
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        for (MenuProvider menuProvider : mMenuProviders) {
            if (menuProvider.onMenuItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the given {@link MenuProvider} to the helper.
     *
     * @param provider the MenuProvider to be added
     */
    public void addMenuProvider(@NonNull MenuProvider provider) {
        mMenuProviders.add(provider);
        mOnInvalidateMenuCallback.run();
    }

    /**
     * Adds the given {@link MenuProvider} to the helper.
     *
     * This {@link MenuProvider} will be removed once the given {@link LifecycleOwner}
     * receives an {@link Lifecycle.Event.ON_DESTROY} event.
     *
     * @param provider the MenuProvider to be added
     * @param owner    the Lifecycle owner whose state will determine the removal of the provider
     */
    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner) {
        addMenuProvider(provider);
        Lifecycle lifecycle = owner.getLifecycle();
        LifecycleContainer lifecycleContainer = mProviderToLifecycleContainers.remove(provider);
        if (lifecycleContainer != null) {
            lifecycleContainer.clearObservers();
        }
        LifecycleEventObserver observer = (source, event) -> {
            if (event == Lifecycle.Event.ON_DESTROY) {
                removeMenuProvider(provider);
            }
        };
        mProviderToLifecycleContainers.put(provider, new LifecycleContainer(lifecycle, observer));
    }

    /**
     * Adds the given {@link MenuProvider} to the helper once the given
     * {@link LifecycleOwner} reaches the given {@link Lifecycle.State}.
     *
     * This {@link MenuProvider} will be removed once the given {@link LifecycleOwner}
     * goes down from the given {@link Lifecycle.State} or receives an
     * {@link Lifecycle.Event.ON_DESTROY} event.
     *
     * @param provider the MenuProvider to be added
     * @param state    the Lifecycle.State to check for automated addition/removal
     * @param owner    the Lifecycle owner whose state will determine the removal of the provider
     */
    @SuppressLint("LambdaLast")
    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
            @NonNull Lifecycle.State state) {
        Lifecycle lifecycle = owner.getLifecycle();
        LifecycleContainer lifecycleContainer = mProviderToLifecycleContainers.remove(provider);
        if (lifecycleContainer != null) {
            lifecycleContainer.clearObservers();
        }
        LifecycleEventObserver observer = (source, event) -> {
            if (event == Lifecycle.Event.upTo(state)) {
                addMenuProvider(provider);
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                removeMenuProvider(provider);
            } else if (event == Lifecycle.Event.downFrom(state)) {
                mMenuProviders.remove(provider);
                mOnInvalidateMenuCallback.run();
            }
        };
        mProviderToLifecycleContainers.put(provider, new LifecycleContainer(lifecycle, observer));
    }

    /**
     * Removes the given {@link MenuProvider} from the helper.
     *
     * @param provider the MenuProvider to be removed
     */
    public void removeMenuProvider(@NonNull MenuProvider provider) {
        mMenuProviders.remove(provider);
        LifecycleContainer lifecycleContainer = mProviderToLifecycleContainers.remove(provider);
        if (lifecycleContainer != null) {
            lifecycleContainer.clearObservers();
        }
        mOnInvalidateMenuCallback.run();
    }

    private static class LifecycleContainer {
        final Lifecycle mLifecycle;
        private LifecycleEventObserver mObserver;

        LifecycleContainer(@NonNull Lifecycle lifecycle, @NonNull LifecycleEventObserver observer) {
            mLifecycle = lifecycle;
            mObserver = observer;
            mLifecycle.addObserver(observer);
        }

        void clearObservers() {
            mLifecycle.removeObserver(mObserver);
            mObserver = null;
        }
    }
}
