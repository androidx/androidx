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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appsearch.app.AppSearchEnvironmentFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.debugview.samples.model.Note;
import androidx.appsearch.debugview.view.AppSearchDebugActivity;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Default Activity for AppSearch Debug View Sample App
 *
 * <p>This activity reads sample data, converts it into {@link Note} objects, and then indexes
 * them into AppSearch.
 *
 * <p>Each sample note's text is added to the list view for display.
 */
public class NotesActivity extends AppCompatActivity {
    private static final String DB_NAME = "notesDb";
    private static final String SAMPLE_NOTES_FILENAME = "sample_notes.json";
    private static final String LONG_TEXT_FILENAME = "book.txt";
    private static final String TAG = "NotesActivity";

    private final SettableFuture<NotesAppSearchManager> mNotesAppSearchManagerFuture =
            SettableFuture.create();
    private TextView mLoadingView;
    private ListeningExecutorService mBackgroundExecutor;
    private List<Note> mSampleNotes;
    private List<PutDocumentsRequest> mSamplePutRequests = new ArrayList<>();
    private Button mSampleNotesButton;
    private Button mPutButton;
    private Button mQueryButton;
    private Button mSetSchemaButton;
    private Button mClearButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        mLoadingView = findViewById(R.id.text_view);

        mBackgroundExecutor = MoreExecutors.listeningDecorator(AppSearchEnvironmentFactory
                .getEnvironmentInstance().createCachedThreadPoolExecutor());

        mNotesAppSearchManagerFuture.setFuture(NotesAppSearchManager.createNotesAppSearchManager(
                getApplicationContext(), mBackgroundExecutor));

        mSampleNotesButton = findViewById(R.id.reload_notes);
        insertSampleNotes();

        mPutButton = findViewById(R.id.put_btn);
        mQueryButton = findViewById(R.id.query_btn);
        mSetSchemaButton = findViewById(R.id.set_schema_btn);
        mClearButton = findViewById(R.id.clear_btn);

