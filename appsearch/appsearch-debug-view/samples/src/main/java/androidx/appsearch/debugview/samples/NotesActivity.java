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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appsearch.debugview.samples.model.Note;
import androidx.appsearch.debugview.view.AppSearchDebugActivity;
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

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
    private static final String TAG = "NotesActivity";

    private final SettableFuture<NotesAppSearchManager> mNotesAppSearchManagerFuture =
            SettableFuture.create();
    private ArrayAdapter<Note> mNotesAdapter;
    private ListView mListView;
    private TextView mLoadingView;
    private ListeningExecutorService mBackgroundExecutor;
    private List<Note> mSampleNotes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        mListView = findViewById(R.id.list_view);
        mLoadingView = findViewById(R.id.text_view);

        mBackgroundExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        mNotesAppSearchManagerFuture.setFuture(NotesAppSearchManager.createNotesAppSearchManager(
                getApplicationContext(), mBackgroundExecutor));
        ListenableFuture<List<Note>> sampleNotesFuture =
                mBackgroundExecutor.submit(() -> loadSampleNotes());

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
                        displayNotes();
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Toast.makeText(NotesActivity.this, "Failed to insert notes "
                                + "into AppSearch.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to insert notes into AppSearch.", t);
                    }
                }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.debug_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.app_search_debug):
                Intent intent = new Intent(this, AppSearchDebugActivity.class);
                intent.putExtra(AppSearchDebugActivity.DB_INTENT_KEY, DB_NAME);
                intent.putExtra(AppSearchDebugActivity.STORAGE_TYPE_INTENT_KEY,
                        AppSearchDebugActivity.STORAGE_TYPE_LOCAL);
                startActivity(intent);
                return true;
        }

        return false;
    }

    @Override
    protected void onStop() {
        Futures.whenAllSucceed(mNotesAppSearchManagerFuture).call(() -> {
            Futures.getDone(mNotesAppSearchManagerFuture).close();
            return null;
        }, mBackgroundExecutor);

        super.onStop();
    }

    @WorkerThread
    private List<Note> loadSampleNotes() {
        List<Note> sampleNotes = new ArrayList<>();
        Gson gson = new Gson();
        try (InputStreamReader r = new InputStreamReader(
                getAssets().open(SAMPLE_NOTES_FILENAME))) {
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

    private void displayNotes() {
        mNotesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mSampleNotes);
        mListView.setAdapter(mNotesAdapter);

        mLoadingView.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
    }
}
