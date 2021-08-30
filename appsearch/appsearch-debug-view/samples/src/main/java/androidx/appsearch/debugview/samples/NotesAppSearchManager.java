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

package androidx.appsearch.debugview.samples;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.debugview.samples.model.Note;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.LocalStorage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Manages interactions with AppSearch.
 */
public class NotesAppSearchManager implements Closeable {
    private static final String DB_NAME = "notesDb";
    private static final boolean FORCE_OVERRIDE = true;

    private final Context mContext;
    private final Executor mExecutor;
    private final SettableFuture<AppSearchSession> mAppSearchSessionFuture =
            SettableFuture.create();

    private NotesAppSearchManager(@NonNull Context context, @NonNull Executor executor) {
        mContext = context;
        mExecutor = executor;
    }

    /**
     * Factory for creating a {@link NotesAppSearchManager} instance.
     *
     * <p>This creates and initializes an {@link AppSearchSession}. It also resets existing
     * {@link Note} objects from the index and re-adds the {@link Note} document class to the
     * AppSearch schema.
     *
     * @param executor to run AppSearch operations on.
     */
    @NonNull
    public static ListenableFuture<NotesAppSearchManager> createNotesAppSearchManager(
            @NonNull Context context, @NonNull Executor executor) {
        NotesAppSearchManager notesAppSearchManager = new NotesAppSearchManager(context, executor);
        return Futures.transform(notesAppSearchManager.initialize(),
                unused -> notesAppSearchManager, executor);
    }

    /**
     * Closes the AppSearch session.
     */
    @Override
    public void close() {
        Futures.whenAllSucceed(mAppSearchSessionFuture).call(() -> {
            Futures.getDone(mAppSearchSessionFuture).close();
            return null;
        }, mExecutor);
    }

    /**
     * Inserts {@link Note} documents into the AppSearch database.
     *
     * @param notes list of notes to index in AppSearch.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> insertNotes(
            @NonNull List<Note> notes) {
        try {
            PutDocumentsRequest request = new PutDocumentsRequest.Builder().addDocuments(notes)
                    .build();
            return Futures.transformAsync(mAppSearchSessionFuture,
                    session -> session.put(request), mExecutor);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @NonNull
    private ListenableFuture<Void> initialize() {
        return Futures.transformAsync(createLocalSession(), session -> {
            mAppSearchSessionFuture.set(session);
            return Futures.transformAsync(resetDocuments(),
                    unusedResetResult -> Futures.transform(setSchema(),
                            unusedSetSchemaResult -> null,
                            mExecutor),
                    mExecutor);
        }, mExecutor);
    }

    private ListenableFuture<AppSearchSession> createLocalSession() {
        return LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(mContext, DB_NAME)
                        .build()
        );
    }

    private ListenableFuture<SetSchemaResponse> resetDocuments() {
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().setForceOverride(FORCE_OVERRIDE).build();
        return Futures.transformAsync(mAppSearchSessionFuture,
                session -> session.setSchema(request),
                mExecutor);
    }

    private ListenableFuture<SetSchemaResponse> setSchema() {
        try {
            SetSchemaRequest request = new SetSchemaRequest.Builder().addDocumentClasses(Note.class)
                    .build();
            return Futures.transformAsync(mAppSearchSessionFuture,
                    session -> session.setSchema(request), mExecutor);
        } catch (AppSearchException e) {
            return Futures.immediateFailedFuture(e);
        }
    }
}
