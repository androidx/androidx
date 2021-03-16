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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.appsearch.debugview.R;
import androidx.appsearch.debugview.model.DocumentsModel;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Debug Activity for AppSearch.
 *
 * <p>This activity provides a view of all the documents that have been put into an application's
 * AppSearch database. The database is specified by creating an {@link android.content.Intent}
 * with a {@code String} extra containing key: {@code databaseName} and value: name of AppSearch
 * database.
 *
 * <p>To launch this activity, declare it in the application's manifest:
 * <pre>
 *     <activity android:name="androidx.appsearch.debugview.view.AppSearchDebugActivity" />
 * </pre>
 *
 * <p>Next, create an {@link android.content.Intent} with the {@code databaseName} to view
 * documents for, and start the activity:
 * <pre>
 *     Intent intent = new Intent(this, AppSearchDebugActivity.class);
 *     intent.putExtra("databaseName", DB_NAME);
 *     startActivity(intent);
 * </pre>
 *
 * <p><b>Note:</b> Debugging is currently only compatible with local storage.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AppSearchDebugActivity extends AppCompatActivity {
    private static final String DB_INTENT_KEY = "databaseName";
    private static final String TAG = "AppSearchDebugActivity";

    private RecyclerView mDocumentListRecyclerView;
    private TextView mLoadingView;
    private String mDbName;
    private ListenableFuture<DebugAppSearchManager> mDebugAppSearchManager;
    private ListeningExecutorService mBackgroundExecutor;

    protected int mPrevDocsSize = 0;
    protected boolean mLoadingPage = false;
    protected boolean mAdditionalPages = true;

    @Nullable
    protected DocumentsModel mDocumentsModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appsearchdebug);

        mDocumentListRecyclerView = findViewById(R.id.document_list_recycler_view);
        mLoadingView = findViewById(R.id.loading_text_view);

        mBackgroundExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        mDbName = getIntent().getExtras().getString(DB_INTENT_KEY);
        mDebugAppSearchManager = DebugAppSearchManager.create(
                getApplicationContext(), mBackgroundExecutor, mDbName);

        Futures.addCallback(mDebugAppSearchManager, new FutureCallback<DebugAppSearchManager>() {
            @Override
            public void onSuccess(DebugAppSearchManager debugAppSearchManager) {
                readDocuments(debugAppSearchManager);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Toast.makeText(AppSearchDebugActivity.this,
                        "Failed to initialize AppSearch: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to initialize AppSearch. Verify that the database name has been"
                        + " provided in the intent with key: databaseName", t);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onStop() {
        Futures.whenAllSucceed(mDebugAppSearchManager).call(() -> {
            Futures.getDone(mDebugAppSearchManager).close();
            return null;
        }, mBackgroundExecutor);

        super.onStop();
    }

    /**
     * Initializes a {@link DocumentsModel} ViewModel instance and sets observer for updating UI
     * with document data.
     */
    protected void readDocuments(@NonNull DebugAppSearchManager debugAppSearchManager) {
        mDocumentsModel =
                new ViewModelProvider(this,
                        new DocumentsModel.DocumentsModelFactory(mBackgroundExecutor,
                                debugAppSearchManager)).get(DocumentsModel.class);

        mDocumentsModel.getAllDocumentsSearchResults().observe(this, results -> {
            mLoadingView.setVisibility(View.GONE);
            initDocumentListRecyclerView(results);
        });
    }

    private void initDocumentListRecyclerView(@NonNull SearchResults searchResults) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);

        DocumentListItemAdapter documentListItemAdapter = new DocumentListItemAdapter(
                new ArrayList<>());
        mDocumentListRecyclerView.setAdapter(documentListItemAdapter);

        mDocumentListRecyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                linearLayoutManager.getOrientation());
        mDocumentListRecyclerView.addItemDecoration(dividerItemDecoration);

        mDocumentsModel.addAdditionalResultsPage(searchResults).observe(this, docs -> {
            // Check if there are additional documents still being added.
            if (docs.size() - mPrevDocsSize == 0) {
                mAdditionalPages = false;
                return;
            }
            documentListItemAdapter.setDocuments(docs);
            mPrevDocsSize = docs.size();
            mLoadingPage = false;
        });

        mDocumentListRecyclerView.addOnScrollListener(
                new ScrollListener(linearLayoutManager) {
                    @Override
                    public void loadNextPage() {
                        mLoadingPage = true;
                        mDocumentsModel.addAdditionalResultsPage(searchResults);
                    }

                    @Override
                    public boolean isLoading() {
                        return mLoadingPage;
                    }

                    @Override
                    public boolean hasAdditionalPages() {
                        return mAdditionalPages;
                    }
                });
    }
}
