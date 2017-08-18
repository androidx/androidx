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

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.persistence.room.integration.testapp.database.Customer;
import android.arch.util.paging.PagedListAdapterHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

/**
 * Sample PagedList activity which uses Room.
 */
public class RoomPagedListActivity extends AppCompatActivity implements LifecycleRegistryOwner {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final CustomerViewModel viewModel = ViewModelProviders.of(this)
                .get(CustomerViewModel.class);
        setContentView(R.layout.activity_recycler_view);
        final RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final PagedListCustomerAdapter adapter = new PagedListCustomerAdapter(
                new PagedListAdapterHelper.Builder<Customer>()
                        .setSource(viewModel.getLivePagedList())
                        .setLifecycleOwner(this)
                        .setDiffCallback(Customer.DIFF_CALLBACK));
        recyclerView.setAdapter(adapter);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.insertCustomer();
            }
        });
    }

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }
}
