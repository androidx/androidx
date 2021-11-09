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

package androidx.preference;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Preference} that visually wraps preferences collapsed in a {@link PreferenceGroup},
 * and expands those preferences into the group when tapped.
 *
 * @hide
 */
final class ExpandButton extends Preference {
    private long mId;

    ExpandButton(@NonNull Context context, List<Preference> collapsedPreferences, long parentId) {
        super(context);
        initLayout();
        setSummary(collapsedPreferences);
        // Since IDs are unique, using the parentId as a reference ensures that this expand
        // button will have a unique ID and hence transitions will be correctly animated by
        // RecyclerView when there are multiple expand buttons.
        mId = parentId + 1000000;
    }

    private void initLayout() {
        setLayoutResource(R.layout.expand_button);
        setIcon(R.drawable.ic_arrow_down_24dp);
        setTitle(R.string.expand_button_title);
        // Sets a high order so that the expand button will be placed at the bottom of the group
        setOrder(999);
    }

    /*
     * Sets the summary of the expand button to a list containing the titles of the collapsed
     * preferences. If there are any nested groups with titles, only add the group's title
     * and not the titles of the group's children.
     */
    private void setSummary(List<Preference> collapsedPreferences) {
        CharSequence summary = null;
        final List<PreferenceGroup> parents = new ArrayList<>();

        for (Preference preference : collapsedPreferences) {
            final CharSequence title = preference.getTitle();
            if (preference instanceof PreferenceGroup && !TextUtils.isEmpty(title)) {
                parents.add((PreferenceGroup) preference);
            }
            if (parents.contains(preference.getParent())) {
                if (preference instanceof PreferenceGroup) {
                    parents.add((PreferenceGroup) preference);
                }
                continue;
            }
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
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
    }

    @Override
    long getId() {
        return mId;
    }
}
