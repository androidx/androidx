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

package androidx.room.integration.testapp;

import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.integration.testapp.database.Customer;
import androidx.room.integration.testapp.database.LastNameAscCustomerDataSource;

/**
 * Sample adapter which uses a AsyncPagedListDiffer.
 */
class PagedListCustomerAdapter extends PagedListAdapter<Customer, RecyclerView.ViewHolder> {
    private RecyclerView mRecyclerView;
    private boolean mSetObserved;
    private int mScrollToPosition = -1;
    private String mScrollToKey = null;

    PagedListCustomerAdapter() {
        super(Customer.DIFF_CALLBACK);
    }

    void setScrollToPosition(int position) {
        mScrollToPosition = position;
    }

    void setScrollToKey(String key) {
        mScrollToKey = key;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(
                new TextView(parent.getContext())) {};
        holder.itemView.setMinimumHeight(400);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Customer customer = getItem(position);

        if (customer != null) {
            ((TextView) (holder.itemView)).setText(customer.getId() + " " + customer.getLastName());
        } else {
            ((TextView) (holder.itemView)).setText(R.string.loading);
        }
    }

    private static int findKeyInPagedList(@NonNull String key, @NonNull PagedList<Customer> list) {
        for (int i = 0; i < list.size(); i++) {
            @Nullable Customer customer = list.get(i);
            if (customer != null
                    && LastNameAscCustomerDataSource.getKeyStatic(customer).equals(key)) {
                return i;
            }
        }
        return 0; // couldn't find, fall back to 0 - could alternately search with comparator
    }

    @Override
    public void submitList(PagedList<Customer> pagedList) {
        super.submitList(pagedList);

        if (pagedList != null) {
            final boolean firstSet = !mSetObserved;
            mSetObserved = true;

            if (firstSet
                    && mRecyclerView != null
                    && (mScrollToPosition >= 0 || mScrollToKey != null)) {
                int localScrollToPosition;
                if (mScrollToKey != null) {
                    localScrollToPosition = findKeyInPagedList(mScrollToKey, pagedList);
                    mScrollToKey = null;
                } else {
                    // if there's 20 items unloaded items (without placeholders holding the spots)
                    // at the beginning of list, we subtract 20 from saved position
                    localScrollToPosition = mScrollToPosition - pagedList.getPositionOffset();
                }
                mRecyclerView.scrollToPosition(localScrollToPosition);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = null;
    }
}
