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
import androidx.savedstate.SavedStateRegistryOwner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * {@link androidx.lifecycle.ViewModelProvider.Factory} that can create ViewModels accessing and contributing
 * to a saved state via {@link SavedStateHandle} received in a constructor. If {@code defaultArgs}
 * bundle was passed in {@link #SavedStateVMFactory(Fragment, Bundle)}
 * or {@link #SavedStateVMFactory(FragmentActivity, Bundle)}, it will provide default values in
 * {@code SavedStateHandle}.
 * <p>
 * If ViewModel is instance of {@link androidx.lifecycle.AndroidViewModel}, it looks for a constructor that
 * receives an {@link Application} and {@link SavedStateHandle} (in this order), otherwise
 * it looks for a constructor that receives {@link SavedStateHandle} only.
 */
public final class SavedStateVMFactory extends AbstractSavedStateVMFactory {
    private final Application mApplication;
    private final ViewModelProvider.AndroidViewModelFactory mFactory;

    /**
     * Creates {@link SavedStateVMFactory}.
     * <p>
     * {@link androidx.lifecycle.ViewModel} created with this factory can access to saved state scoped to
     * the given {@code fragment}.
     *
     * @param fragment scope of this fragment will be used for state saving
     */
    public SavedStateVMFactory(@NonNull Fragment fragment) {
        this(fragment, null);
    }

    /**
     * Creates {@link SavedStateVMFactory}.
     * <p>
     * {@link androidx.lifecycle.ViewModel} created with this factory can access to saved state scoped to
     * the given {@code fragment}.
     *
     * @param fragment scope of this fragment will be used for state saving
     * @param defaultArgs values from this {@code Bundle} will be used as defaults by
     * {@link SavedStateHandle} if there is no previously saved state or previously saved state
     * miss a value by such key.
     */
    public SavedStateVMFactory(@NonNull Fragment fragment, @Nullable Bundle defaultArgs) {
        this(checkApplication(checkActivity(fragment)), fragment, defaultArgs);
    }

    /**
     * Creates {@link SavedStateVMFactory}.
     * <p>
     * {@link androidx.lifecycle.ViewModel} created with this factory can access to saved state scoped to
     * the given {@code activity}.
     *
     * @param activity scope of this activity will be used for state saving
     */
    public SavedStateVMFactory(@NonNull FragmentActivity activity) {
        this(activity, null);
    }

    /**
     * Creates {@link SavedStateVMFactory}.
     * <p>
     * {@link androidx.lifecycle.ViewModel} created with this factory can access to saved state scoped to
     * the given {@code activity}.
     *
     * @param activity scope of this activity will be used for state saving
     * @param defaultArgs values from this {@code Bundle} will be used as defaults by
     * {@link SavedStateHandle} if there is no previously saved state or previously saved state
     * misses a value by such key.
     */
    public SavedStateVMFactory(@NonNull FragmentActivity activity, @Nullable Bundle defaultArgs) {
        this(checkApplication(activity), activity, defaultArgs);
    }

    /**
     * Creates {@link SavedStateVMFactory}.
     * <p>
     * {@link androidx.lifecycle.ViewModel} created with this factory can access to saved state scoped to
     * the given {@code activity}.
     *
     * @param application an application
     * @param owner {@link SavedStateRegistryOwner} that will provide restored state for created
     * {@link androidx.lifecycle.ViewModel ViewModels}
     * @param defaultArgs values from this {@code Bundle} will be used as defaults by
     * {@link SavedStateHandle} if there is no previously saved state or previously saved state
     * misses a value by such key.
     */
    public SavedStateVMFactory(@NonNull Application application,
            @NonNull SavedStateRegistryOwner owner,
            @Nullable Bundle defaultArgs) {
        super(owner, defaultArgs);
        mApplication = application;
        mFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
    }

    @NonNull
    @Override
    protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
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
}
