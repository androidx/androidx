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

package android.arch.background.integration.testapp.sherlockholmes;

import android.arch.background.integration.testapp.R;
import android.arch.background.integration.testapp.db.TestDatabase;
import android.arch.background.integration.testapp.db.WordCount;
import android.arch.background.workmanager.ArrayCreatingInputMerger;
import android.arch.background.workmanager.Constants;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkManager;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * Analyze Sherlock Holmes activity.
 */
public class AnalyzeSherlockHolmesActivity extends AppCompatActivity {
    private Button mAnalyzeButton;
    private ProgressBar mProgressBar;
    private TextView mResultsView;

    final StringBuilder mStringBuilder = new StringBuilder();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze_sherlock_holmes);

        mAnalyzeButton = findViewById(R.id.analyze);
        mAnalyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enqueueWork();
            }
        });

        mProgressBar = findViewById(R.id.progress);
        mResultsView = findViewById(R.id.results);

        TestDatabase.getInstance(this).getWordCountDao().getWordCounts().observe(
                this,
                new Observer<List<WordCount>>() {
                    @Override
                    public void onChanged(@Nullable List<WordCount> wordCounts) {
                        if (wordCounts == null) {
                            return;
                        }
                        mResultsView.setText(getWordCountsString(wordCounts));
                    }
                });
    }

    private void enqueueWork() {
        WorkManager workManager = WorkManager.getInstance();

        Work textReducingWork = Work.newBuilder(TextReducingWorker.class)
                .withInputMerger(ArrayCreatingInputMerger.class)
                .build();

        workManager
                .enqueue(TextStartupWorker.class)
                .then(TextMappingWorker.create("advs.txt").build(),
                        TextMappingWorker.create("case.txt").build(),
                        TextMappingWorker.create("lstb.txt").build(),
                        TextMappingWorker.create("mems.txt").build(),
                        TextMappingWorker.create("retn.txt").build())
                .then(textReducingWork);

        workManager.getWorkStatus(textReducingWork.getId()).observe(
                this,
                new Observer<Integer>() {
                    @Override
                    public void onChanged(@Nullable Integer status) {
                        boolean loading = (status != null
                                && status != Constants.STATUS_SUCCEEDED
                                && status != Constants.STATUS_FAILED);
                        mProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                        mResultsView.setVisibility(loading ? View.GONE : View.VISIBLE);
                    }
                }
        );
    }

    private String getWordCountsString(List<WordCount> wordCounts) {
        // TODO: not efficient, this should be part of its own LiveData thing.
        mStringBuilder.setLength(0);
        for (WordCount wc : wordCounts) {
            mStringBuilder
                    .append(wc.mWord)
                    .append(" ")
                    .append(wc.mCount)
                    .append("\n");
        }
        return mStringBuilder.toString();
    }
}
