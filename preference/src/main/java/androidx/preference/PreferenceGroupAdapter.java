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

package androidx.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter that connects a RecyclerView to the {@link Preference} objects contained in the
 * associated {@link PreferenceGroup}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class PreferenceGroupAdapter extends RecyclerView.Adapter<PreferenceViewHolder>
        implements Preference.OnPreferenceChangeInternalListener,
        PreferenceGroup.PreferencePositionCallback {

    /**
     * The group that we are providing data from.
     */
    private PreferenceGroup mPreferenceGroup;

    /**
     * Maps a position into this adapter -> {@link Preference}. These
     * {@link Preference}s don't have to be direct children of this
     * {@link PreferenceGroup}, they can be grand children or younger)
     */
    private List<Preference> mPreferenceList;

    /**
     * Contains a sorted list of all preferences in this adapter regardless of visibility. This is
     * used to construct {@link #mPreferenceList}
     */
    private List<Preference> mPreferenceListInternal;

    /**
     * List of unique Preference and its subclasses' names and layouts.
     */
    private List<PreferenceLayout> mPreferenceLayouts;


    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();

    private Handler mHandler;

    private CollapsiblePreferenceGroupController mPreferenceGroupController;

    private Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            syncMyPreferences();
        }
    };

    private static class PreferenceLayout {
        private int mResId;
        private int mWidgetResId;
        private String mName;

        PreferenceLayout() {}

        PreferenceLayout(PreferenceLayout other) {
            mResId = other.mResId;
            mWidgetResId = other.mWidgetResId;
            mName = other.mName;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PreferenceLayout)) {
                return false;
            }
            final PreferenceLayout other = (PreferenceLayout) o;
            return mResId == other.mResId
                    && mWidgetResId == other.mWidgetResId
                    && TextUtils.equals(mName, other.mName);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + mResId;
            result = 31 * result + mWidgetResId;
            result = 31 * result + mName.hashCode();
            return result;
        }
    }

    public PreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        this(preferenceGroup, new Handler());
    }

    private PreferenceGroupAdapter(PreferenceGroup preferenceGroup, Handler handler) {
        mPreferenceGroup = preferenceGroup;
        mHandler = handler;
        mPreferenceGroupController =
                new CollapsiblePreferenceGroupController(preferenceGroup, this);
        // If this group gets or loses any children, let us know
        mPreferenceGroup.setOnPreferenceChangeInternalListener(this);

        mPreferenceList = new ArrayList<>();
        mPreferenceListInternal = new ArrayList<>();
        mPreferenceLayouts = new ArrayList<>();

        if (mPreferenceGroup instanceof PreferenceScreen) {
            setHasStableIds(((PreferenceScreen) mPreferenceGroup).shouldUseGeneratedIds());
        } else {
            setHasStableIds(true);
        }

        syncMyPreferences();
    }

    @VisibleForTesting
    static PreferenceGroupAdapter createInstanceWithCustomHandler(PreferenceGroup preferenceGroup,
            Handler handler) {
        return new PreferenceGroupAdapter(preferenceGroup, handler);
    }

    private void syncMyPreferences() {
        for (final Preference preference : mPreferenceListInternal) {
            // Clear out the listeners in anticipation of some items being removed. This listener
            // will be (re-)added to the remaining prefs when we flatten.
            preference.setOnPreferenceChangeInternalListener(null);
        }
        final List<Preference> fullPreferenceList = new ArrayList<>(mPreferenceListInternal.size());
        flattenPreferenceGroup(fullPreferenceList, mPreferenceGroup);

        final List<Preference> visiblePreferenceList =
                mPreferenceGroupController.createVisiblePreferencesList(mPreferenceGroup);

        final List<Preference> oldVisibleList = mPreferenceList;
        mPreferenceList = visiblePreferenceList;
        mPreferenceListInternal = fullPreferenceList;

        final PreferenceManager preferenceManager = mPreferenceGroup.getPreferenceManager();
        if (preferenceManager != null
                && preferenceManager.getPreferenceComparisonCallback() != null) {
            final PreferenceManager.PreferenceComparisonCallback comparisonCallback =
                    preferenceManager.getPreferenceComparisonCallback();
            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldVisibleList.size();
                }

                @Override
                public int getNewListSize() {
                    return visiblePreferenceList.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return comparisonCallback.arePreferenceItemsTheSame(
                            oldVisibleList.get(oldItemPosition),
                            visiblePreferenceList.get(newItemPosition));
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return comparisonCallback.arePreferenceContentsTheSame(
                            oldVisibleList.get(oldItemPosition),
                            visiblePreferenceList.get(newItemPosition));
                }
            });

            result.dispatchUpdatesTo(this);
        } else {
            notifyDataSetChanged();
        }

        for (final Preference preference : fullPreferenceList) {
            preference.clearWasDetached();
        }
    }

    private void flattenPreferenceGroup(List<Preference> preferences, PreferenceGroup group) {
        group.sortPreferences();

        final int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            final Preference preference = group.getPreference(i);

            preferences.add(preference);

            addPreferenceClassName(preference);

            if (preference instanceof PreferenceGroup) {
                final PreferenceGroup preferenceAsGroup = (PreferenceGroup) preference;
                if (preferenceAsGroup.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(preferences, preferenceAsGroup);
                }
            }

            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    /**
     * Creates a string that includes the preference name, layout id and widget layout id.
     * If a particular preference type uses 2 different resources, they will be treated as
     * different view types.
     */
    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout in) {
        PreferenceLayout pl = in != null ? in : new PreferenceLayout();
        pl.mName = preference.getClass().getName();
        pl.mResId = preference.getLayoutResource();
        pl.mWidgetResId = preference.getWidgetLayoutResource();
        return pl;
    }

    private void addPreferenceClassName(Preference preference) {
        final PreferenceLayout pl = createPreferenceLayout(preference, null);
        if (!mPreferenceLayouts.contains(pl)) {
            mPreferenceLayouts.add(pl);
        }
    }

    @Override
    public int getItemCount() {
        return mPreferenceList.size();
    }

    public Preference getItem(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return mPreferenceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (!hasStableIds()) {
            return RecyclerView.NO_ID;
        }
        return this.getItem(position).getId();
    }

    @Override
    public void onPreferenceChange(Preference preference) {
        final int index = mPreferenceList.indexOf(preference);
        // If we don't find the preference, we don't need to notify anyone
        if (index != -1) {
            // Send the pref object as a placeholder to ensure the view holder is recycled in place
            notifyItemChanged(index, preference);
        }
    }

    @Override
    public void onPreferenceHierarchyChange(Preference preference) {
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public void onPreferenceVisibilityChange(Preference preference) {
        if (!mPreferenceListInternal.contains(preference)) {
            return;
        }
        if (mPreferenceGroupController.onPreferenceVisibilityChange(preference)) {
            return;
        }
        if (preference.isVisible()) {
            // The preference has become visible, we need to add it in the correct location.

            // Index (inferred) in mPreferenceList of the item preceding the newly visible pref
            int previousVisibleIndex = -1;
            for (final Preference pref : mPreferenceListInternal) {
                if (preference.equals(pref)) {
                    break;
                }
                if (pref.isVisible()) {
                    previousVisibleIndex++;
                }
            }
            // Insert this preference into the active list just after the previous visible entry
            mPreferenceList.add(previousVisibleIndex + 1, preference);

            notifyItemInserted(previousVisibleIndex + 1);
        } else {
            // The preference has become invisible. Find it in the list and remove it.

            int removalIndex;
            final int listSize = mPreferenceList.size();
            for (removalIndex = 0; removalIndex < listSize; removalIndex++) {
                if (preference.equals(mPreferenceList.get(removalIndex))) {
                    break;
                }
            }
            mPreferenceList.remove(removalIndex);
            notifyItemRemoved(removalIndex);
        }
    }

    @Override
    public int getItemViewType(int position) {
        final Preference preference = this.getItem(position);

        mTempPreferenceLayout = createPreferenceLayout(preference, mTempPreferenceLayout);

        int viewType = mPreferenceLayouts.indexOf(mTempPreferenceLayout);
        if (viewType != -1) {
            return viewType;
        } else {
            viewType = mPreferenceLayouts.size();
            mPreferenceLayouts.add(new PreferenceLayout(mTempPreferenceLayout));
            return viewType;
        }
    }

    @Override
    public PreferenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final PreferenceLayout pl = mPreferenceLayouts.get(viewType);
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TypedArray a
                = parent.getContext().obtainStyledAttributes(null, R.styleable.BackgroundStyle);
        Drawable background
                = a.getDrawable(R.styleable.BackgroundStyle_android_selectableItemBackground);
        if (background == null) {
            background = ContextCompat.getDrawable(parent.getContext(),
                    android.R.drawable.list_selector_background);
        }
        a.recycle();

        final View view = inflater.inflate(pl.mResId, parent, false);
        if (view.getBackground() == null) {
            ViewCompat.setBackground(view, background);
        }

        final ViewGroup widgetFrame = (ViewGroup) view.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            if (pl.mWidgetResId != 0) {
                inflater.inflate(pl.mWidgetResId, widgetFrame);
            } else {
                widgetFrame.setVisibility(View.GONE);
            }
        }

        return new PreferenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        final Preference preference = getItem(position);
        preference.onBindViewHolder(holder);
    }

    @Override
    public int getPreferenceAdapterPosition(String key) {
        final int size = mPreferenceList.size();
        for (int i = 0; i < size; i++) {
            final Preference candidate = mPreferenceList.get(i);
            if (TextUtils.equals(key, candidate.getKey())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public int getPreferenceAdapterPosition(Preference preference) {
        final int size = mPreferenceList.size();
        for (int i = 0; i < size; i++) {
            final Preference candidate = mPreferenceList.get(i);
            if (candidate != null && candidate.equals(preference)) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }
}
