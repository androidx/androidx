/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.debugview.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.appsearch.debugview.R;
import androidx.appsearch.debugview.model.SchemaTypeListModel;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.Collections;

/**
 * A fragment for displaying a list of {@link AppSearchSchema} objects.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SchemaTypeListFragment extends Fragment {
    private static final String TAG = "AppSearchSchemaTypeFrag";

    private TextView mLoadingView;
    private TextView mEmptySchemaTypesView;
    private TextView mSchemaVersionView;
    private RecyclerView mSchemaTypeListRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private SchemaTypeListItemAdapter mSchemaTypeListItemAdapter;
    private ListeningExecutorService mExecutor;
    private ListenableFuture<DebugAppSearchManager> mDebugAppSearchManager;
    private AppSearchDebugActivity mAppSearchDebugActivity;

    @Nullable
    protected SchemaTypeListModel mSchemaTypeListModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_schema_type_list, container, /*attachToRoot=*/
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLoadingView = getView().findViewById(R.id.loading_schema_types_text_view);
        mEmptySchemaTypesView = getView().findViewById(R.id.empty_schema_types_text_view);
        mSchemaTypeListRecyclerView = getView().findViewById(R.id.schema_type_list_recycler_view);
        mSchemaVersionView = getView().findViewById(R.id.schema_version_view);

        mAppSearchDebugActivity = (AppSearchDebugActivity) getActivity();
        mExecutor = mAppSearchDebugActivity.getBackgroundExecutor();
        mDebugAppSearchManager = mAppSearchDebugActivity.getDebugAppSearchManager();

        initSchemaTypeListRecyclerView();

        Futures.addCallback(mDebugAppSearchManager,
                new FutureCallback<DebugAppSearchManager>() {
                    @Override
                    public void onSuccess(DebugAppSearchManager debugAppSearchManager) {
                        readSchema(debugAppSearchManager);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Toast.makeText(getContext(),
                                "Failed to initialize AppSearch: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to initialize AppSearch. Verify that the database name "
                                + "has been provided in the intent with key: databaseName", t);
                    }
                }, ContextCompat.getMainExecutor(mAppSearchDebugActivity));
    }

    /**
     * Initializes a {@link SchemaTypeListModel} ViewModel instance and sets observer for updating
     * UI with the schema.
     */
    protected void readSchema(@NonNull DebugAppSearchManager debugAppSearchManager) {
        mSchemaTypeListModel =
                new ViewModelProvider(this,
                        new SchemaTypeListModel.SchemaTypeListModelFactory(mExecutor,
                                debugAppSearchManager)).get(SchemaTypeListModel.class);

        mSchemaTypeListModel.getSchemaTypes().observe(this, schemaTypeList -> {
            mLoadingView.setVisibility(View.GONE);

            if (schemaTypeList.size() == 0) {
                mEmptySchemaTypesView.setVisibility(View.VISIBLE);
                mSchemaTypeListRecyclerView.setVisibility(View.GONE);
            } else {
                mSchemaTypeListItemAdapter.setSchemaTypes(schemaTypeList);
            }
        });

        mSchemaTypeListModel.getSchemaVersion().observe(this, version -> {
            mSchemaVersionView.setText(
                    getString(R.string.appsearch_schema_version, version));
            mSchemaVersionView.setVisibility(View.VISIBLE);
        });
    }

    private void initSchemaTypeListRecyclerView() {
        mLinearLayoutManager = new LinearLayoutManager(mAppSearchDebugActivity);
        mLinearLayoutManager.setOrientation(RecyclerView.VERTICAL);

        mSchemaTypeListItemAdapter = new SchemaTypeListItemAdapter(Collections.emptyList());

        mSchemaTypeListRecyclerView.setAdapter(mSchemaTypeListItemAdapter);
        mSchemaTypeListRecyclerView.setLayoutManager(mLinearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                mAppSearchDebugActivity, mLinearLayoutManager.getOrientation());
        mSchemaTypeListRecyclerView.addItemDecoration(dividerItemDecoration);
    }
}
