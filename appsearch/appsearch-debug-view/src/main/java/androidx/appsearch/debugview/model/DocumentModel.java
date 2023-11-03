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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.ExecutorService;

/**
 * Document ViewModel for displaying a {@link GenericDocument} object.
 *
 * <p>Instances of the ViewModel are created by {@link DocumentModelFactory}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DocumentModel extends ViewModel {
    private static final String TAG = "AppSearchDocumentModel";

    private final ExecutorService mExecutor;
    private final DebugAppSearchManager mDebugAppSearchManager;
    final MutableLiveData<GenericDocument> mDocumentLiveData = new MutableLiveData<>();

    public DocumentModel(@NonNull ExecutorService executor,
            @NonNull DebugAppSearchManager debugAppSearchManager) {
        mExecutor = Preconditions.checkNotNull(executor);
        mDebugAppSearchManager = Preconditions.checkNotNull(debugAppSearchManager);
    }

    /**
     * Gets a {@link GenericDocument} object by namespace and ID.
     */
    @NonNull
    public LiveData<GenericDocument> getDocument(@NonNull String namespace, @NonNull String id) {
        Futures.addCallback(mDebugAppSearchManager.getDocumentAsync(namespace, id),
                new FutureCallback<GenericDocument>() {
                    @Override
                    public void onSuccess(GenericDocument result) {
                        mDocumentLiveData.postValue(result);
                    }

                    @Override
                    public void onFailure(@Nullable Throwable t) {
                        Log.e(TAG,
                                "Failed to get document with namespace: " + namespace + " and "
                                        + "id: " + id, t);
                    }
                }, mExecutor);
        return mDocumentLiveData;
    }

    /**
     * Factory for creating a {@link DocumentModel} instance.
     */
    public static class DocumentModelFactory extends ViewModelProvider.NewInstanceFactory {
        private final DebugAppSearchManager mDebugAppSearchManager;
        private final ListeningExecutorService mExecutorService;

        public DocumentModelFactory(@NonNull ListeningExecutorService executor,
                @NonNull DebugAppSearchManager debugAppSearchManager) {
            mDebugAppSearchManager = debugAppSearchManager;
            mExecutorService = executor;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass == DocumentModel.class) {
                return (T) new DocumentModel(mExecutorService, mDebugAppSearchManager);
            } else {
                throw new IllegalArgumentException("Expected class: DocumentModel.");
            }
        }
    }
}
