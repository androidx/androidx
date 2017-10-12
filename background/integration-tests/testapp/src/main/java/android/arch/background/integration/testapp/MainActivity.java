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

package android.arch.background.integration.testapp;

import android.arch.background.integration.testapp.db.TestDatabase;
import android.arch.background.integration.testapp.db.WordCount;
import android.arch.background.workmanager.WorkManager;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * Sample activity.
 */
public class MainActivity extends AppCompatActivity {

    private Button mAnalyzeButton;
    private TextView mResultsView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mAnalyzeButton = findViewById(R.id.analyze);
        mAnalyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkManager.getInstance(MainActivity.this)
                        .enqueue(TextStartupWorker.class)
                        .then(
                                TextMappingWorker.create("advs.txt", "advs_out.txt"),
                                TextMappingWorker.create("case.txt", "case_out.txt"),
                                TextMappingWorker.create("lstb.txt", "lstb_out.txt"),
                                TextMappingWorker.create("mems.txt", "mems_out.txt"),
                                TextMappingWorker.create("retn.txt", "retn_out.txt"))
                        .then(TextReducingWorker.create(
                                "advs_out.txt",
                                "case_out.txt",
                                "lstb_out.txt",
                                "mems_out.txt",
                                "retn_out.txt"));
            }
        });

        mResultsView = findViewById(R.id.results);

        final StringBuilder stringBuilder = new StringBuilder();
        TestDatabase.getInstance(this).getWordCountDao().getWordCounts().observe(
                this,
                new Observer<List<WordCount>>() {
                    @Override
                    public void onChanged(@Nullable List<WordCount> wordCounts) {
                        if (wordCounts == null) {
                            return;
                        }

                        // TODO: not efficient, this should be part of its own LiveData thing.
                        stringBuilder.setLength(0);
                        for (WordCount wc : wordCounts) {
                            stringBuilder
                                    .append(wc.mWord)
                                    .append(" ")
                                    .append(wc.mCount)
                                    .append("\n");
                        }
                        mResultsView.setText(stringBuilder.toString());
                    }
                });
    }
}
