/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.disposables.CompositeDisposable;

/**
 * Sample {@code Flowable<PagedList>} activity which uses Room.
 */
public class RoomPagedListRxActivity extends AppCompatActivity {

    private PagedListCustomerAdapter mAdapter;

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private CustomerViewModel mViewModel;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        mViewModel = ViewModelProviders.of(this)
                .get(CustomerViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        mAdapter = new PagedListCustomerAdapter();
        recyclerView.setAdapter(mAdapter);

        final Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> mViewModel.insertCustomer());

        final Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v -> mViewModel.clearAllCustomers());
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDisposable.add(mViewModel.getPagedListFlowable()
                .subscribe(list -> mAdapter.submitList(list)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposable.clear();
    }
}
