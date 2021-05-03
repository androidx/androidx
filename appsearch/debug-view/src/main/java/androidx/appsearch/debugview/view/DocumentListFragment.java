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
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.appsearch.debugview.R;
import androidx.appsearch.debugview.model.DocumentListModel;
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
 * A fragment for displaying a list of {@link GenericDocument} objects.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DocumentListFragment extends Fragment {
    private static final String TAG = "DocumentListFragment";

    private TextView mLoadingView;
    private TextView mEmptyDocumentsView;
    private RecyclerView mDocumentListRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private DocumentListItemAdapter mDocumentListItemAdapter;
    private ListeningExecutorService mExecutor;
    private ListenableFuture<DebugAppSearchManager> mDebugAppSearchManager;
    private AppSearchDebugActivity mAppSearchDebugActivity;

    protected boolean mLoadingPage = false;

    @Nullable
    protected DocumentListModel mDocumentListModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_document_list, container, /*attachToRoot=*/
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingView = getView().findViewById(R.id.loading_text_view);
        mEmptyDocumentsView = getView().findViewById(R.id.empty_documents_text_view);
        mDocumentListRecyclerView = getView().findViewById(R.id.document_list_recycler_view);

        mAppSearchDebugActivity = (AppSearchDebugActivity) getActivity();
        mExecutor = mAppSearchDebugActivity.getBackgroundExecutor();
        mDebugAppSearchManager = mAppSearchDebugActivity.getDebugAppSearchManager();

        initDocumentListRecyclerView();

        Futures.addCallback(mDebugAppSearchManager,
                new FutureCallback<DebugAppSearchManager>() {
                    @Override
                    public void onSuccess(DebugAppSearchManager debugAppSearchManager) {
                        readDocuments(debugAppSearchManager);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Toast.makeText(getContext(),
                                "Failed to initialize AppSearch: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG,
                                "Failed to initialize AppSearch. Verify that the database name "
                                        + "has been"
                                        + " provided in the intent with key: databaseName", t);
                    }
                }, ContextCompat.getMainExecutor(mAppSearchDebugActivity));
    }

    /**
     * Initializes a {@link DocumentListModel} ViewModel instance and sets observer for updating UI
     * with document data.
     */
    protected void readDocuments(@NonNull DebugAppSearchManager debugAppSearchManager) {
        mDocumentListModel =
                new ViewModelProvider(this,
                        new DocumentListModel.DocumentListModelFactory(mExecutor,
                                debugAppSearchManager)).get(DocumentListModel.class);

        if (mDocumentListModel.hasAdditionalPages()) {
            mDocumentListModel.getAllDocumentsSearchResults().observe(this, results -> {
                mLoadingView.setVisibility(View.GONE);
                displayNextSearchResultsPage(results);
            });
        } else {
            mLoadingView.setVisibility(View.GONE);
            mDocumentListItemAdapter.setDocuments(mDocumentListModel.getAllLoadedDocuments());
        }
    }

    private void displayNextSearchResultsPage(@NonNull SearchResults searchResults) {
        mDocumentListModel.addAdditionalResultsPage(searchResults).observe(this, docs -> {
            mDocumentListItemAdapter.setDocuments(docs);
            if (docs.size() == 0) {
                mEmptyDocumentsView.setVisibility(View.VISIBLE);
                mDocumentListRecyclerView.setVisibility(View.GONE);
            }
            mLoadingPage = false;
        });

        mDocumentListRecyclerView.addOnScrollListener(
                new ScrollListener(mLinearLayoutManager) {
                    @Override
                    public void loadNextPage() {
                        mLoadingPage = true;
                        mDocumentListModel.addAdditionalResultsPage(searchResults);
                    }

                    @Override
                    public boolean isLoading() {
                        return mLoadingPage;
                    }

                    @Override
                    public boolean hasAdditionalPages() {
                        return mDocumentListModel.hasAdditionalPages();
                    }
                });
    }

    private void initDocumentListRecyclerView() {
        mLinearLayoutManager = new LinearLayoutManager(mAppSearchDebugActivity);
        mLinearLayoutManager.setOrientation(RecyclerView.VERTICAL);

        mDocumentListItemAdapter = new DocumentListItemAdapter(Collections.emptyList(), this);

        mDocumentListRecyclerView.setAdapter(mDocumentListItemAdapter);
        mDocumentListRecyclerView.setLayoutManager(mLinearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                mAppSearchDebugActivity, mLinearLayoutManager.getOrientation());
        mDocumentListRecyclerView.addItemDecoration(dividerItemDecoration);
    }
}
