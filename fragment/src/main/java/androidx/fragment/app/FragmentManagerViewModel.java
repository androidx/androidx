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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * FragmentManagerViewModel is the always up to date view of the Fragment's
 * non configuration state
 */
class FragmentManagerViewModel extends ViewModel {
    private final HashSet<Fragment> mRetainedFragments = new HashSet<>();
    private final HashMap<String, FragmentManagerViewModel> mChildNonConfigs = new HashMap<>();
    private final HashMap<String, ViewModelStore> mViewModelStores = new HashMap<>();

    private boolean mHasSavedSnapshot = false;

    void addRetainedFragment(@NonNull Fragment fragment) {
        mRetainedFragments.add(fragment);
    }

    @NonNull
    Collection<Fragment> getRetainedFragments() {
        return mRetainedFragments;
    }

    boolean shouldDestroy(@NonNull Fragment fragment) {
        return !mHasSavedSnapshot || !mRetainedFragments.contains(fragment);
    }

    void removeRetainedFragment(@NonNull Fragment fragment) {
        mRetainedFragments.remove(fragment);
    }

    @NonNull
    FragmentManagerViewModel getChildNonConfig(@NonNull Fragment f) {
        FragmentManagerViewModel childNonConfig = mChildNonConfigs.get(f.mWho);
        if (childNonConfig == null) {
            childNonConfig = new FragmentManagerViewModel();
            mChildNonConfigs.put(f.mWho, childNonConfig);
        }
        return childNonConfig;
    }

    @NonNull
    ViewModelStore getViewModelStore(@NonNull Fragment f) {
        ViewModelStore viewModelStore = mViewModelStores.get(f.mWho);
        if (viewModelStore == null) {
            viewModelStore = new ViewModelStore();
            mViewModelStores.put(f.mWho, viewModelStore);
        }
        return viewModelStore;
    }

    void clearNonConfigState(@NonNull Fragment f) {
        // Clear and remove the Fragment's child non config state
        FragmentManagerViewModel childNonConfig = mChildNonConfigs.get(f.mWho);
        if (childNonConfig != null) {
            childNonConfig.onCleared();
            mChildNonConfigs.remove(f.mWho);
        }
        // Clear and remove the Fragment's ViewModelStore
        ViewModelStore viewModelStore = mViewModelStores.get(f.mWho);
        if (viewModelStore != null) {
            viewModelStore.clear();
            mViewModelStores.remove(f.mWho);
        }
    }

    void restoreFromSnapshot(@Nullable FragmentManagerNonConfig nonConfig) {
        mRetainedFragments.clear();
        mChildNonConfigs.clear();
        mViewModelStores.clear();
        if (nonConfig != null) {
            Collection<Fragment> fragments = nonConfig.getFragments();
            if (fragments != null) {
                mRetainedFragments.addAll(fragments);
            }
            Map<String, FragmentManagerNonConfig> childNonConfigs = nonConfig.getChildNonConfigs();
            if (childNonConfigs != null) {
                for (Map.Entry<String, FragmentManagerNonConfig> entry :
                        childNonConfigs.entrySet()) {
                    FragmentManagerViewModel childViewModel = new FragmentManagerViewModel();
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

    @Nullable
    FragmentManagerNonConfig getSnapshot() {
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
                new ArrayList<>(mRetainedFragments),
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

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FragmentManagerViewModel{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("} Fragments (");
        Iterator<Fragment> fragmentIterator = mRetainedFragments.iterator();
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
