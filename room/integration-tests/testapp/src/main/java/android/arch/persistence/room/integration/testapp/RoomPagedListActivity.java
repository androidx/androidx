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
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.arch.persistence.room.integration.testapp.database.Customer;
import android.arch.persistence.room.integration.testapp.database.LastNameAscCustomerDataSource;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

/**
 * Sample PagedList activity which uses Room.
 */
public class RoomPagedListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private PagedListCustomerAdapter mAdapter;

    private static final String STRING_KEY = "STRING_KEY";
    private static final String INT_KEY = "INT_KEY";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        final CustomerViewModel viewModel = ViewModelProviders.of(this)
                .get(CustomerViewModel.class);

        mRecyclerView = findViewById(R.id.recyclerview);
        mAdapter = new PagedListCustomerAdapter();
        mRecyclerView.setAdapter(mAdapter);

        LiveData<PagedList<Customer>> livePagedList;
        if (useKeyedQuery()) {
            String key = null;
            if (savedInstanceState != null) {
                key = savedInstanceState.getString(STRING_KEY);
                mAdapter.setScrollToKey(key);
            }
            livePagedList = viewModel.getLivePagedList(key);
        } else {
            int position = 0;
            if (savedInstanceState != null) {
                position = savedInstanceState.getInt(INT_KEY);
                mAdapter.setScrollToPosition(position);
            }
            livePagedList = viewModel.getLivePagedList(position);
        }
        livePagedList.observe(this, new Observer<PagedList<Customer>>() {
            @Override
            public void onChanged(@Nullable PagedList<Customer> items) {
                mAdapter.setList(items);
            }
        });
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.insertCustomer();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        PagedList<Customer> list = mAdapter.getCurrentList();
        if (list == null) {
            // Can't find anything to restore
            return;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        final int targetPosition = layoutManager.findFirstVisibleItemPosition();

        if (useKeyedQuery()) {
            Customer customer = list.get(targetPosition);
            if (customer != null) {
                String key = LastNameAscCustomerDataSource.getKeyStatic(customer);
                outState.putString(STRING_KEY, key);
            }
        } else {
            // NOTE: in the general case, we can't just rely on RecyclerView/LinearLayoutManager to
            // preserve position, because of position offset which is present when using an
            // uncounted, non-keyed source).
            int absolutePosition = targetPosition + list.getPositionOffset();
            outState.putInt(INT_KEY, absolutePosition);
        }
    }

    protected boolean useKeyedQuery() {
        return false;
    }

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }
}