        setUpSampleNotesButton();
        setUpPutButton();
        setUpQueryButton();
        setUpSetSchemaButton();
        setUpClearButton();
    }

    private void insertSampleNotes() {
        ListenableFuture<List<Note>> sampleNotesFuture =
                mBackgroundExecutor.submit(() -> loadSampleNotes());

        long startTimeMs = SystemClock.elapsedRealtime();
        ListenableFuture<Void> insertNotesFuture =
                Futures.whenAllSucceed(mNotesAppSearchManagerFuture, sampleNotesFuture).call(
                        () -> {
                            mSampleNotes = Futures.getDone(sampleNotesFuture);
                            Futures.getDone(mNotesAppSearchManagerFuture).insertNotes(
                                    mSampleNotes).get();
                            return null;
                        }, mBackgroundExecutor);

        Futures.addCallback(insertNotesFuture,
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            mSampleNotesButton.setBackgroundColor(Color.GREEN);
                            mLoadingView.append(
                                    "\nDone. Put latency: " + (SystemClock.elapsedRealtime()
                                            - startTimeMs) + "ms");
                        });

                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        runOnUiThread(() -> {
                            mSampleNotesButton.setBackgroundColor(Color.RED);
                            new AlertDialog.Builder(NotesActivity.this)
                                    .setTitle("Reload sample notes failed!")
                                    .setMessage("Failure: " + t.getMessage())
                                    .setPositiveButton("OK",
                                            (dialog, which) -> dialog.dismiss())
                                    .show();
                        });
                        Log.e(TAG, "Failed to insert documents into AppSearch", t);
                    }
                }, ContextCompat.getMainExecutor(this));
    }

    @SuppressWarnings("FutureReturnValueIgnored") // mBackgroundExecutor.submit
    private void setUpSampleNotesButton() {
        mSampleNotesButton.setBackgroundColor(Color.GREEN);
        mSampleNotesButton.setOnClickListener(
                v -> mBackgroundExecutor.submit(() -> {
                    mSampleNotesButton.setBackgroundColor(Color.GRAY);
                    runOnUiThread(() -> mLoadingView.setText("Loading sample notes..."));
                    insertSampleNotes();
                }));
    }

    @SuppressWarnings("FutureReturnValueIgnored") // mBackgroundExecutor.submit
    private void setUpPutButton() {
        mPutButton.setBackgroundColor(Color.GREEN);
        mPutButton.setOnClickListener(v -> mBackgroundExecutor.submit(() -> {
            try {
                mPutButton.setBackgroundColor(Color.GRAY);
                int batchSize = 10;

                // Step 1: Get the put requests
                ListenableFuture<List<PutDocumentsRequest>> putDocsFuture;
                if (!mSamplePutRequests.isEmpty()) {
                    // Already loaded, just wrap it as a completed future
                    putDocsFuture = Futures.immediateFuture(mSamplePutRequests);
                } else {
                    // Load and assign to mSamplePutRequests
                    putDocsFuture = mBackgroundExecutor.submit(() ->
                            loadBookPutRequests(batchSize));
                }

                // Step 2: When both futures are ready, insert the documents
                long startTimeMs = SystemClock.elapsedRealtime();
                ListenableFuture<Void> insertDocsFuture =
                        Futures.whenAllSucceed(mNotesAppSearchManagerFuture, putDocsFuture).call(
                                () -> {
                                    mSamplePutRequests = Futures.getDone(putDocsFuture);
                                    Futures.getDone(mNotesAppSearchManagerFuture)
                                            .insertDocuments(mSamplePutRequests).get();
                                    return null;
                                }, mBackgroundExecutor);

                // Step 3: Add callback
                Futures.addCallback(insertDocsFuture,
                        new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                String successMessage = "Successfully inserted "
                                        + mSamplePutRequests.size() * batchSize
                                        + " documents with latency " + (
                                        SystemClock.elapsedRealtime() - startTimeMs) + "ms.";
                                Log.i(TAG, successMessage);
                                runOnUiThread(() -> {
                                    Toast.makeText(NotesActivity.this, successMessage,
                                            Toast.LENGTH_SHORT).show();
                                    mPutButton.setBackgroundColor(Color.GREEN);
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                runOnUiThread(() -> {
                                    mPutButton.setBackgroundColor(Color.RED);
                                    new AlertDialog.Builder(NotesActivity.this)
                                            .setTitle("Put documents failed!")
                                            .setMessage("Failure: " + t.getMessage())
                                            .setPositiveButton("OK",
                                                    (dialog, which) -> dialog.dismiss())
                                            .show();
                                });
                                Log.e(TAG, "Failed to insert documents into AppSearch", t);
                            }
                        }, ContextCompat.getMainExecutor(this));

            } catch (Exception e) {
                Log.e(TAG, "Put button failed", e);
            }
        }));
    }

    @SuppressWarnings("FutureReturnValueIgnored") // mBackgroundExecutor.submit
    private void setUpQueryButton() {
        mQueryButton.setBackgroundColor(Color.GREEN);
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultCountPerPage(10)
                .build();
        mQueryButton.setOnClickListener(v -> mBackgroundExecutor.submit(() -> {
            try {
                mQueryButton.setBackgroundColor(Color.GRAY);
                long startTimeMs = SystemClock.elapsedRealtime();
                ListenableFuture<List<SearchResult>> searchFuture =
                        Futures.whenAllSucceed(mNotesAppSearchManagerFuture).call(
                                () -> Futures.getDone(mNotesAppSearchManagerFuture)
                                        .search("great", searchSpec).get(),
                                mBackgroundExecutor);

                Futures.addCallback(searchFuture,
                        new FutureCallback<List<SearchResult>>() {
                            @Override
                            public void onSuccess(List<SearchResult> results) {
                                StringBuilder messageBuilder = new StringBuilder();
                                messageBuilder.append("Got ")
                                        .append(results.size())
                                        .append(" results in ")
                                        .append(SystemClock.elapsedRealtime() - startTimeMs)
                                        .append("ms.\n");
                                buildResultMessage(results, messageBuilder);
                                Log.i(TAG, messageBuilder.toString());

                                runOnUiThread(() -> {
                                    new AlertDialog.Builder(NotesActivity.this)
                                            .setTitle("Search Results")
                                            .setMessage(messageBuilder)
                                            .setPositiveButton("OK",
                                                    (dialog, which) -> dialog.dismiss())
                                            .show();
                                    mQueryButton.setBackgroundColor(Color.GREEN);
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                runOnUiThread(() -> {
                                    mQueryButton.setBackgroundColor(Color.RED);
                                    new AlertDialog.Builder(NotesActivity.this)
                                            .setTitle("Search failed!")
                                            .setMessage("Failure: " + t.getMessage())
                                            .setPositiveButton("OK",
                                                    (dialog, which) -> dialog.dismiss())
                                            .show();
                                });
                                Log.e(TAG, "Query failed", t);
                            }
                        }, ContextCompat.getMainExecutor(this));
            } catch (Exception e) {
                Log.e(TAG, "Query button failed", e);
            }
        }));
    }

    @SuppressWarnings("FutureReturnValueIgnored") // mBackgroundExecutor.submit
    private void setUpSetSchemaButton() {
        mSetSchemaButton.setBackgroundColor(Color.GREEN);
        mSetSchemaButton.setOnClickListener(v -> mBackgroundExecutor.submit(() -> {
            try {
                mSetSchemaButton.setBackgroundColor(Color.GRAY);

                long startTimeMs = SystemClock.elapsedRealtime();
                ListenableFuture<SetSchemaResponse> setSchemaFuture =
                        Futures.whenAllSucceed(mNotesAppSearchManagerFuture).call(
                                () -> Futures.getDone(mNotesAppSearchManagerFuture)
                                        .setSchema().get(), mBackgroundExecutor);

                Futures.addCallback(setSchemaFuture,
                        new FutureCallback<SetSchemaResponse>() {
                            @Override
                            public void onSuccess(SetSchemaResponse response) {
                                String successMessage = "Successfully set schema with latency "
                                        + (SystemClock.elapsedRealtime() - startTimeMs)
                                        + "ms.";
                                Log.i(TAG, successMessage);
                                runOnUiThread(() -> {
                                    Toast.makeText(NotesActivity.this, successMessage,
                                            Toast.LENGTH_SHORT).show();
                                    mSetSchemaButton.setBackgroundColor(Color.GREEN);
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                runOnUiThread(() -> {
                                    mSetSchemaButton.setBackgroundColor(Color.RED);
                                    new AlertDialog.Builder(NotesActivity.this)
                                            .setTitle("Set schema failed!")
                                            .setMessage("Failure:" + t.getMessage())
                                            .setPositiveButton("OK",
                                                    (dialog, which) -> dialog.dismiss())
                                            .show();
                                });
                                Log.e(TAG, "Set schema failed", t);
                            }
                        }, ContextCompat.getMainExecutor(this));
            } catch (Exception e) {
                Log.e(TAG, "Set schema button failed", e);
            }
        }));
    }

    @SuppressWarnings("FutureReturnValueIgnored") // mBackgroundExecutor.submit
    private void setUpClearButton() {
        mClearButton.setBackgroundColor(Color.GREEN);
        mClearButton.setOnClickListener(v -> mBackgroundExecutor.submit(() -> {
            try {
                mClearButton.setBackgroundColor(Color.GRAY);
                ListenableFuture<SetSchemaResponse> clearFuture =
                        Futures.whenAllSucceed(mNotesAppSearchManagerFuture).call(
                                () -> Futures.getDone(mNotesAppSearchManagerFuture)
                                        .resetDocuments().get(), mBackgroundExecutor);

                Futures.addCallback(clearFuture,
                        new FutureCallback<SetSchemaResponse>() {
                            @Override
                            public void onSuccess(SetSchemaResponse response) {
                                runOnUiThread(() -> {
                                    Toast.makeText(NotesActivity.this,
                                            "Successfully cleared all documents.",
                                            Toast.LENGTH_SHORT).show();
                                    mClearButton.setBackgroundColor(Color.GREEN);
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                runOnUiThread(() -> {
                                    mClearButton.setBackgroundColor(Color.RED);
                                    new AlertDialog.Builder(NotesActivity.this)
                                            .setTitle("Clear failed!")
                                            .setMessage("Failure:" + t.getMessage())
                                            .setPositiveButton("OK",
                                                    (dialog, which) -> dialog.dismiss())
                                            .show();
                                });
                                Log.e(TAG, "Failed to clear: ", t);
                            }
                        }, ContextCompat.getMainExecutor(this));
            } catch (Exception e) {
                Log.e(TAG, "Clear button failed", e);
            }
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.debug_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.app_search_debug) {
            Intent intent = new Intent(this, AppSearchDebugActivity.class);
            intent.putExtra(AppSearchDebugActivity.DB_INTENT_KEY, DB_NAME);
            intent.putExtra(AppSearchDebugActivity.STORAGE_TYPE_INTENT_KEY,
                    AppSearchDebugActivity.STORAGE_TYPE_PLATFORM);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        Futures.whenAllSucceed(mNotesAppSearchManagerFuture).call(() -> {
            Futures.getDone(mNotesAppSearchManagerFuture).close();
            return null;
        }, mBackgroundExecutor);

        super.onDestroy();
    }

    @WorkerThread
    private List<Note> loadSampleNotes() {
        List<Note> sampleNotes = new ArrayList<>();
        Gson gson = new Gson();
        try (InputStreamReader r = new InputStreamReader(getAssets().open(SAMPLE_NOTES_FILENAME))) {
            JsonObject samplesJson = gson.fromJson(r, JsonObject.class);
            JsonArray sampleJsonArr = samplesJson.getAsJsonArray("data");
            for (int i = 0; i < sampleJsonArr.size(); ++i) {
                JsonObject noteJson = sampleJsonArr.get(i).getAsJsonObject();
                sampleNotes.add(new Note.Builder().setId(noteJson.get("id").getAsString())
                        .setNamespace(noteJson.get("namespace").getAsString())
                        .setText(noteJson.get("noteText").getAsString())
                        .build()
                );
            }
        } catch (IOException e) {
            Toast.makeText(NotesActivity.this, "Failed to load sample notes ",
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Sample notes IO failed: ", e);
        }
        return sampleNotes;
    }

    @WorkerThread
    private List<PutDocumentsRequest> loadBookPutRequests(int batchSize) {
        List<Note> documentBatch = new ArrayList<>();
        List<PutDocumentsRequest> putRequests = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(getAssets().open(LONG_TEXT_FILENAME),
                        StandardCharsets.UTF_8))) {
            List<String> validLines = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//")) {
                    validLines.add(line);
                }
            }

            int docId = 0;
            for (int i = 0; i < validLines.size(); i += 3) {
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < i + 3 && j < validLines.size(); j++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(validLines.get(j));
                }

                String text = "Doc " + docId + ": " + sb;
                Note document = new Note.Builder()
                        .setNamespace("book" + (docId % 10))
                        .setId("line" + docId)
                        .setText(text)
                        .build();
                documentBatch.add(document);
                docId++;

                if (documentBatch.size() == batchSize) {
                    PutDocumentsRequest request = new PutDocumentsRequest.Builder()
                            .addDocuments(documentBatch)
                            .build();
                    putRequests.add(request);
                    documentBatch.clear();
                }
            }
            // Add one last time for the last batch
            if (!documentBatch.isEmpty()) {
                PutDocumentsRequest request = new PutDocumentsRequest.Builder()
                        .addDocuments(documentBatch)
                        .build();
                putRequests.add(request);
            }
        } catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(NotesActivity.this, "Failed to load documents.",
                        Toast.LENGTH_LONG).show();
            });
            Log.e(TAG, "Book notes IO failed: ", e);
        } catch (AppSearchException e) {
            runOnUiThread(() -> {
                Toast.makeText(NotesActivity.this, "AppSearch failed to create documents.",
                        Toast.LENGTH_LONG).show();
            });
            Log.e(TAG, "AppSearch failed to create Book notes ", e);
        }
        return putRequests;
    }

    private void buildResultMessage(List<SearchResult> results, StringBuilder messageBuilder) {
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            try {
                GenericDocument document = GenericDocument.fromDocumentClass(
                        result.getDocument(Note.class));
                messageBuilder.append("Result ").append(i + 1).append(": (");
                messageBuilder.append(document.getNamespace()).append(", ");
                messageBuilder.append(document.getId());
                messageBuilder.append(")\n");

                for (String property : document.getPropertyNames()) {
                    String[] values = document.getPropertyStringArray(property);
                    messageBuilder.append("    ").append(property).append(": [");
                    if (values == null) {
                        continue;
                    }
                    String value = values[0];
                    if (value.length() > 200) {
                        value = value.substring(0, 200) + "...";
                    }
                    messageBuilder.append("\"").append(value).append("\"");
                    messageBuilder.append("]\n");
                }
                messageBuilder.append("\n");
            } catch (AppSearchException e) {
                Log.e(TAG, "AppSearchException encountered when loading result " + i + ": " + e);
            }
        }
    }
}
