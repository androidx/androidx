/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.preference;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter that connects a RecyclerView to the {@link Preference} objects contained in the
 * associated {@link PreferenceGroup}.
 *
 * @hide
 */
public class PreferenceGroupAdapter extends RecyclerView.Adapter<PreferenceViewHolder>
        implements Preference.OnPreferenceChangeInternalListener {

    private static final String TAG = "PreferenceGroupAdapter";

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

    private volatile boolean mIsSyncing = false;

    private Handler mHandler = new Handler();

    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            syncMyPreferences();
        }
    };

    private static class PreferenceLayout {
        private int resId;
        private int widgetResId;
        private String name;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PreferenceLayout)) {
                return false;
            }
            final PreferenceLayout other = (PreferenceLayout) o;
            return resId == other.resId
                    && widgetResId == other.widgetResId
                    && TextUtils.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + resId;
            result = 31 * result + widgetResId;
            result = 31 * result + name.hashCode();
            return result;
        }
    }

    public PreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
        // If this group gets or loses any children, let us know
        mPreferenceGroup.setOnPreferenceChangeInternalListener(this);

        mPreferenceList = new ArrayList<>();
        mPreferenceListInternal = new ArrayList<>();
        mPreferenceLayouts = new ArrayList<>();

        setHasStableIds(true);

        syncMyPreferences();
    }

    private void syncMyPreferences() {
        synchronized(this) {
            if (mIsSyncing) {
                return;
            }

            mIsSyncing = true;
        }

        List<Preference> newPreferenceList = new ArrayList<>(mPreferenceListInternal.size());
        flattenPreferenceGroup(newPreferenceList, mPreferenceGroup);
        mPreferenceListInternal = newPreferenceList;

        mPreferenceList = new ArrayList<>(mPreferenceListInternal.size());
        // Copy only the visible preferences to the active list
        for (final Preference preference : mPreferenceListInternal) {
            if (preference.isVisible()) {
                mPreferenceList.add(preference);
            }
        }

        notifyDataSetChanged();

        synchronized(this) {
            mIsSyncing = false;
            notifyAll();
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
        PreferenceLayout pl = in != null? in : new PreferenceLayout();
        pl.name = preference.getClass().getName();
        pl.resId = preference.getLayoutResource();
        pl.widgetResId = preference.getWidgetLayoutResource();
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

    public long getItemId(int position) {
        if (position < 0 || position >= getItemCount()) return ListView.INVALID_ROW_ID;
        return this.getItem(position).getId();
    }

    public void onPreferenceChange(Preference preference) {
        notifyDataSetChanged();
    }

    public void onPreferenceHierarchyChange(Preference preference) {
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public void onPreferenceVisibilityChange(Preference preference) {
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
            // The preference has become invisibile. Find it in the list and remove it.

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

        return mPreferenceLayouts.indexOf(mTempPreferenceLayout);
    }

    @Override
    public PreferenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final PreferenceLayout pl = mPreferenceLayouts.get(viewType);
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final ViewGroup view = (ViewGroup) inflater.inflate(pl.resId, parent, false);

        final ViewGroup widgetFrame = (ViewGroup) view.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            if (pl.widgetResId != 0) {
                inflater.inflate(pl.widgetResId, widgetFrame);
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
}
