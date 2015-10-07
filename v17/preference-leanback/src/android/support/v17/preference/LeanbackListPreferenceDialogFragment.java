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

package android.support.v17.preference;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

public class LeanbackListPreferenceDialogFragment extends LeanbackPreferenceDialogFragment {

    public static LeanbackListPreferenceDialogFragment newInstanceSingle(String key) {
        final Bundle args = new Bundle(5);
        args.putString(ARG_KEY, key);

        final LeanbackListPreferenceDialogFragment
                fragment = new LeanbackListPreferenceDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static LeanbackListPreferenceDialogFragment newInstanceMulti(String key) {
        final Bundle args = new Bundle(5);
        args.putString(ARG_KEY, key);

        final LeanbackListPreferenceDialogFragment
                fragment = new LeanbackListPreferenceDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DialogPreference preference = getPreference();
        if (!(preference instanceof ListPreference) &&
                !(preference instanceof MultiSelectListPreference)) {
            throw new IllegalArgumentException("Preference must be a ListPreference or " +
                    "MultiSelectListPreference");
        }
    }

    @Override
    public @Nullable View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.leanback_list_preference_fragment, container,
                false);
        final VerticalGridView verticalGridView =
                (VerticalGridView) view.findViewById(android.R.id.list);

        verticalGridView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE);
        verticalGridView.setFocusScrollStrategy(VerticalGridView.FOCUS_SCROLL_ALIGNED);
        verticalGridView.setAdapter(onCreateAdapter());
        verticalGridView.requestFocus();

        final DialogPreference preference = getPreference();
        final CharSequence title = preference.getDialogTitle();
        if (!TextUtils.isEmpty(title)) {
            final TextView titleView = (TextView) view.findViewById(R.id.decor_title);
            titleView.setText(title);
        }

        final CharSequence message = preference.getDialogMessage();
        if (!TextUtils.isEmpty(message)) {
            final TextView messageView = (TextView) view.findViewById(android.R.id.message);
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(message);
        }

        return view;
    }

    public RecyclerView.Adapter onCreateAdapter() {
        final DialogPreference preference = getPreference();
        if (preference instanceof MultiSelectListPreference) {
            final MultiSelectListPreference pref = (MultiSelectListPreference) preference;
            final CharSequence[] entries = pref.getEntries();
            final CharSequence[] entryValues = pref.getEntryValues();
            final Set<String> initialSelections = pref.getValues();
            return new AdapterMulti(entries, entryValues, initialSelections);
        } else if (preference instanceof ListPreference) {
            final ListPreference pref = (ListPreference) preference;
            final CharSequence[] entries = pref.getEntries();
            final CharSequence[] entryValues = pref.getEntryValues();
            final String initialSelection = pref.getValue();
            return new AdapterSingle(entries, entryValues, initialSelection);
        } else {
            throw new IllegalStateException("Unknown preference type");
        }
    }

    public class AdapterSingle extends RecyclerView.Adapter<ViewHolder>
            implements ViewHolder.OnItemClickListener {

        private final CharSequence[] mEntries;
        private final CharSequence[] mEntryValues;
        private CharSequence mSelectedValue;

        public AdapterSingle(CharSequence[] entries, CharSequence[] entryValues,
                CharSequence selectedValue) {
            mEntries = entries;
            mEntryValues = entryValues;
            mSelectedValue = selectedValue;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View view = inflater.inflate(R.layout.leanback_list_preference_item_single,
                    parent, false);
            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.getWidgetView().setChecked(mEntryValues[position].equals(mSelectedValue));
            holder.getTitleView().setText(mEntries[position]);
        }

        @Override
        public int getItemCount() {
            return mEntries.length;
        }

        @Override
        public void onItemClick(ViewHolder viewHolder) {
            final int index = viewHolder.getAdapterPosition();
            final CharSequence entry = mEntryValues[index];
            final ListPreference preference = (ListPreference) getPreference();
            if (index >= 0) {
                String value = mEntryValues[index].toString();
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                    mSelectedValue = entry;
                }
            }

            getFragmentManager().popBackStack();
            notifyDataSetChanged();
        }
    }

    public class AdapterMulti extends RecyclerView.Adapter<ViewHolder>
            implements ViewHolder.OnItemClickListener {

        private final CharSequence[] mEntries;
        private final CharSequence[] mEntryValues;
        private final Set<String> mSelections;

        public AdapterMulti(CharSequence[] entries, CharSequence[] entryValues,
                Set<String> initialSelections) {
            mEntries = entries;
            mEntryValues = entryValues;
            mSelections = new HashSet<>(initialSelections);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View view = inflater.inflate(R.layout.leanback_list_preference_item_multi, parent,
                    false);
            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.getWidgetView().setChecked(
                    mSelections.contains(mEntryValues[position].toString()));
            holder.getTitleView().setText(mEntries[position]);
        }

        @Override
        public int getItemCount() {
            return mEntries.length;
        }

        @Override
        public void onItemClick(ViewHolder viewHolder) {
            final int index = viewHolder.getAdapterPosition();
            final String entry = mEntryValues[index].toString();
            if (mSelections.contains(entry)) {
                mSelections.remove(entry);
            } else {
                mSelections.add(entry);
            }
            final MultiSelectListPreference multiSelectListPreference
                    = (MultiSelectListPreference) getPreference();
            // Pass copies of the set to callChangeListener and setValues to avoid mutations
            if (multiSelectListPreference.callChangeListener(new HashSet<>(mSelections))) {
                multiSelectListPreference.setValues(new HashSet<>(mSelections));
            } else {
                // Change refused, back it out
                if (mSelections.contains(entry)) {
                    mSelections.remove(entry);
                } else {
                    mSelections.add(entry);
                }
            }

            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public interface OnItemClickListener {
            void onItemClick(ViewHolder viewHolder);
        }

        private final Checkable mWidgetView;
        private final TextView mTitleView;
        private final ViewGroup mContainer;
        private final OnItemClickListener mListener;

        public ViewHolder(@NonNull View view, @NonNull OnItemClickListener listener) {
            super(view);
            mWidgetView = (Checkable) view.findViewById(R.id.button);
            mContainer = (ViewGroup) view.findViewById(R.id.container);
            mTitleView = (TextView) view.findViewById(android.R.id.title);
            mContainer.setOnClickListener(this);
            mListener = listener;
        }

        public Checkable getWidgetView() {
            return mWidgetView;
        }

        public TextView getTitleView() {
            return mTitleView;
        }

        public ViewGroup getContainer() {
            return mContainer;
        }

        @Override
        public void onClick(View v) {
            mListener.onItemClick(this);
        }
    }
}
