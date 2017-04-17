/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.arch.lifecycle.state;

import android.arch.lifecycle.HolderFragment;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * Factory and utility methods for {@link SavedStateProvider}.
 * <p>
 * {@link SavedStateProvider} provides a convenient way to create variables that are saved with the
 * owner Activity or Fragment. Using it is similar to implementing
 * {@link android.app.Activity#onSaveInstanceState(Bundle)}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class StateProviders {
    /**
     * Returns {@link SavedStateProvider} associated with the given {@link LifecycleOwner}.
     *
     * @param owner The {@code LifecycleOwner} whose SavedStateProvider will be returned.
     */
    @MainThread
    public static SavedStateProvider savedStateProvider(LifecycleOwner owner) {
        if (owner instanceof Fragment) {
            return savedStateProvider((Fragment) owner);
        }
        if (owner instanceof FragmentActivity) {
            return savedStateProvider((FragmentActivity) owner);
        }
        //TODO: figure out how to handle LifecycleOwner
        throw new IllegalArgumentException("SavedStateProvider for " + owner.getClass()
                + " is not implemented yet.");
    }

    /**
     * Returns {@link SavedStateProvider} associated with the given fragment.
     * <p>
     * If this call is made after fragment saved its state, all later operations on this
     * {@link SavedStateProvider} will be lost if the application process is killed.
     */
    @MainThread
    public static SavedStateProvider savedStateProvider(Fragment fragment) {
        return HolderFragment.holderFragmentFor(fragment).getSavedStateProvider();
    }

    /**
     * Returns {@link SavedStateProvider} associated with the given activity.
     * <p>
     * If this call is made after activity saved its state, all later operations on this
     * {@link SavedStateProvider} will be lost if the application process is killed.
     */
    @MainThread
    public static SavedStateProvider savedStateProvider(FragmentActivity activity) {
        return HolderFragment.holderFragmentFor(activity).getSavedStateProvider();
    }
}
