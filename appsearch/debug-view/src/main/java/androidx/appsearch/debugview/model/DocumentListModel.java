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

package androidx.appsearch.debugview.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Documents ViewModel for the database's {@link GenericDocument} objects.
 *
 * <p>This model captures the data for displaying lists of {@link GenericDocument} objects. Each
 * {@link GenericDocument} object is truncated of all properties.
 *
 * <p>Instances of {@link DocumentListModel} are created by {@link DocumentListModelFactory}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DocumentListModel extends ViewModel {
    private static final String TAG = "DocumentListModel";

    private final ExecutorService mExecutor;
    private final DebugAppSearchManager mDebugAppSearchManager;
    final MutableLiveData<List<GenericDocument>> mDocumentsLiveData =
            new MutableLiveData<>();
    final MutableLiveData<SearchResults> mDocumentsSearchResultsLiveData =
            new MutableLiveData<>();

    public DocumentListModel(@NonNull ExecutorService executor,
            @NonNull DebugAppSearchManager debugAppSearchManager) {
        mExecutor = Preconditions.checkNotNull(executor);
        mDebugAppSearchManager = Preconditions.checkNotNull(debugAppSearchManager);
    }

    /**
     * Gets the {@link SearchResults} instance for a search over all documents in the AppSearch
     * database.
     *
     * <p>Call {@link #addAdditionalResultsPage} to get the next page of documents from the
     * {@link SearchResults} instance.
     */
    @NonNull
    public LiveData<SearchResults> getAllDocumentsSearchResults() {
        Futures.addCallback(mDebugAppSearchManager.getAllDocumentsSearchResults(),
                new FutureCallback<SearchResults>() {
                    @Override
                    public void onSuccess(SearchResults result) {
                        mDocumentsSearchResultsLiveData.postValue(result);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.e(TAG, "Failed to get all documents.", t);
                    }
                }, mExecutor);
        return mDocumentsSearchResultsLiveData;
    }

    /**
     * Adds the next page of documents for the provided {@link SearchResults} instance to the
     * running list of retrieved {@link GenericDocument} objects.
     *
     * <p>Each page is represented as a list of {@link GenericDocument} objects.
     *
     * @return a {@link LiveData} encapsulating the list of {@link GenericDocument} objects for
     * documents retrieved from all previous pages and this additional page.
     */
    @NonNull
    public LiveData<List<GenericDocument>> addAdditionalResultsPage(
            @NonNull SearchResults results) {
        Futures.addCallback(mDebugAppSearchManager.getNextPage(results),
                new FutureCallback<List<GenericDocument>>() {
                    @Override
                    public void onSuccess(List<GenericDocument> result) {
                        if (mDocumentsLiveData.getValue() == null) {
                            mDocumentsLiveData.postValue(result);
                        } else {
                            mDocumentsLiveData.getValue().addAll(result);
                            mDocumentsLiveData.postValue(mDocumentsLiveData.getValue());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.e(TAG, "Failed to get next page of documents.", t);
                    }
                }, mExecutor);

        return mDocumentsLiveData;
    }

    /**
     * Factory for creating a {@link DocumentListModel} instance.
     */
    public static class DocumentListModelFactory extends ViewModelProvider.NewInstanceFactory {
        private final DebugAppSearchManager mDebugAppSearchManager;
        private final ListeningExecutorService mExecutorService;

        public DocumentListModelFactory(@NonNull ListeningExecutorService executor,
                @NonNull DebugAppSearchManager debugAppSearchManager) {
            mDebugAppSearchManager = debugAppSearchManager;
            mExecutorService = executor;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass == DocumentListModel.class) {
                return (T) new DocumentListModel(mExecutorService, mDebugAppSearchManager);
            } else {
                throw new IllegalArgumentException("Expected class: DocumentListModel.");
            }
        }
    }
}
