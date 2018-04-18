/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.android.supportv7.widget.selection.fancy;

import static androidx.core.util.Preconditions.checkArgument;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;

final class FancySelectionDemoAdapter extends RecyclerView.Adapter<FancyHolder> {

    private final ContentUriKeyProvider mKeyProvider;
    private final Context mContext;

    // This should be replaced at "bind" time with a real test that
    // asks SelectionTracker.
    private SelectionTest mSelTest;

    FancySelectionDemoAdapter(Context context) {
        mContext = context;
        mKeyProvider = new ContentUriKeyProvider("cheeses", Cheeses.sCheeseStrings);
        mSelTest = new SelectionTest() {
            @Override
            public boolean isSelected(Uri id) {
                throw new IllegalStateException(
                        "Adapter must be initialized with SelectionTracker.");
            }
        };

        // In the fancy edition of selection support we supply access to stable
        // ids using content URI. Since we can map between position and selection key
        // at will we get fancy dependent functionality like band selection and range support.
        setHasStableIds(false);
    }

    ItemKeyProvider<Uri> getItemKeyProvider() {
        return mKeyProvider;
    }

    // Glue together SelectionTracker and the adapter.
    public void bindSelectionHelper(final SelectionTracker<Uri> selectionTracker) {
        checkArgument(selectionTracker != null);
        mSelTest = new SelectionTest() {
            @Override
            public boolean isSelected(Uri id) {
                return selectionTracker.isSelected(id);
            }
        };
    }

    void loadData() {
        onDataReady();
    }

    private void onDataReady() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return Cheeses.sCheeseStrings.length;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(FancyHolder holder, int position) {
        Uri uri = mKeyProvider.getKey(position);
        holder.update(uri, uri.getLastPathSegment(), mSelTest.isSelected(uri));
    }

    @Override
    public FancyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout layout = inflateLayout(mContext, parent, R.layout.selection_demo_list_item);
        return new FancyHolder(layout);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")  // Convenience to avoid clumsy cast.
    private static <V extends View> V inflateLayout(
            Context context, ViewGroup parent, int layout) {

        return (V) LayoutInflater.from(context).inflate(layout, parent, false);
    }

    private interface SelectionTest {
        boolean isSelected(Uri id);
    }
}
