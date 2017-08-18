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

package android.arch.persistence.room.integration.testapp;

import android.arch.persistence.room.integration.testapp.database.Customer;
import android.arch.util.paging.LazyListAdapterHelper;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

import io.reactivex.annotations.NonNull;

/**
 * Sample adapter which uses a LazyListAdapterHelper.
 */
public class LazyListCustomerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final LazyListAdapterHelper<Customer> mHelper;

    public LazyListCustomerAdapter(@NonNull LazyListAdapterHelper.Builder<Customer> builder) {
        this.mHelper = builder.adapter(this).create();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(new TextView(parent.getContext())) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Customer customer = mHelper.get(position);
        ((TextView) (holder.itemView)).setText(customer == null ? "loading" : customer.getLastName());
        holder.itemView.setMinimumHeight(400);
    }

    @Override
    public int getItemCount() {
        return mHelper.getItemCount();
    }
}
