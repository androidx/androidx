/*
 * Copyright 2018 The Android Open Source Project
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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider.KeyedFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Utility methods that provides {@link ViewModelProvider.KeyedFactory} that can create ViewModels
 * that receive {@link SavedStateHandle} in constructor
 */
public class ViewModelsWithStateFactories {

    private static Application checkApplication(Activity activity) {
        Application application = activity.getApplication();
        if (application == null) {
            throw new IllegalStateException("Your activity/fragment is not yet attached to "
                    + "Application. You can't request ViewModelsWithStateFactory "
                    + "before onCreate call.");
        }
        return application;
    }

    private static Activity checkActivity(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            throw new IllegalStateException("Can't create ViewModelsWithStateFactory"
                    + " for detached fragment");
        }
        return activity;
    }

    private ViewModelsWithStateFactories() {
    }

    // TODO: make "KeyedFactory" link, once it will be public (current it is hidden)
    /**
     * Creates  KeyedFactory that wraps the given {@code factory} and
     * manages {@link SavedStateHandle} for it.
     *
     * @param activity an activity to use for state saving.
     * @param factory  a factory responsible for {@code ViewModel} creation.
     */
    @NonNull
    public static KeyedFactory of(@NonNull FragmentActivity activity,
            @Nullable ViewModelWithStateFactory factory) {
        Application app = checkApplication(activity);
        if (factory == null) {
            factory = new DefaultViewModelWithStateFactory(app);
        }
        return new FragmentVmFactory(app, activity.getBundleSavedStateRegistry(), factory,
                activityBundle(activity));
    }

    private static Bundle activityBundle(FragmentActivity activity) {
        return activity.getIntent().getExtras();
    }

    /**
     * Returns a factory that can create ViewModels that receive {@link SavedStateHandle}
     * in constructor.
     * <p>
     * If ViewModel is instance of {@link AndroidViewModel}, it looks for a constructor that
     * receives an {@link Application} and {@link SavedStateHandle} (in this order), otherwise
     * it looks for a constructor that receives {@link SavedStateHandle} only.
     *
     * @param activity an activity to use for state saving
     */
    @NonNull
    public static KeyedFactory of(@NonNull FragmentActivity activity) {
        return of(activity, null);
    }

    // TODO: make "KeyedFactory" link, once it will be public (current it is hidden)
    /**
     * Creates KeyedFactory that wraps the given {@code factory} and
     * manages {@link SavedStateHandle} for it.
     *
     * @param fragment an activity to use for state saving
     * @param factory  a factory responsible for {@code ViewModel} creation.
     */
    @NonNull
    public static KeyedFactory of(@NonNull Fragment fragment,
            @Nullable ViewModelWithStateFactory factory) {
        Application app = checkApplication(checkActivity(fragment));
        if (factory == null) {
            factory = new DefaultViewModelWithStateFactory(app);
        }
        return new FragmentVmFactory(app, fragment.getBundleSavedStateRegistry(), factory,
                fragment.getArguments());
    }

    /**
     * Returns a factory that can create ViewModels that receive {@link SavedStateHandle}
     * in constructor.
     * <p>
     * If ViewModel is instance of {@link AndroidViewModel}, it looks for a constructor that
     * receives an {@link Application} and {@link SavedStateHandle} (in this order), if there is no
     * such constructor it looks for constructor that receives {@link Application} only.
     * <p>
     * if ViewModel is instance of {@link AndroidViewModel}, it looks for a constructor that
     *  receivers {@link SavedStateHandle} or no arguments constructor
     *
     * @param fragment an fragment to use for state saving
     */
    @NonNull
    public static KeyedFactory of(@NonNull Fragment fragment) {
        return of(fragment, null);
    }


    static class FragmentVmFactory extends SavedStateVMFactory {
        FragmentVmFactory(Application app, SavedStateRegistry<Bundle> savedStateStore,
                ViewModelWithStateFactory factory, Bundle initialArgs) {
            super(savedStateStore, initialArgs, factory);
            VMSavedStateInitializer.initializeIfNeeded(app);
        }
    }

    static class DefaultViewModelWithStateFactory implements ViewModelWithStateFactory {

        private final Application mApplication;
        private final ViewModelProvider.AndroidViewModelFactory mFactory;

        DefaultViewModelWithStateFactory(@NonNull Application application) {
            mApplication = application;
            mFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                @NonNull SavedStateHandle handle) {

            boolean isAndroidViewModel = AndroidViewModel.class.isAssignableFrom(modelClass);
            Constructor<T> constructor;
            if (isAndroidViewModel) {
                constructor = findMatchingConstructor(modelClass, ANDROID_VIEWMODEL_SIGNATURE);
            } else {
                constructor = findMatchingConstructor(modelClass, VIEWMODEL_SIGNATURE);
            }
            if (constructor == null) {
                return mFactory.create(modelClass);
            }
            try {
                if (isAndroidViewModel) {
                    return constructor.newInstance(mApplication, handle);
                } else {
                    return constructor.newInstance(handle);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + modelClass, e);
            } catch (InstantiationException e) {
                throw new RuntimeException("A " + modelClass + " cannot be instantiated.", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("An exception happened in constructor of "
                        + modelClass, e.getCause());
            }
        }

        private static final Class<?>[] ANDROID_VIEWMODEL_SIGNATURE = new Class[]{Application.class,
                SavedStateHandle.class};
        private static final Class<?>[] VIEWMODEL_SIGNATURE = new Class[]{SavedStateHandle.class};

        @SuppressWarnings("unchecked")
        private static <T> Constructor<T> findMatchingConstructor(Class<T> modelClass,
                Class<?>[] signature) {
            for (Constructor<?> constructor : modelClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (Arrays.equals(signature, parameterTypes)) {
                    return (Constructor<T>) constructor;
                }
            }
            return null;
        }
    }
}
