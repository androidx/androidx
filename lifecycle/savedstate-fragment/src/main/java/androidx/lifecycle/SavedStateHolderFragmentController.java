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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SavedStateHolderFragmentController {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class SavedStateHolderFragment extends Fragment {

        private BundleSavedStateRegistry mRegistry;

        @SuppressWarnings({"WeakerAccess", "unused"})
        public SavedStateHolderFragment() {
        }

        @SuppressLint("ValidFragment")
        @SuppressWarnings("WeakerAccess")
        SavedStateHolderFragment(BundleSavedStateRegistry registry) {
            mRegistry = registry;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (mRegistry == null) {
                mRegistry = sHolderFragmentManager.mNotCommittedStores.remove(
                        getParentFragment() != null ? getParentFragment() : getActivity());
            }
            if (mRegistry == null) {
                mRegistry = new BundleSavedStateRegistry();
            }
            if (!mRegistry.isRestored()) {
                mRegistry.performRestore(savedInstanceState);
            }
        }

        BundleSavedStateRegistry getSavedStateStore() {
            return mRegistry;
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            mRegistry.performSave(outState);
        }
    }

    private static final String LOG_TAG = "SavedStateRegistrys";

    /**
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static final String HOLDER_TAG = "androidx.lifecycle.SavedStateHolderFragment";

    @SuppressWarnings("WeakerAccess")
    Map<LifecycleOwner, BundleSavedStateRegistry> mNotCommittedStores = new HashMap<>();

    @SuppressWarnings("WeakerAccess")
    static SavedStateHolderFragment findHolderFragment(FragmentManager manager) {
        if (manager.isDestroyed()) {
            throw new IllegalStateException("Can't access SavedStateStores from onDestroy");
        }

        Fragment fragmentByTag = manager.findFragmentByTag(HOLDER_TAG);
        if (fragmentByTag != null && !(fragmentByTag instanceof SavedStateHolderFragment)) {
            throw new IllegalStateException("Unexpected "
                    + "fragment instance was returned by HOLDER_TAG");
        }
        return (SavedStateHolderFragment) fragmentByTag;
    }

    private BundleSavedStateRegistry createHolderFragment(
            final FragmentManagerCall fragmentManager, LifecycleOwner owner) {

        final BundleSavedStateRegistry registry = new BundleSavedStateRegistry();

        if (fragmentManager.call() != null) {
            registry.performRestore(null);
        }

        mNotCommittedStores.put(owner, registry);

        owner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(final LifecycleOwner source, Lifecycle.Event event) {
                final LifecycleObserver thisObserver = this;
                if (event == Lifecycle.Event.ON_CREATE) {
                    FragmentManager fm = fragmentManager.call();
                    source.getLifecycle().removeObserver(this);
                    if (findHolderFragment(fm) != null) {
                        return;
                    }

                    SavedStateHolderFragment fragment = new SavedStateHolderFragment(registry);
                    fm.beginTransaction().add(fragment, HOLDER_TAG).runOnCommit(new Runnable() {
                        @Override
                        public void run() {
                            source.getLifecycle().removeObserver(thisObserver);
                        }
                    }).commitAllowingStateLoss();
                }
                if (event == Lifecycle.Event.ON_DESTROY) {
                    source.getLifecycle().removeObserver(this);
                    BundleSavedStateRegistry notCommitted = mNotCommittedStores.remove(source);
                    if (notCommitted != null) {
                        Log.e(LOG_TAG, "Failed to save a SavedStateComponents for " + source);
                    }
                }
            }
        });
        return registry;
    }

    private BundleSavedStateRegistry savedStateRegistry(FragmentManagerCall fragmentManager,
            LifecycleOwner owner) {
        FragmentManager fm = fragmentManager.call();
        if (fm != null) {
            SavedStateHolderFragment holder = findHolderFragment(fragmentManager.call());
            if (holder != null) {
                return holder.getSavedStateStore();
            }
        }
        BundleSavedStateRegistry store = mNotCommittedStores.get(owner);
        if (store != null) {
            return store;
        }

        return createHolderFragment(fragmentManager, owner);
    }

    static BundleSavedStateRegistry savedStateRegistry(FragmentActivity activity) {
        return sHolderFragmentManager.savedStateRegistry(fragmentManager(activity), activity);
    }

    static BundleSavedStateRegistry savedStateRegistry(Fragment parentFragment) {
        return sHolderFragmentManager.savedStateRegistry(fragmentManager(parentFragment),
                parentFragment);
    }

    static final SavedStateHolderFragmentController sHolderFragmentManager =
            new SavedStateHolderFragmentController();

    // it isn't 100% accurate for eager restoration, but good enough for alpha
    // once we move it fragment, we will be able to do that properly
    private static FragmentManagerCall fragmentManager(final Fragment fragment) {
        return new FragmentManagerCall() {
            @Override
            public FragmentManager call() {
                return fragment.getContext() != null ? fragment.getChildFragmentManager() : null;
            }
        };
    }

    // it isn't 100% accurate for eager restoration, but good enough for alpha
    // once we move it fragmentActivity we will be able to do that properly
    private static FragmentManagerCall fragmentManager(final FragmentActivity activity) {
        return new FragmentManagerCall() {
            @Override
            public FragmentManager call() {
                return activity.getApplication() != null
                        ? activity.getSupportFragmentManager() : null;
            }
        };
    }

    // method reference: fragment manager may not be accessible at the moment
    interface FragmentManagerCall {
        FragmentManager call();
    }
}
