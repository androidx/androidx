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

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider.Factory;

/**
 * Utilities methods for {@link ViewModelStore} class.
 *
 * @deprecated Use the constructors for {@link ViewModelProvider} directly.
 */
@Deprecated
public class ViewModelProviders {

    /**
     * @deprecated This class should not be directly instantiated
     */
    @Deprecated
    public ViewModelProviders() {
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given
     * {@code fragment} is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses the {@link Fragment#getDefaultViewModelProviderFactory() default factory}
     * to instantiate new ViewModels.
     *
     * @param fragment a fragment, in whose scope ViewModels should be retained
     * @return a ViewModelProvider instance
     * @deprecated Use the 'by viewModels()' Kotlin property delegate or
     * {@link ViewModelProvider#ViewModelProvider(ViewModelStoreOwner)},
     * passing in the fragment.
     */
    @Deprecated
    @NonNull
    @MainThread
    public static ViewModelProvider of(@NonNull Fragment fragment) {
        return new ViewModelProvider(fragment);
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given Activity
     * is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses the {@link FragmentActivity#getDefaultViewModelProviderFactory() default factory}
     * to instantiate new ViewModels.
     *
     * @param activity an activity, in whose scope ViewModels should be retained
     * @return a ViewModelProvider instance
     * @deprecated Use the 'by viewModels()' Kotlin property delegate or
     * {@link ViewModelProvider#ViewModelProvider(ViewModelStoreOwner)},
     * passing in the activity.
     */
    @Deprecated
    @NonNull
    @MainThread
    public static ViewModelProvider of(@NonNull FragmentActivity activity) {
        return new ViewModelProvider(activity);
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given
     * {@code fragment} is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses the given {@link Factory} to instantiate new ViewModels.
     *
     * @param fragment a fragment, in whose scope ViewModels should be retained
     * @param factory  a {@code Factory} to instantiate new ViewModels
     * @return a ViewModelProvider instance
     * @deprecated Use the 'by viewModels()' Kotlin property delegate or
     * {@link ViewModelProvider#ViewModelProvider(ViewModelStoreOwner, Factory)},
     * passing in the fragment and factory.
     */
    @Deprecated
    @NonNull
    @MainThread
    public static ViewModelProvider of(@NonNull Fragment fragment, @Nullable Factory factory) {
        if (factory == null) {
            factory = fragment.getDefaultViewModelProviderFactory();
        }
        return new ViewModelProvider(fragment.getViewModelStore(), factory);
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given Activity
     * is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses the given {@link Factory} to instantiate new ViewModels.
     *
     * @param activity an activity, in whose scope ViewModels should be retained
     * @param factory  a {@code Factory} to instantiate new ViewModels
     * @return a ViewModelProvider instance
     * @deprecated Use the 'by viewModels()' Kotlin property delegate or
     * {@link ViewModelProvider#ViewModelProvider(ViewModelStoreOwner, Factory)},
     * passing in the activity and factory.
     */
    @Deprecated
    @NonNull
    @MainThread
    public static ViewModelProvider of(@NonNull FragmentActivity activity,
            @Nullable Factory factory) {
        if (factory == null) {
            factory = activity.getDefaultViewModelProviderFactory();
        }
        return new ViewModelProvider(activity.getViewModelStore(), factory);
    }

    /**
     * {@link Factory} which may create {@link AndroidViewModel} and
     * {@link ViewModel}, which have an empty constructor.
     *
     * @deprecated Use {@link ViewModelProvider.AndroidViewModelFactory}
     */
    @Deprecated
    public static class DefaultFactory extends ViewModelProvider.AndroidViewModelFactory {
        /**
         * Creates a {@code AndroidViewModelFactory}
         *
         * @param application an application to pass in {@link AndroidViewModel}
         * @deprecated Use {@link ViewModelProvider.AndroidViewModelFactory} or
         * {@link ViewModelProvider.AndroidViewModelFactory#getInstance(Application)}.
         */
        @Deprecated
        public DefaultFactory(@NonNull Application application) {
            super(application);
        }
    }
}
