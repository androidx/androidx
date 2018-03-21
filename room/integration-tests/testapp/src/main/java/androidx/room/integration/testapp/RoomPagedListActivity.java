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

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.integration.testapp.database.Customer;
import androidx.room.integration.testapp.database.LastNameAscCustomerDataSource;

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
        livePagedList.observe(this, items -> mAdapter.submitList(items));
        final Button button = findViewById(R.id.addButton);
        button.setOnClickListener(v -> viewModel.insertCustomer());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
}
