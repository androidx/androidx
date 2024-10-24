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

package androidx.fragment.app;

import android.util.Log;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * FragmentManagerViewModel is the always up to date view of the Fragment's
 * non configuration state
 */
final class FragmentManagerViewModel extends ViewModel {
    private static final String TAG = FragmentManager.TAG;

    private static final ViewModelProvider.Factory FACTORY = new ViewModelProvider.Factory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> @NonNull T create(@NonNull Class<T> modelClass) {
            FragmentManagerViewModel viewModel = new FragmentManagerViewModel(true);
            return (T) viewModel;
        }
    };

    static @NonNull FragmentManagerViewModel getInstance(ViewModelStore viewModelStore) {
        ViewModelProvider viewModelProvider = new ViewModelProvider(viewModelStore,
                FACTORY);
        return viewModelProvider.get(FragmentManagerViewModel.class);
    }

    private final HashMap<String, Fragment> mRetainedFragments = new HashMap<>();
    private final HashMap<String, FragmentManagerViewModel> mChildNonConfigs = new HashMap<>();
    private final HashMap<String, ViewModelStore> mViewModelStores = new HashMap<>();

    private final boolean mStateAutomaticallySaved;
    // Only used when mStateAutomaticallySaved is true
    private boolean mHasBeenCleared = false;
    // Only used when mStateAutomaticallySaved is false
    private boolean mHasSavedSnapshot = false;

    // Flag set by the FragmentManager to indicate when we should allow
    // changes to the set of retained fragments
    private boolean mIsStateSaved = false;

    /**
     * FragmentManagerViewModel simultaneously supports two modes:
     * <ol>
     *     <li>Automatically saved: in this model, it is assumed that the ViewModel is added to
     *     an appropriate {@link ViewModelStore} that has the same lifecycle as the
     *     FragmentManager and that {@link #onCleared()} indicates that the Fragment's host
     *     is being permanently destroyed.</li>
     *     <li>Not automatically saved: in this model, the FragmentManager is responsible for
     *     calling {@link #getSnapshot()} and later restoring the ViewModel with
     *     <code>restoreFromSnapshot</code>.</li>
     * </ol>
     * These states are mutually exclusive.
     *
     * @param stateAutomaticallySaved Whether the ViewModel will be automatically saved.
     */
    FragmentManagerViewModel(boolean stateAutomaticallySaved) {
        mStateAutomaticallySaved = stateAutomaticallySaved;
    }

    /**
     * Set whether the FragmentManager has saved its state
     *
     * @param isStateSaved Whether the FragmentManager has saved its state
     */
    void setIsStateSaved(boolean isStateSaved) {
        mIsStateSaved = isStateSaved;
    }

    @Override
    protected void onCleared() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "onCleared called for " + this);
        }
        mHasBeenCleared = true;
    }

    boolean isCleared() {
        return mHasBeenCleared;
    }

    void addRetainedFragment(@NonNull Fragment fragment) {
        if (mIsStateSaved) {
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(TAG, "Ignoring addRetainedFragment as the state is already saved");
            }
            return;
        }
        if (mRetainedFragments.containsKey(fragment.mWho)) {
            return;
        }
        mRetainedFragments.put(fragment.mWho, fragment);
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Updating retained Fragments: Added " + fragment);
        }
    }

    @Nullable Fragment findRetainedFragmentByWho(String who) {
        return mRetainedFragments.get(who);
    }

    @NonNull Collection<Fragment> getRetainedFragments() {
        return new ArrayList<>(mRetainedFragments.values());
    }

    boolean shouldDestroy(@NonNull Fragment fragment) {
        if (!mRetainedFragments.containsKey(fragment.mWho)) {
            // Always destroy non-retained Fragments
            return true;
        }
        if (mStateAutomaticallySaved) {
            // If we automatically save our state, then only
            // destroy a retained Fragment when we've been cleared
            return mHasBeenCleared;
        } else {
            // Else, only destroy retained Fragments if they've
            // been reaped before the state has been saved
            return !mHasSavedSnapshot;
        }
    }

    void removeRetainedFragment(@NonNull Fragment fragment) {
        if (mIsStateSaved) {
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(TAG, "Ignoring removeRetainedFragment as the state is already saved");
            }
            return;
        }
        boolean removed = mRetainedFragments.remove(fragment.mWho) != null;
        if (removed && FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Updating retained Fragments: Removed " + fragment);
        }
    }

    @NonNull FragmentManagerViewModel getChildNonConfig(@NonNull Fragment f) {
        FragmentManagerViewModel childNonConfig = mChildNonConfigs.get(f.mWho);
        if (childNonConfig == null) {
            childNonConfig = new FragmentManagerViewModel(mStateAutomaticallySaved);
            mChildNonConfigs.put(f.mWho, childNonConfig);
        }
        return childNonConfig;
    }

    @NonNull ViewModelStore getViewModelStore(@NonNull Fragment f) {
        ViewModelStore viewModelStore = mViewModelStores.get(f.mWho);
        if (viewModelStore == null) {
            viewModelStore = new ViewModelStore();
            mViewModelStores.put(f.mWho, viewModelStore);
        }
        return viewModelStore;
    }

    void clearNonConfigState(@NonNull Fragment f, boolean destroyChildNonConfig) {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "Clearing non-config state for " + f);
        }
        clearNonConfigStateInternal(f.mWho, destroyChildNonConfig);
    }

    void clearNonConfigState(@NonNull String who, boolean destroyChildNonConfig) {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "Clearing non-config state for saved state of Fragment " + who);
        }
        clearNonConfigStateInternal(who, destroyChildNonConfig);
    }

    private void clearNonConfigStateInternal(@NonNull String who, boolean destroyChildNonConfig) {
        // Clear and remove the Fragment's child non config state
        FragmentManagerViewModel childNonConfig = mChildNonConfigs.get(who);
        if (childNonConfig != null) {
            // destroy child nonConfig immediately if it hasn't gone through init
            if (destroyChildNonConfig) {
                ArrayList<String> clearList = new ArrayList<>();
                clearList.addAll(childNonConfig.mChildNonConfigs.keySet());
                for (String childWho : clearList) {
                    childNonConfig.clearNonConfigState(childWho, true);
                }
            }
            childNonConfig.onCleared();
            mChildNonConfigs.remove(who);
        }
        // Clear and remove the Fragment's ViewModelStore
        ViewModelStore viewModelStore = mViewModelStores.get(who);
        if (viewModelStore != null) {
            viewModelStore.clear();
            mViewModelStores.remove(who);
        }
    }

    /**
     * @deprecated Ideally, we only support mStateAutomaticallySaved = true and remove this
     * code, alongside
     * {@link FragmentController#restoreAllState(android.os.Parcelable, FragmentManagerNonConfig)}.
     */
    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    @Deprecated
    void restoreFromSnapshot(@Nullable FragmentManagerNonConfig nonConfig) {
        mRetainedFragments.clear();
        mChildNonConfigs.clear();
        mViewModelStores.clear();
        if (nonConfig != null) {
            Collection<Fragment> fragments = nonConfig.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    if (fragment != null) {
                        mRetainedFragments.put(fragment.mWho, fragment);
                    }
                }
            }
            Map<String, FragmentManagerNonConfig> childNonConfigs = nonConfig.getChildNonConfigs();
            if (childNonConfigs != null) {
                for (Map.Entry<String, FragmentManagerNonConfig> entry :
                        childNonConfigs.entrySet()) {
                    FragmentManagerViewModel childViewModel =
                            new FragmentManagerViewModel(mStateAutomaticallySaved);
                    childViewModel.restoreFromSnapshot(entry.getValue());
                    mChildNonConfigs.put(entry.getKey(), childViewModel);
                }
            }
            Map<String, ViewModelStore> viewModelStores = nonConfig.getViewModelStores();
            if (viewModelStores != null) {
                mViewModelStores.putAll(viewModelStores);
            }
        }
        mHasSavedSnapshot = false;
    }

    /**
     * @deprecated Ideally, we only support mStateAutomaticallySaved = true and remove this
     * code, alongside {@link FragmentController#retainNestedNonConfig()}.
     */
    @Deprecated
    @Nullable FragmentManagerNonConfig getSnapshot() {
        if (mRetainedFragments.isEmpty() && mChildNonConfigs.isEmpty()
                && mViewModelStores.isEmpty()) {
            return null;
        }
        HashMap<String, FragmentManagerNonConfig> childNonConfigs = new HashMap<>();
        for (Map.Entry<String, FragmentManagerViewModel> entry : mChildNonConfigs.entrySet()) {
            FragmentManagerNonConfig childNonConfig = entry.getValue().getSnapshot();
            if (childNonConfig != null) {
                childNonConfigs.put(entry.getKey(), childNonConfig);
            }
        }

        mHasSavedSnapshot = true;
        if (mRetainedFragments.isEmpty() && childNonConfigs.isEmpty()
                && mViewModelStores.isEmpty()) {
            return null;
        }
        return new FragmentManagerNonConfig(
                new ArrayList<>(mRetainedFragments.values()),
                childNonConfigs,
                new HashMap<>(mViewModelStores));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FragmentManagerViewModel that = (FragmentManagerViewModel) o;

        return mRetainedFragments.equals(that.mRetainedFragments)
                && mChildNonConfigs.equals(that.mChildNonConfigs)
                && mViewModelStores.equals(that.mViewModelStores);
    }

    @Override
    public int hashCode() {
        int result = mRetainedFragments.hashCode();
        result = 31 * result + mChildNonConfigs.hashCode();
        result = 31 * result + mViewModelStores.hashCode();
        return result;
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder("FragmentManagerViewModel{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("} Fragments (");
        Iterator<Fragment> fragmentIterator = mRetainedFragments.values().iterator();
        while (fragmentIterator.hasNext()) {
            sb.append(fragmentIterator.next());
            if (fragmentIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(") Child Non Config (");
        Iterator<String> childNonConfigIterator = mChildNonConfigs.keySet().iterator();
        while (childNonConfigIterator.hasNext()) {
            sb.append(childNonConfigIterator.next());
            if (childNonConfigIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(") ViewModelStores (");
        Iterator<String> viewModelStoreIterator = mViewModelStores.keySet().iterator();
        while (viewModelStoreIterator.hasNext()) {
            sb.append(viewModelStoreIterator.next());
            if (viewModelStoreIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return sb.toString();
    }
}
