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

package android.arch.paging.integration.testapp.pagedlist;

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.integration.testapp.Item;
import android.arch.paging.integration.testapp.R;
import android.arch.util.paging.PagedList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

/**
 * Sample PagedList activity.
 */
public class PagedListRecyclerViewActivity extends AppCompatActivity
        implements LifecycleRegistryOwner {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PagedListItemViewModel viewModel = ViewModelProviders.of(this)
                .get(PagedListItemViewModel.class);
        setContentView(R.layout.activity_recycler_view);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        final PagedListItemAdapter adapter = new PagedListItemAdapter();

        // TODO: Create and use PagedListAdapterHelper.builder
        viewModel.getPagedList().observe(this, new Observer<PagedList<Item>>() {
            @Override
            public void onChanged(@Nullable PagedList<Item> itemPagedList) {
                adapter.setPagedList(itemPagedList);
                if (recyclerView.getAdapter() == null && itemPagedList != null) {
                    itemPagedList.triggerInitialLoad(null); // TODO: Persist
                    recyclerView.setAdapter(adapter);
                }
            }
        });
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.invalidateList();
            }
        });
    }

    private LifecycleRegistry  mLifecycleRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }
}
