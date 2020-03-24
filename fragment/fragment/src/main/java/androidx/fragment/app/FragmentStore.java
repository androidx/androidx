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

package androidx.fragment.app;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class FragmentStore {
    private static final String TAG = FragmentManager.TAG;

    private final ArrayList<Fragment> mAdded = new ArrayList<>();
    private final HashMap<String, FragmentStateManager> mActive = new HashMap<>();

    private FragmentManagerViewModel mNonConfig;

    void setNonConfig(@NonNull FragmentManagerViewModel nonConfig) {
        mNonConfig = nonConfig;
    }

    FragmentManagerViewModel getNonConfig() {
        return mNonConfig;
    }

    void resetActiveFragments() {
        mActive.clear();
    }

    void restoreAddedFragments(@Nullable List<String> added) {
        mAdded.clear();
        if (added != null) {
            for (String who : added) {
                Fragment f = findActiveFragment(who);
                if (f == null) {
                    throw new IllegalStateException("No instantiated fragment for (" + who + ")");
                }
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "restoreSaveState: added (" + who + "): " + f);
                }
                addFragment(f);
            }
        }
    }

    void makeActive(@NonNull FragmentStateManager newlyActive) {
        Fragment f = newlyActive.getFragment();
        if (containsActiveFragment(f.mWho)) {
            return;
        }
        mActive.put(f.mWho, newlyActive);
        if (f.mRetainInstanceChangedWhileDetached) {
            if (f.mRetainInstance) {
                mNonConfig.addRetainedFragment(f);
            } else {
                mNonConfig.removeRetainedFragment(f);
            }
            f.mRetainInstanceChangedWhileDetached = false;
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Added fragment to active set " + f);
        }
    }

    void addFragment(@NonNull Fragment fragment) {
        if (mAdded.contains(fragment)) {
            throw new IllegalStateException("Fragment already added: " + fragment);
        }
        synchronized (mAdded) {
            mAdded.add(fragment);
        }
        fragment.mAdded = true;
    }

    void dispatchStateChange(int state) {
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                fragmentStateManager.setFragmentManagerState(state);
            }
        }
    }

    void moveToExpectedState() {
        // Must add them in the proper order. mActive fragments may be out of order
        for (Fragment f : mAdded) {
            FragmentStateManager fragmentStateManager = mActive.get(f.mWho);
            if (fragmentStateManager != null) {
                fragmentStateManager.moveToExpectedState();
            }
        }

        // Now iterate through all active fragments. These will include those that are removed
        // and detached.
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                fragmentStateManager.moveToExpectedState();

                Fragment f = fragmentStateManager.getFragment();
                boolean beingRemoved = f.mRemoving && !f.isInBackStack();
                if (beingRemoved) {
                    makeInactive(fragmentStateManager);
                }
            }
        }
    }

    void removeFragment(@NonNull Fragment fragment) {
        synchronized (mAdded) {
            mAdded.remove(fragment);
        }
        fragment.mAdded = false;
    }

    void makeInactive(@NonNull FragmentStateManager newlyInactive) {
        Fragment f = newlyInactive.getFragment();

        if (f.mRetainInstance) {
            mNonConfig.removeRetainedFragment(f);
        }

        // Don't remove yet. That happens in burpActive(). This prevents
        // concurrent modification while iterating over mActive
        FragmentStateManager removedStateManager = mActive.put(f.mWho, null);
        if (removedStateManager == null) {
            // It was already removed, so there's nothing more to do
            return;
        }

        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Removed fragment from active set " + f);
        }
    }

    /**
     * To prevent list modification errors, mActive sets values to null instead of
     * removing them when the Fragment becomes inactive. This cleans up the list at the
     * end of executing the transactions.
     */
    void burpActive() {
        Collection<FragmentStateManager> values = mActive.values();
        // values() provides a view into the map, so removing elements from it
        // removes the relevant pairs in the Map
        values.removeAll(Collections.singleton(null));
    }

    @NonNull
    ArrayList<FragmentState> saveActiveFragments() {
        ArrayList<FragmentState> active = new ArrayList<>(mActive.size());
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                Fragment f = fragmentStateManager.getFragment();

                FragmentState fs = fragmentStateManager.saveState();
                active.add(fs);

                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "Saved state of " + f + ": " + fs.mSavedFragmentState);
                }
            }
        }
        return active;
    }

    @Nullable
    ArrayList<String> saveAddedFragments() {
        synchronized (mAdded) {
            if (mAdded.isEmpty()) {
                return null;
            }
            ArrayList<String> added = new ArrayList<>(mAdded.size());
            for (Fragment f : mAdded) {
                added.add(f.mWho);
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "saveAllState: adding fragment (" + f.mWho
                            + "): " + f);
                }
            }
            return added;
        }
    }

    @NonNull
    List<FragmentStateManager> getActiveFragmentStateManagers() {
        ArrayList<FragmentStateManager> activeFragmentStateManagers = new ArrayList<>();
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                activeFragmentStateManagers.add(fragmentStateManager);
            }
        }
        return activeFragmentStateManagers;
    }

    @NonNull
    List<Fragment> getFragments() {
        if (mAdded.isEmpty()) {
            return Collections.emptyList();
        }
        synchronized (mAdded) {
            return new ArrayList<>(mAdded);
        }
    }

    @NonNull
    List<Fragment> getActiveFragments() {
        ArrayList<Fragment> activeFragments = new ArrayList<>();
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                activeFragments.add(fragmentStateManager.getFragment());
            } else {
                activeFragments.add(null);
            }
        }
        return activeFragments;
    }

    int getActiveFragmentCount() {
        return mActive.size();
    }

    @Nullable
    Fragment findFragmentById(@IdRes int id) {
        // First look through added fragments.
        for (int i = mAdded.size() - 1; i >= 0; i--) {
            Fragment f = mAdded.get(i);
            if (f != null && f.mFragmentId == id) {
                return f;
            }
        }
        // Now for any known fragment.
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                Fragment f = fragmentStateManager.getFragment();
                if (f.mFragmentId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    @Nullable
    Fragment findFragmentByTag(@Nullable String tag) {
        if (tag != null) {
            // First look through added fragments.
            for (int i = mAdded.size() - 1; i >= 0; i--) {
                Fragment f = mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (tag != null) {
            // Now for any known fragment.
            for (FragmentStateManager fragmentStateManager : mActive.values()) {
                if (fragmentStateManager != null) {
                    Fragment f = fragmentStateManager.getFragment();
                    if (tag.equals(f.mTag)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    boolean containsActiveFragment(@NonNull String who) {
        return mActive.get(who) != null;
    }

    @Nullable
    FragmentStateManager getFragmentStateManager(@NonNull String who) {
        return mActive.get(who);
    }

    @Nullable
    Fragment findFragmentByWho(@NonNull String who) {
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                Fragment f = fragmentStateManager.getFragment();
                if ((f = f.findFragmentByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Nullable
    Fragment findActiveFragment(@NonNull String who) {
        FragmentStateManager fragmentStateManager = mActive.get(who);
        if (fragmentStateManager != null) {
            return fragmentStateManager.getFragment();
        }
        return null;
    }

    /**
     * Find a fragment within the fragment's container whose View should be below the passed
     * fragment. {@code null} is returned when the fragment has no View or if there should be
     * no fragment with a View below the given fragment.
     *
     * As an example, if mAdded has two Fragments with Views sharing the same container:
     * FragmentA
     * FragmentB
     *
     * Then, when processing FragmentB, FragmentA will be returned. If, however, FragmentA
     * had no View, null would be returned.
     *
     * @param f The fragment that may be on top of another fragment.
     * @return The fragment with a View under f, if one exists or null if f has no View or
     * there are no fragments with Views in the same container.
     */
    Fragment findFragmentUnder(@NonNull Fragment f) {
        final ViewGroup container = f.mContainer;
        final View view = f.mView;

        if (container == null || view == null) {
            return null;
        }

        final int fragmentIndex = mAdded.indexOf(f);
        for (int i = fragmentIndex - 1; i >= 0; i--) {
            Fragment underFragment = mAdded.get(i);
            if (underFragment.mContainer == container && underFragment.mView != null) {
                // Found the fragment under this one
                return underFragment;
            }
        }
        return null;
    }

    void dump(@NonNull String prefix, @Nullable FileDescriptor fd,
            @NonNull PrintWriter writer, @Nullable String[] args) {
        String innerPrefix = prefix + "    ";

        if (!mActive.isEmpty()) {
            writer.print(prefix);
            writer.print("Active Fragments:");
            for (FragmentStateManager fragmentStateManager : mActive.values()) {
                writer.print(prefix);
                if (fragmentStateManager != null) {
                    Fragment f = fragmentStateManager.getFragment();
                    writer.println(f);
                    f.dump(innerPrefix, fd, writer, args);
                } else {
                    writer.println("null");
                }
            }
        }

        int count = mAdded.size();
        if (count > 0) {
            writer.print(prefix); writer.println("Added Fragments:");
            for (int i = 0; i < count; i++) {
                Fragment f = mAdded.get(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.print(": ");
                writer.println(f.toString());
            }
        }
    }
}
