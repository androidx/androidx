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

package androidx.navigation;

import android.app.Activity;
import android.content.Context;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStore;

/**
 * Subclass of {@link NavController} that offers additional APIs for use by a
 * {@link NavHost} to connect the NavController to external dependencies.
 *
 * <p>Apps should generally not construct controllers, instead obtain a relevant controller
 * directly from a navigation host via {@link NavHost#getNavController()} or by using one of
 * the utility methods on the {@link Navigation} class.</p>
 */
public class NavHostController extends NavController {

    /**
     * Construct a new controller for a given {@link Context} suitable for use in a
     * {@link NavHost}. Controllers should not be used outside of their context and retain a
     * hard reference to the context supplied. If you need a global controller, pass
     * {@link Context#getApplicationContext()}.
     *
     * <p>Note that controllers that are not constructed with an {@link Activity} context
     * (or a wrapped activity context) will only be able to navigate to
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK new tasks} or
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_DOCUMENT new document tasks} when
     * navigating to new activities.</p>
     *
     * @param context context for this controller
     */
    public NavHostController(@NonNull Context context) {
        super(context);
    }

    /**
     * Sets the host's {@link LifecycleOwner}.
     *
     * @param owner The {@link LifecycleOwner} associated with the containing {@link NavHost}.
     * @see NavHostController#setOnBackPressedDispatcher(OnBackPressedDispatcher)
     */
    @Override
    public final void setLifecycleOwner(@NonNull LifecycleOwner owner) {
        super.setLifecycleOwner(owner);
    }

    /**
     * Sets the host's {@link OnBackPressedDispatcher}. If set, NavController will
     * register a {@link OnBackPressedCallback} to handle system Back button events.
     * <p>
     * You must explicitly called {@link #setLifecycleOwner(LifecycleOwner)} before calling this
     * method as the owner set there will be used as the {@link LifecycleOwner} for registering
     * the {@link OnBackPressedCallback}.
     * <p>
     * You can dynamically enable and disable whether the NavController should handle the
     * system Back button events by calling {@link #enableOnBackPressed(boolean)}.
     *
     * @param dispatcher The {@link OnBackPressedDispatcher} associated with the containing
     * {@link NavHost}.
     * @throws IllegalStateException if you have not called
     * {@link #setLifecycleOwner(LifecycleOwner)} before calling this method.
     * @see #setLifecycleOwner(LifecycleOwner)
     */
    @Override
    public final void setOnBackPressedDispatcher(@NonNull OnBackPressedDispatcher dispatcher) {
        super.setOnBackPressedDispatcher(dispatcher);
    }

    /**
     * Set whether the NavController should handle the system Back button events via the
     * registered {@link OnBackPressedDispatcher}.
     *
     * @param enabled True if the NavController should handle system Back button events.
     */
    @Override
    public final void enableOnBackPressed(boolean enabled) {
        super.enableOnBackPressed(enabled);
    }

    /**
     * Sets the host's ViewModelStore used by the NavController to store ViewModels at the
     * navigation graph level. This is required to call {@link #getViewModelStoreOwner} and
     * should generally be called for you by your {@link NavHost}.
     * <p>
     * You must call this method before {@link #setGraph(int)} or similar methods, because the
     * {@link ViewModelStore} set here will be used by the created {@link NavBackStackEntry} items.
     *
     * @param viewModelStore ViewModelStore used to store ViewModels at the navigation graph level
     * @throws IllegalStateException if this method is called when graph was already set via
     * {@link #setGraph(int)} or similar methods.
     */
    @Override
    public final void setViewModelStore(@NonNull ViewModelStore viewModelStore) {
        super.setViewModelStore(viewModelStore);
    }
}
