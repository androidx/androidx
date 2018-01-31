/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v7.preference;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller to handle advanced children display logic with collapsible functionality.
 */
final class CollapsiblePreferenceGroupController
        implements PreferenceGroup.PreferenceInstanceStateCallback {

    private final PreferenceGroupAdapter mPreferenceGroupAdapter;
    private int mMaxPreferenceToShow;
    private final Context mContext;

    CollapsiblePreferenceGroupController(PreferenceGroup preferenceGroup,
            PreferenceGroupAdapter preferenceGroupAdapter) {
        mPreferenceGroupAdapter = preferenceGroupAdapter;
        mMaxPreferenceToShow = preferenceGroup.getInitialExpandedChildrenCount();
        mContext = preferenceGroup.getContext();
        preferenceGroup.setPreferenceInstanceStateCallback(this);
    }

    /**
     * Creates the visible portion of the flattened preferences.
     *
     * @param flattenedPreferenceList the flattened children of the preference group
     * @return the visible portion of the flattened preferences
     */
    public List<Preference> createVisiblePreferencesList(List<Preference> flattenedPreferenceList) {
        int visiblePreferenceCount = 0;
        final List<Preference> visiblePreferenceList =
                new ArrayList<>(flattenedPreferenceList.size());
        // Copy only the visible preferences to the active list up to the maximum specified
        for (final Preference preference : flattenedPreferenceList) {
            if (preference.isVisible()) {
                if (visiblePreferenceCount < mMaxPreferenceToShow) {
                    visiblePreferenceList.add(preference);
                }
                // Do no count PreferenceGroup as expanded preference because the list of its child
                // is already contained in the flattenedPreferenceList
                if (!(preference instanceof PreferenceGroup)) {
                    visiblePreferenceCount++;
                }
            }
        }
        // If there are any visible preferences being hidden, add an expand button to show the rest
        // of the preferences. Clicking the expand button will show all the visible preferences and
        // reset mMaxPreferenceToShow
        if (showLimitedChildren() && visiblePreferenceCount > mMaxPreferenceToShow) {
            final ExpandButton expandButton  = createExpandButton(visiblePreferenceList,
                    flattenedPreferenceList);
            visiblePreferenceList.add(expandButton);
        }
        return visiblePreferenceList;
    }

    /**
     * Called when a preference has changed its visibility.
     *
     * @param preference The preference whose visibility has changed.
     * @return {@code true} if view update has been handled by this controller.
     */
    public boolean onPreferenceVisibilityChange(Preference preference) {
        if (showLimitedChildren()) {
            // We only want to show up to the max number of preferences. Preference visibility
            // change can result in the expand button being added/removed, as well as expand button
            // summary change. Rebulid the data to ensure the correct data is shown.
            mPreferenceGroupAdapter.onPreferenceHierarchyChange(preference);
            return true;
        }
        return false;
    }

    @Override
    public Parcelable saveInstanceState(Parcelable state) {
        final SavedState myState = new SavedState(state);
        myState.mMaxPreferenceToShow = mMaxPreferenceToShow;
        return myState;
    }

    @Override
    public Parcelable restoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in saveInstanceState
            return state;
        }
        SavedState myState = (SavedState) state;
        final int restoredMaxToShow = myState.mMaxPreferenceToShow;
        if (mMaxPreferenceToShow != restoredMaxToShow) {
            mMaxPreferenceToShow = restoredMaxToShow;
            mPreferenceGroupAdapter.onPreferenceHierarchyChange(null);
        }
        return myState.getSuperState();
    }

    private ExpandButton createExpandButton(List<Preference> visiblePreferenceList,
            List<Preference> flattenedPreferenceList) {
        final ExpandButton preference = new ExpandButton(mContext, visiblePreferenceList,
                flattenedPreferenceList);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mMaxPreferenceToShow = Integer.MAX_VALUE;
                mPreferenceGroupAdapter.onPreferenceHierarchyChange(preference);
                return true;
            }
        });
        return preference;
    }

    private boolean showLimitedChildren() {
        return mMaxPreferenceToShow != Integer.MAX_VALUE;
    }

    /**
     * A {@link Preference} that provides capability to expand the collapsed items in the
     * {@link PreferenceGroup}.
     */
    static class ExpandButton extends Preference {
        ExpandButton(Context context, List<Preference> visiblePreferenceList,
                List<Preference> flattenedPreferenceList) {
            super(context);
            initLayout();
            setSummary(visiblePreferenceList, flattenedPreferenceList);
        }

        private void initLayout() {
            setLayoutResource(R.layout.expand_button);
            setIcon(R.drawable.ic_arrow_down_24dp);
            setTitle(R.string.expand_button_title);
            // Sets a high order so that the expand button will be placed at the bottom of the group
            setOrder(999);
        }

        /*
         * The summary of this will be the list of title for collapsed preferences. Iterate through
         * the preferences not in the visible list and add its title to the summary text.
         */
        private void setSummary(List<Preference> visiblePreferenceList,
                List<Preference> flattenedPreferenceList) {
            final Preference lastVisiblePreference =
                    visiblePreferenceList.get(visiblePreferenceList.size() - 1);
            final int collapsedIndex = flattenedPreferenceList.indexOf(lastVisiblePreference) + 1;
            CharSequence summary = null;
            for (int i = collapsedIndex; i < flattenedPreferenceList.size(); i++) {
                final Preference preference = flattenedPreferenceList.get(i);
                if (preference instanceof PreferenceGroup || !preference.isVisible()) {
                    continue;
                }
                final CharSequence title = preference.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    if (summary == null) {
                        summary = title;
                    } else {
                        summary = getContext().getString(
                                R.string.summary_collapsed_preference_list, summary, title);
                    }
                }
            }
            setSummary(summary);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            holder.setDividerAllowedAbove(false);
        }
    }

    /**
     * A class for managing the instance state of a {@link PreferenceGroup}.
     */
    static class SavedState extends Preference.BaseSavedState {
        int mMaxPreferenceToShow;

        SavedState(Parcel source) {
            super(source);
            mMaxPreferenceToShow = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mMaxPreferenceToShow);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
