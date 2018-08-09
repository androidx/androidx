/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.adapter;

import android.os.Parcelable;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Similar in behavior to {@link FragmentStatePagerAdapter}
 * <p>
 * Lifecycle within {@link RecyclerView}:
 * <ul>
 * <li>{@link RecyclerView.ViewHolder} initially an empty {@link FrameLayout}, serves as a
 * re-usable container for a {@link Fragment} in later stages.
 * <li>{@link RecyclerView.Adapter#onBindViewHolder} we ask for a {@link Fragment} for the
 * position. If we already have the fragment, or have previously saved its state, we use those.
 * <li>{@link RecyclerView.Adapter#onAttachedToWindow} we attach the {@link Fragment} to a
 * container.
 * <li>{@link RecyclerView.Adapter#onViewRecycled} and
 * {@link RecyclerView.Adapter#onFailedToRecycleView} we remove, save state, destroy the
 * {@link Fragment}.
 * </ul>
 */
public class FragmentStateAdapter extends RecyclerView.Adapter<FragmentViewHolder> {
    private final List<Fragment> mFragments = new ArrayList<>();

    private final List<Fragment.SavedState> mSavedStates = new ArrayList<>();
    // TODO: handle current item's menuVisibility userVisibleHint as FragmentStatePagerAdapter

    private final FragmentManager mFragmentManager;
    private final FragmentProvider mFragmentProvider;

    public FragmentStateAdapter(FragmentManager fragmentManager,
            FragmentProvider fragmentProvider) {
        this.mFragmentManager = fragmentManager;
        this.mFragmentProvider = fragmentProvider;
    }

    @NonNull
    @Override
    public FragmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return FragmentViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position) {
        if (ViewCompat.isAttachedToWindow(holder.getContainer())) {
            // this should never happen; if it does, it breaks our assumption that attaching
            // a Fragment can reliably happen inside onViewAttachedToWindow
            throw new IllegalStateException(
                    String.format("View %s unexpectedly attached to a window.",
                            holder.getContainer()));
        }

        holder.mFragment = getFragment(position);
    }

    private Fragment getFragment(int position) {
        Fragment fragment = mFragmentProvider.getItem(position);
        if (mSavedStates.size() > position) {
            Fragment.SavedState savedState = mSavedStates.get(position);
            if (savedState != null) {
                fragment.setInitialSavedState(savedState);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        mFragments.set(position, fragment);
        return fragment;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull FragmentViewHolder holder) {
        if (holder.mFragment.isAdded()) {
            return;
        }
        mFragmentManager.beginTransaction().add(holder.getContainer().getId(),
                holder.mFragment).commitNowAllowingStateLoss();
    }

    @Override
    public int getItemCount() {
        return mFragmentProvider.getCount();
    }

    @Override
    public void onViewRecycled(@NonNull FragmentViewHolder holder) {
        removeFragment(holder);
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull FragmentViewHolder holder) {
        // This happens when a ViewHolder is in a transient state (e.g. during custom
        // animation). We don't have sufficient information on how to clear up what lead to
        // the transient state, so we are throwing away the ViewHolder to stay on the
        // conservative side.
        removeFragment(holder);
        return false; // don't recycle the view
    }

    private void removeFragment(@NonNull FragmentViewHolder holder) {
        removeFragment(holder.mFragment, holder.getAdapterPosition());
        holder.mFragment = null;
    }

    /**
     * Removes a Fragment and commits the operation.
     */
    private void removeFragment(Fragment fragment, int position) {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        removeFragment(fragment, position, fragmentTransaction);
        fragmentTransaction.commitNowAllowingStateLoss();
    }

    /**
     * Adds a remove operation to the transaction, but does not commit.
     */
    private void removeFragment(Fragment fragment, int position,
            @NonNull FragmentTransaction fragmentTransaction) {
        if (fragment == null) {
            return;
        }

        if (fragment.isAdded()) {
            while (mSavedStates.size() <= position) {
                mSavedStates.add(null);
            }
            mSavedStates.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
        }

        mFragments.set(position, null);
        fragmentTransaction.remove(fragment);
    }

    /**
     * Saves adapter state.
     */
    public Parcelable[] saveState() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        for (int i = 0; i < mFragments.size(); i++) {
            removeFragment(mFragments.get(i), i, fragmentTransaction);
        }
        fragmentTransaction.commitNowAllowingStateLoss();
        return mSavedStates.toArray(new Fragment.SavedState[mSavedStates.size()]);
    }

    /**
     * Restores adapter state.
     */
    public void restoreState(@NonNull Parcelable[] savedStates) {
        for (Parcelable savedState : savedStates) {
            mSavedStates.add((Fragment.SavedState) savedState);
        }
    }
}
