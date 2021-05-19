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
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Schema Type List ViewModel for the database schema's.
 *
 * <p>This model captures the data for displaying a list of {@link AppSearchSchema} objects that
 * compose of the schema. This also captures the overall schema version.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SchemaTypeListModel extends ViewModel {
    private static final String TAG = "AppSearchSchemaTypeList";

    private final ExecutorService mExecutor;
    private final DebugAppSearchManager mDebugAppSearchManager;
    final MutableLiveData<GetSchemaResponse> mSchemaResponseMutableLiveData =
            new MutableLiveData<>();

    public SchemaTypeListModel(@NonNull ExecutorService executor,
            @NonNull DebugAppSearchManager debugAppSearchManager) {
        mExecutor = Preconditions.checkNotNull(executor);
        mDebugAppSearchManager = Preconditions.checkNotNull(debugAppSearchManager);
    }

    /**
     * Gets list of {@link AppSearchSchema} objects that compose of the schema.
     *
     * @return live data of list of {@link AppSearchSchema} objects.
     */
    @NonNull
    public LiveData<List<AppSearchSchema>> getSchemaTypes() {
        return Transformations.map(getSchema(),
                input -> new ArrayList<>(input.getSchemas()));
    }

    /**
     * Gets overall schema version.
     *
     * @return live data of {@link Integer} representing the overall schema version.
     */
    @NonNull
    public LiveData<Integer> getSchemaVersion() {
        return Transformations.map(getSchema(), GetSchemaResponse::getVersion);
    }

    /**
     * Gets schema of database.
     *
     * @return live data of {@link GetSchemaResponse}
     */
    @NonNull
    private LiveData<GetSchemaResponse> getSchema() {
        Futures.addCallback(mDebugAppSearchManager.getSchema(),
                new FutureCallback<GetSchemaResponse>() {
                    @Override
                    public void onSuccess(GetSchemaResponse result) {
                        mSchemaResponseMutableLiveData.postValue(result);
                    }

                    @Override
                    public void onFailure(@Nullable Throwable t) {
                        Log.e(TAG, "Failed to get schema.", t);
                    }
                }, mExecutor);
        return mSchemaResponseMutableLiveData;
    }

    /**
     * Factory for creating a {@link SchemaTypeListModel} instance.
     */
    public static class SchemaTypeListModelFactory extends ViewModelProvider.NewInstanceFactory {
        private final DebugAppSearchManager mDebugAppSearchManager;
        private final ListeningExecutorService mExecutorService;

        public SchemaTypeListModelFactory(@NonNull ListeningExecutorService executor,
                @NonNull DebugAppSearchManager debugAppSearchManager) {
            mDebugAppSearchManager = debugAppSearchManager;
            mExecutorService = executor;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass == SchemaTypeListModel.class) {
                return (T) new SchemaTypeListModel(mExecutorService, mDebugAppSearchManager);
            } else {
                throw new IllegalArgumentException("Expected class: SchemaTypeListModel.");
            }
        }
    }
}
